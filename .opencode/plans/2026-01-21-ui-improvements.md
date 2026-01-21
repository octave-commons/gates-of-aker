# UI/UX Improvement Plan

## Overview
Analysis of current frontend (web/) vs backend data structures to identify missing information, UX gaps, and space optimization opportunities.

## Current Frontend Panels

### Existing Display Components
- **StatusBar**: Connection status, tick health (web/src/components/StatusBar.tsx:1-51)
- **WorldInfoPanel**: Calendar, time-of-day, temperature, daylight, cold snap (web/src/components/WorldInfoPanel.tsx:1-66)
- **ResourceTotalsPanel**: Stockpile resource totals (web/src/components/ResourceTotalsPanel.tsx:1-33)
- **TickControls**: Tick controls, play/pause, FPS slider
- **SelectedPanel**: Tile info, items, agents, selected agent details (web/src/components/SelectedPanel.tsx:1-188)
- **FactionsPanel**: Colonists and wildlife with AgentCards (web/src/components/FactionsPanel.tsx:1-210)
- **JobQueuePanel**: Active/assigned/unassigned jobs with progress (web/src/components/JobQueuePanel.tsx:1-212)
- **ThoughtsPanel**: Agent needs, thoughts, urgency levels (web/src/components/ThoughtsPanel.tsx:1-243)
- **TraceFeed**: Communication traces with spread/mention data (web/src/components/TraceFeed.tsx:1-94)
- **BuildingPalette**: Build controls
- **AgentCard**: Individual agent with needs, inventory, facets, relationships (web/src/components/AgentCard.tsx:1-283)

## Missing Backend Data Not Displayed

### 1. Myth/Attribution System (backend/src/fantasia/sim/myth.clj)
**Missing UI**: No AttributionPanel or MythVisualizationPanel

Backend provides via snapshot:
- `:ledger` - Map of `[event-type, claim] -> {:buzz, :tradition, :mentions, :event-instances}`
- Attribution tracking per event type with claim probabilities
- Event instances with grounding to specific world events

**Why important**: This is core to the game's storytelling and belief propagation. Seeing which deities/events are becoming attributed to phenomena is crucial for player agency and understanding emergent narratives.

### 2. Memories System (backend/src/fantasia/sim/memories.clj)
**Missing UI**: No MemoriesPanel or MemoryVisualization

Backend provides:
- `:memories` - Map of memory-id -> `{:id, :type, :location, :created-at, :strength, :decay-rate, :entity-id, :facets}`
- Memory queries by range and strength threshold
- Memory decay and cleanup system

**Why important**: Memories form the basis of agent perception and decision-making. Visualizing what agents "remember" about the world provides insight into their behavior and emergent stories.

### 3. House Management (backend/src/fantasia/sim/houses.clj)
**Missing UI**: HousePanel or expanded SelectedPanel house details

Backend provides:
- `:bed-capacity` per house tile (default 2)
- `:occupied-beds` - Current occupancy
- `:residents` - List of agent IDs assigned to this house

**Why important**: Housing is a critical resource. Players need to see bed availability, which agents are housed where, and manage housing allocation effectively.

### 4. Agent Stats & Thresholds
**Partially displayed**: AgentCard shows needs but not full context

Backend provides via snapshot:
- `:stats` - Agent stat block (not shown anywhere)
- `:need-thresholds` - Per agent thresholds like `:food-hungry`, `:water-thirsty`, etc. (used in App.tsx:31-40 for audio but not displayed)
- `:idle?` - Agent idle status (not prominently displayed)
- `:inventories` vs `:inventory` - Multiple inventory tracking

**Why important**: Understanding why agents make specific decisions requires seeing their full stat profile and thresholds. Idle status helps identify workforce issues.

### 5. Campfire System
**Missing UI**: No campfire information display

Backend tracks:
- `:campfire` position in world state
- `campfire-radius` constant (backend/src/fantasia/sim/agents.clj:19-20)
- Warmth bonus from campfire proximity

**Why important**: Campfires are critical warmth sources. Players need to see campfire range, which agents are in range, and warmth benefits.

### 6. Recent Events & Event Feed
**Missing UI**: No EventFeedPanel (TraceFeed shows communication, not world events)

Backend provides via snapshot:
- `:recent-events` - List of recent world event instances

