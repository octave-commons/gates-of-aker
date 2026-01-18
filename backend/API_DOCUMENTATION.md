# Fantasia Backend API Documentation

Generated from Clojure docstrings in `backend/src/`

## Table of Contents

- [fantasia.server](#fantasiaserver)
- [fantasia.dev](#fantesiadev)
  - [fantasia.dev.coverage](#fantesiadevcoverage)
  - [fantasia.dev.watch](#fantesiadevwatch)
- [fantasia.sim](#fantesiasim)
  - [fantasia.sim.agents](#fantesiasimagents)
  - [fantasia.sim.events](#fantesiasimevents)
  - [fantasia.sim.facets](#fantesiasimfacets)
  - [fantasia.sim.hex](#fantesiasimhex)
  - [fantasia.sim.institutions](#fantesiasiminstitutions)
  - [fantasia.sim.jobs](#fantesiasimjobs)
  - [fantasia.sim.myth](#fantesiasimmyth)
  - [fantasia.sim.pathing](#fantesiasimpathing)
  - [fantasia.sim.spatial](#fantesiasimspatial)
  - [fantasia.sim.tick.core](#fantesiasimtickcore)
  - [fantasia.sim.tick.actions](#fantesiasimtickactions)
  - [fantasia.sim.tick.initial](#fantesiasimtickinitial)
  - [fantasia.sim.tick.movement](#fantesiasimtickmovement)
  - [fantasia.sim.tick.trees](#fantesiasimticktrees)
  - [fantasia.sim.events.runtime](#fantesiasimeventsruntime)
  - [fantasia.sim.world](#fantesiasimworld)

---

## fantasia.server

HTTP/WebSocket server exposing simulation endpoints. Main entry point at `-main`.

**Key Functions:**
- `json-resp` - Generate JSON response with CORS headers
- `start-runner!` - Start automatic simulation runner
- `stop-runner!` - Stop automatic simulation runner
- `set-fps!` - Set simulation frame rate
- `handle-ws` - WebSocket connection handler

---

## fantasia.dev

### fantasia.dev.coverage

**Functions:**

#### run
```clojure
(run [request])
(run [])
```
Run Cloverage with repo-specific defaults. Accepts optional overrides map merged into the base opts.

---

### fantasia.dev.watch

**Functions:**

#### watch-server
```clojure
(watch-server [{:keys [paths] :or {paths ["src" "deps.edn"]}}])
```
Watch source files and restart the Fantasia server on change.

---

## fantasia.sim

### fantasia.sim.agents

Agent behavior, interaction, and belief propagation logic.

**Functions:**

#### update-needs
```clojure
(update-needs [world agent])
```
Decay warmth/food/sleep relative to cold snap.

#### choose-packet
```clojure
(choose-packet [world agent])
```
Convert agent state + local context into a broadcast packet.

#### interactions
```clojure
(interactions [agents])
```
Generate adjacent agent pairs for conversation (using hex distance).

#### scaled-edges
```clojure
(scaled-edges [world])
```
Apply lever modifiers to frontier spread edges.

#### score-claim
```clojure
(score-claim [frontier claim {:keys [claim-hint speaker]}])
```
Score a claim based on frontier state, claim hints, and speaker role.

#### pick-claim
```clojure
(pick-claim [event-type frontier {:keys [claim-hint speaker]}])
```
Pick the best claim for an event type based on scoring.

#### recall-and-mentions
```clojure
(recall-and-mentions [old-recall frontier packet speaker])
```
Update listener recall metrics and emit mention/trace metadata.

#### apply-packet-to-listener
```clojure
(apply-packet-to-listener [world listener speaker packet])
```
Apply a packet from speaker→listener, updating frontier/recall.

---

### fantasia.sim.events

Event archetypes (NOT instances). Pure data + a tiny macro DSL.

Event types drive:
- facet signatures used for recall scoring
- claim candidates for attribution battles
- thresholds for when recall becomes a 'mention'

**Event Types:**

#### winter-pyre
"The Winter Pyre"

**Attributes:**
- Kind: `:battle`
- Threshold: `0.78`
- Mention-delta: `0.03`
- Signature: `{:winter 0.8, :cold 0.6, :trees 0.7, :fire 1.0, :smoke 0.35, :enemy-rout 0.7, :awe 0.6, :judgment 0.6, :patron/fire 0.7, :deity/storm 0.35}`
- Claims:
  - `:claim/winter-judgment-flame` (deity: `:patron/fire`, bonus: `0.02`)
  - `:claim/storm-wrath` (deity: `:deity/storm`, bonus: `0.01`)
  - `:rebuttal/natural-chance` (deity: `nil`, bonus: `0.00`)

#### lightning-commander
"Lightning Strikes the Commander"

**Attributes:**
- Kind: `:battle`
- Threshold: `0.76`
- Mention-delta: `0.18`
- Signature: `{:storm 0.8, :lightning 1.0, :battle 0.55, :enemy-leader 1.0, :awe 0.8, :judgment 0.35, :patron/fire 0.25, :deity/storm 0.85}`
- Claims:
  - `:claim/storm-spear` (deity: `:deity/storm`, bonus: `0.03`)
  - `:claim/patron-smiting` (deity: `:patron/fire`, bonus: `0.01`)
  - `:rebuttal/weather-happens` (deity: `nil`, bonus: `0.00`)

---

### fantasia.sim.facets

Facet activation, decay, and propagation logic.

**Functions:**

#### bump-facet
```clojure
(bump-facet [frontier facet delta])
```
Add delta to facet activation in an agent frontier.

#### decay-frontier
```clojure
(decay-frontier [frontier {:keys [decay drop-threshold] :or {decay 0.92 drop-threshold 0.02}}])
```
Decay activations each tick. Keep sparse frontier by dropping tiny activations.

#### seed
```clojure
(seed [frontier facets {:keys [seed-strength] :or {seed-strength 0.28}}])
```
Seed facets directly from a packet bundle.

#### spread-step
```clojure
(spread-step [frontier edges {:keys [spread-gain max-hops] :or {spread-gain 0.55 max-hops 2}}])
```
One hop of spreading along weighted edges. `edges` is a map `{[from to] w}` with `w` in `[0..1]`.

#### event-recall
```clojure
(event-recall [frontier signature {:keys [threshold] :or {threshold 0.70}}])
```
Compute recall activation for an event from a facet signature. `signature: {facet weight}`. Returns a normalized score in `[0,1]`.

---

### fantasia.sim.hex

Hexagonal grid utilities using axial coordinates (q, r).

**Constants:**

#### pointy-dirs
Axial direction vectors for pointy-top hexes (q, r).

**Functions:**

#### add
```clojure
(add [[aq ar] [bq br]])
```
Add two axial coordinates together.

#### neighbors
```clojure
(neighbors [pos])
```
Return the six axial neighbors around `pos`.

#### distance
```clojure
(distance [[aq ar] [bq br]])
```
Hex (axial) distance between points a and b.

#### normalize-bounds
```clojure
(normalize-bounds [input default])
```
Normalize user-supplied bounds into `{:shape :rect ... :origin [0 0]}`. Supported inputs:
- `{:shape :rect :w 128 :h 128}`
- `{:shape :radius :r 10}`
- `{:width 128 :height 128}`, `{:w 128 :h 128}`
- `[w h]`
Returns `{:shape :rect ...}` for now; `:radius` bounds are passed through.

#### in-bounds?
```clojure
(in-bounds? [hex-map pos])
```
True when given position lies within provided map metadata. `hex-map` expects `{:bounds {:shape ...}}` as stored in world state.

#### rand-pos
```clojure
(rand-pos [^java.util.Random rng hex-map])
```
Sample a random axial coordinate inside the provided `hex-map`. Accepts a java.util.Random instance for determinism.

#### rect-width
```clojure
(rect-width [hex-map])
```
Return the width of the bounds if `:rect`, otherwise nil.

#### rect-height
```clojure
(rect-height [hex-map])
```
Return the height of the bounds if `:rect`, otherwise nil.

---

### fantasia.sim.institutions

Institution-based broadcasting and message propagation.

**Functions:**

#### broadcasts
```clojure
(broadcasts [world])
```
Return institution packets that should fire on this tick.

#### apply-broadcast
```clojure
(apply-broadcast [world agents {:keys [institution packet]}])
```
Apply an institution broadcast packet to all agents. Returns `{:agents [...] :mentions [...] :traces [...]}`.

---

### fantasia.sim.jobs

Job system for agent tasks, resource management, and stockpiles.

**Job Types:**
- `:job/eat` (priority 100)
- `:job/sleep` (priority 90)
- `:job/chop-tree` (priority 60)
- `:job/haul` (priority 50)
- `:job/build-wall` (priority 40)

**Functions:**

#### create-job
```clojure
(create-job [job-type target])
```
Create a job with given type and target position.

#### assign-job!
```clojure
(assign-job! [world job agent-id])
```
Assign a job to an agent.

#### claim-next-job!
```clojure
(claim-next-job! [world agent-id])
```
Find and claim the best available job for an agent.

#### auto-assign-jobs!
```clojure
(auto-assign-jobs! [world])
```
Automatically assign jobs to all agents without current jobs.

#### add-item!
```clojure
(add-item! [world pos resource qty])
```
Add resource items to a tile position.

#### consume-items!
```clojure
(consume-items! [world pos resource qty])
```
Consume items from a tile position. Returns `[world' consumed_qty]`.

#### create-stockpile!
```clojure
(create-stockpile! [world pos resource max-qty])
```
Create a new stockpile at position.

#### add-to-stockpile!
```clojure
(add-to-stockpile! [world pos resource qty])
```
Add resources to a stockpile.

#### take-from-stockpile!
```clojure
(take-from-stockpile! [world pos resource qty])
```
Take resources from a stockpile. Returns `[world' taken_qty]`.

#### stockpile-has-space?
```clojure
(stockpile-has-space? [world pos])
```
Check if stockpile has space available.

#### stockpile-space-remaining
```clojure
(stockpile-space-remaining [world pos])
```
Get remaining space in stockpile.

#### find-nearest-stockpile-with-space
```clojure
(find-nearest-stockpile-with-space [world pos resource])
```
Find nearest stockpile with space for given resource.

#### complete-build-wall!
```clojure
(complete-build-wall! [world job])
```
Complete a build-wall job, consuming wood from tile.

#### complete-chop-tree!
```clojure
(complete-chop-tree! [world job])
```
Complete a chop-tree job, producing wood at tile.

#### complete-haul!
```clojure
(complete-haul! [world job agent-id])
```
Complete a haul job, dropping agent inventory at target.

#### complete-eat!
```clojure
(complete-eat! [world job agent-id])
```
Complete an eat job, consuming food and restoring food need.

#### complete-sleep!
```clojure
(complete-sleep! [world agent-id])
```
Complete a sleep job, restoring sleep need.

#### complete-job!
```clojure
(complete-job! [world agent-id])
```
Complete agent's current job.

#### advance-job!
```clojure
(advance-job! [world agent-id delta])
```
Advance job progress by delta amount.

#### get-agent-job
```clojure
(get-agent-job [world agent-id])
```
Get current job for agent.

#### job-complete?
```clojure
(job-complete? [job])
```
Check if job is complete.

#### adjacent-to-job?
```clojure
(adjacent-to-job? [world agent-id])
```
Check if agent is adjacent to their job target.

#### pickup-items!
```clojure
(pickup-items! [world agent-id pos resource qty])
```
Agent picks up items from position.

#### drop-items!
```clojure
(drop-items! [world agent-id])
```
Agent drops all inventory at current position.

#### generate-haul-jobs-for-items!
```clojure
(generate-haul-jobs-for-items! [world threshold])
```
Generate haul jobs for items exceeding threshold quantity.

#### generate-need-jobs!
```clojure
(generate-need-jobs! [world])
```
Generate need-based jobs (eat, sleep) for agents.

#### auto-generate-jobs!
```clojure
(auto-generate-jobs! [world])
```
Auto-generate all types of jobs.

#### clear-jobs!
```clojure
(clear-jobs! [world])
```
Clear all jobs from world.

---

### fantasia.sim.myth

Belief ledger management, decay, and attribution tracking.

**Functions:**

#### decay-ledger
```clojure
(decay-ledger [ledger])
```
Decay buzz/tradition each tick. Keeps belief from becoming permanently stuck.

#### add-mention
```clojure
(add-mention [ledger {:keys [event-type claim weight event-instance]}])
```
Update ledger for (event-type, claim) with optional event-instance grounding.

#### attribution
```clojure
(attribution [ledger event-type])
```
Compute attribution probabilities per event-type from ledger.

---

### fantasia.sim.pathing

Pathfinding algorithms for agent movement.

**Functions:**

#### bfs-path
```clojure
(bfs-path [world start goal])
```
Find shortest path from start to goal using BFS. Returns sequence of positions from start to goal (inclusive). Returns nil if no path exists.

#### a-star-path
```clojure
(a-star-path [world start goal])
```
Find shortest path from start to goal using A* algorithm. Returns sequence of positions from start to goal (inclusive). Returns nil if no path exists.

#### next-step-toward
```clojure
(next-step-toward [world start goal])
```
Get next position on path toward goal. Returns one step along path, or current pos if already there.

#### reachable?
```clojure
(reachable? [world start goal])
```
Check if goal is reachable from start.

---

### fantasia.sim.spatial

Spatial queries and agent movement helpers.

**Functions:**

#### in-bounds?
```clojure
(in-bounds? [world pos])
```
Check whether a coordinate lies inside world bounds (using hex map).

#### manhattan
```clojure
(manhattan [[ax ay] [bx by]])
```
Compute Manhattan distance between two coordinates (legacy helper).

#### neighbors
```clojure
(neighbors [[x y]])
```
Return orthogonal neighbor coordinates for a given position (legacy helper).

#### at-trees?
```clojure
(at-trees? [world pos])
```
True when the world has a tree at the given position.

#### passable?
```clojure
(passable? [world pos])
```
True when a position can be walked through (no wall structures).

#### near-shrine?
```clojure
(near-shrine? [world pos])
```
True when a position is within 3 steps of the world shrine (using hex distance).

#### move-agent
```clojure
(move-agent [world agent])
```
Deterministically move an agent one step among in-bounds, passable hex neighbors.

---

### fantasia.sim.tick.core

Core tick processing pipeline.

**Functions:**

#### process-jobs!
```clojure
(process-jobs! [world])
```
Process jobs for agents: advance progress if adjacent to target.

#### tick-once
```clojure
(tick-once [world])
```
Execute a single simulation tick, returning world and output.

#### tick!
```clojure
(tick! [n])
```
Execute n simulation ticks, updating global state.

---

### fantasia.sim.tick.actions

User-initiated world actions.

**Functions:**

#### assign-build-wall-job!
```clojure
(assign-build-wall-job! [agent-id pos])
```
Manually assign a build-wall job to an agent at a specific position.

#### place-wall-ghost!
```clojure
(place-wall-ghost! [pos])
```
Place a wall ghost marker at position for building.

#### place-stockpile!
```clojure
(place-stockpile! [pos resource max-qty])
```
Place a stockpile at position with given resource type and max quantity.

---

### fantasia.sim.tick.initial

World initialization and initial state creation.

**Functions:**

#### rng
```clojure
(rng [seed])
```
Create a java.util.Random instance from seed.

#### rand-int*
```clojure
(rand-int* [^java.util.Random r n])
```
Get random int from 0 to n-1 using java.util.Random.

#### ->agent
```clojure
(->agent [id q r role])
```
Create an agent with given id, position, and role.

#### initial-world
```clojure
(initial-world [opts])
```
Create initial world state with given options (:seed, :bounds).

---

### fantasia.sim.tick.movement

Agent movement logic with job support.

**Functions:**

#### move-agent-with-job
```clojure
(move-agent-with-job [world agent])
```
Move an agent one step. If agent has job, use pathing toward target. Otherwise, use random movement.

---

### fantasia.sim.tick.trees

Tree spawning, spreading, and fruit dropping.

**Functions:**

#### parse-tile-key
```clojure
(parse-tile-key [k])
```
Parse a tile key string 'q,r' into [q r] coordinate vector.

#### spawn-initial-trees!
```clojure
(spawn-initial-trees! [world])
```
Spawn initial trees randomly throughout map. Approximately 5% of tiles get trees.

#### spread-trees!
```clojure
(spread-trees! [world])
```
Spread trees to adjacent empty tiles. Each tree has a chance to spawn a new tree in an adjacent empty tile every 20-160 ticks.

#### drop-tree-fruits!
```clojure
(drop-tree-fruits! [world])
```
Process fruit dropping for all trees. Each tree drops fruit randomly every 5-20 turns. Fruits accumulate at the tree position in :items map.

---

### fantasia.sim.events.runtime

Runtime event generation and application.

**Functions:**

#### generate
```clojure
(generate [world agents])
(generate [world agents rng])
```
Sample a world event candidate from current world + agent list. Returns nil or an event map with :id/:type/:pos/:witnesses/etc. Optional rng overrides random sampling for deterministic tests.

#### apply-to-witness
```clojure
(apply-to-witness [world agent event-instance])
```
Apply a world event instance to a single agent, returning `{:agent updated-agent :mentions [...] :traces [...]}`.

---

### fantasia.sim.world

World state management and snapshot generation.

**Functions:**

#### snapshot
```clojure
(snapshot [world attribution])
```
Produce a UI-friendly snapshot of world state + attribution map.

#### update-ledger
```clojure
(update-ledger [world mentions])
```
Apply decay + mentions to the ledger and return `{:ledger ledger2 :attr {...}}`.

---

*Generated from Clojure source code documentation*
