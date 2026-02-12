import { useCallback, type KeyboardEvent as ReactKeyboardEvent, type MouseEvent as ReactMouseEvent } from "react";
import { CONFIG } from "../config/constants";
import { pixelToAxial, type AxialCoords, type HexConfig } from "../hex";
import { hexToFrequency, playTone } from "../audio";
import { colorForRole } from "../utils";
import { Agent, Snapshot, hasPos } from "../types";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type MutableRef<T> = { current: T };

type UseCanvasSelectionHandlersOptions = {
  snapshot: Snapshot | null;
  mapConfig: HexConfig | null;
  camera: CameraState;
  canvasRef: MutableRef<HTMLCanvasElement | null>;
  selectedCell: [number, number] | null;
  getTileVisibilityState: (q: number, r: number) => "hidden" | "revealed" | "visible";
  onCellSelect: (cell: [number, number], agentId: number | null) => void;
};

const MOVEMENT_KEYS: Record<string, [number, number]> = {
  ArrowUp: [0, -1],
  ArrowDown: [0, 1],
  ArrowLeft: [-1, 0],
  ArrowRight: [1, 0],
};

const toNumericAgentId = (id: Agent["id"]): number | null => {
  if (typeof id === "number") {
    return Number.isFinite(id) ? id : null;
  }
  if (typeof id === "string" && id.trim() !== "") {
    const parsed = Number(id);
    return Number.isFinite(parsed) ? parsed : null;
  }
  return null;
};

const findAgentAt = (agents: Agent[] | undefined, q: number, r: number): Agent | undefined =>
  (agents ?? []).find((a: Agent) => {
    if (!hasPos(a)) {
      return false;
    }
    const [aq, ar] = a.pos as AxialCoords;
    return aq === q && ar === r;
  });

export function useCanvasSelectionHandlers({
  snapshot,
  mapConfig,
  camera,
  canvasRef,
  selectedCell,
  getTileVisibilityState,
  onCellSelect,
}: UseCanvasSelectionHandlersOptions) {
  const handleClick = useCallback(
    (event: ReactMouseEvent<HTMLCanvasElement>) => {
      if (!snapshot || !mapConfig) {
        return;
      }
      const canvas = canvasRef.current;
      if (!canvas) {
        return;
      }

      const rect = canvas.getBoundingClientRect();
      const x = event.clientX - rect.left;
      const y = event.clientY - rect.top;

      const centerX = rect.width / 2;
      const centerY = rect.height / 2;

      const worldX = (x - centerX) / camera.zoom - camera.offsetX;
      const worldY = (y - centerY) / camera.zoom - camera.offsetY;

      const [q, r] = pixelToAxial(worldX, worldY, CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING);
      const cell: AxialCoords = [q, r];

      const visibilityState = getTileVisibilityState(q, r);
      if (visibilityState === "hidden") {
        return;
      }

      const hit = findAgentAt(snapshot.agents, cell[0], cell[1]);
      onCellSelect(cell, hit ? toNumericAgentId(hit.id) : null);

      if (hit) {
        const color = colorForRole(hit.role);
        const frequency = hexToFrequency(color);
        playTone(frequency, 0.15);
      } else {
        playTone(330, 0.05);
      }
    },
    [camera.offsetX, camera.offsetY, camera.zoom, canvasRef, getTileVisibilityState, mapConfig, onCellSelect, snapshot]
  );

  const handleCanvasKeyDown = useCallback(
    (event: ReactKeyboardEvent<HTMLCanvasElement>) => {
      if (!snapshot || !mapConfig) {
        return;
      }
      const movement = MOVEMENT_KEYS[event.key];
      const isSelectKey = event.key === "Enter" || event.key === " ";

      if (!movement && !isSelectKey) {
        return;
      }

      event.preventDefault();
      const origin = selectedCell ?? [0, 0];
      const nextCell: AxialCoords = movement
        ? [origin[0] + movement[0], origin[1] + movement[1]]
        : [origin[0], origin[1]];
      const visibilityState = getTileVisibilityState(nextCell[0], nextCell[1]);
      if (visibilityState === "hidden") {
        return;
      }

      const hit = findAgentAt(snapshot.agents, nextCell[0], nextCell[1]);
      onCellSelect(nextCell, hit ? toNumericAgentId(hit.id) : null);
    },
    [getTileVisibilityState, mapConfig, onCellSelect, selectedCell, snapshot]
  );

  return {
    handleClick,
    handleCanvasKeyDown,
  };
}
