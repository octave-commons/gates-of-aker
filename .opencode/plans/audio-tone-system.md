# Spec: Tone-Based Sound Effects System

## Prompt
"Add simple tone based sound effects to the frontend, loosely tie color to tones" — implement a minimal Web Audio API system that maps entity colors and events to synthesized tones, enhancing simulation's feedback without external assets.

## Key Code References
- `web/src/App.tsx` (line 63-74) — Keyboard event handling for spacebar toggling
- `web/src/App.tsx` (line 82-146) — WebSocket message handlers for tick/trace/event/runner_state
- `web/src/components/SimulationCanvas.tsx` (line 183-188, 217-227) — Color definitions for biomes and resources
- `web/src/components/SimulationCanvas.tsx` (line 346-355) — Role-based agent colors (priest, knight)
- `web/src/components/TickControls.tsx` (line 24-26) — Play/pause button with spacebar shortcut
- `web/setupTests.ts` — Test setup patterns for mocking browser APIs
- `backend/src/fantasia/sim/events/runtime.clj` (line 32-48) — Event types (winter-pyre, lightning-commander)

## Existing Issues / PRs
- None related to audio system (verified via `gh issue list` / `gh pr list`)

## Definition of Done
1. Web Audio API module created in `web/src/audio.ts` with tone generation utilities
2. Color-to-frequency mapping system implemented using hex color analysis
3. Sound effects triggered for key interactions:
   - Tick updates (short, neutral tone)
   - Agent selection (tone based on agent role color)
   - Event arrival (tone based on event type/witness impact)
   - Play/pause toggle (distinct start/stop tones)
   - Lever slider adjustments (continuous tone sweep during drag)
4. Mute toggle added to UI for user control
5. Tests verify tone generation and color-to-frequency mapping
6. `npm run test` passes with new audio tests
7. No external audio dependencies added (pure Web Audio API)

## Requirements & Notes

### Core Principles
- **Keep it minimal**: No external audio files or libraries. Use Web Audio API OscillatorNode for all sounds.
- **Performance**: Reuse AudioContext and limit concurrent oscillators to prevent audio spam.
- **User control**: Always provide a mute option. Respect browser autoplay policies (require user gesture first).
- **Loose color-to-tone mapping**: Map color hue to frequency, not exact pitch matching. Allow for musical ambiguity.

### Color-to-Frequency Mapping Strategy
Use hex color parsing to extract hue from RGB values, then map to a pentatonic scale for pleasant tones:

| Color Hue Range | Frequency (Hz) | Musical Note | Example Entities |
|---------------|----------------|--------------|-----------------|
| 0° (Red) | 261.63 | C4 | Priest agents, shrine |
| 60° (Yellow) | 293.66 | D4 | Grain resources |
| 120° (Green) | 329.63 | E4 | Forest biomes, trees |
| 180° (Cyan) | 392.00 | G4 | Knight agents |
| 240° (Blue) | 440.00 | A4 | Cold/winter events |
| 300° (Magenta) | 523.25 | C5 | High-impact events |

For colors outside pentatonic scale, quantize to nearest scale tone or use microtones for more complex sounds.

### Audio Events to Implement

#### High Priority (Core Feedback)
1. **Tick Update** - Short, neutral tone (approx. 440 Hz A4) when simulation advances
2. **Agent Selection** - Tone based on agent's role color when clicked
3. **Play/Pause** - Distinct tones for run state change (ascending for start, descending for pause)

#### Medium Priority (Event Feedback)
4. **World Events** - Tone based on event type:
   - `winter-pyre` → Warm, lower tone (200-300 Hz)
   - `lightning-commander` → Sharp, higher tone (600-800 Hz)
5. **Lever Adjustments** - Continuous tone sweep while dragging sliders, stops on release

#### Low Priority (Polish)
6. **Tile Selection** - Subtle tone when clicking empty hex
7. **Job Assignment** - Confirmatory tone when job successfully assigned

### Implementation Phases

#### Phase 1: Audio Engine Foundation
- Create `web/src/audio.ts` with singleton AudioContext manager
- Implement `hexToFrequency()` function for color-to-tone mapping
- Implement `playTone()` with envelope (attack, decay) for pleasant sounds
- Add tests for color-to-frequency mapping

#### Phase 2: Core Interaction Sounds
- Integrate audio with TickControls (play/pause)
- Integrate audio with SimulationCanvas (agent/tile selection)
- Add mute toggle to UI (initially in TickControls or StatusBar)
- Add tests mocking AudioContext

#### Phase 3: Event Feedback
- Connect WebSocket "event" messages to audio
- Map event types to tones
- Connect "runner_state" to play/pause sounds
- Add tests for event-to-audio routing

#### Phase 4: Polish
- Add lever sweep tones
- Refine envelope parameters (attack/decay/sustain)
- Document frequency mappings in comments
- Coverage reporting for audio module

### Technical Constraints

- **Audio Context Lifecycle**: Browsers suspend AudioContext until user interaction. Initialize on first click/key interaction.
- **Oscillator Cleanup**: Always call `oscillator.stop()` and `oscillator.disconnect()` after playback to prevent memory leaks.
- **Volume Control**: Use GainNode for master volume (0-1) to implement mute and prevent clipping.
- **Frequency Range**: Keep tones between 200-800 Hz for comfortable listening. Avoid sub-100 Hz (too muddy) or >1000 Hz (too harsh).
- **Polyphony**: Limit to max 3 concurrent tones to prevent cacophony during rapid events.

### Testing Strategy

1. **Unit Tests**:
   - `hexToFrequency()` verifies correct hue extraction and frequency mapping
   - `playTone()` tests that oscillator is created, started, and cleaned up
   - Mock `AudioContext` in `setupTests.ts` similar to canvas mock pattern

2. **Integration Tests**:
   - TickControls triggers correct audio on play/pause button click
   - Agent selection plays tone matching agent role color

3. **Manual Verification**:
   - Mute toggle successfully silences all sounds
   - Rapid ticking does not cause audio glitches or clipping
   - Browser autoplay policies handled gracefully

### Dependencies
No new npm packages. Web Audio API is built into all modern browsers. Type definitions are in `lib.dom.d.ts`.

### Browser Compatibility
- Chrome 10+, Firefox 25+, Safari 6+, Edge 12+ all support Web Audio API
- No polyfills needed—target modern browsers only per AGENTS.md

### File Structure
```
web/src/
  audio.ts                    # Audio engine (new file)
  components/
    TickControls.tsx          # Add mute button
    SimulationCanvas.tsx      # Add audio triggers
  components/__tests__/
    audio.test.ts             # Audio engine tests (new file)
  setupTests.ts              # Add AudioContext mocks
```

### Future Enhancements (Out of Scope for Initial Implementation)
- Spatial audio (pan sounds left/right based on screen position)
- Envelope customization per event type
- Chord generation for complex events
- User-configurable scales (pentatonic, chromatic, etc.)