**Why important**: World events drive narrative. Players need to see what's happening: battles, discoveries, disasters, etc., separate from agent-to-agent communication traces.

### 7. Spatial Facet System (backend/src/fantasia/sim/spatial_facets.clj)
**Missing UI**: No spatial facet visualization or perception overlay

Backend provides:
- Embedding cache for semantic similarity
- Entity facets for M3 entity types
- Query function `query-concept-axis!` with scores in [-1, 1]
- Vision radius (default 10 tiles) for facet gathering

**Why important**: This is how agents "see" the world. A perception overlay showing what concepts are present in each area would be invaluable for debugging and player understanding.

### 8. Belief/Frontier Detail
**Partially displayed**: Top facets shown in AgentCard, but not full frontier state

Backend provides per agent:
- `:frontier` - Full semantic activation map `{facet -> {:a activation}}`
- Used for belief propagation, event recall, claim scoring

**Why important**: The frontier is an agent's working model of the world. Seeing the full activation state helps debug why agents make specific decisions.

### 9. Institutions & Broadcasts (backend/src/fantasia/sim/institutions.clj)
**Missing UI**: No InstitutionPanel

Backend provides:
- Institution-based broadcasting
- Broadcast packets that fire on each tick

**Why important**: Institutions shape collective belief. Seeing institutional messaging helps understand how doctrines spread through the colony.

### 10. Levers Configuration
**Missing UI**: No LeverControlsPanel (only world size, tree density shown)

Backend provides:
- `:levers` map in world state
- `:iconography` - Fire->Patron, Lightning->Storm, Storm->Deity modifiers
- Various simulation levers for tweaking game balance

**Why important**: Levers are the player's "knobs" for tuning the world. A dedicated panel would expose these for debugging and potential game mechanics.

## Information Experience Improvements

### High Priority UX Improvements

#### 1. Attribution/Myth Panel
**Goal**: Make belief propagation visible and manipulable
**Display**:
- Attribution probabilities per event-type (e.g., "Storm Deity 85%, Fire Patron 15%")
- Recent mentions with event instances grounded to world events
- Buzz vs Tradition bars showing momentum
- Interactive claim highlighting
- Time-series graph of attribution changes

**Location**: Third column or tabbed panel

#### 2. Memory Visualization
**Goal**: Show what agents "know" about the world
**Display**:
- Map overlay with memory heatmaps (stronger memories = more intense color)
- Filter by memory type, time range, strength
- Click to see memory details (facets, creator, decay rate)
- Agent-specific memory view when agent selected

**Location**: Toggle overlay on SimulationCanvas

#### 3. Event Timeline Feed
**Goal**: Chronicle world events for narrative understanding
**Display**:
- Scrolling timeline of world events (not just communication traces)
- Event type icons with severity indicators
- Expandable event details with witnesses, effects
- Filtering by event type (battle, discovery, miracle)
- Link to attribution claims

**Location**: Below WorldInfoPanel or as collapsible section

#### 4. House Management Dashboard
**Goal**: Manage housing resources effectively
**Display**:
- List of all houses with: capacity, occupancy, residents list
- Color coding: Green (empty beds), Yellow (full), Red (overcrowded if applicable)
- Click to focus house on map
- Click resident to jump to agent details
- "Find empty bed for agent" action button

**Location**: New tab in FactionsPanel or separate HousePanel

#### 5. Need-Based Alert System
**Goal**: Surface urgent agent needs proactively
**Display**:
- Priority queue of agents with critical needs (< 0.3 threshold)
- Flashing indicators when needs cross threshold
- One-click actions: "Feed all hungry", "Warm all cold"
- Historical need trend lines per agent

**Location**: Top alert bar or expanded WorldInfoPanel

#### 6. Agent Relationship View
**Goal**: Visualize social connections and family trees
**Display**:
- Network graph showing family relationships (parents, children)
- Social connections based on conversation frequency
- Click node to center on agent
- Toggle between family and communication graphs

**Location**: In SelectedPanel when agent selected, or standalone modal

#### 7. Inventory Management
**Goal**: Track resources across agents and world
**Display**:
- Total inventory breakdown by resource type across all agents
- Per-agent inventory list sortable by role, position
- "Gather all X" commands
- Stockpile vs inventory breakdown

