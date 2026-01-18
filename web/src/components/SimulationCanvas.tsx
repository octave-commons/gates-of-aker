import { useEffect, useRef } from "react";
import type { MouseEvent } from "react";
import { Agent, hasPos } from "../types";
import type { HexConfig } from "../hex";
import { axialToPixel, pixelToAxial, hexCorner, getMapBoundsInPixels, type AxialCoords } from "../hex";

type SimulationCanvasProps = {
  snapshot: any;
  mapConfig: HexConfig | null;
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
};

const HEX_SIZE = 16;
const HEX_SPACING = 1;

export function SimulationCanvas({ snapshot, mapConfig, selectedCell, selectedAgentId, onCellSelect }: SimulationCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !snapshot || !mapConfig) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const hasCanvasMethods = typeof ctx.save === "function" && typeof ctx.translate === "function" && typeof ctx.restore === "function";
    if (!hasCanvasMethods) {
      return;
    }

    const { width: mapWidth, height: mapHeight } = getMapBoundsInPixels(mapConfig.bounds, HEX_SIZE + HEX_SPACING);
    const padding = HEX_SIZE * 2;

    canvas.width = mapWidth + padding * 2;
    canvas.height = mapHeight + padding * 2;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.translate(padding, padding);

    const size = HEX_SIZE + HEX_SPACING;
    const origin = mapConfig.bounds.origin ?? [0, 0] as AxialCoords;
    const { bounds } = mapConfig;

    const hexesToDraw: AxialCoords[] = [];

    if (bounds.shape === "rect") {
      for (let q = 0; q < bounds.w; q++) {
        for (let r = 0; r < bounds.h; r++) {
          hexesToDraw.push([origin[0] + q, origin[1] + r]);
        }
      }
    } else if (bounds.shape === "radius") {
      const { r: radius } = bounds;
      for (let q = -radius; q <= radius; q++) {
        const r1 = Math.max(-radius, -q - radius);
        const r2 = Math.min(radius, -q + radius);
        for (let r = r1; r <= r2; r++) {
          hexesToDraw.push([origin[0] + q, origin[1] + r]);
        }
      }
    }

    ctx.globalAlpha = 0.25;
    ctx.strokeStyle = "#777";
    ctx.lineWidth = 1;

    for (const hex of hexesToDraw) {
      const [px, py] = axialToPixel(hex, size);
      ctx.beginPath();
      for (let i = 0; i < 6; i++) {
        const [cx, cy] = hexCorner([px, py], HEX_SIZE, i);
        if (i === 0) {
          ctx.moveTo(cx, cy);
        } else {
          ctx.lineTo(cx, cy);
        }
      }
      ctx.closePath();
      ctx.stroke();

       const tileKey = `${hex[0]},${hex[1]}`;
       const tile = snapshot.tiles?.[tileKey];
       if (tile?.resource === "tree") {
         ctx.fillStyle = "#2e7d32";
         ctx.beginPath();
         ctx.arc(px, py, HEX_SIZE * 0.4, 0, Math.PI * 2);
         ctx.fill();
       }
       if (tile?.structure === "wall-ghost") {
         ctx.strokeStyle = "#ffae00";
         ctx.lineWidth = 2;
         ctx.setLineDash([4, 2]);
         ctx.beginPath();
         for (let i = 0; i < 6; i++) {
           const [cx, cy] = hexCorner([px, py], HEX_SIZE - 4, i);
           if (i === 0) {
             ctx.moveTo(cx, cy);
           } else {
             ctx.lineTo(cx, cy);
           }
         }
         ctx.closePath();
         ctx.stroke();
         ctx.setLineDash([]);
         ctx.lineWidth = 1;
       }
       if (tile?.structure === "wall") {
         ctx.fillStyle = "#666";
         ctx.beginPath();
         for (let i = 0; i < 6; i++) {
           const [cx, cy] = hexCorner([px, py], HEX_SIZE - 3, i);
           if (i === 0) {
             ctx.moveTo(cx, cy);
           } else {
             ctx.lineTo(cx, cy);
           }
         }
         ctx.closePath();
         ctx.fill();
         ctx.strokeStyle = "#444";
         ctx.lineWidth = 1;
         ctx.stroke();
       }
     }
    ctx.globalAlpha = 1;
    ctx.strokeStyle = "#111";

    if (Array.isArray(snapshot.shrine) && snapshot.shrine.length === 2) {
      const [sq, sr] = snapshot.shrine as AxialCoords;
      const [sx, sy] = axialToPixel([sq, sr], size);
      ctx.fillStyle = "#ffae00";
      ctx.beginPath();
      ctx.arc(sx, sy, HEX_SIZE * 0.5, 0, Math.PI * 2);
      ctx.fill();
    }

    const stockpiles = snapshot.stockpiles ?? {};
    for (const [tileKey, stockpile] of Object.entries(stockpiles)) {
      const [q, r] = tileKey.split(",").map(Number) as [number, number];
      const [sx, sy] = axialToPixel([q, r], size);
      const sp = stockpile as { resource: string; maxQty: number; currentQty: number };
      const fillLevel = sp.currentQty / sp.maxQty;
      
      const stockpileColor = (resource: string) => {
        switch (resource) {
          case "wood":
            return "#8d6e63";
          case "food":
            return "#ff9800";
          default:
            return "#9e9e9e";
        }
      };

      ctx.fillStyle = stockpileColor(sp.resource);
      ctx.fillRect(sx - HEX_SIZE * 0.4, sy - HEX_SIZE * 0.4, HEX_SIZE * 0.8, HEX_SIZE * 0.8);
      ctx.strokeStyle = "#333";
      ctx.lineWidth = 1;
      ctx.strokeRect(sx - HEX_SIZE * 0.4, sy - HEX_SIZE * 0.4, HEX_SIZE * 0.8, HEX_SIZE * 0.8);
      
      ctx.fillStyle = "white";
      ctx.font = "8px sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(`${sp.currentQty}/${sp.maxQty}`, sx, sy);
    }

    if (selectedCell) {
      const [selQ, selR] = selectedCell;
      const [sx, sy] = axialToPixel([selQ, selR], size);
      ctx.strokeStyle = "#ff6b00";
      ctx.lineWidth = 2;
      ctx.beginPath();
      for (let i = 0; i < 6; i++) {
        const [cx, cy] = hexCorner([sx, sy], HEX_SIZE - 2, i);
        if (i === 0) {
          ctx.moveTo(cx, cy);
        } else {
          ctx.lineTo(cx, cy);
        }
      }
      ctx.closePath();
      ctx.stroke();
      ctx.lineWidth = 1;
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
      const [aq, ar] = agent.pos as AxialCoords;
      const [ax, ay] = axialToPixel([aq, ar], size);
      ctx.beginPath();
      ctx.fillStyle = colorForRole(agent.role);
      ctx.arc(ax, ay, HEX_SIZE * 0.35, 0, Math.PI * 2);
      ctx.fill();

      if (agent.id === selectedAgentId) {
        ctx.beginPath();
        ctx.strokeStyle = "#ffae00";
        ctx.lineWidth = 2;
        ctx.arc(ax, ay, HEX_SIZE * 0.55, 0, Math.PI * 2);
        ctx.stroke();
        ctx.lineWidth = 1;
      }
    }

    ctx.restore();
  }, [snapshot, mapConfig, selectedCell, selectedAgentId]);

  const handleClick = (event: MouseEvent<HTMLCanvasElement>) => {
    if (!snapshot || !mapConfig) return;
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const padding = HEX_SIZE * 2;

    const x = (event.clientX - rect.left) * scaleX;
    const y = (event.clientY - rect.top) * scaleY;

    const [q, r] = pixelToAxial(x - padding, y - padding, HEX_SIZE + HEX_SPACING);
    const cell: AxialCoords = [q, r];

    const hit = (snapshot.agents ?? []).find((a: Agent) => {
      if (!hasPos(a)) return false;
      const [aq, ar] = a.pos as AxialCoords;
      return aq === cell[0] && ar === cell[1];
    });
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
