type StatusBarProps = {
  status: "open" | "closed" | "error";
  tickHealth?: {
    targetMs: number;
    tickMs: number;
    health: "healthy" | "degraded" | "unhealthy" | "unknown";
  } | null;
};

export function StatusBar({ status, tickHealth }: StatusBarProps) {
  const healthColor = {
    healthy: "#22c55e",
    degraded: "#eab308",
    unhealthy: "#ef4444",
    unknown: "#666",
  } as const;

  const healthIcon = {
    healthy: "✓",
    degraded: "⚠",
    unhealthy: "✗",
    unknown: "?",
  } as const;

  const getHealthIndicator = () => {
    if (!tickHealth || tickHealth.tickMs == null || tickHealth.targetMs == null) {
      return null;
    }

    const color = healthColor[tickHealth.health];
    const icon = healthIcon[tickHealth.health];

    return (
      <span style={{ opacity: 0.7, marginLeft: 8 }}>
        Health: <span style={{ color, fontWeight: "bold" }}>{icon}</span>
        <span style={{ fontSize: "0.85em", marginLeft: 4 }}>
          ({tickHealth.tickMs.toFixed(1)}ms / {tickHealth.targetMs.toFixed(1)}ms)
        </span>
      </span>
    );
  };

  return (
    <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 8 }}>
      <strong>Fantasia — Myth Debugger</strong>
      <span style={{ opacity: 0.7 }}>WS: {status}</span>
      {getHealthIndicator()}
    </div>
  );
}
