import React, { useEffect, useMemo, useRef, useState } from "react";
import { WSClient, WSMessage } from "./ws";

type Trace = Record<string, any>;
type Agent = {
  id: number;
  pos: [number, number] | null;
  role: string;
  needs: Record<string, number>;
  recall: Record<string, number>;
  [key: string]: any;
};

const fmt = (n: any) => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));
const clamp01 = (x: number) => Math.max(0, Math.min(1, x));
const hasPos = (agent?: Agent | null): agent is Agent & { pos: [number, number] } =>
  !!agent && Array.isArray(agent.pos) && agent.pos.length === 2 && agent.pos.every((v) => typeof v === "number");

const DEFAULT_WORLD_SIZE: [number, number] = [20, 20];
const MIN_WORLD_DIMENSION = 6;
const MAX_WORLD_DIMENSION = 60;

const parseWorldSize = (raw: any): [number, number] | null => {
  if (!raw) return null;
  if (Array.isArray(raw) && raw.length >= 2) {
    const [w, h] = raw;
    if (typeof w === "number" && typeof h === "number") return [w, h];
  }
  if (typeof raw === "object") {
    const maybeWidth = raw?.width ?? raw?.w;
    const maybeHeight = raw?.height ?? raw?.h;
    if (typeof maybeWidth === "number" && typeof maybeHeight === "number") {
      return [maybeWidth, maybeHeight];
    }
  }
  return null;
};

const clampWorldDimension = (value: number) => {
  if (!Number.isFinite(value)) return DEFAULT_WORLD_SIZE[0];
  const rounded = Math.round(value);
  const absValue = Math.abs(rounded);
  return Math.max(MIN_WORLD_DIMENSION, Math.min(MAX_WORLD_DIMENSION, absValue));
};

const sizesEqual = (a: [number, number], b: [number, number]) => a[0] === b[0] && a[1] === b[1];

