* **Clojure backend** (http-kit + WebSocket + JSON)
* **Web view** (Vite + React + TS)
* A tiny sim with:

  * agents moving on a grid
  * facet packets (`:cold`, `:trees`, …)
  * sparse “frontier activation” + edge spread (`cold→fire→patron`)
  * a single event archetype (`:winter-pyre`)
  * mention traces streamed over WebSocket
  * simple player levers (`iconography`, `shrine`, `mouthpiece`)


# Next tight iteration

1. Add a **rival deity path** (e.g. `:storm → :deity/storm`) and competing iconography lever.
2. Make the **temple broadcast** depend on **mouthpiece-agent-id** (so appointing matters).
3. Replace the hardcoded `:winter-pyre` recall with:

   * event archetype registry (the `defeventtype` DSL you wanted)
   * multiple event types and contested claims
