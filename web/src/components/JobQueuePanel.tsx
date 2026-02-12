import React from "react";

type JobQueuePanelProps = {
  jobs: any[] | null | undefined;
  collapsed?: boolean;
  onToggleCollapse?: () => void;
};

const jobTypeColors: Record<string, string> = {
  ":job/eat": "#4CAF50",
  ":job/warm-up": "#ff7043",
  ":job/sleep": "#2196F3",
  ":job/hunt": "#8e24aa",
  ":job/chop-tree": "#8BC34A",
  ":job/haul": "#FF9800",
  ":job/deliver-food": "#f57c00",
  ":job/build-wall": "#9E9E9E",
  ":job/build-house": "#8d6e63",
  ":job/build-structure": "#7e7e7e",
  ":job/harvest-wood": "#6d4c41",
  ":job/harvest-fruit": "#ffb74d",
  ":job/harvest-grain": "#fbc02d",
  ":job/harvest-stone": "#757575",
  ":job/builder": "#5c6bc0",
  ":job/improve": "#26a69a",
  ":job/mine": "#546e7a",
  ":job/smelt": "#ef6c00",
  ":job/scribe": "#9c27b0"
};

const jobTypeNames: Record<string, string> = {
  ":job/eat": "Eat",
  ":job/warm-up": "Warm Up",
  ":job/sleep": "Sleep",
  ":job/hunt": "Hunt",
  ":job/chop-tree": "Chop Tree",
  ":job/haul": "Haul",
  ":job/deliver-food": "Deliver Food",
  ":job/build-wall": "Build Wall",
  ":job/build-house": "Build House",
  ":job/build-structure": "Build",
  ":job/harvest-wood": "Harvest Wood",
  ":job/harvest-fruit": "Harvest Fruit",
  ":job/harvest-grain": "Harvest Grain",
  ":job/harvest-stone": "Harvest Stone",
  ":job/builder": "Builder",
  ":job/improve": "Improve",
  ":job/mine": "Mine",
  ":job/smelt": "Smelt",
  ":job/scribe": "Scribe"
};

