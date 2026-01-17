import React, { useEffect, useMemo, useState } from "react";
import { WSClient, WSMessage } from "./ws";
import {
  AgentCard,
  AgentList,
  EventFeed,
  LeverControls,
  RawJSONFeedPanel,
  SimulationCanvas,
  StatusBar,
  TickControls,
  TraceFeed,
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

  const [fireToPatron, setFireToPatron] = useState(0.8);
  const [lightningToStorm, setLightningToStorm] = useState(0.75);
  const [stormToDeity, setStormToDeity] = useState(0.85);

  const [worldWidth, setWorldWidth] = useState(20);
  const [worldHeight, setWorldHeight] = useState(20);

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
      },
      (s) => setStatus(s)
    );
  }, []);

  useEffect(() => {
    client.connect();
    return () => client.close();
  }, [client]);

  const sendTick = (n: number) => client.send({ op: "tick", n });
  const reset = (seed: number, bounds?: { w: number; h: number }) => {
    const payload: { op: string; seed: number; bounds?: { w: number; h: number } } = { op: "reset", seed };
    if (bounds) {
      payload.bounds = bounds;
    }
    client.send(payload);
  };

  const handleCellSelect = (cell: [number, number], agentId: number | null) => {
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

        <RawJSONFeedPanel
          title="Selected"
          data={{ selectedCell, selectedAgentId, mouthpieceId }}
          style={{ marginTop: 12 }}
        />
        <RawJSONFeedPanel title="Selected agent details" data={selectedAgent} style={{ marginTop: 12 }} />

        <div style={{ marginTop: 12 }}>
          <AgentList agents={agents} />
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Traces</strong> <span style={{ opacity: 0.7 }}>({traces.length})</span>
        </div>
        <div style={{ display: "grid", gap: 10 }}>
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

        <RawJSONFeedPanel title="Attribution" data={attribution} style={{ marginTop: 12 }} />
        <EventFeed events={recentEvents} title="Recent events (snapshot)" compact />
        <EventFeed events={events} title="Live event feed" compact />
        <RawJSONFeedPanel title="Ledger" data={snapshot?.ledger ?? null} style={{ marginTop: 12 }} />
      </div>
    </div>
  );
}
