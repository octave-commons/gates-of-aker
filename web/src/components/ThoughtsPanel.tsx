import React, { memo } from "react";
import { Agent } from "../types";

type ThoughtsPanelProps = {
  agents: Agent[];
  selectedAgent: Agent | null;
  collapsible?: boolean;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
};

export const ThoughtsPanel = memo(function ThoughtsPanel({
  agents,
  selectedAgent,
  collapsible = false,
  collapsed = false,
  onToggleCollapse,
}: ThoughtsPanelProps) {
  const displayAgents = collapsible && collapsed ? [] : agents.slice(0, 8);

  const getThoughtColor = (needValue: number) => {
    if (needValue < 0.3) return "#f44336";
    if (needValue < 0.6) return "#FFC107";
    return "#4CAF50";
  };

  const getThoughtText = (agent: Agent) => {
    const needs = (agent as any).needs ?? {};
    const facets = (agent["top-facets"] ?? agent.topFacets ?? []) as any[];
    const facetNames = facets.map((f: any) => f.facet).slice(0, 3);

    const { food = 1.0, warmth = 1.0, sleep = 1.0 } = needs;
    const status = (agent as any).status ?? {};
    const asleep = status.asleep ?? false;
    const currentJob = (agent as any).current_job ?? null;

    if (asleep) {
      return "💤 Sleeping peacefully...";
    }

    if (food < 0.3) {
      return "🍽️ I'm very hungry, need to find food!";
    }
    if (warmth < 0.3) {
      return "🔥 It's too cold, need warmth!";
    }
    if (sleep < 0.3) {
      return "😴 I'm exhausted, need to rest.";
    }

    if (currentJob) {
      return `Working on job: ${String(currentJob).slice(0, 20)}...`;
    }

    if (facetNames.length > 0) {
      const topFacet = facetNames[0];
      return `Thinking about: ${String(topFacet)}`;
    }

    return "Looking around, wondering what to do...";
  };

  const getUrgencyLevel = (agent: Agent) => {
    const needs = (agent as any).needs ?? {};
    const { food = 1.0, warmth = 1.0, sleep = 1.0 } = needs;

    if (food < 0.2 || warmth < 0.2 || sleep < 0.2) return "CRITICAL";
    if (food < 0.4 || warmth < 0.4 || sleep < 0.4) return "WARNING";
    return "OK";
  };

  const getUrgencyColor = (agent: Agent) => {
    const level = getUrgencyLevel(agent);
    switch (level) {
      case "CRITICAL": return "#f44336";
      case "WARNING": return "#FF9800";
      default: return "#4CAF50";
    }
  };

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      {collapsible && (
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            cursor: "pointer",
            padding: "8px 0",
            borderBottom: collapsed ? "1px solid #ddd" : "none"
          }}
          onClick={onToggleCollapse}
        >
          <strong style={{ margin: 0 }}>Agent Thoughts</strong>
          <span style={{
            fontSize: "1.2em",
            color: "#666",
            transition: "transform 0.2s ease",
            transform: collapsed ? "rotate(-90deg)" : "rotate(0deg)"
          }}>
            ▼
          </span>
        </div>
      )}

      {!collapsed && (
        <div style={{ marginTop: 8 }}>
          {selectedAgent && (
            <div style={{
              marginBottom: 12,
              padding: 8,
              backgroundColor: "#fff3e0",
              border: "1px solid #ff9800",
              borderRadius: 4
            }}>
              <div style={{ fontWeight: "bold", fontSize: 13, marginBottom: 4 }}>
                Selected Agent #{selectedAgent.id}
              </div>
              <div style={{ fontSize: 12, color: "#555" }}>
                {getThoughtText(selectedAgent)}
              </div>
              <div style={{
                marginTop: 6,
                fontSize: 11,
                fontWeight: 600,
                color: getUrgencyColor(selectedAgent),
                textTransform: "uppercase"
              }}>
                {getUrgencyLevel(selectedAgent)}
              </div>
              {Object.keys((selectedAgent as any).needs ?? {}).length > 0 && (
                <div style={{ marginTop: 6, fontSize: 11 }}>
                  {["food", "warmth", "sleep"].map((needKey) => {
                    const needs = (selectedAgent as any).needs ?? {};
                    const value = needs[needKey];
                    if (value === undefined) return null;

                    return (
                      <div key={needKey} style={{ marginBottom: 2 }}>
                        <span style={{ display: "inline-block", width: 35, color: "#666" }}>
                          {needKey}:
                        </span>
                        <span style={{
                          color: getThoughtColor(value),
                          fontWeight: 600
                        }}>
                          {(value * 100).toFixed(0)}%
                        </span>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>
          )}

          {displayAgents.length > 0 && (
            <div>
              <div style={{
                fontWeight: "bold",
                fontSize: 12,
                marginBottom: 8,
                color: "#555",
                borderBottom: "1px solid #ddd",
                paddingBottom: 4
              }}>
                Recent Agent Thoughts
              </div>
              {displayAgents.map((agent) => {
                const isSelected = selectedAgent?.id === agent.id;
                const needs = (agent as any).needs ?? {};
                const { food = 1.0, warmth = 1.0, sleep = 1.0 } = needs;

                return (
                  <div
                    key={agent.id}
                    style={{
                      padding: 6,
                      marginBottom: 6,
                      backgroundColor: isSelected ? "#e3f2fd" : "#fafafa",
                      border: isSelected ? "1px solid #2196f3" : "1px solid #ddd",
                      borderRadius: 4,
                      fontSize: 11
                    }}
                  >
                    <div style={{
                      display: "flex",
                      justifyContent: "space-between",
                      marginBottom: 4
                    }}>
                      <span style={{ fontWeight: 600, color: "#333" }}>
                        #{agent.id} {agent.role ?? ""}
                      </span>
                      <span style={{
                        color: getUrgencyColor(agent),
                        fontWeight: 600,
                        fontSize: 10
                      }}>
                        {getUrgencyLevel(agent)}
                      </span>
                    </div>
                    <div style={{ color: "#555", marginBottom: 4 }}>
                      {getThoughtText(agent)}
                    </div>
                    <div style={{ display: "flex", gap: 8 }}>
                      <span style={{ color: getThoughtColor(food) }}>
                        Food: {(food * 100).toFixed(0)}%
                      </span>
                      <span style={{ color: getThoughtColor(warmth) }}>
                        Warmth: {(warmth * 100).toFixed(0)}%
                      </span>
                      <span style={{ color: getThoughtColor(sleep) }}>
                        Sleep: {(sleep * 100).toFixed(0)}%
                      </span>
                    </div>
                  </div>
                );
              })}
              {agents.length > 8 && (
                <div style={{ fontSize: 11, color: "#888", textAlign: "center", marginTop: 4 }}>
                  +{agents.length - 8} more agents not shown
                </div>
              )}
            </div>
          )}

          {displayAgents.length === 0 && (
            <div style={{
              fontSize: 12,
              color: "#999",
              textAlign: "center",
              padding: 16
            }}>
              No agents to display thoughts for
            </div>
          )}
        </div>
      )}
    </div>
  );
});
