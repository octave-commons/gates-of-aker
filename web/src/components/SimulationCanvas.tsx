import { useEffect, useRef } from "react";
import type { MouseEvent } from "react";
import { Agent, hasPos } from "../types";

type SimulationCanvasProps = {
  snapshot: any;
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
};

const GRID_SIZE = 20;
const CANVAS_SIZE = 480;

export function SimulationCanvas({ snapshot, selectedCell, selectedAgentId, onCellSelect }: SimulationCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !snapshot) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const cell = Math.floor(CANVAS_SIZE / GRID_SIZE);
    canvas.width = CANVAS_SIZE;
    canvas.height = CANVAS_SIZE;

    ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

    ctx.globalAlpha = 0.25;
    ctx.strokeStyle = "#777";
    for (let x = 0; x < GRID_SIZE; x++) {
      for (let y = 0; y < GRID_SIZE; y++) {
        ctx.strokeRect(x * cell, y * cell, cell, cell);
      }
    }
    ctx.globalAlpha = 1;
    ctx.strokeStyle = "#111";

    if (Array.isArray(snapshot.shrine) && snapshot.shrine.length === 2) {
      const [sx, sy] = snapshot.shrine;
      ctx.fillRect(sx * cell + cell * 0.2, sy * cell + cell * 0.2, cell * 0.6, cell * 0.6);
    }

    if (selectedCell) {
      const [selX, selY] = selectedCell;
      ctx.strokeRect(selX * cell + 2, selY * cell + 2, cell - 4, cell - 4);
    }

    const colorForRole = (role?: string) => {
      switch (role) {
        case "priest":
          return "#d7263d";
        case "knight":
          return "#3366ff";
        default:
          return "#111";
      }
    };

    for (const agent of snapshot.agents ?? []) {
      if (!hasPos(agent)) continue;
      const [ax, ay] = agent.pos;
      ctx.beginPath();
      ctx.fillStyle = colorForRole(agent.role);
      ctx.arc(ax * cell + cell / 2, ay * cell + cell / 2, cell * 0.27, 0, Math.PI * 2);
      ctx.fill();

      if (agent.id === selectedAgentId) {
        ctx.beginPath();
        ctx.strokeStyle = "#ffae00";
        ctx.arc(ax * cell + cell / 2, ay * cell + cell / 2, cell * 0.4, 0, Math.PI * 2);
        ctx.stroke();
        ctx.strokeStyle = "#111";
      }
    }
  }, [snapshot, selectedCell, selectedAgentId]);

  const handleClick = (event: MouseEvent<HTMLCanvasElement>) => {
    if (!snapshot) return;
    const rect = (event.target as HTMLCanvasElement).getBoundingClientRect();
    const x = Math.floor(((event.clientX - rect.left) / rect.width) * GRID_SIZE);
    const y = Math.floor(((event.clientY - rect.top) / rect.height) * GRID_SIZE);
    const cell: [number, number] = [x, y];
    const hit = (snapshot.agents ?? []).find((a: Agent) => hasPos(a) && a.pos[0] === x && a.pos[1] === y);
    onCellSelect(cell, hit ? hit.id : null);
  };

  return (
    <canvas
      data-testid="simulation-canvas"
      ref={canvasRef}
      style={{ border: "1px solid #aaa", borderRadius: 8, cursor: "crosshair" }}
      onClick={handleClick}
    />
  );
}
