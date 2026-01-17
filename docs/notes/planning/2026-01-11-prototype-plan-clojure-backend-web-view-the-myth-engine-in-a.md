# Prototype plan: Clojure backend + web view (the “Myth Engine in a Box”)

This is a really good fit for your workflow: Clojure for the sim/DSL/data, and a browser for instrumentation + iteration speed.

Below is a concrete, low-risk starting architecture that gets you to “watch belief form” fast.

---

## 0) What we’re building first

A **headless sim server** that:

* runs ticks (step-by-step or real-time)
* emits a stream of state snapshots + “explain traces”
* accepts a few player levers (iconography weights, appoint mouthpiece, place shrine)

A **web view** that:

* shows agents, events, institutions, and the myth ledger
* lets you click an agent and see:

  * active facets frontier
  * last packet received
  * why a mention happened (trace graph)
* lets you run scenario loops repeatedly

No champion control yet. No hex sphere yet. No LLM yet.

---

## 1) Suggested stack (minimal friction)

### Backend (Clojure)

* **Ring + Reitit** (routes)
* **http-kit** (websocket + server)
* **Transit** (fast structured data over WS) or just JSON
* **malli** (optional) for schema + validation (very nice for sim messages)

### Frontend (web)

* Plain **Vite + React** (or just vanilla + lit)
* One websocket connection
* Render:

  * a 2D grid + agents (simple canvas)
  * side panels for traces + ledgers

If you’re trying to stay lispier later, you can swap frontend to **ClojureScript**—but for “start now”, React/TS is the shortest path.

---

## 2) The “must-have” endpoints

### HTTP

* `GET /healthz` → `{ok:true}`
* `POST /sim/reset` → start scenario with seed + params
* `POST /sim/tick` → advance N ticks (for deterministic stepping)
* `POST /sim/run` → start real-time loop (server driven)
* `POST /sim/pause`
* `GET /sim/state` → latest snapshot (for page refresh)

### WebSocket

* `/ws`

  * server → client: `:tick`, `:event`, `:mention`, `:trace`, `:snapshot`
  * client → server: `:set-levers`, `:select-agent`, `:place-shrine`, `:appoint-mouthpiece`

You’ll want WS early because the debug UI is basically “watching a living system.”

---

## 3) Minimal internal data model (start tiny, but aligned with your design)

### Core state

* `world`: grid cells + tags (cold, trees, shrine, firepit)
* `agents`: vector of agents with:

  * `pos`, `needs`, `role` (peasant/priest/knight), `group-ids`
  * `trust` map (agent-id → weight) and/or group trust
  * `memory-frontier` (top N nodes with activation/strength)
* `groups`: institutions with:

  * `facets`, `sacrality`, `broadcast-spec`
* `events`: raw sim events emitted this tick
* `myth-ledger`: per (event-type, claim-id):

  * `buzz`, `tradition`, `attribution`
* `traces`: last K explanation traces (indexed by id)

### Don’t build a full knowledge graph yet

Start with:

* a **facet space** (keyword facets + weights)
* a **sparse frontier** per agent (map facet→activation/strength/valence)
* a **co-occur edge table** as a simple weighted map `{[f1 f2] w}`
  This gets you 80% of the behavior without graph complexity.

You can upgrade to explicit nodes/edges later.

---

## 4) The first scenario (do this first)

**Scenario: Winter Pyre**

* Environment: cold snap begins on tick 0
* Map has trees region + camp region
* Allies fear metric rises as enemies approach
* A rare event triggers probabilistically:

  * forest fire near enemies OR lightning on commander
* Two claims compete:

  * patron/fire judgment
  * storm god / “natural chance”

Institutions:

* Temple (religious broadcaster)
* Healers guild (secular service that can sacralize later)

Player levers:

* iconography weights (fire/judgment/winter/mercy)
* appoint mouthpiece (pick priest/knight to broadcast)
* place shrine marker (adds recall trigger in radius)

---

## 5) Tick loop (what happens every tick)

In order:

1. update needs + movement
2. generate local interactions (agent pairs within radius)
3. speakers emit packets (facet bundles)
4. listeners seed facets + spread activation
5. detect mentions (Δ event recall)
6. update myth ledger (buzz/tradition + attribution edges)
7. institutions broadcast canonical bundles (low entropy)
8. emit telemetry to WS:

   * snapshot delta
   * any events, mentions, traces

This directly matches your conceptual stack.

---

## 6) “Explain trace” format (so the UI can show *why*)

Every time a mention occurs, emit something like:

```clojure
{:trace/id "t-123"
 :listener 7
 :speaker 2
 :packet {:intent :warn
          :facets [:cold :trees]
          :tone {:awe 0.2 :urgency 0.5}}
 :seeded [{:facet :cold :delta 0.33}
          {:facet :trees :delta 0.21}]
 :spread [{:from :cold :to :fire :edge :co-occur :w 0.6 :delta 0.18}
          {:from :fire :to :patron/fire :edge :symbolic :w 0.8 :delta 0.22}]
 :event-recall {:event-type :winter-pyre :delta 0.27 :new 0.74}
 :claim-activation [{:claim :claim/winter-judgment-flame :delta 0.19}
                   {:claim :rebuttal/arson :delta 0.05}]
 :mention {:event-type :winter-pyre
           :claim :claim/winter-judgment-flame
           :weight 0.31}}
```

This is the single best “make it real” artifact you can build.

---

## 7) Repo layout suggestion (keeps it clean for solo dev)

```
fantasia/
  backend/
    deps.edn
    src/fantasia/
      server.clj         ;; ring + ws
      sim/core.clj       ;; tick loop, state
      sim/scenario.clj   ;; reset + initial state
      sim/facets.clj     ;; frontier + spread + seeding
      sim/mentions.clj   ;; mention detection + trace building
      sim/myth.clj       ;; buzz/tradition + attribution
      sim/groups.clj     ;; institution broadcasts + sacrality
      dsl/...
  web/
    package.json
    src/
      ws.ts
      App.tsx
      panels/
      canvas/
```

---

## 8) What to prototype first (2-hour sprint version)

1. Clojure server with `/ws` and `POST /sim/tick`
2. Hardcode initial state with 10 agents
3. Implement:

   * facet frontier (map facet→activation)
   * packet seeding (speaker sends 2 facets)
   * spread via a tiny co-occur table (cold→fire)
   * mention if event-recall crosses threshold
   * emit trace over WS
4. Web UI that lists traces and renders them as a graph/tree

If you get that working, you’ll *feel* the design immediately.

---

## One concrete next step (no extra questions)

If you want, I’ll write the **backend skeleton** (deps.edn + server + websocket message protocol + tick loop stub) as full file replacements, plus a minimal Vite web client that connects and prints traces.

Just tell me:

* do you prefer `http-kit` or `jetty`?
* and do you want payloads as JSON or Transit?

(If you don’t answer, I’ll default to **http-kit + JSON**.)
