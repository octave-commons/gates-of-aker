import { useEffect, useRef, useState } from "react";
import type { MouseEvent } from "react";
import { Agent, hasPos, PathPoint } from "../types";
import type { HexConfig } from "../hex";
import { axialToPixel, pixelToAxial, hexCorner, getMapBoundsInPixels, type AxialCoords } from "../hex";
import { hexToFrequency, playTone } from "../audio";
import { colorForRole, getAgentIcon } from "../utils";
import { CONFIG } from "../config/constants";

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
  agentPaths: Record<number, PathPoint[]>;
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
};

export function SimulationCanvas({ snapshot, mapConfig, selectedCell, selectedAgentId, agentPaths, onCellSelect }: SimulationCanvasProps) {
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
          const moveAmount = CONFIG.canvas.PAN_SPEED / prev.zoom;

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
    if (canvas) {
      const handleWheel = (event: globalThis.WheelEvent) => {
        event.preventDefault();
        if (!snapshot || !mapConfig || !canvasRef.current) return;

        const rect = canvas.getBoundingClientRect();
        const mouseX = event.clientX - rect.left;
        const mouseY = event.clientY - rect.top;

        const centerX = canvas.width / 2;
        const centerY = canvas.height / 2;

        const worldX = (mouseX - centerX) / camera.zoom - camera.offsetX;
        const worldY = (mouseY - centerY) / camera.zoom - camera.offsetY;

        const zoomDelta = event.deltaY > 0 ? -CONFIG.canvas.ZOOM_STEP : CONFIG.canvas.ZOOM_STEP;
        const newZoom = Math.max(CONFIG.canvas.ZOOM_MIN, Math.min(CONFIG.canvas.ZOOM_MAX, camera.zoom + zoomDelta));

        const newOffsetX = worldX - (mouseX - centerX) / newZoom;
        const newOffsetY = worldY - (mouseY - centerY) / newZoom;

        setCamera({ ...camera, zoom: newZoom, offsetX: newOffsetX, offsetY: newOffsetY });
      };
      canvas.addEventListener("wheel", handleWheel, { passive: false });
      return () => canvas.removeEventListener("wheel", handleWheel);
    }
  }, [camera, snapshot, mapConfig]);

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

    const { width: mapWidth, height: mapHeight } = getMapBoundsInPixels(mapConfig.bounds, CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING);
    const padding = CONFIG.canvas.HEX_SIZE * 4;

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

    const size = CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING;
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
      forest: CONFIG.colors.BIOME.forest,
      village: CONFIG.colors.BIOME.village,
      field: CONFIG.colors.BIOME.field,
      rocky: CONFIG.colors.BIOME.rocky
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
        const [cx, cy] = hexCorner([px, py], CONFIG.canvas.HEX_SIZE, i);
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
          ctx.fillStyle = CONFIG.colors.RESOURCE.tree;
          ctx.beginPath();
          ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.4, 0, Math.PI * 2);
          ctx.fill();
        }
        if (tile?.resource === "grain") {
          ctx.fillStyle = CONFIG.colors.RESOURCE.grain;
          ctx.beginPath();
          ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.25, 0, Math.PI * 2);
          ctx.fill();
        }
        if (tile?.resource === "rock") {
          ctx.fillStyle = CONFIG.colors.RESOURCE.rock;
          ctx.beginPath();
          ctx.rect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.5);
          ctx.fill();
        }
        if (tile?.structure === "wall-ghost") {
          ctx.strokeStyle = CONFIG.colors.STRUCTURE.wallGhost;
          ctx.lineWidth = 2;
          ctx.setLineDash([4, 2]);
         ctx.beginPath();
         for (let i = 0; i < 6; i++) {
           const [cx, cy] = hexCorner([px, py], CONFIG.canvas.HEX_SIZE - 4, i);
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
           ctx.fillStyle = CONFIG.colors.STRUCTURE.wall;
           ctx.beginPath();
           for (let i = 0; i < 6; i++) {
             const [cx, cy] = hexCorner([px, py], CONFIG.canvas.HEX_SIZE - 3, i);
             if (i === 0) {
               ctx.moveTo(cx, cy);
             } else {
               ctx.lineTo(cx, cy);
             }
           }
           ctx.closePath();
           ctx.fill();
           ctx.strokeStyle = CONFIG.colors.STRUCTURE.wallStroke;
           ctx.lineWidth = 1;
           ctx.stroke();
         }

         // Render additional structures
         if (tile?.structure === "campfire") {
           ctx.fillStyle = "#ff6b00";
           ctx.beginPath();
           ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.3, 0, Math.PI * 2);
           ctx.fill();
           
           // Add flames effect
           ctx.fillStyle = "#ffa726";
           ctx.beginPath();
           ctx.arc(px - 2, py - 2, CONFIG.canvas.HEX_SIZE * 0.15, 0, Math.PI * 2);
           ctx.fill();
         }

         if (tile?.structure === "statue/dog") {
           ctx.fillStyle = "#9e9e9e";
           ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.6);
           ctx.strokeStyle = "#616161";
           ctx.lineWidth = 1;
           ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.6);
         }

         if (tile?.structure === "warehouse") {
           ctx.fillStyle = CONFIG.colors.STRUCTURE.wall;
           ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.8, CONFIG.canvas.HEX_SIZE * 0.6);
           ctx.strokeStyle = CONFIG.colors.STRUCTURE.wallStroke;
           ctx.lineWidth = 2;
           ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.8, CONFIG.canvas.HEX_SIZE * 0.6);
           
           // Add roof detail
           ctx.fillStyle = "#757575";
           ctx.beginPath();
           ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.5, py - CONFIG.canvas.HEX_SIZE * 0.3);
           ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.5);
           ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.5, py - CONFIG.canvas.HEX_SIZE * 0.3);
           ctx.closePath();
           ctx.fill();
         }
     }
     ctx.globalAlpha = 1;
     ctx.strokeStyle = "#111";

     if (Array.isArray(snapshot.shrine) && snapshot.shrine.length === 2) {
       const [sq, sr] = snapshot.shrine as AxialCoords;
       const [sx, sy] = axialToPixel([sq, sr], size);
       ctx.fillStyle = CONFIG.colors.SHRINE;
       ctx.beginPath();
       ctx.arc(sx, sy, CONFIG.canvas.HEX_SIZE * 0.5, 0, Math.PI * 2);
       ctx.fill();
     }

    const stockpiles = snapshot.stockpiles ?? {};
    for (const [tileKey, stockpile] of Object.entries(stockpiles)) {
      const [q, r] = tileKey.split(",").map(Number) as [number, number];
      const [sx, sy] = axialToPixel([q, r], size);
      const spRaw = stockpile as Record<string, unknown>;
      const normalizeResource = (val: unknown) => (typeof val === "string" ? val.replace(/^:/, "") : "unknown");
      const resource = normalizeResource(spRaw.resource ?? spRaw[":resource"]);
      const maxQty = Number(spRaw.maxQty ?? spRaw["max-qty"] ?? 0) || 0;
      const currentQty = Number(spRaw.currentQty ?? spRaw["current-qty"] ?? 0) || 0;
      const safeMaxQty = maxQty === 0 ? 1 : maxQty;
      const fillLevel = Math.min(1, Math.max(0, currentQty / safeMaxQty));
      
      const stockpileColor = (res: string) => {
         switch (res) {
           case "wood":
             return CONFIG.colors.RESOURCE.wood;
           case "food":
             return CONFIG.colors.RESOURCE.food;
           default:
             return CONFIG.colors.RESOURCE.unknown;
         }
       };

       ctx.fillStyle = stockpileColor(resource);
      ctx.fillRect(sx - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER, sy - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER, CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER * 2, CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER * 2);
      ctx.strokeStyle = "#333";
      ctx.lineWidth = 1;
      ctx.strokeRect(sx - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER, sy - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER, CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER * 2, CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_SIZE_MULTIPLIER * 2);

      ctx.fillStyle = "rgba(255, 255, 255, 0.35)";
      ctx.fillRect(
        sx - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_OFFSET_MULTIPLIER,
        sy + CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_FILL_BAR_OFFSET - CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILE_FILL_BAR_HEIGHT,
        CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILLE_BAR_WIDTH_MULTIPLIER * fillLevel,
        CONFIG.canvas.HEX_SIZE * CONFIG.ui.STOCKPILE_FILL_BAR_HEIGHT
      );

      ctx.fillStyle = "white";
      ctx.font = "8px sans-serif";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      ctx.fillText(`${currentQty}/${maxQty}`, sx, sy);
    }

    if (selectedCell) {
      const [selQ, selR] = selectedCell;
      const [sx, sy] = axialToPixel([selQ, selR], size);
      ctx.strokeStyle = CONFIG.colors.SELECTION;
      ctx.lineWidth = 2;
      ctx.beginPath();
      for (let i = 0; i < 6; i++) {
        const [cx, cy] = hexCorner([sx, sy], CONFIG.canvas.HEX_SIZE - 2, i);
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
      const agentId = agent.id;
      const path = agentPaths[agentId] ?? [];
      ctx.beginPath();
      const agentColor = colorForRole(agent.role);
      ctx.fillStyle = agentColor;
      ctx.arc(ax, ay, CONFIG.canvas.HEX_SIZE * 0.35, 0, Math.PI * 2);
      ctx.fill();

      // Draw agent icon
      ctx.fillStyle = "white";
      const fontSize = CONFIG.canvas.HEX_SIZE * 0.4;
      ctx.font = `${fontSize}px Arial`;
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      const icon = getAgentIcon(agent.role);
      ctx.fillText(icon, ax, ay);

      if (agent.id === selectedAgentId) {
        ctx.beginPath();
        ctx.strokeStyle = CONFIG.colors.SHRINE;
        ctx.lineWidth = 2;
        ctx.arc(ax, ay, CONFIG.canvas.HEX_SIZE * 0.55, 0, Math.PI * 2);
        ctx.stroke();
        ctx.lineWidth = 1;

        if (path.length > 1) {
          ctx.beginPath();
          ctx.strokeStyle = "rgba(100, 200, 255, 0.5)";
          ctx.lineWidth = 2;
          ctx.setLineDash([5, 5]);
          ctx.moveTo(ax, ay);
          for (let i = 1; i < path.length; i++) {
            const [pq, pr] = path[i];
            const [px, py] = axialToPixel([pq, pr], size);
            ctx.lineTo(px, py);
          }
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }
    }

    ctx.restore();
  }, [snapshot, mapConfig, selectedCell, selectedAgentId, camera]);

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

    const [q, r] = pixelToAxial(worldX, worldY, CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING);
    const cell: AxialCoords = [q, r];

    const hit = (snapshot.agents ?? []).find((a: Agent) => {
      if (!hasPos(a)) return false;
      const [aq, ar] = a.pos as AxialCoords;
      return aq === cell[0] && ar === cell[1];
    });
    onCellSelect(cell, hit ? hit.id : null);

    if (hit) {
      const color = colorForRole(hit.role);
      const frequency = hexToFrequency(color);
      playTone(frequency, 0.15);
    } else {
      playTone(330, 0.05);
    }
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
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onMouseUp={handleMouseUp}
        onMouseLeave={handleMouseUp}
        onContextMenu={handleContextMenu}
      />
    </div>
  );
}
