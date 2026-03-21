# Fork Tales Story Engine

---
Type: spec
Component: backend, myth, world
Priority: high
Status: proposed
Related-Issues: []
Milestone: 7
Estimated-Effort: 12 hours
---

## Context

The user wants Gates of Aker to act as a never-ending story engine for `devel/vaults/fork_tales/narrative/`.

Desired outcome:
- Gates of Aker should be able to continue the Fork Tales narrative, not just emit short generic myths.
- The existing Fork Tales cast should appear inside the game world as named agents/characters.
- Story generation should use the proxy instance at `https://ussy.promethean.rest` via existing environment variables, not only localhost Ollama.
- Generated chapters should be able to append directly into `devel/vaults/fork_tales/narrative/`.

## Current State

- `fantasia.sim.scribes` can generate short 2-3 sentence book text via local Ollama-style `/api/generate` calls.
- `.myth/myths.jsonl` already acts as a persistent myth memory layer.
- Initial ECS agents are spawned with generic names like `agent-<uuid>`.
- The frontend can already display `agent.name` if present.
- No dedicated API exists for serial chapter generation into the Fork Tales repository.
- No generic OpenAI-compatible client exists in Gates of Aker backend yet.

## Requirements

### Phase 1 — Proxy-backed storyteller client

1. Add a small text-generation client that supports an OpenAI-compatible `/v1/chat/completions` endpoint.
2. Default the Fork Tales storyteller path to the existing proxy env vars:
   - `OPEN_HAX_OPENAI_PROXY_URL`
   - `OPEN_HAX_OPENAI_PROXY_AUTH_TOKEN`
3. Keep existing local Ollama behavior intact for unrelated systems unless explicitly routed through the new storyteller path.
4. Provide a status/readiness function so the API can report whether the storyteller is configured.

### Phase 2 — Fork Tales corpus + chapter generation

1. Read the existing Fork Tales chapters from `devel/vaults/fork_tales/narrative/`.
2. Compute the next chapter number and output filename deterministically.
3. Build a prompt from:
   - recent chapters
   - current world snapshot / recent events
   - Fork Tales roster / style constraints
   - optional operator prompt from request body
4. Generate chapter text long enough to feel like a real continuation, not a 2-sentence myth blurb.
5. Write the generated chapter into the Fork Tales narrative directory.
6. Also append a myth-memory entry into `.myth/myths.jsonl` so the game remembers what it wrote.

### Phase 3 — Put Fork Tales people into the game

1. Spawn the initial ECS roster with Fork Tales names instead of generic UUID-derived labels.
2. Preserve those names in ECS snapshot projection so the web UI displays them.
3. Keep the roster small and stable for the initial slice.
4. Use a named roster aligned with the active narrative cast, prioritizing:
   - Duct
   - Null
   - Patch
   - Sei
   - 莉津律宗利都

### Phase 4 — API surface

1. Add `GET /api/fork-tales/status` returning:
   - configured / not configured
   - narrative directory
   - chapter count
   - latest chapter metadata if available
2. Add `POST /api/fork-tales/continue` to generate the next chapter.
3. Support a request option for `dry_run` so the caller can preview without writing when desired.
4. Return structured JSON with:
   - `ok`
   - `chapter_number`
   - `title`
   - `path`
   - `written`
   - `text`

### Phase 5 — History browser

1. Add `GET /api/fork-tales/history` returning recent chapter metadata and previews.
2. Add `GET /api/fork-tales/history/:chapter-number` returning full chapter text for inspection.
3. Expose chapter history in the Fork Tales UI so operators can browse recent chapters without leaving Gates of Aker.
4. Auto-load the latest chapter detail on first visit so the tool opens on the freshest canon.

## Risks

- The proxy may return provider-specific response shapes or empty `content` for some models; the implementation must choose a model/response parsing path that actually yields text.
- Cross-repo file output is intentional here, but the path must stay configurable and explicit.
- Existing short-form scribe generation should not regress.
- Story prompts can drift stylistically; corpus grounding should use the latest chapters and named roster to reduce nonsense.

## Open Questions

- Should chapter generation be manual-only for now, or eventually tied to night boundaries / scribe jobs automatically?
- Should generated chapters always write to Fork Tales, or should dry-run be the default in UI-facing flows?
- Should later chapters include explicit world-state receipts or remain purely diegetic prose?

## Implementation Plan

### Phase 1
- Extend config access for proxy-backed storyteller defaults.
- Add OpenAI-compatible text generation helper.
- Add unit tests for response parsing and env-backed config.

### Phase 2
- Add Fork Tales chapter loader, next-chapter allocator, prompt builder, and writer.
- Add dry-run + write modes.
- Add unit tests with mocked model responses and temp narrative directories.

### Phase 3
- Extend ECS agent creation to accept explicit names.
- Seed initial world with Fork Tales roster names.
- Ensure agent snapshots include names.
- Add tests proving named agents survive reset + snapshot projection.

### Phase 4
- Add server endpoints for status + continuation.
- Add docs/history note summarizing the new behavior.
- Run targeted backend tests.

## Definition of Done

- A backend API can generate the next Fork Tales chapter from Gates of Aker state.
- Generated chapters can be written into `devel/vaults/fork_tales/narrative/`.
- Story generation uses the proxy at `ussy.promethean.rest` via env-backed auth.
- Initial world agents appear with Fork Tales names in snapshots/UI.
- `.myth/myths.jsonl` receives a memory entry for each written chapter.
- Relevant backend tests pass.
