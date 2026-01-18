import React, { useEffect, useMemo, useState } from "react";
import { WSClient, WSMessage } from "./ws";
 import {
     AgentCard,
     AgentList,
     AttributionPanel,
     EventFeed,
     LedgerPanel,
     LeverControls,
     RawJSONFeedPanel,
     SelectedPanel,
     SimulationCanvas,
     StatusBar,
     TickControls,
     TraceFeed,
     BuildControls,
   } from "./components";
import { Agent, Trace, hasPos } from "./types";
import type { HexConfig, AxialCoords } from "./hex";

const clamp01 = (x: number) => Math.max(0, Math.min(1, x));
const fmt = (n: any) => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

export function App() {
  const [status, setStatus] = useState<"open" | "closed" | "error">("closed");
  const [tick, setTick] = useState(0);
  const [snapshot, setSnapshot] = useState<any>(null);
  const [mapConfig, setMapConfig] = useState<HexConfig | null>(null);
  const [traces, setTraces] = useState<Trace[]>([]);
  const [events, setEvents] = useState<any[]>([]);

  const [selectedCell, setSelectedCell] = useState<[number, number] | null>(null);
   const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);

   const [buildMode, setBuildMode] = useState(false);

  const [fireToPatron, setFireToPatron] = useState(0.8);
  const [lightningToStorm, setLightningToStorm] = useState(0.75);
  const [stormToDeity, setStormToDeity] = useState(0.85);

  const [worldWidth, setWorldWidth] = useState(20);
  const [worldHeight, setWorldHeight] = useState(20);

  const [tracesCollapsed, setTracesCollapsed] = useState(false);
  const [isInitializing, setIsInitializing] = useState(false);

  const client = useMemo(() => {
    const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
    const wsUrl = backendOrigin.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";
    return new WSClient(
      wsUrl,
      (m: WSMessage) => {
        if (m.op === "hello") {
          const state = m.state ?? {};
          setTick(state.tick ?? 0);
          setSnapshot(state);
          if (state.map) {
            setMapConfig(state.map as HexConfig);
          }
        }
        if (m.op === "tick") {
          setTick(m.data?.tick ?? 0);
          setSnapshot(m.data?.snapshot ?? null);
        }
        if (m.op === "trace") {
          const incoming = m.data as Trace;
          setTraces((prev) => {
            const next = [...prev, incoming];
            return next.slice(Math.max(0, next.length - 250));
          });
        }
        if (m.op === "event") {
          setEvents((prev) => {
            const next = [...prev, m.data];
            return next.slice(Math.max(0, next.length - 50));
          });
        }
        if (m.op === "reset") {
           setTraces([]);
           setEvents([]);
           setSelectedCell(null);
           setSelectedAgentId(null);
           const state = m.state ?? {};
           setSnapshot(state);
           if (state.map) {
             setMapConfig(state.map as HexConfig);
           }
         }
        if (m.op === "tiles") {
          setSnapshot((prev: any) => {
            if (!prev) return prev;
            return { ...prev, tiles: m.tiles };
          });
        }
       },
      (s) => setStatus(s)
    );
  }, []);

  useEffect(() => {
    client.connect();
    return () => client.close();
  }, [client]);

  // Initialize snapshot - fetch latest or create new one
  useEffect(() => {
    const initializeSnapshot = async () => {
      setIsInitializing(true);
      try {
        const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
        const response = await fetch(`${backendOrigin}/sim/state`);
        
        if (response.ok) {
          const state = await response.json();
          
          // Check if the state has meaningful data
          const hasData = state && (
            (state.tick && state.tick > 0) || 
            (state.agents && state.agents.length > 0) ||
            (state.ledger && Object.keys(state.ledger).length > 0)
          );
          
          if (hasData) {
            console.log("Loaded existing snapshot:", { tick: state.tick, agents: state.agents?.length });
            // Use existing state
            setTick(state.tick ?? 0);
            setSnapshot(state);
            if (state.map) {
              setMapConfig(state.map as HexConfig);
            }
          } else {
            console.log("Existing state is empty, creating new snapshot");
            // Create new snapshot with default seed
            createNewSnapshot();
          }
        } else {
          console.warn("Failed to fetch state, creating new snapshot");
          // If fetch fails, create new snapshot
          createNewSnapshot();
        }
      } catch (error) {
        console.warn("Error fetching existing state, creating new one:", error);
        createNewSnapshot();
      } finally {
        setIsInitializing(false);
      }
    };

    const createNewSnapshot = () => {
      const defaultSeed = Math.floor(Math.random() * 1000000);
      console.log("Creating new snapshot with seed:", defaultSeed);
      client.send({ op: "reset", seed: defaultSeed });
    };

    // Initialize snapshot when component mounts
    const timeoutId = setTimeout(() => {
      // Only initialize if WebSocket hasn't provided data yet
      if (!snapshot && status === "open") {
        initializeSnapshot();
      }
    }, 1500); // Wait 1.5 seconds for WebSocket "hello" message

    return () => clearTimeout(timeoutId);
  }, [client, snapshot, status]);

  const sendTick = (n: number) => client.send({ op: "tick", n });
  const reset = (seed: number, bounds?: { w: number; h: number }) => {
    const payload: { op: string; seed: number; bounds?: { w: number; h: number } } = { op: "reset", seed };
    if (bounds) {
      payload.bounds = bounds;
    }
    client.send(payload);
  };

  const handleCellSelect = (cell: [number, number], agentId: number | null) => {
     if (buildMode) {
       client.sendPlaceWallGhost(cell);
     }
     setSelectedCell(cell);
     setSelectedAgentId(agentId);
   };

  const placeShrineAtSelected = () => {
    if (!selectedCell) return;
    client.send({ op: "place_shrine", pos: selectedCell });
  };

  const applyLevers = () => {
    client.send({
      op: "set_levers",
      levers: {
        iconography: {
          "fire->patron": clamp01(fireToPatron),
          "lightning->storm": clamp01(lightningToStorm),
          "storm->deity": clamp01(stormToDeity),
        },
      },
    });
  };

  const setMouthpieceToSelected = () => {
    if (selectedAgentId == null) return;
    client.send({ op: "appoint_mouthpiece", agent_id: selectedAgentId });
  };

  const applyWorldSize = () => {
    reset(1, { w: worldWidth, h: worldHeight });
  };

  const recentEvents = snapshot?.["recent-events"] ?? snapshot?.recentEvents ?? snapshot?.recent_events ?? [];
  const mouthpieceId = snapshot?.levers?.["mouthpiece-agent-id"] ?? snapshot?.levers?.mouthpiece_agent_id ?? null;
  const selectedAgent =
    selectedAgentId == null ? null : (snapshot?.agents ?? []).find((a: Agent) => a.id === selectedAgentId) ?? null;
  const attribution = snapshot?.attribution ?? {};
  const agents: Agent[] = (snapshot?.agents ?? []) as Agent[];

  const selectedTile = selectedCell ? snapshot?.tiles?.[`${selectedCell[0]},${selectedCell[1]}`] : null;
  const selectedTileAgents = selectedCell ? (snapshot?.agents ?? []).filter((a: Agent) => {
    if (!hasPos(a)) return false;
      const [aq, ar] = a.pos as AxialCoords;
      return aq === selectedCell[0] && ar === selectedCell[1];
    }) : [];

  return (
    <div style={{ display: "grid", gridTemplateColumns: "1fr 420px", gap: 16, padding: 16, height: "calc(100vh - 32px)" }}>
      <div style={{ overflow: "auto", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", position: "relative" }}>
        {/* Loading overlay for snapshot initialization */}
        {isInitializing && !snapshot && (
          <div style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(255, 255, 255, 0.9)",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            gap: 16
          }}>
            <div style={{ fontSize: "1.2em", fontWeight: "bold", color: "#333" }}>
              Initializing Simulation...
            </div>
            <div style={{ fontSize: "0.9em", color: "#666" }}>
              {status === "open" ? "Fetching latest snapshot..." : "Connecting to server..."}
            </div>
          </div>
        )}
        
        <SimulationCanvas
          snapshot={snapshot}
          mapConfig={mapConfig}
          selectedCell={selectedCell}
          selectedAgentId={selectedAgentId}
          onCellSelect={handleCellSelect}
        />
        {selectedCell && (
          <div style={{
            position: "absolute",
            bottom: 16,
            left: "50%",
            transform: "translateX(-50%)",
            backgroundColor: "rgba(255, 255, 255, 0.95)",
            border: "1px solid #aaa",
            borderRadius: 8,
            padding: 12,
            minWidth: 300,
            maxWidth: 500,
            boxShadow: "0 4px 12px rgba(0, 0, 0, 0.15)"
          }}>
            <div style={{ fontWeight: "bold", marginBottom: 8 }}>
              Selected Cell: {selectedCell[0]}, {selectedCell[1]}
            </div>
            {selectedTile && (
              <div style={{ marginBottom: 8 }}>
                <div><strong>Resource:</strong> {selectedTile.resource ?? "none"}</div>
              </div>
            )}
            {selectedTileAgents.length > 0 && (
              <div>
                <div><strong>Agents ({selectedTileAgents.length}):</strong></div>
                {selectedTileAgents.map((agent: Agent) => (
                  <AgentCard key={agent.id} agent={agent} />
                ))}
              </div>
            )}
            {!selectedTile && selectedTileAgents.length === 0 && (
              <div style={{ opacity: 0.6 }}>No entities in this cell</div>
            )}
          </div>
        )}
      </div>

      <div style={{ overflow: "auto", paddingRight: 8 }}>
        <StatusBar status={status} />
        <TickControls
           onTick={sendTick}
           onReset={() => reset(1)}
           onPlaceShrine={placeShrineAtSelected}
           onSetMouthpiece={setMouthpieceToSelected}
           canPlaceShrine={!!selectedCell}
           canSetMouthpiece={selectedAgentId != null}
         />

        <BuildControls
          onPlaceWallGhost={client.sendPlaceWallGhost}
          onModeChange={(mode: "select" | "build") => setBuildMode(mode === "build")}
          buildMode={buildMode}
        />

         <div style={{ marginTop: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
          <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>World Size</h3>
          <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
            <label style={{ fontSize: 12 }}>Width:</label>
            <input
              type="number"
              min={0}
              max={1200}
              value={worldWidth}
              onChange={(e) => {
                const val = parseInt(e.target.value, 10);
                if (!isNaN(val)) setWorldWidth(val);
              }}
              style={{ width: 60 }}
            />
            <label style={{ fontSize: 12 }}>Height:</label>
            <input
              type="number"
              min={0}
              max={1200}
              value={worldHeight}
              onChange={(e) => {
                const val = parseInt(e.target.value, 10);
                if (!isNaN(val)) setWorldHeight(val);
              }}
              style={{ width: 60 }}
            />
            <button
              onClick={applyWorldSize}
              style={{ padding: "4px 8px", fontSize: 12 }}
            >
              Apply
            </button>
          </div>
        </div>

        <LeverControls
          tick={tick}
          fireToPatron={fireToPatron}
          lightningToStorm={lightningToStorm}
          stormToDeity={stormToDeity}
          onFireChange={setFireToPatron}
          onLightningChange={setLightningToStorm}
          onStormChange={setStormToDeity}
          onApply={applyLevers}
        />

        <SelectedPanel
          selectedCell={selectedCell}
          selectedAgentId={selectedAgentId}
          selectedAgent={selectedAgent}
          mouthpieceId={mouthpieceId}
          style={{ marginTop: 12 }}
        />

        <div style={{ marginTop: 12 }}>
          <AgentList agents={agents} collapsible />
        </div>

        <div style={{ marginTop: 12 }}>
          <div 
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              cursor: "pointer",
              padding: "8px 0",
              borderBottom: tracesCollapsed ? "1px solid #ddd" : "none"
            }}
            onClick={() => setTracesCollapsed(!tracesCollapsed)}
          >
            <strong style={{ margin: 0 }}>Traces</strong> 
            <span style={{ opacity: 0.7, marginRight: 8 }}>({traces.length})</span>
            <span style={{ 
              fontSize: "1.2em", 
              color: "#666",
              transition: "transform 0.2s ease",
              transform: tracesCollapsed ? "rotate(-90deg)" : "rotate(0deg)"
            }}>
              ▼
            </span>
          </div>
          
          {!tracesCollapsed && (
            <div style={{ display: "grid", gap: 10, marginTop: 8 }}>
              {[...traces].reverse().map((trace: Trace, idx) => (
            <div
              key={trace["trace/id"] ?? idx}
              data-testid="trace-card"
              style={{ border: "1px solid #aaa", borderRadius: 10, padding: 10 }}
            >
              <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
                <div>
                  <strong>{trace["trace/id"]}</strong>
                </div>
                <div style={{ opacity: 0.7 }}>tick {trace.tick}</div>
                <div style={{ opacity: 0.7 }}>
                  {trace.speaker} → {trace.listener}
                </div>
              </div>
              <div style={{ marginTop: 8 }}>
                <strong>packet</strong> <span style={{ opacity: 0.7 }}>{trace.packet?.intent}</span>
                {trace.packet?.facets && (
                  <span style={{ marginLeft: 6 }}>
                    facets: {Array.isArray(trace.packet.facets) ? trace.packet.facets.join(", ") : trace.packet.facets}
                  </span>
                )}
                {trace.packet?.["claim-hint"] ? (
                  <span style={{ marginLeft: 6, opacity: 0.7 }}>hint: {String(trace.packet?.["claim-hint"])} </span>
                ) : null}
              </div>
              <div style={{ marginTop: 8 }}>
                <strong>spread</strong>
                <div style={{ display: "grid", gap: 4, marginTop: 4 }}>
                  {(trace.spread ?? []).slice(0, 12).map((entry: any, i: number) => (
                    <div key={i} style={{ fontFamily: "monospace", fontSize: 12 }}>
                      {String(entry.from)} → {String(entry.to)} w={fmt(entry.w)} Δ={fmt(entry.delta)}
                    </div>
                  ))}
                </div>
              </div>
              <div style={{ marginTop: 8, fontFamily: "monospace" }}>
                <strong>event</strong> {trace["event-recall"]?.["event-type"]} Δ=
                {fmt(trace["event-recall"]?.delta)} new={fmt(trace["event-recall"]?.new)}
              </div>
              <div style={{ marginTop: 8, fontFamily: "monospace" }}>
                <strong>mention</strong> {trace.mention?.["event-type"]}/{trace.mention?.claim} w=
                {fmt(trace.mention?.weight)}
              </div>
            </div>
          ))}
            </div>
          )}
        </div>

        <AttributionPanel data={attribution} style={{ marginTop: 12 }} />
        <EventFeed events={recentEvents} title="Recent events (snapshot)" compact collapsible />
        <EventFeed events={events} title="Live event feed" compact collapsible />
        <LedgerPanel data={snapshot?.ledger ?? null} style={{ marginTop: 12 }} />
      </div>
    </div>
  );
}
