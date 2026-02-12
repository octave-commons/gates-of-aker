import { useCallback } from "react";
import { logDebug, logInfo } from "../logging";
import type { WSClient } from "../ws";

type Bounds = { w: number; h: number };

type UseSimulationControlsOptions = {
  client: WSClient;
  isRunning: boolean;
  treeDensity: number;
  worldWidth: number | null;
  worldHeight: number | null;
  setIsRunning: (value: boolean) => void;
  clearTraces: () => void;
  setSelectedCell: (value: [number, number] | null) => void;
  setSelectedAgentId: (value: number | null) => void;
  clearSpeechBubbles: () => void;
  setTileVisibility: (value: Record<string, "hidden" | "revealed" | "visible">) => void;
  setRevealedTilesSnapshot: (value: Record<string, unknown>) => void;
  setFps: (value: number) => void;
  setFacetLimit: (value: number) => void;
  setVisionRadius: (value: number) => void;
  markUserInteraction: () => void;
};

export function useSimulationControls({
  client,
  isRunning,
  treeDensity,
  worldWidth,
  worldHeight,
  setIsRunning,
  clearTraces,
  setSelectedCell,
  setSelectedAgentId,
  clearSpeechBubbles,
  setTileVisibility,
  setRevealedTilesSnapshot,
  setFps,
  setFacetLimit,
  setVisionRadius,
  markUserInteraction,
}: UseSimulationControlsOptions) {
  const toggleRun = useCallback(() => {
    markUserInteraction();
    if (isRunning) {
      client.sendStopRun();
      setIsRunning(false);
      return;
    }
    client.sendStartRun();
  }, [client, isRunning, markUserInteraction, setIsRunning]);

  const setFpsValue = useCallback(
    (value: number) => {
      client.sendSetFps(value);
      setFps(value);
    },
    [client, setFps]
  );

  const sendTick = useCallback(
    (n: number) => {
      if (isRunning) {
        logDebug("[sendTick] Simulation is running, ignoring tick request");
        return;
      }
      client.send({ op: "tick", n });
    },
    [client, isRunning]
  );

  const reset = useCallback(
    (seed: number, bounds?: Bounds, density?: number) => {
      logInfo("[App] Resetting world with seed:", seed);
      markUserInteraction();
      const payload: { op: string; seed: number; bounds?: Bounds; tree_density?: number } = {
        op: "reset",
        seed,
      };
      if (bounds) {
        payload.bounds = bounds;
      }
      if (density !== undefined) {
        payload.tree_density = density;
      }
      setIsRunning(false);
      clearTraces();
      setSelectedCell(null);
      setSelectedAgentId(null);
      clearSpeechBubbles();
      setTileVisibility({});
      setRevealedTilesSnapshot({});
      client.send(payload);
    },
    [
      client,
      clearSpeechBubbles,
      clearTraces,
      markUserInteraction,
      setIsRunning,
      setRevealedTilesSnapshot,
      setSelectedAgentId,
      setSelectedCell,
      setTileVisibility,
    ]
  );

  const handleFacetLimitChange = useCallback(
    (limit: number) => {
      setFacetLimit(limit);
      client.send({ op: "set_facet_limit", limit });
    },
    [client, setFacetLimit]
  );

  const handleVisionRadiusChange = useCallback(
    (radius: number) => {
      setVisionRadius(radius);
      client.send({ op: "set_vision_radius", radius });
    },
    [client, setVisionRadius]
  );

  const applyWorldSize = useCallback(() => {
    if (worldWidth == null || worldHeight == null) {
      return;
    }
    reset(1, { w: worldWidth, h: worldHeight }, treeDensity);
  }, [reset, treeDensity, worldHeight, worldWidth]);

  return {
    toggleRun,
    setFpsValue,
    sendTick,
    reset,
    handleFacetLimitChange,
    handleVisionRadiusChange,
    applyWorldSize,
  };
}
