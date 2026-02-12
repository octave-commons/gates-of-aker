import React, { memo } from "react";
import { Agent, hasPos } from "../types";
import { getMovementSteps } from "../utils";

type CurrentJob = {
  type?: string;
  [key: string]: unknown;
};

type Relationship = {
  name?: string;
  affinity?: number;
  agentId?: number | string;
  "agent-id"?: number | string;
  [key: string]: unknown;
};

type FacetEntry = {
  facet?: string;
  [key: string]: unknown;
};

type AgentCardProps = {
  agent: Agent;
  compact?: boolean;
  currentJob?: CurrentJob;
  onSelect?: (agent: Agent) => void;
};

const jobTypeNames: Record<string, string> = {
  ":job/eat": "Eating",
  ":job/warm-up": "Warming up",
  ":job/sleep": "Sleeping",
  ":job/hunt": "Hunting",
  ":job/chop-tree": "Chopping",
  ":job/haul": "Hauling",
  ":job/deliver-food": "Delivering food",
  ":job/build-wall": "Building",
  ":job/build-house": "Building house",
  ":job/build-structure": "Building",
  ":job/harvest-wood": "Harvesting wood",
  ":job/harvest-fruit": "Harvesting fruit",
  ":job/harvest-grain": "Harvesting grain",
  ":job/harvest-stone": "Harvesting stone",
  ":job/builder": "Building",
  ":job/improve": "Improving",
  ":job/mine": "Mining",
  ":job/smelt": "Smelting",
  ":job/scribe": "Writing"
};

