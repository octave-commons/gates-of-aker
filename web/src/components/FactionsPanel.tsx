import React, { useState } from "react";
import { Agent } from "../types";
import { AgentCard } from "./AgentCard";
import { CONFIG } from "../config/constants";

type FactionsPanelProps = {
  agents: Agent[];
  jobs?: any[];
  collapsible?: boolean;
  onFocusAgent?: (agent: Agent) => void;
};

type Faction = ":player" | ":wilderness";

interface FactionConfig {
  name: string;
  icon: string;
  color: string;
  bgColor: string;
}

const FACTION_CONFIGS: Record<Faction, FactionConfig> = {
  ":player": {
    name: "Colonists",
    icon: "🏰",
    color: "#2196F3",
    bgColor: "#E3F2FD"
  },
  ":wilderness": {
    name: "Wildlife",
    icon: "🐺",
    color: "#795548",
    bgColor: "#EFEBE9"
  }
};

const getAgentFaction = (agent: Agent): Faction => {
  const faction = agent.faction;
  if (faction === ":player" || faction === "player") {
    return ":player";
  }
  if (faction === ":wilderness" || faction === "wilderness") {
    return ":wilderness";
  }
  const role = (agent as any).role;
  if (["wolf", "deer", "bear"].includes(role)) {
    return ":wilderness";
  }
  return ":player";
};

function FactionSection({
  faction,
  agents,
  jobs,
  collapsed,
  onToggleCollapse,
  onFocusAgent
}: {
  faction: Faction;
  agents: Agent[];
  jobs?: any[];
  collapsed: boolean;
  onToggleCollapse: () => void;
  onFocusAgent?: (agent: Agent) => void;
}) {
  const config = FACTION_CONFIGS[faction];

  const getAgentJob = (agentId: number) => {
    const agent = agents.find((a: Agent) => a.id === agentId);
    const jobId = agent?.current_job;
    return Array.isArray(jobs) ? jobs.find((j: any) => j.id === jobId) : undefined;
  };

  return (
    <div style={{ marginBottom: 12 }}>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          cursor: "pointer",
          padding: "8px 12px",
          backgroundColor: config.bgColor,
          border: `1px solid ${config.color}`,
          borderRadius: 8,
          userSelect: "none"
        }}
        onClick={onToggleCollapse}
      >
        <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <span style={{ fontSize: "1.2em" }}>{config.icon}</span>
          <strong style={{ margin: 0, color: config.color }}>{config.name}</strong>
          <span style={{ fontSize: "0.9em", color: "#666", fontWeight: "normal" }}>
            ({agents.length})
          </span>
        </div>
        <span style={{
          fontSize: "1.2em",
          color: "#666",
          transition: "transform 0.2s ease",
          transform: collapsed ? "rotate(-90deg)" : "rotate(0deg)"
        }}>
          ▼
        </span>
      </div>

      {!collapsed && (
        <div style={{
          maxHeight: CONFIG.ui.AGENT_LIST_MAX_HEIGHT,
          overflowY: "auto",
          border: `1px solid ${config.color}`,
          borderTop: "none",
          borderRadius: "0 0 8px 8px",
          padding: 8,
          backgroundColor: "#fff"
        }}>
          {agents.length === 0 ? (
            <div style={{ padding: "12px 8px", color: "#999", fontStyle: "italic" }}>
              No {config.name.toLowerCase()} found
            </div>
          ) : (
            agents.map((agent) => (
              <AgentCard
                key={String(agent.id)}
                agent={agent}
                compact
                currentJob={getAgentJob(typeof agent.id === 'number' ? agent.id : Number(agent.id))}
                onSelect={onFocusAgent}
              />
            ))
          )}
        </div>
      )}
    </div>
  );
}

export function FactionsPanel({ agents, jobs, collapsible = false, onFocusAgent }: FactionsPanelProps) {
  const [playerCollapsed, setPlayerCollapsed] = useState(false);
  const [wildernessCollapsed, setWildernessCollapsed] = useState(false);

  const playerAgents = agents.filter((a: Agent) => getAgentFaction(a) === ":player");
  const wildernessAgents = agents.filter((a: Agent) => getAgentFaction(a) === ":wilderness");

  if (!collapsible) {
    return (
      <div>
        <FactionSection
          faction=":player"
          agents={playerAgents}
          jobs={jobs}
          collapsed={false}
          onToggleCollapse={() => {}}
          onFocusAgent={onFocusAgent}
        />
        <FactionSection
          faction=":wilderness"
          agents={wildernessAgents}
          jobs={jobs}
          collapsed={false}
          onToggleCollapse={() => {}}
          onFocusAgent={onFocusAgent}
        />
      </div>
    );
  }

  return (
    <div>
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          cursor: "pointer",
          padding: "8px 0",
          borderBottom: "1px solid #ddd"
        }}
        onClick={() => {
          setPlayerCollapsed(!playerCollapsed);
          setWildernessCollapsed(!wildernessCollapsed);
        }}
      >
        <strong style={{ margin: 0 }}>Factions ({agents.length})</strong>
        <span style={{ opacity: 0.7, marginRight: 8 }}>
          🏰 {playerAgents.length} | 🐺 {wildernessAgents.length}
        </span>
      </div>

      <FactionSection
        faction=":player"
        agents={playerAgents}
        jobs={jobs}
        collapsed={playerCollapsed}
        onToggleCollapse={() => setPlayerCollapsed(!playerCollapsed)}
        onFocusAgent={onFocusAgent}
      />
      <FactionSection
        faction=":wilderness"
        agents={wildernessAgents}
        jobs={jobs}
        collapsed={wildernessCollapsed}
        onToggleCollapse={() => setWildernessCollapsed(!wildernessCollapsed)}
        onFocusAgent={onFocusAgent}
      />
    </div>
  );
}
