 import { useState, useCallback } from "react";
 import { toggleMute, isMuted, markUserInteraction, playTone } from "../audio";

 type TickControlsProps = {
   onTick: (amount: number) => void;
   onReset: () => void;
   isRunning: boolean;
   onToggleRun: () => void;
   tick: number;
   fps: number;
   onSetFps: (value: number) => void;
 };
 
  export function TickControls({
    onTick,
    onReset,
    isRunning,
    onToggleRun,
    tick,
    fps,
    onSetFps,
  }: TickControlsProps) {
   const [localMuted, setLocalMuted] = useState(isMuted());
  const [tickDisabled, setTickDisabled] = useState(false);

  const handleToggleMute = () => {
    markUserInteraction();
    const newState = toggleMute();
    setLocalMuted(newState);
    if (!newState) {
      playTone(440, 0.1);
    }
  };

  const handleToggleRun = () => {
    markUserInteraction();
    onToggleRun();
    if (isRunning) {
      playTone(293.66, 0.15);
    } else {
      playTone(392.00, 0.15);
    }
  };

  const handleTick = useCallback((amount: number) => {
    if (tickDisabled) return;
    markUserInteraction();
    setTickDisabled(true);
    onTick(amount);
    setTimeout(() => setTickDisabled(false), 200);
  }, [onTick, tickDisabled]);

  const handleReset = useCallback(() => {
    markUserInteraction();
    onReset();
  }, [onReset]);

  const handleFpsChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    markUserInteraction();
    const val = parseInt(e.target.value, 10);
    if (!isNaN(val)) onSetFps(val);
  }, [onSetFps]);

   return (
      <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
          <h3 style={{ margin: 0, fontSize: 14 }}>Tick Controls</h3>
          <div style={{ fontSize: 16, fontWeight: "bold", color: "#333" }}>
            Tick: {tick}
          </div>
        </div>
         <div style={{ display: "flex", gap: 8, flexWrap: "wrap", marginBottom: 12 }}>
          <button onClick={handleToggleRun} title="Spacebar">
            {isRunning ? "⏸ Pause" : "▶ Play"}
          </button>
          <button onClick={() => onTick(1)}>Tick</button>
          <button onClick={() => onTick(10)}>Tick×10</button>
          <button onClick={onReset}>Reset</button>
          <button onClick={handleToggleMute} title={localMuted ? "Unmute" : "Mute"}>
            {localMuted ? "🔇" : "🔊"}
          </button>
        </div>
        <div style={{ display: "flex", gap: 8, alignItems: "center", paddingTop: 8, borderTop: "1px solid #eee" }}>
          <label style={{ fontSize: 12 }}>
            {fps} FPS:
          </label>
          <input
            type="range"
            min={1}
            max={120}
            value={fps}
            onChange={(e) => {
              const val = parseInt(e.target.value, 10);
              if (!isNaN(val)) onSetFps(val);
            }}
            style={{ flex: 1 }}
          />
        </div>
      </div>
    );
 }

