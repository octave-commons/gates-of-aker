import { Agent, hasPos } from "../types";

type AgentCardProps = {
  agent: Agent;
  compact?: boolean;
};

export function AgentCard({ agent, compact = false }: AgentCardProps) {
  const topFacets = (agent["top-facets"] ?? agent.topFacets ?? []);
  const facetNames = topFacets.map((f: any) => f.facet).filter(Boolean);

  return (
    <div style={{
      backgroundColor: "#fff",
      border: "1px solid #ccc",
      borderRadius: 6,
      padding: 8,
      marginBottom: compact ? 4 : 8,
      fontSize: 13
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
        <div style={{ fontWeight: "bold", color: "#333" }}>
          #{agent.id} <span style={{ fontWeight: "normal", color: "#666" }}>{agent.role ?? "unknown"}</span>
        </div>
        {hasPos(agent) && (
          <div style={{
            backgroundColor: "#e8e8e8",
            padding: "2px 6px",
            borderRadius: 4,
            fontSize: 11,
            fontFamily: "monospace"
          }}>
            ({agent.pos[0]}, {agent.pos[1]})
          </div>
        )}
      </div>
      <div style={{ display: "flex", gap: 12, fontSize: 12, color: "#555" }}>
        <div>beliefs: {Object.keys(agent.beliefs ?? {}).length}</div>
        <div>facets: {facetNames.length}</div>
      </div>
      {facetNames.length > 0 && (
        <div style={{
          marginTop: 6,
          display: "flex",
          flexWrap: "wrap",
          gap: 4
        }}>
          {facetNames.slice(0, compact ? 4 : 8).map((name: string, idx: number) => (
            <span key={idx} style={{
              backgroundColor: "#f0f0f0",
              padding: "2px 6px",
              borderRadius: 3,
              fontSize: 11,
              color: "#444"
            }}>
              {String(name)}
            </span>
          ))}
          {facetNames.length > (compact ? 4 : 8) && (
            <span style={{ fontSize: 11, color: "#888" }}>+{facetNames.length - (compact ? 4 : 8)} more</span>
          )}
        </div>
      )}
    </div>
  );
}
