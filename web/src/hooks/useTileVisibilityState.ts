import { useCallback } from "react";
import type { Snapshot } from "../types";

type TileVisibilityState = "hidden" | "revealed" | "visible";

type UseTileVisibilityStateOptions = {
  tileVisibility: Record<string, TileVisibilityState>;
  snapshot: Snapshot | null;
};

export function useTileVisibilityState({ tileVisibility, snapshot }: UseTileVisibilityStateOptions) {
  return useCallback(
    (q: number, r: number): TileVisibilityState => {
      const tileKey = `${q},${r}`;
      const vis = tileVisibility[tileKey];
      const tileInSnapshot = snapshot?.tiles?.[tileKey];

      if (tileInSnapshot && vis === undefined) {
        return "visible";
      }

      return vis ?? "hidden";
    },
    [tileVisibility, snapshot?.tiles]
  );
}
