import { useState } from "react";
import { Agent } from "../types";
import { AgentCard } from "./AgentCard";

type AgentListProps = {
  agents: Agent[];
  collapsible?: boolean;
};

export function AgentList({ agents, collapsible = false }: AgentListProps) {
  const [isCollapsed, setIsCollapsed] = useState(false);

  if (collapsible) {
    return (
      <div style={{ marginTop: 12 }}>
        <div 
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            cursor: "pointer",
            padding: "8px 0",
            borderBottom: isCollapsed ? "1px solid #ddd" : "none"
          }}
          onClick={() => setIsCollapsed(!isCollapsed)}
        >
          <strong style={{ margin: 0 }}>Agents ({agents.length})</strong>
          <span style={{ 
            fontSize: "1.2em", 
            color: "#666",
            transition: "transform 0.2s ease",
            transform: isCollapsed ? "rotate(-90deg)" : "rotate(0deg)"
          }}>
            ▼
          </span>
        </div>
        
        {!isCollapsed && (
          <div style={{ 
            maxHeight: 220, 
            overflowY: "auto", 
            border: "1px solid #ccc", 
            borderRadius: 8, 
            padding: 8,
            marginTop: 8
          }}>
            {agents.map((agent) => (
              <AgentCard key={agent.id} agent={agent} compact />
            ))}
          </div>
        )}
      </div>
    );
  }

  // Original non-collapsible behavior
  return (
    <div style={{ marginTop: 12 }}>
      <strong>Agents ({agents.length})</strong>
      <div style={{ maxHeight: 220, overflowY: "auto", border: "1px solid #ccc", borderRadius: 8, padding: 8 }}>
        {agents.map((agent) => (
          <AgentCard key={agent.id} agent={agent} compact />
        ))}
      </div>
    </div>
  );
}