export const AgentCard = memo(function AgentCard({ agent, compact = false, currentJob, onSelect }: AgentCardProps) {
  const agentRecord = agent as Record<string, unknown>;
  const topFacetsRaw = agentRecord["top-facets"] ?? agentRecord["topFacets"] ?? [];
  const topFacets = Array.isArray(topFacetsRaw) ? (topFacetsRaw as FacetEntry[]) : [];
  const facetNames = topFacets
    .map((f: FacetEntry) => f.facet)
    .filter((facet): facet is string => typeof facet === "string" && facet.length > 0);
  const needs = (agentRecord["needs"] as Record<string, number> | undefined) ?? {};
  const inventory = (agentRecord["inventory"] as Record<string, unknown> | undefined) ?? {};
  const isAsleep = Boolean(agentRecord["asleep"] ?? false);
  const status = (agentRecord["status"] as Record<string, unknown> | undefined) ?? {};
  const alive = status["alive?"] ?? status.alive ?? true;
  const causeOfDeath = status["cause-of-death"] ?? status.causeOfDeath ?? null;
  const relationshipsRaw = agentRecord["relationships"];
  const relationships: Relationship[] = Array.isArray(relationshipsRaw) ? (relationshipsRaw as Relationship[]) : [];
  const topRelationship = Array.isArray(relationships) ? relationships[0] : null;
  const lastSocialThought = agentRecord["lastSocialThought"] ?? agentRecord["last-social-thought"] ?? null;
  const stats = (agentRecord["stats"] as Record<string, number> | undefined) ?? {};
  const statKeys = ["strength", "dexterity", "fortitude", "charisma"];
  const speed = getMovementSteps(stats);
  const statLine = statKeys
    .filter((key) => typeof stats[key] === "number")
    .map((key) => `${key.slice(0, 3).toUpperCase()} ${Math.round(stats[key] * 100)}`)
    .concat(`SPD ${speed.base}/${speed.road}`)
    .join(" · ");

  const currentJobType = typeof currentJob?.type === "string" ? currentJob.type : null;
  const jobTypeName = currentJobType ? (jobTypeNames[currentJobType] ?? currentJobType.replace(":job/", "")) : null;

  const mood = needs.mood ?? 0.5;
  const moodLabel = mood < 0.3 ? "DEPRESSED" : mood > 0.8 ? "HAPPY" : "OK";
  const moodColor = mood < 0.3 ? "#4A148C" : mood > 0.8 ? "#9C27B0" : "#7B1FA2";

  const parentIdsRaw = agentRecord["parentIds"] ?? agentRecord["parent-ids"] ?? [];
  const childrenIdsRaw = agentRecord["childrenIds"] ?? agentRecord["children-ids"] ?? [];
  const parentIds = Array.isArray(parentIdsRaw) ? parentIdsRaw : [];
  const childrenIds = Array.isArray(childrenIdsRaw) ? childrenIdsRaw : [];
  const childStage = agentRecord["childStage"] ?? agentRecord["child-stage"] ?? null;
  const carryingChild = agentRecord["carryingChild"] ?? agentRecord["carrying-child"] ?? null;
  const childStageLabel = childStage ? (String(childStage).toUpperCase()) : null;
  const childStageIcon = childStage === "infant" ? "👶" : childStage === "child" ? "👦" : "";

  const clickable = typeof onSelect === "function";

  return (
    <button
      type="button"
      onClick={clickable ? () => onSelect?.(agent) : undefined}
      disabled={!clickable}
      style={{
      backgroundColor: alive ? (isAsleep ? "#e8f0fe" : "#fff") : "#f3f3f3",
      border: `1px solid ${alive ? (isAsleep ? "#4285f4" : "#ccc") : "#c62828"}`,
      borderRadius: 6,
      padding: 8,
      marginBottom: compact ? 4 : 8,
      fontSize: 13,
      opacity: alive ? (isAsleep ? 0.8 : 1) : 0.7,
      cursor: clickable ? "pointer" : "default",
      width: "100%",
      textAlign: "left",
      color: "inherit"
    }}
    >
       <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 4 }}>
           <div style={{ fontWeight: "bold", color: "#333" }}>
             {(typeof agentRecord["name"] === "string" ? agentRecord["name"] : null) || <span>#{agent.id} <span style={{ fontWeight: "normal", color: "#666" }}>{agent.role ?? "unknown"}</span></span>}
             {isAsleep && <span> 💤</span>}
           </div>
           <div style={{ display: "flex", gap: 4, alignItems: "center" }}>
             <div style={{
               backgroundColor: moodColor,
               color: "#fff",
               padding: "2px 6px",
               borderRadius: 4,
               fontSize: 11,
               fontWeight: 600,
               display: "inline-block"
             }}>
               {moodLabel}
             </div>
             {jobTypeName && (
               <div style={{
                 backgroundColor: "#f5f5f5",
                 color: "#666",
                 padding: "2px 6px",
                 borderRadius: 4,
                 fontSize: 11,
                 display: "inline-block"
               }}>
                 {jobTypeName}
               </div>
             )}
           </div>
         </div>
          <div style={{
            backgroundColor: alive ? "#e8f5e9" : "#ffebee",
            color: alive ? "#2e7d32" : "#c62828",
            padding: "2px 6px",
            borderRadius: 4,
            fontSize: 11,
            fontWeight: 600
          }}>
            {alive ? "ALIVE" : "DEAD"}
          </div>
        {!alive && causeOfDeath && (
          <div style={{
            fontSize: 11,
            color: "#b71c1c",
            padding: "2px 6px",
            borderRadius: 4,
            backgroundColor: "#ffebee",
            fontWeight: 600
          }}>
            {String(causeOfDeath).replace(":", "").replace(/_/g, " ")}
          </div>
         )}
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

       {childStageLabel && (
         <div style={{
           backgroundColor: childStage === "infant" ? "#FFECB3" : "#C8E6C9",
           color: childStage === "infant" ? "#FF6F00" : "#2E7D32",
           padding: "2px 6px",
           borderRadius: 4,
           fontSize: 11,
           fontWeight: 600,
           display: "inline-block",
           marginRight: 4
         }}>
           {childStageIcon} {childStageLabel}
         </div>
       )}

       {carryingChild && (
         <div style={{
           backgroundColor: "#E1BEE7",
           color: "#4A148C",
           padding: "2px 6px",
           borderRadius: 4,
           fontSize: 11,
           fontWeight: 600,
           display: "inline-block",
           marginRight: 4
         }}>
            🤱 CARRYING #{String(carryingChild)}
          </div>
        )}

       {parentIds.length > 0 && (
         <div style={{
           backgroundColor: "#F3E5F5",
           color: "#4A148C",
           padding: "2px 6px",
           borderRadius: 4,
           fontSize: 10,
           fontWeight: 500,
           display: "inline-block",
           marginRight: 4
         }}>
           Parents: #{parentIds.join(", #")}
         </div>
       )}

       {childrenIds.length > 0 && (
         <div style={{
           backgroundColor: "#FFF3E0",
           color: "#E65100",
           padding: "2px 6px",
           borderRadius: 4,
           fontSize: 10,
           fontWeight: 500,
           display: "inline-block"
         }}>
           Children: #{childrenIds.join(", #")}
         </div>
       )}

       {Object.keys(needs).length > 0 && (
        <div style={{ marginBottom: 4 }}>
           {["mood", "social", "food", "sleep", "warmth"].map((needKey) => {
            const value = needs[needKey];
            if (value === undefined) return null;
            
             const colors: Record<string, { high: string; mid: string; low: string }> = {
               mood: { high: "#9C27B0", mid: "#7B1FA2", low: "#4A148C" },
               social: { high: "#26A69A", mid: "#00796B", low: "#004D40" },
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

      {statLine && (
        <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
          <strong>Stats:</strong> {statLine}
        </div>
      )}

      {topRelationship && (
        <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
          <strong>Bond:</strong> {topRelationship.name ?? `#${topRelationship.agentId ?? topRelationship["agent-id"]}`} · {Math.round(((topRelationship.affinity ?? 0) as number) * 100)}%
        </div>
      )}

      {Array.isArray(relationships) && relationships.length > 1 && (
        <div style={{ fontSize: 11, color: "#777", marginBottom: 4 }}>
          <strong>Links:</strong> {relationships.slice(1, 3).map((rel: Relationship) => {
            const label = rel.name ?? `#${rel.agentId ?? rel["agent-id"]}`;
            const affinity = Math.round(((rel.affinity ?? 0) as number) * 100);
            return `${label} ${affinity}%`;
          }).join(", ")}
        </div>
      )}

      {lastSocialThought && (
        <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
          <strong>Social:</strong> {String(lastSocialThought)}
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
          {facetNames.slice(0, 8).map((name: string) => (
            <span key={name} style={{
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
    </button>
  );
});