**Location**: Below ResourceTotalsPanel

#### 8. Enhanced Trace Communication
**Goal**: Make traces readable and actionable
**Improvements**:
- Collapse verbose technical details by default
- Show natural language summary ("Agent #3 warned Agent #5 about cold weather")
- Filter by intent type (warn, convert, chatter)
- Threaded conversation view (see follow-up messages)
- Impact indicators (did this change beliefs?)

**Current**: TraceFeed (web/src/components/TraceFeed.tsx:1-94) shows raw spread deltas

#### 9. Perception Overlay
**Goal**: Visualize agent perception
**Display**:
- Toggle overlay on map showing vision radii
- Color-coded by agent type/role
- Hover to see top facets in that area
- "What can agent X see from here?" mode

**Location**: Canvas toggle buttons

#### 10. Quick Actions Toolbar
**Goal**: Reduce clicks for common tasks
**Display**:
- Floating toolbar with: Tick, Pause, Reset, Save, Load
- Context-aware: selected agent shows "Feed", "Sleep", "Warm" buttons
- Keyboard shortcuts displayed on hover

**Location**: Fixed top bar or floating near controls

### Medium Priority UX Improvements

- **Minimap**: Small map overview with world bounds, agent icons, resource clusters
- **Agent Search**: Quick filter by name, role, need level, position
- **Bookmark Positions**: Save map positions (Campfire, Stockpile #1, etc.) for quick navigation
- **Tooltips**: Hover over any value to see formula, last tick change, historical average
- **Data Export**: Export world state, agent list, ledger to JSON/CSV for analysis
- **Replay Controls**: Rewind simulation to specific tick, play backward
- **Snapshot Comparison**: Diff between two world states
- **Agent Inspector Modal**: Full screen view of single agent with all data
- **Bulk Agent Commands**: Select multiple agents and issue group orders
- **Custom Alerts**: Set thresholds for conditions (e.g., "Alert when population > 20")

## Space Optimization Strategy

### 1. Aggressive Use of Tabs
**Current**: All panels visible at once (2-column layout: 1fr 320px 320px in App.tsx:605)

**Proposed**: Tab system with context-aware visibility

**Tabs for Middle Column**:
- World (default): Calendar, tick info, alerts
- Resources: Stockpiles, inventory totals
- Jobs: Job queue with filters

**Tabs for Right Column**:
- Agents (default): Factions panel with search
- Selected: Agent/Tile details when something selected
- Myth: Attribution, memories, events
- Traces: Communication logs

**Benefit**: Reduces vertical scrolling by 60-70%, allows each panel to be larger

### 2. Compact Metrics Display
**Current**: Labels with numbers side-by-side

**Proposed**:
- Sparkline mini-charts for trends (last 10-20 ticks)
- Progress bars for 0-1 values (needs, attribution probabilities)
- Color-coded badges (green/yellow/red) with numeric value on hover
- Icon-only mode toggle (show icons, hide text until hover)

**Example**:
```typescript
// Current (WorldInfoPanel.tsx:50-61):
<div>Temperature</div><span>{temperature}</span>

// Compact:
<span title="Temperature: 0.45">🌡️ 0.45</span>
// or with sparkline:
<span title="Temperature: 0.45 (trend: +0.02)">🌡️ 0.45 ▲</span>
```

### 3. Collapsible Sections with Smart Defaults
**Current**: Some collapsible (ThoughtsPanel, Traces, Jobs), others always expanded

**Proposed**:
- All panels collapsible
- Smart defaults: Expand when data changes, collapse after inactivity
- "Collapse All" / "Expand All" buttons
- Remember user's collapsed state per panel

### 4. Context-Sensitive Panels
**Current**: All panels always visible

**Proposed**:
- Hide FactionsPanel when viewing detailed agent info
- Auto-show SelectedPanel when clicking on tile/agent
- Replace WorldInfoPanel with HousePanel when house selected
- Hide TraceFeed when not debugging

### 5. Tooltips for Detailed Information
**Current**: SelectedPanel shows all details (188 lines)

**Proposed**:
- Show only key info by default: ID, role, position, critical needs
- Hover needs bar to see full detail (threshold, rate of change)
- Hover role to see role-specific stats
- Click to expand full panel

### 6. Icon-Based Indicators
**Current**: Text labels everywhere

**Proposed**:
- Status icons: 💤 sleep, ⚠️ warning, ✓ healthy, ✗ dead
- Need icons: 🍎 food, 🔥 warmth, 😴 sleep, 😊 mood
- Job icons: 🪓 chop, 🏗️ build, 📦 haul
- Structure icons: 🏠 house, 🏛️ shrine, 🧱 wall, 🔥 campfire

**Benefit**: 60-70% space reduction for status indicators

### 7. Dense Grid Layouts
**Current**: Vertical lists in AgentCard, JobQueuePanel

**Proposed**:
- 2-column grid for agent cards (halves height)
- Compact job cards with inline progress bars
- Resource grid instead of list
- Table view for sortable columns

### 8. Right-Click Context Menus
**Current**: Click to select, then look at panels

**Proposed**:
- Right-click agent: Feed, Sleep, Warm, Inspect, Dismiss
- Right-click tile: Build, Inspect, Set as Rally Point
- Right-click job: Cancel, Prioritize, Assign Agent

**Benefit**: Eliminates need to find controls in separate panels

### 9. Modal Dialogs for Complex Views
**Current**: Agent details shown in panel (SelectedPanel:128-181)

**Proposed**:
- Modal window for full agent inspector
- Modal for attribution graph visualization
- Modal for memory map visualization
- Modal for event timeline

**Benefit**: Main UI stays clean, complex views on-demand

### 10. Responsive Column Widths
**Current**: Fixed 320px for both side columns (App.tsx:605)

**Proposed**:
- Draggable column dividers
- Minimum widths to prevent collapse
- "Focus Mode" hides left panel, expands right panel
- "Compact Mode" shrinks all panels to 240px minimum

### 11. Progressive Disclosure
**Current**: All information shown at once

**Proposed**:
- Level 1: Summary (icons + critical values)
- Level 2: Details (hover/click shows full data)
- Level 3: History (trend lines, delta from last tick)
- Level 4: Raw data (JSON viewer for debugging)

### 12. Keyboard Navigation
**Current**: Only Space for pause/play (App.tsx:281-294)

**Proposed**:
- `Space`: Toggle play/pause
- `Arrow keys`: Pan camera
- `+/-`: Zoom in/out
- `N`: Next tick
- `R`: Reset simulation
- `Tab`: Cycle through agents
- `Escape`: Clear selection
- `1-4`: Switch tabs
- `F`: Search/filter agents

## Missing Features

### Phase 1: Core Simulation Features (Immediate)

1. **Day/Night Cycle Visual Indicator**
   - Current: Only text in WorldInfoPanel
   - Needed: Visual representation (sun/moon icon, background color shift)
   - Backend support: `:calendar` has `:time-of-day`, `:daylight` (backend/src/fantasia/sim/world.clj)

2. **Campfire Range Visualization**
   - Current: No visualization
   - Needed: Show campfire heat radius on map, affected agents
   - Backend support: `:campfire` position, warmth bonus calculation (backend/src/fantasia/sim/agents.clj:19-31)

3. **House Management UI**
   - Current: Only shows "structure: house" in SelectedPanel
   - Needed: Bed capacity, occupancy, resident list, assign/unassign
   - Backend support: Full house system (backend/src/fantasia/sim/houses.clj)

4. **Event Log/Feed**
   - Current: Only communication traces (TraceFeed)
   - Needed: World events (battles, discoveries, miracles)
   - Backend support: `:recent-events` in snapshot (backend/src/fantasia/sim/world.clj:17)

### Phase 2: Myth & Narrative Features

5. **Attribution Panel**
   - Current: No UI for myth/ledger system
   - Needed: Show event-to-deity attribution, buzz/tradition metrics
   - Backend support: `:ledger` in snapshot (backend/src/fantasia/sim/world.clj:18-23)

6. **Memory Visualization**
   - Current: No memory UI
   - Needed: View agent memories, memory heatmap overlay
   - Backend support: `:memories` system (backend/src/fantasia/sim/memories.clj)

7. **Institution Panel**
   - Current: No institution UI
   - Needed: Show active institutions, broadcast messages
   - Backend support: Institution broadcasts (backend/src/fantasia/sim/institutions.clj:78-88)

### Phase 3: Divine & Faction Features

8. **Divine Resources Display**
   - Current: No faith/favor/wrath display
   - Needed: Show divine currencies, spending costs
   - Spec: spec/2026-01-15-core-loop.md:27-31 mentions faith/favor/wrath

9. **Miracle/Divine Intervention UI**
   - Current: No deity ability interface
   - Needed: Invoke deity moves, see costs and effects
   - Spec: spec/2026-01-15-core-loop.md:70-75 describes major deity moves

10. **Faction Relations Overview**
    - Current: Only shows player vs wildlife
    - Needed: Diplomacy panel, war/peace status, trade relations
    - Spec: spec/2026-01-15-core-loop.md mentions neighboring faction with intent-driven raid

### Phase 4: Advanced Gameplay Features

11. **Champion Control Interface**
    - Current: No champion mechanics
    - Needed: Day mode controls, champion selection, action points
    - Spec: spec/2026-01-15-core-loop.md:18-23 describes day mode

12. **Prestige/Token System Display**
    - Current: No prestige UI
    - Needed: Show prestige tokens, retirement mechanics
    - Spec: spec/2026-01-15-core-loop.md:64-68 describes prestige loop

13. **Ritual System UI**
    - Current: No ritual interface
    - Needed: Trigger rituals, see divine favor gain
    - Backend support: `:divine-favor` from rituals (backend/src/fantasia/sim/social.clj:10)

14. **Sleep Safety Mechanics**
    - Current: No sleep prep or alarm UI
    - Needed: Night mode entry conditions, wake triggers
    - Spec: spec/2026-01-15-core-loop.md:25-31 describes sleep safety requirements

15. **Communication Channel Visualization**
    - Current: Only raw traces
    - Needed: Show communication network, channel reliability, interception risk
    - Spec: spec/2026-01-15-core-loop.md:55-57 describes channel types

### Phase 5: Polish & Debug Features

16. **Minimap**
    - Needed: Overview of world, zoom-to-location, bookmarks

17. **Agent Search & Filter**
    - Needed: Quick find, filter by need level/role, bulk select

18. **Data Export Tools**
    - Needed: Export world state, ledger, agent list to JSON/CSV

19. **Replay System**
    - Needed: Rewind to previous tick, playback controls

20. **Custom Alerts/Notifications**
    - Needed: User-defined thresholds (e.g., "Alert when population > 20")

## Implementation Priority

### Phase 1 (Immediate - 1-2 weeks)
1. Space optimization: Tab system for panels
2. Compact metrics with progress bars
3. House management UI
4. Campfire range visualization
5. Need-based alert system

### Phase 2 (Short-term - 2-4 weeks)
1. Attribution/Myth panel
2. Event timeline feed
3. Agent relationship view
4. Tooltips for detailed info
5. Right-click context menus

### Phase 3 (Medium-term - 1-2 months)
1. Memory visualization overlay
2. Inventory management dashboard
3. Divine resources display
4. Champion control interface
5. Minimap

### Phase 4 (Long-term - 2-3 months)
1. Miracle/divine intervention UI
2. Faction relations overview
3. Ritual system UI
4. Prestige/token system display
5. Replay system

## Definition of Done

For each feature:
- [ ] Component implemented with TypeScript types
- [ ] Integration with WebSocket messages (op types)
- [ ] Tests covering main interactions
- [ ] Accessible keyboard navigation
- [ ] Responsive design (works on reasonable screen sizes)
- [ ] Performance acceptable (60fps with reasonable data sizes)
- [ ] Documentation updated in this spec with code references
- [ ] Screenshot/video of feature in action included

## References

- Frontend main layout: web/src/App.tsx:602-791
- Panel components: web/src/components/
- Backend snapshot structure: backend/src/fantasia/sim/world.clj:12-27
- Myth/ledger system: backend/src/fantasia/sim/myth.clj:1-41
- Memories system: backend/src/fantasia/sim/memories.clj:1-75
- Houses system: backend/src/fantasia/sim/houses.clj:1-89
- Agents: backend/src/fantasia/sim/agents.clj:1-178
- Core loop spec: spec/2026-01-15-core-loop.md
- AGENTS.md: /home/err/devel/orgs/octave-commons/gates-of-aker/AGENTS.md
