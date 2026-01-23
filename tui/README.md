# Fantasia TUI Client

A Babashka-based terminal UI client for Fantasia simulation backend.

## Setup

The TUI client uses Babashka's built-in `babashka.http-client` library - no external dependencies required.

Babashka is already available in the project.

## Usage

### Start the backend first:
```bash
cd backend
clojure -M:server
```

### In another terminal, run one of the TUIs:

#### Simple TUI (Basic Controls):
```bash
# From project root
./bin/fantasia-tui

# Or directly
bb tui/fantasia_tui.clj
```

#### Advanced CLI (Ultimate Debugging Tool):
```bash
# From project root
bb tui/fantasia_cli.clj
```

## Simple TUI Controls

- **q**: Quit the TUI
- **r**: Reset simulation
- **Space**: Start/Pause simulation
- **t**: Tick simulation one step
- **s**: Refresh status (get world state)

## Advanced CLI Commands

### Navigation
```
view <grid|agents|events|state|tile>  - Switch view mode
tile <q,r>                             - Show tile info at position
```

### Simulation Control
```
tick [n]   - Advance simulation (default 1)
start      - Start continuous simulation
pause      - Pause simulation
reset      - Reset simulation
```

### Information Views
```
agents  - Show agents list
events  - Show events
state   - Show simulation state
```

### Grid View Navigation
```
hjkl    - Move cursor (vim-style)
+/-     - Zoom in/out
```

### Utility
```
help   - Show command help
quit   - Exit CLI
```

## Display

### Simple TUI shows:
- Header with tick count, running status, and connection status
- List of up to 10 agents with their names, roles, positions, jobs, food, and health
- Recent events and trace messages

### Advanced CLI shows:
- Multiple view modes (grid, agents, events, state, tile)
- Hex grid view with cursor navigation
- Tile inspection at specific coordinates
- Agent information with needs tracking
- Event traces and last event
- Simulation state summary

## Features

- HTTP API connection to backend (localhost:3000)
- Real-time state updates via /sim/state endpoint
- Simulation control via /sim/reset, /sim/tick, /sim/run, /sim/pause endpoints
- Agent information display with needs tracking
- Clear screen terminal UI with ANSI codes
- Hex grid navigation with zoom controls

## Testing

Run the test suite to verify connectivity:
```bash
# Simple TUI tests
bb tui/test_tui.bb

# CLI API tests
bb tui/test_cli.bb
```

## Implementation Details

### Backend API Endpoints Used
- `GET /healthz` - Check backend connectivity
- `GET /sim/state` - Get current world state
- `POST /sim/reset` - Reset simulation
- `POST /sim/tick` - Advance simulation by N ticks
- `POST /sim/run` - Start continuous simulation
- `POST /sim/pause` - Pause continuous simulation

### Agent Display Format
Shows: `Name (Role) @ [x y] | job: <id-or-idle> | food: <value> | health: <value>`

### Architecture
- Pure Clojure with Babashka for script execution
- HTTP client built into Babashka (no extra dependencies)
- ANSI escape codes for terminal control (clear screen, cursor positioning)
- State management with atoms for reactive UI updates
- Hex grid rendering with cursor-based navigation
