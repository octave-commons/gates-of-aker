import { Agent, hasPos } from "../types";

type AgentListProps = {
  agents: Agent[];
};

export function AgentList({ agents }: AgentListProps) {
  return (
    <div style={{ marginTop: 12 }}>
      <strong>Agents ({agents.length})</strong>
      <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid #ccc", borderRadius: 8, padding: 8 }}>
        {agents.map((agent) => (
          <div key={agent.id} style={{ padding: "4px 0", borderBottom: "1px solid #eee" }}>
            <div style={{ display: "flex", justifyContent: "space-between" }}>
              <span>
                #{agent.id} — {agent.role ?? "unknown"}
              </span>
              <span style={{ opacity: 0.6 }}>{hasPos(agent) ? `(${agent.pos[0]},${agent.pos[1]})` : "pos:n/a"}</span>
            </div>
            <div style={{ fontSize: 12, opacity: 0.8 }}>
              top facets: {JSON.stringify(agent["top-facets"] ?? agent.topFacets ?? [])}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