export function App() {
  const [status, setStatus] = useState<"open" | "closed" | "error">("closed");
  const [tick, setTick] = useState(0);
  const [snapshot, setSnapshot] = useState<any>(null);
  const [traces, setTraces] = useState<Trace[]>([]);
  const [events, setEvents] = useState<any[]>([]);

  const [selectedCell, setSelectedCell] = useState<[number, number] | null>(null);
  const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);
  const [worldSize, setWorldSize] = useState<[number, number]>(DEFAULT_WORLD_SIZE);
  const [worldWidthInput, setWorldWidthInput] = useState(DEFAULT_WORLD_SIZE[0]);
  const [worldHeightInput, setWorldHeightInput] = useState(DEFAULT_WORLD_SIZE[1]);

  const [fireToPatron, setFireToPatron] = useState(0.8);
  const [lightningToStorm, setLightningToStorm] = useState(0.75);
  const [stormToDeity, setStormToDeity] = useState(0.85);

  const client = useMemo(() => {
    const url = `ws://localhost:3000/ws`;
    const applyWorldSize = (raw: any) => {
      const parsed = parseWorldSize(raw);
      if (!parsed) return;
      setWorldSize((prev) => (sizesEqual(prev, parsed) ? prev : parsed));
    };
    return new WSClient(
      url,
      (m: WSMessage) => {
        if (m.op === "hello") {
          applyWorldSize(m.state?.size);
        }
        if (m.op === "tick") {
          setTick(m.data?.tick ?? 0);
          setSnapshot(m.data?.snapshot ?? null);
          applyWorldSize(m.data?.snapshot?.size ?? m.data?.size);
        }
        if (m.op === "trace") {
          setTraces((prev) => {
            const next = [...prev, m.data];
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
          applyWorldSize(m.state?.size);
        }
      },
      (s) => setStatus(s)
    );
  }, []);

  useEffect(() => {
    client.connect();
    return () => client.close();
  }, [client]);

  useEffect(() => {
    setWorldWidthInput(worldSize[0]);
    setWorldHeightInput(worldSize[1]);
  }, [worldSize]);

  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !snapshot) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const [rawCols, rawRows] = worldSize;
    const gridCols = Math.max(1, Math.round(rawCols));
    const gridRows = Math.max(1, Math.round(rawRows));
    const maxCells = Math.max(gridCols, gridRows);
    const cell = Math.max(4, Math.floor(480 / Math.max(1, maxCells)));
    const widthPx = cell * gridCols;
    const heightPx = cell * gridRows;

    canvas.width = widthPx;
    canvas.height = heightPx;

    ctx.clearRect(0, 0, widthPx, heightPx);

    ctx.globalAlpha = 0.25;
    ctx.strokeStyle = "#777";
    for (let x = 0; x < gridCols; x++) {
      for (let y = 0; y < gridRows; y++) {
        ctx.strokeRect(x * cell, y * cell, cell, cell);
      }
    }
    ctx.globalAlpha = 1;
    ctx.strokeStyle = "#111";

    if (Array.isArray(snapshot.shrine) && snapshot.shrine.length === 2) {
      const [sx, sy] = snapshot.shrine;
      ctx.fillRect(sx * cell + cell * 0.2, sy * cell + cell * 0.2, cell * 0.6, cell * 0.6);
    }

    const highlightCell =
      selectedCell &&
      selectedCell[0] >= 0 &&
      selectedCell[1] >= 0 &&
      selectedCell[0] < gridCols &&
      selectedCell[1] < gridRows
        ? selectedCell
        : null;

    if (highlightCell) {
      const [selX, selY] = highlightCell;
      ctx.strokeRect(selX * cell + 2, selY * cell + 2, cell - 4, cell - 4);
    }

    const colorForRole = (role?: string) => {
      switch (role) {
        case "priest":
          return "#d7263d";
        case "knight":
          return "#3366ff";
        default:
          return "#111";
      }
    };

    for (const agent of snapshot.agents ?? []) {
      if (!hasPos(agent)) continue;
      const [ax, ay] = agent.pos;
      ctx.beginPath();
      ctx.fillStyle = colorForRole(agent.role);
      ctx.arc(ax * cell + cell / 2, ay * cell + cell / 2, cell * 0.27, 0, Math.PI * 2);
      ctx.fill();

      if (agent.id === selectedAgentId) {
        ctx.beginPath();
        ctx.strokeStyle = "#ffae00";
        ctx.arc(ax * cell + cell / 2, ay * cell + cell / 2, cell * 0.4, 0, Math.PI * 2);
        ctx.stroke();
        ctx.strokeStyle = "#111";
      }
    }
  }, [snapshot, selectedCell, selectedAgentId, worldSize]);

  const sendTick = (n: number) => client.send({ op: "tick", n });
  const reset = (seed: number, size?: [number, number]) => {
    const payload: Record<string, unknown> = { op: "reset", seed };
    if (size) payload.size = size;
    client.send(payload);
  };
  const handleReset = () => {
    const width = clampWorldDimension(worldWidthInput);
    const height = clampWorldDimension(worldHeightInput);
    setWorldWidthInput(width);
    setWorldHeightInput(height);
    reset(1, [width, height]);
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
  const selectedAgent = selectedAgentId == null ? null : (snapshot?.agents ?? []).find((a: Agent) => a.id === selectedAgentId) ?? null;
  const attribution = snapshot?.attribution ?? {};

  const onCanvasClick = (event: React.MouseEvent<HTMLCanvasElement>) => {
    if (!snapshot) return;
    const rect = (event.target as HTMLCanvasElement).getBoundingClientRect();
    const [rawCols, rawRows] = worldSize;
    const gridCols = Math.max(1, Math.round(rawCols));
    const gridRows = Math.max(1, Math.round(rawRows));
    const relX = (event.clientX - rect.left) / rect.width;
    const relY = (event.clientY - rect.top) / rect.height;
    const clampIndex = (value: number, max: number) => Math.min(max - 1, Math.max(0, value));
    const x = clampIndex(Math.floor(relX * gridCols), gridCols);
    const y = clampIndex(Math.floor(relY * gridRows), gridRows);
    setSelectedCell([x, y]);
    const hit = (snapshot.agents ?? []).find((a: Agent) => hasPos(a) && a.pos[0] === x && a.pos[1] === y);
    setSelectedAgentId(hit ? hit.id : null);
  };

  return (
    <div style={{ display: "grid", gridTemplateColumns: "520px 1fr", gap: 16, padding: 16 }}>
      <div>
        <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 8 }}>
          <strong>Fantasia — Myth Debugger</strong>
          <span style={{ opacity: 0.7 }}>WS: {status}</span>
        </div>

        <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
          <button onClick={() => sendTick(1)}>Tick</button>
          <button onClick={() => sendTick(10)}>Tick×10</button>
          <button onClick={placeShrineAtSelected} disabled={!selectedCell}>
            Place shrine @ selected
          </button>
          <button onClick={setMouthpieceToSelected} disabled={selectedAgentId == null}>
            Set mouthpiece = agent
          </button>
        </div>

        <div style={{ display: "flex", gap: 12, marginBottom: 12, flexWrap: "wrap", alignItems: "center" }}>
          <label style={{ display: "flex", gap: 6, alignItems: "center" }}>
            width
            <input
              type="number"
              min={MIN_WORLD_DIMENSION}
              max={MAX_WORLD_DIMENSION}
              step={1}
              value={worldWidthInput}
              onChange={(e) => setWorldWidthInput(Number(e.target.value))}
              style={{ width: 72 }}
            />
          </label>
          <label style={{ display: "flex", gap: 6, alignItems: "center" }}>
            height
            <input
              type="number"
              min={MIN_WORLD_DIMENSION}
              max={MAX_WORLD_DIMENSION}
              step={1}
              value={worldHeightInput}
              onChange={(e) => setWorldHeightInput(Number(e.target.value))}
              style={{ width: 72 }}
            />
          </label>
          <button onClick={handleReset}>Reset world</button>
          <span style={{ opacity: 0.7 }}>
            Current: {worldSize[0]}×{worldSize[1]} (range {MIN_WORLD_DIMENSION}-{MAX_WORLD_DIMENSION})
          </span>
        </div>
 
         <div style={{ marginBottom: 12 }}>

          <div style={{ marginBottom: 6 }}>
            <strong>Tick:</strong> {tick}
          </div>
          <div style={{ display: "grid", gap: 8 }}>
            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              fire→patron
              <input type="range" min={0} max={1} step={0.01} value={fireToPatron} onChange={(e) => setFireToPatron(Number(e.target.value))} />
              <span>{fireToPatron.toFixed(2)}</span>
            </label>
            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              lightning→storm
              <input
                type="range"
                min={0}
                max={1}
                step={0.01}
                value={lightningToStorm}
                onChange={(e) => setLightningToStorm(Number(e.target.value))}
              />
              <span>{lightningToStorm.toFixed(2)}</span>
            </label>
            <label style={{ display: "flex", gap: 8, alignItems: "center" }}>
              storm→deity
              <input type="range" min={0} max={1} step={0.01} value={stormToDeity} onChange={(e) => setStormToDeity(Number(e.target.value))} />
              <span>{stormToDeity.toFixed(2)}</span>
            </label>
            <button onClick={applyLevers}>Apply levers</button>
          </div>
        </div>

        <canvas
          ref={canvasRef}
          style={{ border: "1px solid #aaa", borderRadius: 8, cursor: "crosshair" }}
          onClick={onCanvasClick}
        />

        <div style={{ marginTop: 12 }}>
          <strong>Selected</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>
            {JSON.stringify({ selectedCell, selectedAgentId, mouthpieceId }, null, 2)}
          </pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Selected agent details</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(selectedAgent, null, 2)}</pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Agents ({(snapshot?.agents ?? []).length})</strong>
          <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid #ccc", borderRadius: 8, padding: 8 }}>
            {(snapshot?.agents ?? []).map((agent: Agent) => (
              <div key={agent.id} style={{ padding: "4px 0", borderBottom: "1px solid #eee" }}>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span>
                    #{agent.id} — {agent.role ?? "unknown"}
                  </span>
                  <span style={{ opacity: 0.6 }}>
                    {hasPos(agent) ? `(${agent.pos[0]},${agent.pos[1]})` : "pos:n/a"}
                  </span>
                </div>
                <div style={{ fontSize: 12, opacity: 0.8 }}>
                  top facets: {JSON.stringify(agent["top-facets"] ?? agent.topFacets ?? [])}
                </div>
              </div>
            ))}
          </div>
        </div>


        <div style={{ marginTop: 12 }}>
          <strong>Selected agent details</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(selectedAgent, null, 2)}</pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Attribution</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(attribution, null, 2)}</pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Recent events (snapshot)</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(recentEvents, null, 2)}</pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Live event feed</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(events, null, 2)}</pre>
        </div>

        <div style={{ marginTop: 12 }}>
          <strong>Ledger</strong>
          <pre style={{ whiteSpace: "pre-wrap" }}>{JSON.stringify(snapshot?.ledger ?? null, null, 2)}</pre>
        </div>
      </div>

      <div>
        <div style={{ marginBottom: 8 }}>
          <strong>Traces</strong> <span style={{ opacity: 0.7 }}>({traces.length})</span>
        </div>
        <div style={{ display: "grid", gap: 10 }}>
          {[...traces].reverse().map((trace: Trace, idx) => (
            <div key={trace["trace/id"] ?? idx} style={{ border: "1px solid #aaa", borderRadius: 10, padding: 10 }}>
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
                <code style={{ marginLeft: 6 }}>{JSON.stringify(trace.packet?.facets)}</code>
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
      </div>
    </div>
  );
}
