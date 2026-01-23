import React from "react";

type LeverControlsProps = {
  tick: number;
  fireToPatron: number;
  lightningToStorm: number;
  stormToDeity: number;
  onFireChange: (value: number) => void;
  onLightningChange: (value: number) => void;
  onStormChange: (value: number) => void;
  onApply: () => void;
};

const sliderStyle = { display: "flex", gap: 8, alignItems: "center" } as const;

export function LeverControls({
  tick,
  fireToPatron,
  lightningToStorm,
  stormToDeity,
  onFireChange,
  onLightningChange,
  onStormChange,
  onApply,
}: LeverControlsProps) {
  return (
    <div style={{ marginBottom: 12 }}>
      <div style={{ marginBottom: 6 }}>
        <strong>Tick:</strong> {tick}
      </div>
      <div style={{ display: "grid", gap: 8 }}>
        <label style={sliderStyle}>
          fire→patron
          <input type="range" min={0} max={1} step={0.01} value={fireToPatron} onChange={(e) => onFireChange(Number(e.target.value))} />
          <span>{fireToPatron.toFixed(2)}</span>
        </label>
        <label style={sliderStyle}>
          lightning→storm
          <input type="range" min={0} max={1} step={0.01} value={lightningToStorm} onChange={(e) => onLightningChange(Number(e.target.value))} />
          <span>{lightningToStorm.toFixed(2)}</span>
        </label>
        <label style={sliderStyle}>
          storm→deity
          <input type="range" min={0} max={1} step={0.01} value={stormToDeity} onChange={(e) => onStormChange(Number(e.target.value))} />
          <span>{stormToDeity.toFixed(2)}</span>
        </label>
        <button onClick={onApply}>Apply levers</button>
      </div>
    </div>
  );
}
