type BuildControlsProps = {
  onPlaceWallGhost: (pos: [number, number]) => void;
  onModeChange: (mode: "select" | "build") => void;
  buildMode: boolean;
};

export function BuildControls({
  onPlaceWallGhost,
  onModeChange,
  buildMode,
}: BuildControlsProps) {
  return (
    <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
      <button
        onClick={() => onModeChange("select")}
        style={{
          backgroundColor: buildMode ? "#ccc" : "#4CAF50",
        }}
      >
        Select Mode
      </button>
      <button
        onClick={() => onModeChange("build")}
        style={{
          backgroundColor: buildMode ? "#4CAF50" : "#ccc",
        }}
      >
        Build Wall Mode
      </button>
    </div>
  );
}
