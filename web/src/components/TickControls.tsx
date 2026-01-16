type TickControlsProps = {
  onTick: (amount: number) => void;
  onReset: () => void;
  onPlaceShrine: () => void;
  onSetMouthpiece: () => void;
  canPlaceShrine: boolean;
  canSetMouthpiece: boolean;
};

export function TickControls({
  onTick,
  onReset,
  onPlaceShrine,
  onSetMouthpiece,
  canPlaceShrine,
  canSetMouthpiece,
}: TickControlsProps) {
  return (
    <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
      <button onClick={() => onTick(1)}>Tick</button>
      <button onClick={() => onTick(10)}>Tick×10</button>
      <button onClick={onReset}>Reset</button>
      <button onClick={onPlaceShrine} disabled={!canPlaceShrine}>
        Place shrine @ selected
      </button>
      <button onClick={onSetMouthpiece} disabled={!canSetMouthpiece}>
        Set mouthpiece = agent
      </button>
    </div>
  );
}
