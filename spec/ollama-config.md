---
Type: spec
Component: backend
Priority: high
Status: draft
Related-Issues: []
Estimated-Effort: 4 hours

# Ollama Model Configuration

## Summary
Add support for selecting Ollama models from a configuration file with primary model and fallback models.

## Requirements

### Configuration File Structure
Create `config/ollama.edn` with the following structure:
```edn
{
 :ollama {
   :url "http://localhost:11434/api/generate"
   :embed-url "http://localhost:11434/api/embed"
   :primary-model "qwen3:4b"
   :fallback-models ["llama3.2:1b" "mistral:7b"]
   :timeout-ms 60000
   :retries 1
   :retry-delay-ms 2000
   :keep-alive-enabled true
   :keep-alive-interval-ms 300000}
 :ollama-embed {
   :model "nomic-embed-text"
   :timeout-ms 10000}
}
```

### Fallback Logic
1. When calling Ollama, try the primary model first
2. If the primary model fails, try each fallback model in order
3. Log which model was used for each successful call
4. If all models fail, use existing fallback behavior (return nil/fallback text)

### Files to Modify

#### `backend/src/fantasia/sim/scribes.clj`
- Add `get-ollama-config-with-fallbacks` function to load config with fallback models
- Modify `call-ollama-with-retry!` to accept list of models and try each
- Update `call-ollama!` and `call-ollama-sync!` to use fallback logic
- Update `keep-alive-ping!` to use primary model only

#### `backend/src/fantasia/sim/embeddings.clj`
- Update `get-ollama-config` to read from config file
- Add fallback support for embeddings model

#### `backend/src/fantasia/server.clj`
- Add function to load Ollama config on startup
- Merge loaded config into global state levers

#### New File: `backend/src/fantasia/config.clj`
- Add `load-ollama-config!` function to read `config/ollama.edn`
- Provide default values if config file missing
- Return merged config

## Implementation Plan

### Phase 1: Config Loading
1. Create `backend/config/ollama.edn` with default configuration
2. Create `backend/src/fantasia/config.clj` with config loading utilities
3. Add tests for config loading

### Phase 2: Fallback Logic in Scribes
1. Modify `call-ollama-with-retry!` to try multiple models
2. Update logging to show which model succeeded
3. Test fallback behavior manually

### Phase 3: Server Integration
1. Add config loading to server startup
2. Merge config into global state levers
3. Verify endpoint `/api/ollama/test` uses correct model

## Definition of Done
- [ ] Config file `config/ollama.edn` created with primary and fallback models
- [ ] Config loading utility reads and merges configuration correctly
- [ ] Primary model is tried first, then fallbacks in order
- [ ] Logging shows which model was used for each call
- [ ] All existing tests pass
- [ ] Manual testing confirms fallback behavior works
- [ ] Documentation in `docs/notes` updated

## Notes
- Default primary model: "qwen3:4b"
- Default fallbacks: ["llama3.2:1b" "mistral:7b"]
- Embedding model: "nomic-embed-text" (no fallbacks for now)
- Config file is optional; if missing, use hardcoded defaults
