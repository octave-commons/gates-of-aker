import { useEffect, type MutableRefObject } from "react";
import type { HexConfig } from "../hex";
import type { Snapshot } from "../types";
import type { WSClient } from "../ws";

type SimulationStatus = "open" | "closed" | "error";

type UseSimulationInitializationOptions = {
  client: WSClient;
  snapshot: Snapshot | null;
  status: SimulationStatus;
  treeDensity: number;
  defaultSeedRange: number;
  initializationTimeout: number;
  normalizeSnapshot: (input: unknown) => Snapshot;
  focusOnTownCenter: (state: Snapshot | null) => void;
  initialFocusRef: MutableRefObject<boolean>;
  prevSnapshotRef: MutableRefObject<Snapshot | null>;
  setIsInitializing: (value: boolean) => void;
  setTick: (value: number) => void;
  setSnapshot: (value: Snapshot | null) => void;
  setMapConfig: (value: HexConfig | null) => void;
};

export function useSimulationInitialization({
  client,
  snapshot,
  status,
  treeDensity,
  defaultSeedRange,
  initializationTimeout,
  normalizeSnapshot,
  focusOnTownCenter,
  initialFocusRef,
  prevSnapshotRef,
  setIsInitializing,
  setTick,
  setSnapshot,
  setMapConfig,
}: UseSimulationInitializationOptions) {
  useEffect(() => {
    const createNewSnapshot = () => {
      const defaultSeed = Math.floor(Math.random() * defaultSeedRange);
      client.send({ op: "reset", seed: defaultSeed, tree_density: treeDensity });
    };

    const initializeSnapshot = async () => {
      setIsInitializing(true);
      try {
        const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
        const response = await fetch(`${backendOrigin}/sim/state`);

        if (response.ok) {
          const state = normalizeSnapshot(await response.json());
          const hasData = Boolean(
            (state.tick && state.tick > 0) ||
              (state.agents && state.agents.length > 0) ||
              (state.ledger && Object.keys(state.ledger).length > 0)
          );

          if (hasData) {
            setTick(state.tick ?? 0);
            setSnapshot(state);
            prevSnapshotRef.current = state;
            if (state.map) {
              setMapConfig(state.map as HexConfig);
            }
            if (!initialFocusRef.current) {
              focusOnTownCenter(state);
              initialFocusRef.current = true;
            }
          } else {
            createNewSnapshot();
          }
        } else {
          createNewSnapshot();
        }
      } catch {
        createNewSnapshot();
      } finally {
        setIsInitializing(false);
      }
    };

    const timeoutId = setTimeout(() => {
      if (!snapshot && status === "open") {
        void initializeSnapshot();
      }
    }, initializationTimeout);

    return () => clearTimeout(timeoutId);
  }, [
    client,
    defaultSeedRange,
    focusOnTownCenter,
    initialFocusRef,
    initializationTimeout,
    normalizeSnapshot,
    prevSnapshotRef,
    setIsInitializing,
    setMapConfig,
    setSnapshot,
    setTick,
    snapshot,
    status,
    treeDensity,
  ]);
}
