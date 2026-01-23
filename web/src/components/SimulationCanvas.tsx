import React, { useEffect, useRef, useState } from "react";
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

type SpeechBubble = {
  agentId: number;
  text: string;
  interactionType: string;
  timestamp: number;
};

type SimulationCanvasProps = {
  snapshot: any;
  mapConfig: HexConfig | null;
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
  agentPaths: Record<number, PathPoint[]>;
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
  focusPos?: [number, number] | null;
  focusTrigger?: number;
  showRelationships?: boolean;
  showNames?: boolean;
  showStats?: boolean;
  speechBubbles?: SpeechBubble[];
  visibilityData?: Record<string, any> | null;
  selectedVisibilityAgentId?: number | null;
  tileVisibility?: Record<string, "hidden" | "revealed" | "visible">;
  revealedTilesSnapshot?: Record<string, any>;
};

export function SimulationCanvas({
  snapshot,
  mapConfig,
  selectedCell,
  selectedAgentId,
  agentPaths,
  onCellSelect,
  focusPos,
  focusTrigger,
  showRelationships = true,
  showNames = true,
  showStats = true,
  speechBubbles = [],
  visibilityData = null,
  selectedVisibilityAgentId = null,
  tileVisibility = {},
  revealedTilesSnapshot = {},
}: SimulationCanvasProps) {
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

  const getTileVisibilityState = (q: number, r: number): "hidden" | "revealed" | "visible" => {
    const hasVisibilityData = tileVisibility && Object.keys(tileVisibility).length > 0;
    const tileKey = `${q},${r}`;
    const vis = tileVisibility[tileKey];

    if (!hasVisibilityData) {
      return "visible";
    }

    return vis ?? "hidden";
  };

  const isVisible = (entity: any, type: "agent" | "tile" | "item" | "stockpile") => {
    if (!selectedVisibilityAgentId || !visibilityData) return true;

    const selectedAgent = (snapshot.agents ?? []).find((a: Agent) => a.id === selectedVisibilityAgentId);
    if (!selectedAgent || !hasPos(selectedAgent)) return true;

    const viewerPos = selectedAgent.pos as AxialCoords;
    const viewerPosStr = `${viewerPos[0]},${viewerPos[1]}`;
    const visibilityMap = visibilityData[viewerPosStr];

    if (!visibilityMap) return true;

    switch (type) {
      case "agent": {
        const visibleAgentIds = visibilityMap.visible_agent_ids ?? [];
        return visibleAgentIds.includes(entity.id);
      }
      case "tile": {
        const visibleTiles = visibilityMap.visible_tiles ?? [];
        const tileKey = typeof entity.q === "number" ? `${entity.q},${entity.r}` : entity;
        return visibleTiles.includes(tileKey);
      }
      case "item": {
        const visibleItems = visibilityMap.visible_items ?? [];
        const itemKey = typeof entity.q === "number" ? `${entity.q},${entity.r}` : entity;
        return visibleItems.includes(itemKey);
      }
      case "stockpile": {
        const visibleStockpiles = visibilityMap.visible_stockpiles ?? [];
        const stockpileKey = typeof entity.q === "number" ? `${entity.q},${entity.r}` : entity;
        return visibleStockpiles.includes(stockpileKey);
      }
      default:
        return true;
    }
  };

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
    if (!canvas) return;

    const handleWheel = (event: globalThis.WheelEvent) => {
      event.preventDefault();
      const rect = canvas.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;

      if (event.deltaY === 0) return;
      const zoomFactor = Math.pow(1 + CONFIG.canvas.ZOOM_STEP, -Math.sign(event.deltaY));
      setCamera((prev) => {
        const centerX = rect.width / 2;
        const centerY = rect.height / 2;
        const worldX = (mouseX - centerX) / prev.zoom - prev.offsetX;
        const worldY = (mouseY - centerY) / prev.zoom - prev.offsetY;
        const newZoom = Math.max(CONFIG.canvas.ZOOM_MIN, Math.min(CONFIG.canvas.ZOOM_MAX, prev.zoom * zoomFactor));
        const newOffsetX = worldX - (mouseX - centerX) / newZoom;
        const newOffsetY = worldY - (mouseY - centerY) / newZoom;
        return { ...prev, zoom: newZoom, offsetX: newOffsetX, offsetY: newOffsetY };
      });
    };

    canvas.addEventListener("wheel", handleWheel, { passive: false });
    return () => canvas.removeEventListener("wheel", handleWheel);
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

    const { width: mapWidth, height: mapHeight } = getMapBoundsInPixels(mapConfig.bounds, CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING);
    const padding = CONFIG.canvas.HEX_SIZE * 4;

    const containerRect = container.getBoundingClientRect();
    const dpr = window.devicePixelRatio || 1;
    canvas.width = Math.max(1, Math.floor(containerRect.width * dpr));
    canvas.height = Math.max(1, Math.floor(containerRect.height * dpr));
    canvas.style.width = `${containerRect.width}px`;
    canvas.style.height = `${containerRect.height}px`;

    ctx.clearRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = "#333";
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.save();
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    const centerX = containerRect.width / 2;
    const centerY = containerRect.height / 2;
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
       forest: CONFIG.colors.BIOME?.forest ?? "#2e7d32",
       field: CONFIG.colors.BIOME?.field ?? "#9e9e24",
       rocky: CONFIG.colors.BIOME?.rocky ?? "#616161"
    };

     const isVisibilityFiltered = selectedVisibilityAgentId !== null && visibilityData !== null;
     ctx.globalAlpha = isVisibilityFiltered ? 0.2 : 0.4;
     const gridLineWidth = Math.max(0.5, 1 / camera.zoom);

         for (const hex of hexesToDraw) {
           const tileKey = `${hex[0]},${hex[1]}`;
           const visibilityState = getTileVisibilityState(hex[0], hex[1]);
           const tiles = snapshot.tiles ?? {};
           const tile = visibilityState === "revealed" ? revealedTilesSnapshot[tileKey] : tiles[tileKey];

        const [px, py] = axialToPixel(hex, size);
        const isTileVisible = isVisible({ q: hex[0], r: hex[1] }, "tile");

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

        if (visibilityState === "hidden") {
          ctx.fillStyle = "rgba(0, 0, 0, 0.85)";
          ctx.fill();
          ctx.strokeStyle = "#222";
         } else if (visibilityState === "revealed") {
           const biomeColor = tile?.biome ? biomeColors[tile.biome as string] : null;
           if (biomeColor) {
              ctx.fillStyle = biomeColor;
              ctx.globalAlpha = 0.35;
              ctx.fill();
            }
           ctx.strokeStyle = "#555";
         } else {
           const biomeKey = tile?.biome as string;
           const biomeColor = biomeKey ? biomeColors[biomeKey] : null;
            if (biomeColor) {
              ctx.fillStyle = biomeColor;
              ctx.globalAlpha = isVisibilityFiltered ? 0.6 : 0.4;
              ctx.fill();
            } else {
               ctx.fillStyle = "#777";
               ctx.globalAlpha = 0.3;
               ctx.fill();
             }
          ctx.strokeStyle = isVisibilityFiltered ? "#999" : "#777";
        }
        ctx.lineWidth = gridLineWidth;
        ctx.stroke();
        ctx.globalAlpha = 1;

        if (visibilityState !== "hidden" && tile?.resource === "tree") {
            const treeColor = visibilityState === "revealed" ? "#5a7a5a" : CONFIG.colors.RESOURCE.tree;
            ctx.fillStyle = treeColor;
            ctx.beginPath();
            ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.4, 0, Math.PI * 2);
            ctx.fill();
          }
          if (visibilityState !== "hidden" && tile?.resource === "grain") {
            const grainColor = visibilityState === "revealed" ? "#b8a878" : CONFIG.colors.RESOURCE.grain;
            ctx.fillStyle = grainColor;
            ctx.beginPath();
            ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.25, 0, Math.PI * 2);
            ctx.fill();
          }
          if (visibilityState !== "hidden" && tile?.resource === "rock") {
            const rockColor = visibilityState === "revealed" ? "#6a6a6a" : CONFIG.colors.RESOURCE.rock;
            ctx.fillStyle = rockColor;
            ctx.beginPath();
            ctx.rect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.5);
            ctx.fill();
          }
         if (visibilityState !== "hidden" && tile?.structure === "wall-ghost") {
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
          if (visibilityState !== "hidden" && tile?.structure === "wall") {
            const wallColor = visibilityState === "revealed" ? "#5a5a5a" : CONFIG.colors.STRUCTURE.wall;
            ctx.fillStyle = wallColor;
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

          if (visibilityState !== "hidden" && tile?.structure === "road") {
            const roadWidth = Math.max(2, CONFIG.canvas.HEX_SIZE * 0.12);
            const roadColor = visibilityState === "revealed" ? "#5a5a5a" : CONFIG.colors.STRUCTURE.road;
            ctx.strokeStyle = roadColor;
            ctx.lineWidth = roadWidth;
            ctx.lineCap = "round";
            ctx.beginPath();
            ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.45, py);
            ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.45, py);
            ctx.stroke();
            ctx.strokeStyle = CONFIG.colors.STRUCTURE.roadStroke;
            ctx.lineWidth = roadWidth * 0.4;
            ctx.beginPath();
            ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.35, py);
            ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.35, py);
            ctx.stroke();
            ctx.lineCap = "butt";
            ctx.lineWidth = 1;
          }

         // Render additional structures
          if (visibilityState !== "hidden" && tile?.structure === "campfire") {
              const campfireColor = visibilityState === "revealed" ? "#cc5500" : "#ff6b00";
              ctx.fillStyle = campfireColor;
              ctx.beginPath();
              ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.3, 0, Math.PI * 2);
              ctx.fill();

             // Add flames effect
             const flameColor = visibilityState === "revealed" ? "#cc8600" : "#ffa726";
             ctx.fillStyle = flameColor;
             ctx.beginPath();
              ctx.arc(px - 2, py - 2, CONFIG.canvas.HEX_SIZE * 0.15, 0, Math.PI * 2);
             ctx.fill();
            }

            if (visibilityState !== "hidden" && tile?.structure === "house") {
              const houseColor = visibilityState === "revealed" ? "#7c6952" : CONFIG.colors.STRUCTURE.house;
              ctx.fillStyle = houseColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.35, py + CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.35);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.35, py + CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.closePath();
              ctx.fill();

              const doorColor = visibilityState === "revealed" ? "#4a352c" : "#5d4037";
              ctx.fillStyle = doorColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.22, py + CONFIG.canvas.HEX_SIZE * 0.15, CONFIG.canvas.HEX_SIZE * 0.44, CONFIG.canvas.HEX_SIZE * 0.25);
            }

           if (visibilityState !== "hidden" && tile?.structure === "lumberyard") {
              const lumberyardColor = visibilityState === "revealed" ? "#8b7355" : CONFIG.colors.STRUCTURE.lumberyard;
              ctx.fillStyle = lumberyardColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.28, py - CONFIG.canvas.HEX_SIZE * 0.18, CONFIG.canvas.HEX_SIZE * 0.56, CONFIG.canvas.HEX_SIZE * 0.36);
              ctx.strokeStyle = "#3e2723";
              ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.28, py - CONFIG.canvas.HEX_SIZE * 0.18, CONFIG.canvas.HEX_SIZE * 0.56, CONFIG.canvas.HEX_SIZE * 0.36);
            }

            if (visibilityState !== "hidden" && tile?.structure === "orchard") {
              const orchardColor = visibilityState === "revealed" ? "#8b835c" : CONFIG.colors.STRUCTURE.orchard;
              ctx.fillStyle = orchardColor;
              ctx.beginPath();
              ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.28, 0, Math.PI * 2);
              ctx.fill();
              const topColor = visibilityState === "revealed" ? "#236327" : "#2e7d32";
              ctx.fillStyle = topColor;
              ctx.beginPath();
              ctx.arc(px, py - CONFIG.canvas.HEX_SIZE * 0.08, CONFIG.canvas.HEX_SIZE * 0.16, 0, Math.PI * 2);
              ctx.fill();
            }

            if (visibilityState !== "hidden" && tile?.structure === "granary") {
              const granaryColor = visibilityState === "revealed" ? "#8b7355" : CONFIG.colors.STRUCTURE.granary;
              ctx.fillStyle = granaryColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.3, py + CONFIG.canvas.HEX_SIZE * 0.18);
              ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.28);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.3, py + CONFIG.canvas.HEX_SIZE * 0.18);
              ctx.closePath();
              ctx.fill();
              const baseColor = visibilityState === "revealed" ? "#704e4f" : "#8d6e63";
              ctx.fillStyle = baseColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.18, py + CONFIG.canvas.HEX_SIZE * 0.18, CONFIG.canvas.HEX_SIZE * 0.36, CONFIG.canvas.HEX_SIZE * 0.18);
            }

            if (visibilityState !== "hidden" && tile?.structure === "farm") {
              const farmColor = visibilityState === "revealed" ? "#6a6612" : CONFIG.colors.STRUCTURE.farm;
              ctx.fillStyle = farmColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.2, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.4);
              ctx.strokeStyle = "#827717";
              ctx.lineWidth = 1;
              ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.2, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.4);
              const lineColor = visibilityState === "revealed" ? "#7e7e1d" : "#9e9d24";
              ctx.strokeStyle = lineColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.2, py - CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.lineTo(px - CONFIG.canvas.HEX_SIZE * 0.2, py + CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.moveTo(px, py - CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.lineTo(px, py + CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.moveTo(px + CONFIG.canvas.HEX_SIZE * 0.2, py - CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.2, py + CONFIG.canvas.HEX_SIZE * 0.15);
              ctx.stroke();
            }

            if (visibilityState !== "hidden" && tile?.structure === "quarry") {
              const quarryColor = visibilityState === "revealed" ? "#6a6a6a" : CONFIG.colors.STRUCTURE.quarry;
              ctx.fillStyle = quarryColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.3, py + CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.lineTo(px - CONFIG.canvas.HEX_SIZE * 0.2, py - CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.1);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.2, py + CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.closePath();
              ctx.fill();
            }

          if (visibilityState !== "hidden" && tile?.structure === "statue/dog") {
            const statueColor = visibilityState === "revealed" ? "#7e7e7e" : "#9e9e9e";
            ctx.fillStyle = statueColor;
            ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.6);
            ctx.strokeStyle = "#616161";
            ctx.lineWidth = 1;
            ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.3, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.6, CONFIG.canvas.HEX_SIZE * 0.6);
          }

            if (visibilityState !== "hidden" && tile?.structure === "warehouse") {
              const warehouseColor = visibilityState === "revealed" ? "#5a5a5a" : CONFIG.colors.STRUCTURE.wall;
              ctx.fillStyle = warehouseColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.8, CONFIG.canvas.HEX_SIZE * 0.6);
              ctx.strokeStyle = CONFIG.colors.STRUCTURE.wallStroke;
              ctx.lineWidth = 2;
              ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.3, CONFIG.canvas.HEX_SIZE * 0.8, CONFIG.canvas.HEX_SIZE * 0.6);

            // Add roof detail
            const roofColor = visibilityState === "revealed" ? "#5e5e5e" : "#757575";
            ctx.fillStyle = roofColor;
            ctx.beginPath();
            ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.5, py - CONFIG.canvas.HEX_SIZE * 0.3);
            ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.5);
            ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.5, py - CONFIG.canvas.HEX_SIZE * 0.3);
            ctx.closePath();
            ctx.fill();
            }

            if (visibilityState !== "hidden" && tile?.structure === "temple") {
              const templeColor = visibilityState === "revealed" ? "#141c63" : "#1a237e";
              ctx.fillStyle = templeColor;
              ctx.beginPath();
              ctx.moveTo(px, py - CONFIG.canvas.HEX_SIZE * 0.4);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.1);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.35, py + CONFIG.canvas.HEX_SIZE * 0.25);
              ctx.lineTo(px - CONFIG.canvas.HEX_SIZE * 0.35, py + CONFIG.canvas.HEX_SIZE * 0.25);
              ctx.lineTo(px - CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.1);
              ctx.closePath();
              ctx.fill();

              // Add pillars
              const pillarColor = visibilityState === "revealed" ? "#2d3b88" : "#3949ab";
              ctx.fillStyle = pillarColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.25, py - CONFIG.canvas.HEX_SIZE * 0.05, CONFIG.canvas.HEX_SIZE * 0.1, CONFIG.canvas.HEX_SIZE * 0.3);
              ctx.fillRect(px + CONFIG.canvas.HEX_SIZE * 0.15, py - CONFIG.canvas.HEX_SIZE * 0.05, CONFIG.canvas.HEX_SIZE * 0.1, CONFIG.canvas.HEX_SIZE * 0.3);
            }

            if (visibilityState !== "hidden" && tile?.structure === "school") {
              const schoolColor = visibilityState === "revealed" ? "#604438" : "#795548";
              ctx.fillStyle = schoolColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.2, CONFIG.canvas.HEX_SIZE * 0.7, CONFIG.canvas.HEX_SIZE * 0.4);
              ctx.strokeStyle = "#4e342e";
              ctx.lineWidth = 1;
              ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.2, CONFIG.canvas.HEX_SIZE * 0.7, CONFIG.canvas.HEX_SIZE * 0.4);

              // Add roof
              const roofColor = visibilityState === "revealed" ? "#4a342c" : "#5d4037";
              ctx.fillStyle = roofColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.45);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.2);
              ctx.closePath();
              ctx.fill();
            }

            if (visibilityState !== "hidden" && tile?.structure === "library") {
              const libraryColor = visibilityState === "revealed" ? "#3e2a25" : "#4e342e";
              ctx.fillStyle = libraryColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.25, CONFIG.canvas.HEX_SIZE * 0.7, CONFIG.canvas.HEX_SIZE * 0.5);
              ctx.strokeStyle = "#3e2723";
              ctx.lineWidth = 1;
              ctx.strokeRect(px - CONFIG.canvas.HEX_SIZE * 0.35, py - CONFIG.canvas.HEX_SIZE * 0.25, CONFIG.canvas.HEX_SIZE * 0.7, CONFIG.canvas.HEX_SIZE * 0.5);

              // Add roof
              const roofColor = visibilityState === "revealed" ? "#573d35" : "#6d4c41";
              ctx.fillStyle = roofColor;
              ctx.beginPath();
              ctx.moveTo(px - CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.25);
              ctx.lineTo(px, py - CONFIG.canvas.HEX_SIZE * 0.5);
              ctx.lineTo(px + CONFIG.canvas.HEX_SIZE * 0.4, py - CONFIG.canvas.HEX_SIZE * 0.25);
              ctx.closePath();
              ctx.fill();

              // Add book symbol
              const bookColor = visibilityState === "revealed" ? "#71574f" : "#8d6e63";
              ctx.fillStyle = bookColor;
              ctx.fillRect(px - CONFIG.canvas.HEX_SIZE * 0.1, py - CONFIG.canvas.HEX_SIZE * 0.05, CONFIG.canvas.HEX_SIZE * 0.2, CONFIG.canvas.HEX_SIZE * 0.15);
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

    const items = snapshot.items ?? {};
    const itemOffsets: Array<[number, number]> = [
      [-0.25, -0.2],
      [0.25, -0.2],
      [0, 0.25],
      [-0.25, 0.25],
      [0.25, 0.25]
    ];
    const normalizeItemResource = (val: unknown) => (typeof val === "string" ? val.replace(/^:/, "") : "unknown");
    const itemColor = (res: string) => {
      switch (res) {
        case "fruit":
          return CONFIG.colors.RESOURCE.fruit;
        case "log":
          return CONFIG.colors.RESOURCE.log;
        case "wood":
          return CONFIG.colors.RESOURCE.wood;
        case "food":
          return CONFIG.colors.RESOURCE.food;
        default:
          return CONFIG.colors.RESOURCE.unknown;
      }
    };
    for (const [tileKey, itemData] of Object.entries(items)) {
      if (!isVisible(tileKey, "item")) continue;
      const [q, r] = tileKey.split(",").map(Number) as [number, number];
      const [ix, iy] = axialToPixel([q, r], size);
      const entries = Object.entries(itemData as Record<string, unknown>)
        .map(([res, qty]) => [normalizeItemResource(res), Number(qty)] as const)
        .filter(([, qty]) => qty > 0);
      entries.slice(0, itemOffsets.length).forEach(([res], idx) => {
        const [ox, oy] = itemOffsets[idx];
        const px = ix + ox * CONFIG.canvas.HEX_SIZE;
        const py = iy + oy * CONFIG.canvas.HEX_SIZE;
        ctx.fillStyle = itemColor(res);
        if (res === "log") {
          const size = CONFIG.canvas.HEX_SIZE * 0.22;
          ctx.fillRect(px - size, py - size * 0.4, size * 2, size * 0.8);
        } else {
          ctx.beginPath();
          ctx.arc(px, py, CONFIG.canvas.HEX_SIZE * 0.16, 0, Math.PI * 2);
          ctx.fill();
        }
      });
    }

    const stockpiles = snapshot.stockpiles ?? {};
    for (const [tileKey, stockpile] of Object.entries(stockpiles)) {
      if (!isVisible(tileKey, "stockpile")) continue;
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
           case "log":
             return CONFIG.colors.RESOURCE.log;
           case "fruit":
             return CONFIG.colors.RESOURCE.fruit;
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

    const rolePalette = (role?: string) => {
      switch (role) {
        case "priest":
          return { body: "#c62828", armor: "#f5c06b", accent: "#8e0000" };
        case "knight":
          return { body: "#1e40af", armor: "#d0d4da", accent: "#0f172a" };
        case "champion":
          return { body: "#0f766e", armor: "#f97316", accent: "#0b3b38" };
        case "wolf":
          return { body: "#795548", armor: "#4e342e", accent: "#3e2723" };
        case "bear":
          return { body: "#5d4037", armor: "#3e2723", accent: "#2c1c18" };
        case "deer":
          return { body: "#8d6e63", armor: "#6d4c41", accent: "#4e342e" };
        default:
          return { body: "#222", armor: "#607d8b", accent: "#111" };
      }
    };

    const drawDot = (x: number, y: number, r: number, color: string) => {
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(x, y, r, 0, Math.PI * 2);
      ctx.fill();
    };

    const drawStatPips = (x: number, y: number, stats: Record<string, number> | undefined, baseSize: number) => {
      if (!stats || !showStats) return;
      const pipRadius = baseSize * 0.35;
      const offsets = [
        [-baseSize * 1.6, -baseSize * 0.4],
        [baseSize * 1.6, -baseSize * 0.4],
        [-baseSize * 1.4, baseSize * 0.9],
        [baseSize * 1.4, baseSize * 0.9],
      ];
      const colors: Array<[string, number]> = [
        ["#ef4444", stats.strength ?? 0],
        ["#3b82f6", stats.dexterity ?? 0],
        ["#22c55e", stats.fortitude ?? 0],
        ["#f59e0b", stats.charisma ?? 0],
      ];

      colors.forEach(([color, value], idx) => {
        if (value <= 0) return;
        const scale = Math.max(0.4, Math.min(1, value));
        const [ox, oy] = offsets[idx];
        drawDot(x + ox, y + oy, pipRadius * scale, color);
      });
    };

    const drawColonist = (x: number, y: number, role?: string, stats?: Record<string, number>) => {
      const palette = rolePalette(role);
      const baseSize = CONFIG.canvas.HEX_SIZE * 0.12;
      const headOffset = CONFIG.canvas.HEX_SIZE * -0.22;
      const bodyOffset = CONFIG.canvas.HEX_SIZE * 0.02;
      const legOffset = CONFIG.canvas.HEX_SIZE * 0.26;

      drawDot(x, y + headOffset, baseSize * 0.9, "#f2d6c9");
      drawDot(x, y + bodyOffset, baseSize, palette.body);
      drawDot(x, y + legOffset, baseSize * 0.8, palette.accent);

      if (role === "knight" || role === "champion") {
        ctx.strokeStyle = palette.armor;
        ctx.lineWidth = 2;
        ctx.beginPath();
        ctx.arc(x, y + bodyOffset, baseSize * 1.4, 0, Math.PI * 2);
        ctx.stroke();
        ctx.lineWidth = 1;
        ctx.fillStyle = palette.armor;
        ctx.fillRect(x - baseSize * 0.9, y + headOffset - baseSize * 0.7, baseSize * 1.8, baseSize * 0.5);
      }

      if (role === "priest") {
        ctx.fillStyle = palette.armor;
        ctx.beginPath();
        ctx.moveTo(x - baseSize * 0.8, y + bodyOffset);
        ctx.lineTo(x, y + legOffset + baseSize * 0.1);
        ctx.lineTo(x + baseSize * 0.8, y + bodyOffset);
        ctx.closePath();
        ctx.fill();
      }

      drawStatPips(x, y, stats, baseSize);
    };

    const drawSpeechBubble = (x: number, y: number, text: string, bubbleAge: number) => {
      const fontSize = Math.max(8, Math.min(12, CONFIG.canvas.HEX_SIZE * 0.25));
      ctx.font = `${fontSize}px sans-serif`;
      ctx.textAlign = "center";
      ctx.textBaseline = "bottom";

      const paddingX = 6;
      const paddingY = 4;
      const textMetrics = ctx.measureText(text);
      const textWidth = textMetrics.width;
      const textHeight = fontSize;

      const bubbleWidth = textWidth + paddingX * 2;
      const bubbleHeight = textHeight + paddingY * 2;
      const bubbleX = x - bubbleWidth / 2;
      const bubbleY = y - CONFIG.canvas.HEX_SIZE * 0.6 - bubbleHeight;

      const maxAge = 3000;
      const fadeAlpha = Math.max(0, 1 - bubbleAge / maxAge);

      ctx.save();
      ctx.globalAlpha = fadeAlpha;

      ctx.fillStyle = "rgba(255, 255, 255, 0.95)";
      ctx.beginPath();
      ctx.roundRect(bubbleX, bubbleY, bubbleWidth, bubbleHeight, 4);
      ctx.fill();
      ctx.strokeStyle = "rgba(0, 0, 0, 0.3)";
      ctx.lineWidth = 1;
      ctx.stroke();

      const tailSize = 6;
      ctx.beginPath();
      ctx.moveTo(x, bubbleY + bubbleHeight);
      ctx.lineTo(x - tailSize, bubbleY + bubbleHeight);
      ctx.lineTo(x, bubbleY + bubbleHeight + tailSize);
      ctx.lineTo(x + tailSize, bubbleY + bubbleHeight);
      ctx.closePath();
      ctx.fillStyle = "rgba(255, 255, 255, 0.95)";
      ctx.fill();
      ctx.stroke();

      ctx.fillStyle = "#333";
      ctx.fillText(text, x, bubbleY + bubbleHeight - paddingY);

      ctx.restore();
    };

    const drawRelationshipLinks = () => {
      if (!showRelationships) return;
      const agents = snapshot.agents ?? [];
      const agentById = new Map<number, Agent>();
      agents.forEach((agent: Agent) => agentById.set(agent.id, agent));

      ctx.save();
      ctx.lineWidth = Math.max(1, 1 / camera.zoom);
      for (const agent of agents) {
        const rels = (agent as any).relationships ?? [];
        if (!hasPos(agent) || !Array.isArray(rels)) continue;
        for (const rel of rels) {
          const targetId = rel.agentId ?? rel["agent-id"];
          if (typeof targetId !== "number" || agent.id > targetId) continue;
          const target = agentById.get(targetId);
          if (!target || !hasPos(target)) continue;
          const affinity = Number(rel.affinity ?? 0.5);
          const intensity = Math.min(1, Math.abs(affinity - 0.5) * 2);
          const isPositive = affinity >= 0.5;
          const baseAlpha = 0.12 + intensity * 0.35;
          const isSelected = selectedAgentId === agent.id || selectedAgentId === targetId;
          const alpha = selectedAgentId ? (isSelected ? baseAlpha + 0.2 : baseAlpha * 0.4) : baseAlpha;
          ctx.strokeStyle = isPositive
            ? `rgba(46, 125, 80, ${alpha})`
            : `rgba(198, 40, 40, ${alpha})`;
          const [sx, sy] = axialToPixel(agent.pos as AxialCoords, size);
          const [tx, ty] = axialToPixel(target.pos as AxialCoords, size);
          ctx.beginPath();
          ctx.moveTo(sx, sy);
          ctx.lineTo(tx, ty);
          ctx.stroke();
        }
      }
      ctx.restore();
    };

    drawRelationshipLinks();

    for (const agent of snapshot.agents ?? []) {
      if (!hasPos(agent) || !isVisible(agent, "agent")) continue;
      const [aq, ar] = agent.pos as AxialCoords;
      const tileVisibilityState = getTileVisibilityState(aq, ar);
      if (tileVisibilityState === "hidden") continue;
      const [ax, ay] = axialToPixel([aq, ar], size);
      const agentId = agent.id;
      const path = agentPaths[agentId] ?? [];
      const status = agent.status as any;
      const alive = status?.["alive?"] ?? status?.alive ?? true;
      const isColonist = ["peasant", "priest", "knight", "champion"].includes(String(agent.role));

      if (alive && isColonist) {
        drawColonist(ax, ay, String(agent.role), (agent as any).stats);
      } else {
        const agentColor = alive ? rolePalette(agent.role).body : "#555";
        ctx.beginPath();
        ctx.fillStyle = agentColor;
        ctx.arc(ax, ay, CONFIG.canvas.HEX_SIZE * 0.35, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = "white";
        const fontSize = CONFIG.canvas.HEX_SIZE * 0.4;
        ctx.font = `${fontSize}px Arial`;
        ctx.textAlign = "center";
        ctx.textBaseline = "middle";
        const icon = alive ? getAgentIcon(agent.role) : "X";
        ctx.fillText(icon, ax, ay);
      }

      if (!alive) {
        ctx.beginPath();
        ctx.strokeStyle = "#c62828";
        ctx.lineWidth = 2;
        ctx.arc(ax, ay, CONFIG.canvas.HEX_SIZE * 0.5, 0, Math.PI * 2);
        ctx.stroke();
        ctx.lineWidth = 1;
      }

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

      if (showNames && camera.zoom > 1.1 && agent.name) {
         ctx.fillStyle = "rgba(15, 23, 42, 0.8)";
         ctx.font = `${CONFIG.canvas.HEX_SIZE * 0.22}px serif`;
         ctx.textAlign = "center";
         ctx.textBaseline = "bottom";
         ctx.fillText(String(agent.name), ax, ay - CONFIG.canvas.HEX_SIZE * 0.45);
       }

       const bubble = speechBubbles.find((b) => b.agentId === agentId);
       if (bubble) {
         const bubbleAge = Date.now() - bubble.timestamp;
         if (bubbleAge < 3000) {
           drawSpeechBubble(ax, ay, bubble.text, bubbleAge);
         }
       }
     }

    ctx.restore();

    const daylight = snapshot.daylight ?? 1;
    const nightAlpha = Math.min(0.7, Math.max(0, (1 - daylight) * 0.75));
    if (nightAlpha > 0) {
      ctx.save();
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.fillStyle = `rgba(10, 18, 32, ${nightAlpha})`;
      ctx.fillRect(0, 0, ctx.canvas.width, ctx.canvas.height);
      ctx.restore();
    }
  }, [snapshot, mapConfig, selectedCell, selectedAgentId, camera, showRelationships, showNames, showStats, speechBubbles, visibilityData, selectedVisibilityAgentId, agentPaths, tileVisibility, revealedTilesSnapshot]);

  useEffect(() => {
    if (!mapConfig || !focusPos) return;
    const size = CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING;
    const [px, py] = axialToPixel(focusPos, size);
    setCamera((prev) => ({ ...prev, offsetX: -px, offsetY: -py }));
  }, [focusTrigger, focusPos, mapConfig]);

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

    const visibilityState = getTileVisibilityState(q, r);
    if (visibilityState === "hidden") {
      return;
    }

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
