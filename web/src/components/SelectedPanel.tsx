import React, { useState, useMemo, type CSSProperties } from "react";
import { Agent } from "../types";
import { getMovementSteps, safeStringify } from "../utils";

   type SelectedPanelProps = {
     selectedCell: [number, number] | null;
     selectedTile: Record<string, unknown> | null;
     selectedTileItems: Record<string, number>;
     selectedTileAgents: Agent[];
     selectedAgentId: number | null;
     selectedAgent: Agent | null;
     selectedVisibilityAgentId: number | null;
     agentVisibilityMaps: Record<number, Set<string>>;
     agents: Agent[];
     onSetVisibilityAgentId: (id: number | null) => void;
     tileVisibility: Record<string, "hidden" | "revealed" | "visible">;
     style?: CSSProperties;
   };

export function SelectedPanel({
    selectedCell,
    selectedTile,
    selectedTileItems,
    selectedTileAgents,
    selectedAgentId,
    selectedAgent,
    selectedVisibilityAgentId,
    agentVisibilityMaps,
    agents,
    onSetVisibilityAgentId,
    tileVisibility,
    style = {},
  }: SelectedPanelProps) {
  const visibilityState = useMemo(() => {
    if (!selectedCell) return "unknown";
    const keys = Object.keys(tileVisibility);
    const testKeys = [
      `${selectedCell[0]},${selectedCell[1]}`,
      `${selectedCell[0]}, ${selectedCell[1]}`,
      `${selectedCell[0]},  ${selectedCell[1]}`,
      `  ${selectedCell[0]},${selectedCell[1]}`,
    ];
    for (const key of testKeys) {
      if (tileVisibility[key]) {
        return tileVisibility[key];
      }
    }
    console.log("[SelectedPanel] tileVisibility keys:", keys.slice(0, 10));
    console.log("[SelectedPanel] Looking for tile:", selectedCell, "testKeys:", testKeys);
    return "unknown";
  }, [selectedCell, tileVisibility]);
  const [isCollapsed, setIsCollapsed] = useState(false);

  const normalizeValue = (value: unknown, fallback = "None") => {
    if (value == null) return fallback;
    if (typeof value === "string") {
      return value.replace(/^:/, "").replace(/_/g, " ");
    }
    return String(value);
  };

  const renderRow = (label: string, value: string, isMono = false) => (
    <div key={label} style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
      <span style={{ color: "#666" }}>{label}</span>
      <span style={{ fontFamily: isMono ? "monospace" : "inherit" }}>{value}</span>
    </div>
  );

  const tileRows = [
    renderRow("Coordinates", selectedCell ? `(${selectedCell[0]}, ${selectedCell[1]})` : "None", true),
    renderRow("Biome", normalizeValue(selectedTile?.biome)),
    renderRow("Terrain", normalizeValue(selectedTile?.terrain)),
    renderRow("Structure", normalizeValue(selectedTile?.structure)),
    renderRow("Elevation", normalizeValue(selectedTile?.elevation)),
    renderRow("Moisture", normalizeValue(selectedTile?.moisture)),
    renderRow("Temperature", normalizeValue(selectedTile?.temperature)),
    renderRow("Fertility", normalizeValue(selectedTile?.fertility)),
    renderRow("Vegetation", normalizeValue(selectedTile?.vegetation)),
    renderRow("Light", normalizeValue(selectedTile?.light)),
    renderRow("Wind", normalizeValue(selectedTile?.wind)),
    renderRow("Humidity", normalizeValue(selectedTile?.humidity)),
    renderRow("Pressure", normalizeValue(selectedTile?.pressure)),
    renderRow("Visibility", normalizeValue(visibilityState)),
    renderRow("Time", normalizeValue(selectedTile?.time)),
    renderRow("Season", normalizeValue(selectedTile?.season)),
    renderRow("Day/Night", normalizeValue(selectedTile?.dayNight)),
    renderRow("Weather", normalizeValue(selectedTile?.weather)),
    renderRow("Resource", normalizeValue(selectedTile?.resource)),
    renderRow("Resource Amount", normalizeValue(selectedTile?.resourceAmount)),
    renderRow("Resource Type", normalizeValue(selectedTile?.resourceType)),
    renderRow("Special", normalizeValue(selectedTile?.special)),
    renderRow("Special Value", normalizeValue(selectedTile?.specialValue)),
    renderRow("Special Type", normalizeValue(selectedTile?.specialType)),
  ];

  const itemEntries = Object.entries(selectedTileItems ?? {}).sort(([a], [b]) => a.localeCompare(b));
  const itemRows = itemEntries.length
    ? itemEntries.map(([resource, qty]) => renderRow(normalizeValue(resource), normalizeValue(qty)))
    : [renderRow("Items", "None")];

  const agentRows = selectedTileAgents.length
    ? selectedTileAgents.map((agent) => renderRow(`Agent ${agent.id}`, normalizeValue(agent.role ?? "unknown")))
    : [renderRow("Agents", "None")];

  const statusLabel = (agent: Agent | null) => {
    if (!agent) return "None";
    const status = (agent as any).status ?? {};
    const alive = status["alive?"] ?? status.alive ?? true;
    return alive ? "Alive" : "Dead";
  };

  const statusCause = (agent: Agent | null) => {
    if (!agent) return "None";
    const status = (agent as any).status ?? {};
    const cause = status["cause-of-death"] ?? status.causeOfDeath ?? null;
    return cause ? String(cause).replace(":", "").replace(/_/g, " ") : "None";
  };

  return (
    <div className="selected-panel" style={style}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          cursor: "pointer",
          padding: "8px 0",
          borderBottom: isCollapsed ? "1px solid #ddd" : "none",
        }}
        onClick={() => setIsCollapsed(!isCollapsed)}
      >
        <h2 style={{ margin: 0 }}>Selected</h2>
        <span
          style={{
            fontSize: "1.2em",
            color: "#666",
            transition: "transform 0.2s ease",
            transform: isCollapsed ? "rotate(-90deg)" : "rotate(0deg)",
          }}
        >
          ▼
        </span>
      </div>

      {!isCollapsed && (
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <div style={{ border: "1px solid #ddd", borderRadius: 6, padding: 8, backgroundColor: "#f9f9f9" }}>
            <div style={{ fontWeight: "bold", fontSize: "0.9em", color: "#666", marginBottom: 6 }}>
              Tile
            </div>
            <div style={{ display: "grid", gap: 4 }}>{tileRows}</div>
          </div>

          <div style={{ border: "1px solid #ddd", borderRadius: 6, padding: 8, backgroundColor: "#f9f9f9" }}>
            <div style={{ fontWeight: "bold", fontSize: "0.9em", color: "#666", marginBottom: 6 }}>
              Items
            </div>
            <div style={{ display: "grid", gap: 4 }}>{itemRows}</div>
          </div>

          <div style={{ border: "1px solid #ddd", borderRadius: 6, padding: 8, backgroundColor: "#f9f9f9" }}>
            <div style={{ fontWeight: "bold", fontSize: "0.9em", color: "#666", marginBottom: 6 }}>
              Agents
            </div>
            <div style={{ display: "grid", gap: 4 }}>{agentRows}</div>
          </div>

           <div style={{ border: "1px solid #ddd", borderRadius: 6, padding: 8, backgroundColor: "#f9f9f9" }}>
             <div style={{ fontWeight: "bold", fontSize: "0.9em", color: "#666", marginBottom: 6 }}>
               Focus
             </div>
             <div style={{ display: "grid", gap: 4 }}>
               {renderRow("Selected Agent", normalizeValue(selectedAgentId))}
             </div>
           </div>

          {selectedAgent && (
            <div style={{ border: "2px solid #4a90e2", borderRadius: 6, padding: 12, backgroundColor: "#f0f7ff" }}>
              <div style={{ fontWeight: "bold", fontSize: "1em", color: "#4a90e2", marginBottom: 8 }}>
                Agent Details
              </div>
              <div style={{ display: "grid", gap: 6 }}>
                <div><strong>ID:</strong> {selectedAgent.id}</div>
                <div><strong>Name:</strong> {(selectedAgent as any).name ?? "Unknown"}</div>
                <div><strong>Role:</strong> {selectedAgent.role}</div>
                <div><strong>Status:</strong> {statusLabel(selectedAgent)}</div>
                <div><strong>Cause of Death:</strong> {statusCause(selectedAgent)}</div>
                <div><strong>Position:</strong> {selectedAgent.pos ? `(${selectedAgent.pos[0]}, ${selectedAgent.pos[1]})` : "None"}</div>

                {(selectedAgent as any).stats && (
                  <div style={{ marginTop: 8 }}>
                    <strong>Stat Sheet:</strong>
                    <div style={{ marginLeft: 12, marginTop: 4, fontSize: "0.9em" }}>
                      {Object.entries((selectedAgent as any).stats).map(([key, value]) => (
                        <div key={key}>
                          {key}: {typeof value === "number" ? value.toFixed(3) : String(value)}
                        </div>
                      ))}
                      {(() => {
                        const movement = getMovementSteps((selectedAgent as any).stats);
                        return (
                          <div>
                            move steps: {movement.base} base / {movement.road} road
                          </div>
                        );
                      })()}
                    </div>
                  </div>
                )}

                {selectedAgent.needs && Object.keys(selectedAgent.needs).length > 0 && (
                  <div style={{ marginTop: 8 }}>
                    <strong>Needs:</strong>
                    <div style={{ marginLeft: 12, marginTop: 4, fontSize: "0.9em" }}>
                      {Object.entries(selectedAgent.needs).map(([key, value]) => (
                        <div key={key}>
                          {key}: {typeof value === "number" ? value.toFixed(3) : value}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {Array.isArray((selectedAgent as any).relationships) && (selectedAgent as any).relationships.length > 0 && (
                  <div style={{ marginTop: 8 }}>
                    <strong>Relationships:</strong>
                    <div style={{ marginLeft: 12, marginTop: 4, fontSize: "0.9em" }}>
                      {(selectedAgent as any).relationships.map((rel: any) => (
                        <div key={rel.agentId ?? rel["agent-id"]}>
                          {(rel.name ?? `#${rel.agentId ?? rel["agent-id"]}`)}: {typeof rel.affinity === "number" ? rel.affinity.toFixed(2) : rel.affinity}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {selectedAgent.recall && Object.keys(selectedAgent.recall).length > 0 && (
                  <div style={{ marginTop: 8 }}>
                    <strong>Recall:</strong>
                    <div style={{ marginLeft: 12, marginTop: 4, fontSize: "0.9em" }}>
                      {Object.entries(selectedAgent.recall).map(([key, value]) => (
                        <div key={key}>
                          {key}: {typeof value === "number" ? value.toFixed(3) : value}
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {Object.entries(selectedAgent).filter(([key]) => !["id", "role", "pos", "needs", "recall"].includes(key)).length > 0 && (
                  <div style={{ marginTop: 8 }}>
                    <strong>Other:</strong>
                    <div style={{ marginLeft: 12, marginTop: 4, fontSize: "0.9em" }}>
                      {Object.entries(selectedAgent)
                        .filter(([key]) => !["id", "role", "pos", "needs", "recall"].includes(key))
                        .map(([key, value]) => (
                          <div key={key}>
                            {key}: {typeof value === "number" ? value.toFixed(3) : String(value)}
                          </div>
                        ))}
                    </div>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
