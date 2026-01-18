import { Agent, hasPos } from "../types";

type AgentCardProps = {
  agent: Agent;
  compact?: boolean;
  currentJob?: any;
};

const jobTypeNames: Record<string, string> = {
  ":job/eat": "Eating",
  ":job/sleep": "Sleeping",
  ":job/chop-tree": "Chopping",
  ":job/haul": "Hauling",
  ":job/build-wall": "Building"
};

export function AgentCard({ agent, compact = false, currentJob }: AgentCardProps) {
  const topFacets = (agent["top-facets"] ?? agent.topFacets ?? []);
  const facetNames = topFacets.map((f: any) => f.facet).filter(Boolean);
  const needs = (agent as any).needs ?? {};
  const inventory = (agent as any).inventory ?? {};
  const isAsleep = (agent as any).asleep ?? false;

  const jobTypeName = currentJob ? (jobTypeNames[currentJob.type] ?? String(currentJob.type).replace(":job/", "")) : null;

  return (
    <div style={{
      backgroundColor: isAsleep ? "#e8f0fe" : "#fff",
      border: `1px solid ${isAsleep ? "#4285f4" : "#ccc"}`,
      borderRadius: 6,
      padding: 8,
      marginBottom: compact ? 4 : 8,
      fontSize: 13,
      opacity: isAsleep ? 0.8 : 1
    }}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
        <div style={{ fontWeight: "bold", color: "#333" }}>
          #{agent.id} <span style={{ fontWeight: "normal", color: "#666" }}>{agent.role ?? "unknown"}</span>
          {isAsleep && <span style={{ marginLeft: 4, color: "#4285f4", fontSize: 11 }}>💤</span>}
        </div>
        {hasPos(agent) ? (
          <div style={{
            backgroundColor: "#e8e8e8",
            padding: "2px 6px",
            borderRadius: 4,
            fontSize: 11,
            fontFamily: "monospace"
          }}>
            ({agent.pos[0]}, {agent.pos[1]})
          </div>
        ) : (
          <div style={{
            backgroundColor: "#e8e8e8",
            padding: "2px 6px",
            borderRadius: 4,
            fontSize: 11,
            fontFamily: "monospace"
          }}>
            pos:n/a
          </div>
        )}
      </div>

      {jobTypeName && (
        <div style={{ 
          fontSize: 12, 
          color: "#555", 
          marginBottom: 4,
          backgroundColor: "#f0f0f0",
          padding: "2px 6px",
          borderRadius: 3,
          display: "inline-block"
        }}>
          {jobTypeName}
        </div>
      )}

      {Object.keys(needs).length > 0 && (
        <div style={{ marginBottom: 4 }}>
          {["food", "sleep", "warmth"].map((needKey) => {
            const value = needs[needKey];
            if (value === undefined) return null;
            
            const colors: Record<string, { high: string; mid: string; low: string }> = {
              food: { high: "#4CAF50", mid: "#FFC107", low: "#f44336" },
              sleep: { high: "#2196F3", mid: "#FF9800", low: "#9C27B0" },
              warmth: { high: "#FF5722", mid: "#607D8B", low: "#E91E63" }
            };
            
            let color = colors[needKey]?.mid;
            if (value > 0.7) color = colors[needKey]?.high;
            else if (value < 0.3) color = colors[needKey]?.low;
            
            return (
              <div key={needKey} style={{ fontSize: 11, marginBottom: 2 }}>
                <span style={{ display: "inline-block", width: 35 }}>{needKey}:</span>
                <div style={{
                  display: "inline-block",
                  width: 60,
                  backgroundColor: "#e0e0e0",
                  borderRadius: 3,
                  height: 8,
                  marginLeft: 4,
                  verticalAlign: "middle"
                }}>
                  <div style={{
                    backgroundColor: color,
                    height: "100%",
                    width: `${value * 100}%`,
                    borderRadius: 3
                  }} />
                </div>
              </div>
            );
          })}
        </div>
      )}

      {Object.keys(inventory).length > 0 && (
        <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
          <strong>Inventory:</strong> {Object.entries(inventory).map(([k, v]) => `${k}:${v}`).join(", ")}
        </div>
      )}

      <div style={{ display: "flex", gap: 12, fontSize: 12, color: "#555" }}>
        <div>beliefs: {Object.keys(agent.beliefs ?? {}).length}</div>
        <div>facets: {facetNames.length}</div>
      </div>
      {facetNames.length > 0 && !compact && (
        <div style={{
          marginTop: 6,
          display: "flex",
          flexWrap: "wrap",
          gap: 4
        }}>
          {facetNames.slice(0, 8).map((name: string, idx: number) => (
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
          {facetNames.length > 8 && (
            <span style={{ fontSize: 11, color: "#888" }}>+{facetNames.length - 8} more</span>
          )}
        </div>
      )}
    </div>
  );
}
