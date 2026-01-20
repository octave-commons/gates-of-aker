import { useState } from "react";
import { toggleMute, isMuted, markUserInteraction, playTone } from "../audio";

 type TickControlsProps = {
   onTick: (amount: number) => void;
   onReset: () => void;
   onPlaceShrine: () => void;
   onSetMouthpiece: () => void;
   canPlaceShrine: boolean;
   canSetMouthpiece: boolean;
   isRunning: boolean;
   onToggleRun: () => void;
 };
 
 export function TickControls({
   onTick,
   onReset,
   onPlaceShrine,
   onSetMouthpiece,
   canPlaceShrine,
   canSetMouthpiece,
   isRunning,
   onToggleRun,
 }: TickControlsProps) {
  const [localMuted, setLocalMuted] = useState(isMuted());

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

   return (
     <div style={{ display: "flex", gap: 8, marginBottom: 8, flexWrap: "wrap" }}>
       <button onClick={handleToggleRun} title="Spacebar">
         {isRunning ? "⏸ Pause" : "▶ Play"}
       </button>
       <button onClick={() => onTick(1)}>Tick</button>
       <button onClick={() => onTick(10)}>Tick×10</button>
       <button onClick={onReset}>Reset</button>
       <button onClick={onPlaceShrine} disabled={!canPlaceShrine}>
         Place shrine @ selected
       </button>
       <button onClick={onSetMouthpiece} disabled={!canSetMouthpiece}>
         Set mouthpiece = agent
       </button>
       <button onClick={handleToggleMute} title={localMuted ? "Unmute" : "Mute"}>
         {localMuted ? "🔇" : "🔊"}
       </button>
     </div>
   );
 }

