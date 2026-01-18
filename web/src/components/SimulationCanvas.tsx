import { useEffect, useRef, useState } from "react";
import type { MouseEvent, WheelEvent } from "react";
import { Agent, hasPos } from "../types";
import type { HexConfig } from "../hex";
import { axialToPixel, pixelToAxial, hexCorner, getMapBoundsInPixels, type AxialCoords } from "../hex";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type SimulationCanvasProps = {
  snapshot: any;
  mapConfig: HexConfig | null;
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
};

const HEX_SIZE = 16;
const HEX_SPACING = 1;

const ZOOM_MIN = 0.1;
const ZOOM_MAX = 5.0;
const ZOOM_STEP = 0.1;
const PAN_SPEED = 10;

export function SimulationCanvas({ snapshot, mapConfig, selectedCell, selectedAgentId, onCellSelect }: SimulationCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const [camera, setCamera] = useState<CameraState>({
    offsetX: 0,
    offsetY: 0,
    zoom: 1.0,
  });

  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState<[number, number] | null>(null);
  const [cameraStart, setCameraStart] = useState<CameraState | null>(null);

  const keysPressed = useRef<Set<string>>(new Set());

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (["KeyW", "KeyA", "KeyS", "KeyD"].includes(e.code)) {
        keysPressed.current.add(e.code);
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      if (["KeyW", "KeyA", "KeyS", "KeyD"].includes(e.code)) {
        keysPressed.current.delete(e.code);
      }
    };

    const canvas = canvasRef.current;
    if (canvas) {
      canvas.setAttribute("tabIndex", "0");
    }

    window.addEventListener("keydown", handleKeyDown, { passive: false });
    window.addEventListener("keyup", handleKeyUp, { passive: false });
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, []);

  useEffect(() => {
    let animationFrameId: number;

    const handleCameraMovement = () => {
      const keys = Array.from(keysPressed.current);
      if (keys.length > 0) {
        setCamera((prev) => {
          let newOffsetX = prev.offsetX;
          let newOffsetY = prev.offsetY;
          const moveAmount = PAN_SPEED / prev.zoom;

          if (keys.includes("KeyW")) newOffsetY += moveAmount;
          if (keys.includes("KeyS")) newOffsetY -= moveAmount;
          if (keys.includes("KeyA")) newOffsetX += moveAmount;
          if (keys.includes("KeyD")) newOffsetX -= moveAmount;

          if (newOffsetX !== prev.offsetX || newOffsetY !== prev.offsetY) {
            return { ...prev, offsetX: newOffsetX, offsetY: newOffsetY };
          }
          return prev;
        });
      }
      animationFrameId = requestAnimationFrame(handleCameraMovement);
    };

    animationFrameId = requestAnimationFrame(handleCameraMovement);
    return () => cancelAnimationFrame(animationFrameId);
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container || !snapshot || !mapConfig) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    const hasCanvasMethods = typeof ctx.save === "function" && typeof ctx.translate === "function" && typeof ctx.restore === "function" && typeof ctx.scale === "function";
    if (!hasCanvasMethods) {
      return;
    }

    const { width: mapWidth, height: mapHeight } = getMapBoundsInPixels(mapConfig.bounds, HEX_SIZE + HEX_SPACING);
    const padding = HEX_SIZE * 4;

    const containerRect = container.getBoundingClientRect();
    canvas.width = containerRect.width;
    canvas.height = containerRect.height;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.save();

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    ctx.translate(centerX, centerY);
    ctx.scale(camera.zoom, camera.zoom);
    ctx.translate(camera.offsetX, camera.offsetY);

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

    const biomeColors: Record<string, string> = {
      forest: "#2e7d32",
      village: "#8d6e63",
      field: "#9e9e24",
      rocky: "#616161"
    };

    ctx.globalAlpha = 0.4;
    ctx.strokeStyle = "#777";
    ctx.lineWidth = 1;

    for (const hex of hexesToDraw) {
      const tileKey = `${hex[0]},${hex[1]}`;
      const tile = snapshot.tiles?.[tileKey];

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

      const biomeColor = tile?.biome ? biomeColors[tile.biome as string] : null;
      if (biomeColor) {
        ctx.fillStyle = biomeColor;
        ctx.fill();
      }
      ctx.stroke();

       if (tile?.resource === "tree") {
         ctx.fillStyle = "#2e7d32";
         ctx.beginPath();
         ctx.arc(px, py, HEX_SIZE * 0.4, 0, Math.PI * 2);
         ctx.fill();
       }
       if (tile?.resource === "grain") {
         ctx.fillStyle = "#ffeb3b";
         ctx.beginPath();
         ctx.arc(px, py, HEX_SIZE * 0.25, 0, Math.PI * 2);
         ctx.fill();
       }
       if (tile?.resource === "rock") {
         ctx.fillStyle = "#757575";
         ctx.beginPath();
         ctx.rect(px - HEX_SIZE * 0.3, py - HEX_SIZE * 0.3, HEX_SIZE * 0.6, HEX_SIZE * 0.5);
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
    const x = event.clientX - rect.left;
    const y = event.clientY - rect.top;

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    const worldX = (x - centerX) / camera.zoom - camera.offsetX;
    const worldY = (y - centerY) / camera.zoom - camera.offsetY;

    const [q, r] = pixelToAxial(worldX, worldY, HEX_SIZE + HEX_SPACING);
    const cell: AxialCoords = [q, r];

    const hit = (snapshot.agents ?? []).find((a: Agent) => {
      if (!hasPos(a)) return false;
      const [aq, ar] = a.pos as AxialCoords;
      return aq === cell[0] && ar === cell[1];
    });
    onCellSelect(cell, hit ? hit.id : null);
  };

  const handleWheel = (event: WheelEvent<HTMLCanvasElement>) => {
    event.preventDefault();
    if (!canvasRef.current) return;

    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    const mouseX = event.clientX - rect.left;
    const mouseY = event.clientY - rect.top;

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    const worldX = (mouseX - centerX) / camera.zoom - camera.offsetX;
    const worldY = (mouseY - centerY) / camera.zoom - camera.offsetY;

    const zoomDelta = event.deltaY > 0 ? -ZOOM_STEP : ZOOM_STEP;
    const newZoom = Math.max(ZOOM_MIN, Math.min(ZOOM_MAX, camera.zoom + zoomDelta));

    const newOffsetX = worldX - (mouseX - centerX) / newZoom;
    const newOffsetY = worldY - (mouseY - centerY) / newZoom;

    setCamera({ ...camera, zoom: newZoom, offsetX: newOffsetX, offsetY: newOffsetY });
  };

  const handleMouseDown = (event: MouseEvent<HTMLCanvasElement>) => {
    if (event.button === 1) {
      event.preventDefault();
      setIsDragging(true);
      setDragStart([event.clientX, event.clientY]);
      setCameraStart({ ...camera });
    }
  };

  const handleMouseMove = (event: MouseEvent<HTMLCanvasElement>) => {
    if (!isDragging || !dragStart || !cameraStart) return;

    const dx = event.clientX - dragStart[0];
    const dy = event.clientY - dragStart[1];

    const newOffsetX = cameraStart.offsetX + dx / camera.zoom;
    const newOffsetY = cameraStart.offsetY + dy / camera.zoom;

    setCamera({ ...camera, offsetX: newOffsetX, offsetY: newOffsetY });
  };

  const handleMouseUp = () => {
    setIsDragging(false);
    setDragStart(null);
    setCameraStart(null);
  };

  const handleContextMenu = (event: MouseEvent<HTMLCanvasElement>) => {
    event.preventDefault();
  };

  return (
    <div
      ref={containerRef}
      style={{ position: "relative", width: "100%", height: "100%", overflow: "hidden" }}
    >
      <canvas
        data-testid="simulation-canvas"
        ref={canvasRef}
        style={{ display: "block", width: "100%", height: "100%", cursor: isDragging ? "grabbing" : "crosshair" }}
        onClick={handleClick}
        onWheel={handleWheel}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onContextMenu={handleContextMenu}
      />
    </div>
  );
}
