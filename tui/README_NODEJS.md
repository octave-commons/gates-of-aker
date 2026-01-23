# Fantasia TUI (Node.js)

A Node.js/TypeScript terminal UI for Fantasia simulation, an alternative implementation to the Babashka-based TUI.

## Why This Implementation?

This TUI provides:
- Full TypeScript type safety
- WebSocket-based real-time updates (vs HTTP polling in Babashka version)
- Richer UI with blessed library components
- Three-panel layout (map, selection, agents)
- Interactive map navigation and selection

This complements the existing Babashka TUI (`fantasia_tui.clj`) by offering a more feature-rich experience with WebSocket support.

## Setup

1. Install dependencies:
   ```bash
   cd tui
   npm install
   ```

2. Make sure backend is running:
   ```bash
   cd ../backend
   clojure -M:server
   ```

3. Start the Node.js TUI:
   ```bash
   npm run dev
   ```

Or run the built version:
   ```bash
   npm run build
   npm start
   ```

## Configuration

Set backend origin via environment variable:
```bash
BACKEND_ORIGIN=http://localhost:3000 npm run dev
```

Default is `http://localhost:3000`.

## Keyboard Shortcuts

- **Q / Ctrl+C**: Quit application
- **T**: Tick forward one step (only when paused)
- **R**: Toggle run/pause
- **Shift+R / Ctrl+R**: Reset world with new seed
- **Arrow Keys**: Navigate map
- **Enter**: Select agent at current cell
- **Escape**: Clear selection

## UI Layout

The screen is divided into three columns:

1. **Left (60%)**: World map showing tiles, agents, and structures
   - `A`: Agent (number shows multiple agents)
   - `▲`: Shrine/Campfire
   - `#`: Wall
   - `H`: House
   - `"`: Grass
   - `,`: Dirt
   - `~`: Water
   - Selected cells highlighted in blue

2. **Middle (20%)**: Selection panel
   - Shows details for selected cell or agent
   - Agent details: name, role, faction, position, needs, stats
   - Cell details: terrain, structure, agents at location

3. **Right (20%)**: Agent list
   - All agents in the world
   - Shows ID, name, role, and position
   - Color-coded by alive status (green=alive, red=dead)

## Status Bar (Bottom)

Shows:
- Connection status (CONNECTED/DISCONNECTED/ERROR)
- Current tick number
- Runner state (RUNNING/PAUSED)
- Available keyboard shortcuts

## Architecture

- `src/index.ts`: Entry point
- `src/app.ts`: Main application controller and keybindings
- `src/ws-client.ts`: WebSocket client for backend communication
- `src/types.ts`: TypeScript type definitions
- `src/ui/`: UI components
  - `map-view.ts`: Hex map renderer with cell display
  - `panels/`: Panel components
    - `status-bar.ts`: Bottom status bar
    - `info-panel.ts`: Selection details panel
    - `agent-list.ts`: List of all agents

## WebSocket Integration

This TUI connects to the same WebSocket endpoint as the web frontend (`/ws`), receiving real-time updates instead of polling HTTP endpoints:

**Messages Handled:**
- `hello`: Initial world state
- `tick`: Full snapshot after tick
- `tick_delta`: Incremental updates (TODO: proper delta handling)
- `reset`: World reset
- `runner_state`: Running state
- `error`: Backend errors

**Messages Sent:**
- `{ op: "start_run" }`
- `{ op: "stop_run" }`
- `{ op: "tick", n: 1 }`
- `{ op: "reset", seed: <seed> }`

## Development

Run in watch mode with hot reload:
```bash
npm run dev
```

Build for production:
```bash
npm run build
```

## Comparison with Babashka TUI

| Feature | Babashka TUI | Node.js TUI |
|---------|--------------|------------|
| Language | Clojure/Babashka | TypeScript/Node.js |
| Connection | HTTP polling | WebSocket |
| UI Updates | Manual refresh | Real-time |
| UI Library | ANSI codes | blessed |
| Map View | ASCII hex grid | Colorized ASCII with highlighting |
| Panels | Single panel | Three-panel layout |
| Selection | None | Cell and agent selection |
| Dependencies | Babashka only | npm packages |

## Future Enhancements

- [ ] Proper tick_delta handling for incremental updates
- [ ] Build controls and building palette
- [ ] Job queue panel
- [ ] Faction panel
- [ ] Visibility/fog of war overlay
- [ ] Memory overlay
- [ ] Zoom and scroll controls
- [ ] Mouse interaction support
- [ ] Agent needs visualization on map
- [ ] Path visualization for agent movement
