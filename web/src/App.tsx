import React, { useEffect, useMemo, useState } from "react";
import { WSClient, WSMessage } from "./ws";
import {
  AgentList,
  LeverControls,
  RawJSONFeedPanel,
  SimulationCanvas,
  StatusBar,
  TickControls,
  TraceFeed,
} from "./components";
import { Agent, Trace } from "./types";
import { axialToPixel, pixelToAxial, hexPolygonPoints, inBounds, randAxial } from "./hex";
import { axialToPixel, pixelToAxial, hexPolygonPoints, neighborsAxial, distanceAxial, inBounds, randAxial } from "./hex";

const clamp01 = (x: number) => Math.max(0, Math.min(1, x));

export function App() {
  const [status, setStatus] = useState<"open" | "closed" | "error">("closed");
  const [tick, setTick] = useState(0);
  const [snapshot, setSnapshot] = useState<any>(null);
  const [traces, setTraces] = useState<Trace[]>([]);
  const [events, setEvents] = useState<any[]>([]);

  const [selectedCell, setSelectedCell] = useState<[number, number] | null>(null);
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);

  const [fireToPatron, setFireToPatron] = useState(0.8);
  const [lightningToStorm, setLightningToStorm] = useState(0.75);
  const [stormToDeity, setStormToDeity] = useState(0.85);

  const client = useMemo(() => {
    const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
    const wsUrl = backendOrigin.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";
    return new WSClient(
      wsUrl,
      (m: WSMessage) => {
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
  const reset = (seed: number) => client.send({ op: "reset", seed });

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

  const recentEvents = snapshot?.["recent-events"] ?? snapshot?.recentEvents ?? snapshot?.recent_events ?? [];
  const mouthpieceId = snapshot?.levers?.["mouthpiece-agent-id"] ?? snapshot?.levers?.mouthpiece_agent_id ?? null;
  const selectedAgent =
    selectedAgentId == null ? null : (snapshot?.agents ?? []).find((a: Agent) => a.id === selectedAgentId) ?? null;
  const attribution = snapshot?.attribution ?? {};
  const agents: Agent[] = (snapshot?.agents ?? []) as Agent[];

  return (
    <div style={{ display: "grid", gridTemplateColumns: "520px 1fr", gap: 16, padding: 16 }}>
      <div>
        <StatusBar status={status} />
        <TickControls
          onTick={sendTick}
          onReset={() => reset(1)}
          onPlaceShrine={placeShrineAtSelected}
          onSetMouthpiece={setMouthpieceToSelected}
          canPlaceShrine={!!selectedCell}
          canSetMouthpiece={selectedAgentId != null}
        />

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

        <SimulationCanvas
          snapshot={snapshot}
          selectedCell={selectedCell}
          selectedAgentId={selectedAgentId}
          onCellSelect={handleCellSelect}
        />

        <RawJSONFeedPanel
          title="Selected"
          data={{ selectedCell, selectedAgentId, mouthpieceId }}
          style={{ marginTop: 12 }}
        />
        <RawJSONFeedPanel title="Selected agent details" data={selectedAgent} style={{ marginTop: 12 }} />

        <AgentList agents={agents} />

        <RawJSONFeedPanel title="Attribution" data={attribution} style={{ marginTop: 12 }} />
        <RawJSONFeedPanel title="Recent events (snapshot)" data={recentEvents} style={{ marginTop: 12 }} />
        <RawJSONFeedPanel title="Live event feed" data={events} style={{ marginTop: 12 }} />
        <RawJSONFeedPanel title="Ledger" data={snapshot?.ledger ?? null} style={{ marginTop: 12 }} />
      </div>

      <TraceFeed traces={traces} />
    </div>
  );
}
