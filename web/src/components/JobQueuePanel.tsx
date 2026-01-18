type JobQueuePanelProps = {
  jobs: any[];
  collapsed?: boolean;
  onToggleCollapse?: () => void;
};

const jobTypeColors: Record<string, string> = {
  ":job/eat": "#4CAF50",
  ":job/sleep": "#2196F3",
  ":job/chop-tree": "#8BC34A",
  ":job/haul": "#FF9800",
  ":job/build-wall": "#9E9E9E"
};

const jobTypeNames: Record<string, string> = {
  ":job/eat": "Eat",
  ":job/sleep": "Sleep",
  ":job/chop-tree": "Chop Tree",
  ":job/haul": "Haul",
  ":job/build-wall": "Build Wall"
};

export function JobQueuePanel({ jobs, collapsed = false, onToggleCollapse }: JobQueuePanelProps) {
  const activeJobs = jobs.filter((j: any) => j.state !== ":completed");

  if (collapsed) {
    return (
      <div 
        style={{
          padding: 12,
          border: "1px solid #aaa",
          borderRadius: 8,
          cursor: "pointer",
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center"
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
      </div>
    );
  }

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <div 
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: 8,
          cursor: "pointer"
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
      </div>
      {activeJobs.length === 0 ? (
        <div style={{ fontSize: 13, color: "#666" }}>No active jobs</div>
      ) : (
        <div style={{ display: "flex", flexDirection: "column", gap: 6 }}>
          {activeJobs.map((job: any, idx: number) => {
            const jobType = job.type ?? ":job/unknown";
            const color = jobTypeColors[jobType] ?? "#999";
            const typeName = jobTypeNames[jobType] ?? String(jobType).replace(":job/", "");
            const progress = (job.progress ?? 0) / (job.required ?? 1);
            
            return (
              <div 
                key={job.id ?? idx}
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
                  <span style={{ fontSize: 11, color: "#666" }}>
                    {job.worker ? `Agent #${job.worker}` : "Unassigned"}
                  </span>
                </div>
                <div style={{ fontSize: 11, color: "#555", marginBottom: 4 }}>
                  Target: [{job.target?.[0]}, {job.target?.[1]}]
                  {job.from_pos && ` ← [${job.from_pos[0]}, ${job.from_pos[1]}]`}
                </div>
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
          })}
        </div>
      )}
    </div>
   );
  }
