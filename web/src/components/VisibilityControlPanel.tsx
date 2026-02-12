import React from "react";
import { Agent } from "../types";

type VisibilityControlPanelProps = {
  agents: Agent[];
  selectedVisibilityAgentId: number | null;
  onSelectVisibilityAgent: (agentId: number | null) => void;
};

export function VisibilityControlPanel({
  agents,
  selectedVisibilityAgentId,
  onSelectVisibilityAgent,
}: VisibilityControlPanelProps) {
  const playerAgents = agents.filter((a: Agent) => a.faction === "player");

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>Visibility (LoS)</h3>
      <div style={{ display: "grid", gap: 6, fontSize: 12 }}>
        <label style={{ display: "flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
          <input
            type="radio"
            name="visibility"
            checked={selectedVisibilityAgentId === null}
            onChange={() => onSelectVisibilityAgent(null)}
          />
          <span>All visible (no filter)</span>
        </label>
        {playerAgents.map((agent: Agent) => {
          const agentId = typeof agent.id === "number" ? agent.id : Number(agent.id);
          if (!Number.isFinite(agentId)) {
            return null;
          }

          return (
            <label key={String(agent.id)} style={{ display: "flex", alignItems: "center", gap: 6, cursor: "pointer" }}>
              <input
                type="radio"
                name="visibility"
                checked={selectedVisibilityAgentId === agentId}
                onChange={() => onSelectVisibilityAgent(agentId)}
              />
              <span>{agent.name || `Agent ${agent.id}`}</span>
              <span style={{ opacity: 0.6 }}>
                ({agent.role || "unknown"} at [{agent.pos?.[0]}, {agent.pos?.[1]}])
              </span>
            </label>
          );
        })}
        {playerAgents.length === 0 && (
          <div style={{ opacity: 0.6, fontStyle: "italic" }}>
            No player agents available
          </div>
        )}
      </div>
    </div>
  );
}
