type JobQueuePanelProps = {
  jobs: any[];
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

export function JobQueuePanel({ jobs }: JobQueuePanelProps) {
  const activeJobs = jobs.filter((j: any) => j.state !== ":completed");
  
  if (activeJobs.length === 0) {
    return (
      <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8, opacity: 0.6 }}>
        <strong>Job Queue</strong>
        <div style={{ fontSize: 13, color: "#666", marginTop: 4 }}>No active jobs</div>
      </div>
    );
  }

  return (
    <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <strong>Job Queue ({activeJobs.length})</strong>
      <div style={{ display: "flex", flexDirection: "column", gap: 6, marginTop: 8 }}>
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
    </div>
  );
}
