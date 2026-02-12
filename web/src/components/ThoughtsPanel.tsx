import React, { memo } from "react";
import { Agent } from "../types";

type FacetEntry = {
  facet?: string;
  [key: string]: unknown;
};

type AgentStatus = {
  asleep?: boolean;
  "alive?"?: boolean;
  alive?: boolean;
  "cause-of-death"?: string;
  causeOfDeath?: string;
  [key: string]: unknown;
};

const asNeeds = (agent: Agent): Record<string, number> => {
  const needs = (agent as Record<string, unknown>)["needs"];
  return needs && typeof needs === "object" ? (needs as Record<string, number>) : {};
};

const asStatus = (agent: Agent): AgentStatus => {
  const status = (agent as Record<string, unknown>)["status"];
  return status && typeof status === "object" ? (status as AgentStatus) : {};
};

const asFacets = (agent: Agent): FacetEntry[] => {
  const raw = (agent as Record<string, unknown>)["top-facets"] ?? (agent as Record<string, unknown>)["topFacets"];
  return Array.isArray(raw) ? (raw as FacetEntry[]) : [];
};

const asCurrentJob = (agent: Agent): string | null => {
  const value = (agent as Record<string, unknown>)["current_job"];
  return typeof value === "string" ? value : null;
};

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
    const needs = asNeeds(agent);
    const facets = asFacets(agent);
    const facetNames = facets.map((f: FacetEntry) => f.facet).filter((f): f is string => typeof f === "string").slice(0, 3);

    const { food = 1.0, warmth = 1.0, sleep = 1.0 } = needs;
    const status = asStatus(agent);
    const asleep = status.asleep ?? false;
    const alive = (status["alive?"] ?? status.alive ?? true) !== false;
    const causeOfDeath = status["cause-of-death"] ?? status.causeOfDeath;
    const currentJob = asCurrentJob(agent);

    if (!alive) {
      const cause = typeof causeOfDeath === "string" ? causeOfDeath.replace(/^:/, "").replace(/_/g, " ") : "unknown";
      return `⚰️ Dead (${cause})`;
    }

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
    const needs = asNeeds(agent);
    const status = asStatus(agent);
    const alive = (status["alive?"] ?? status.alive ?? true) !== false;
    const { food = 1.0, warmth = 1.0, sleep = 1.0 } = needs;

    if (!alive) return "DEAD";

    if (food < 0.2 || warmth < 0.2 || sleep < 0.2) return "CRITICAL";
    if (food < 0.4 || warmth < 0.4 || sleep < 0.4) return "WARNING";
    return "OK";
  };

  const getUrgencyColor = (agent: Agent) => {
    const level = getUrgencyLevel(agent);
    switch (level) {
      case "DEAD": return "#9e9e9e";
      case "CRITICAL": return "#f44336";
      case "WARNING": return "#FF9800";
      default: return "#4CAF50";
    }
  };

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      {collapsible && (
        <button
          type="button"
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            cursor: "pointer",
            padding: "8px 0",
            borderBottom: collapsed ? "1px solid #ddd" : "none",
            borderTop: "none",
            borderLeft: "none",
            borderRight: "none",
            background: "transparent",
            width: "100%",
            textAlign: "left",
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
        </button>
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
              {Object.keys(asNeeds(selectedAgent)).length > 0 && (
                <div style={{ marginTop: 6, fontSize: 11 }}>
                  {["food", "warmth", "sleep"].map((needKey) => {
                    const needs = asNeeds(selectedAgent);
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
                const needs = asNeeds(agent);
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
