type StatusBarProps = {
  status: "open" | "closed" | "error";
};

export function StatusBar({ status }: StatusBarProps) {
  return (
    <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 8 }}>
      <strong>Fantasia — Myth Debugger</strong>
      <span style={{ opacity: 0.7 }}>WS: {status}</span>
    </div>
  );
}
