type TreeSpreadControlsProps = {
  spreadProbability: number;
  minInterval: number;
  maxInterval: number;
  onSpreadProbabilityChange: (value: number) => void;
  onMinIntervalChange: (value: number) => void;
  onMaxIntervalChange: (value: number) => void;
  onApply: () => void;
};

const sliderStyle = { display: "flex", gap: 8, alignItems: "center" } as const;

export function TreeSpreadControls({
  spreadProbability,
  minInterval,
  maxInterval,
  onSpreadProbabilityChange,
  onMinIntervalChange,
  onMaxIntervalChange,
  onApply,
}: TreeSpreadControlsProps) {
  return (
    <div style={{ marginBottom: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
      <div style={{ marginBottom: 6 }}>
        <strong>Tree Spread</strong>
      </div>
      <div style={{ display: "grid", gap: 8 }}>
        <label style={sliderStyle}>
          Spread probability
          <input type="range" min={0} max={1} step={0.01} value={spreadProbability} onChange={(e) => onSpreadProbabilityChange(Number(e.target.value))} />
          <span>{spreadProbability.toFixed(2)}</span>
        </label>
        <label style={sliderStyle}>
          Min interval (ticks)
          <input type="range" min={1} max={100} step={1} value={minInterval} onChange={(e) => onMinIntervalChange(Number(e.target.value))} />
          <span>{minInterval}</span>
        </label>
        <label style={sliderStyle}>
          Max interval (ticks)
          <input type="range" min={10} max={200} step={1} value={maxInterval} onChange={(e) => onMaxIntervalChange(Number(e.target.value))} />
          <span>{maxInterval}</span>
        </label>
        <button onClick={onApply}>Apply tree spread settings</button>
      </div>
    </div>
  );
}