export function JobQueuePanel({ jobs, collapsed = false, onToggleCollapse }: JobQueuePanelProps) {
  const safeJobs = Array.isArray(jobs) ? jobs : [];
  const activeJobs = safeJobs.filter((j: any) => j.state !== ":completed");
  const getField = (job: any, key: string) => job?.[key] ?? job?.[key.replace(/-/g, "_")] ?? job?.[key.replace(/-(\w)/g, (_, c) => c.toUpperCase())];
  const getAgentId = (job: any) => getField(job, "worker-id") ?? getField(job, "worker");
  const getPriority = (job: any) => {
    const value = getField(job, "priority");
    return typeof value === "number" ? value : Number(value ?? 0) || 0;
  };
  const formatAgentLabel = (id: unknown) => {
    if (id == null) return "Unassigned";
    const value = String(id);
    const short = value.length > 12 ? `${value.slice(0, 8)}...` : value;
    return `Agent ${short}`;
  };
  const getPos = (value: any) => {
    if (!value || !Array.isArray(value)) return "-";
    return `[${value[0]}, ${value[1]}]`;
  };
  const formatJobMeta = (job: any) => {
    const resource = getField(job, "resource");
    const qty = getField(job, "qty");
    const structure = getField(job, "structure");
    const stage = getField(job, "stage");
    const state = getField(job, "state");
    const meta: string[] = [];
    if (resource) meta.push(`res: ${String(resource).replace(":", "")}`);
    if (qty != null) meta.push(`qty: ${qty}`);
    if (structure) meta.push(`structure: ${String(structure).replace(":", "")}`);
    if (stage) meta.push(`stage: ${String(stage).replace(":", "")}`);
    if (state) meta.push(`state: ${String(state).replace(":", "")}`);
    return meta.join(" • ");
  };
  const renderJob = (job: any, idx: number) => {
    const jobType = job.type ?? ":job/unknown";
    const color = jobTypeColors[jobType] ?? "#999";
    const typeName = jobTypeNames[jobType] ?? String(jobType).replace(":job/", "");
    const progress = (job.progress ?? 0) / (job.required ?? 1);
    const uniqueKey = job.id ? `${job.id}-${idx}` : `idx-${idx}`;
    const workerId = getAgentId(job);
    const priority = getPriority(job);
    const target = getPos(getField(job, "target"));
    const fromPos = getPos(getField(job, "from-pos"));
    const toPos = getPos(getField(job, "to-pos"));
    const meta = formatJobMeta(job);

    return (
      <div
        key={uniqueKey}
        style={{
          backgroundColor: "#f9f9f9",
          borderLeft: `4px solid ${color}`,
          borderRadius: 4,
          padding: 8,
          fontSize: 13
        }}
      >
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <strong style={{ color }}>{typeName}</strong>
          <span style={{ fontSize: 11, color: "#666", fontWeight: 700 }}>
            P{priority}
          </span>
        </div>
        <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 4 }}>
          <span style={{ fontSize: 11, color: "#666" }}>
            {formatAgentLabel(workerId)}
          </span>
        </div>
        <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
          Target: {target}
          {fromPos !== "-" && ` ← ${fromPos}`}
          {toPos !== "-" && ` → ${toPos}`}
        </div>
        {meta && <div style={{ fontSize: 11, color: "#777", marginBottom: 4 }}>{meta}</div>}
        {progress > 0 && (
          <div style={{ marginTop: 4 }}>
            <div style={{
              backgroundColor: "#e0e0e0",
              borderRadius: 4,
              height: 6,
              overflow: "hidden"
            }}>
              <div style={{
                backgroundColor: color,
                height: "100%",
                width: `${Math.min(progress * 100, 100)}%`,
                transition: "width 0.3s ease"
              }} />
            </div>
          </div>
        )}
      </div>
    );
  };
  const byPriorityDesc = (a: any, b: any) => getPriority(b) - getPriority(a);
  const assignedJobs = activeJobs.filter((job: any) => getAgentId(job)).sort(byPriorityDesc);
  const unassignedJobs = activeJobs.filter((job: any) => !getAgentId(job)).sort(byPriorityDesc);

  if (collapsed) {
    return (
      <button
        type="button"
        style={{
          padding: 12,
          border: "1px solid #aaa",
          borderRadius: 8,
          cursor: "pointer",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          width: "100%",
          backgroundColor: "#fff",
          textAlign: "left"
        }}
        onClick={onToggleCollapse}
      >
        <strong style={{ margin: 0 }}>Job Queue</strong>
        <span style={{ opacity: 0.7, marginRight: 8 }}>({activeJobs.length})</span>
        <span style={{ 
          fontSize: "1.2em", 
          color: "#666",
          transition: "transform 0.2s ease",
          transform: "rotate(-90deg)"
        }}>
          ▼
        </span>
      </button>
    );
  }

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <button
        type="button"
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 8,
          cursor: "pointer",
          width: "100%",
          border: "none",
          padding: 0,
          backgroundColor: "transparent",
          textAlign: "left"
        }}
        onClick={onToggleCollapse}
      >
        <strong>Job Queue ({activeJobs.length})</strong>
        <span style={{ 
          fontSize: "1.2em", 
          color: "#666",
          transition: "transform 0.2s ease",
          transform: "rotate(0deg)"
        }}>
          ▼
        </span>
      </button>
      {activeJobs.length === 0 ? (
        <div style={{ fontSize: 13, color: "#666" }}>No active jobs</div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: "#333", marginBottom: 6 }}>
              Assigned ({assignedJobs.length})
            </div>
            {assignedJobs.length === 0 ? (
              <div style={{ fontSize: 12, color: "#777" }}>No assigned jobs</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {assignedJobs.map(renderJob)}
              </div>
            )}
          </div>
          <div>
            <div style={{ fontSize: 12, fontWeight: 700, color: "#333", marginBottom: 6 }}>
              Unassigned ({unassignedJobs.length})
            </div>
            {unassignedJobs.length === 0 ? (
              <div style={{ fontSize: 12, color: "#777" }}>No unassigned jobs</div>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
                {unassignedJobs.map(renderJob)}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
   );
  }
