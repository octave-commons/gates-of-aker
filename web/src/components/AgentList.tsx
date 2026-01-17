import { Agent } from "../types";
import { AgentCard } from "./AgentCard";

type AgentListProps = {
  agents: Agent[];
};

export function AgentList({ agents }: AgentListProps) {
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
