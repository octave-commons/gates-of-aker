import { normalizeKeyedMap } from "./utils";

export type TileVisibilityState = "hidden" | "revealed" | "visible";

type VisibilityPayload = {
  tile_visibility?: Record<string, TileVisibilityState>;
  "tile-visibility"?: Record<string, TileVisibilityState>;
  revealed_tiles_snapshot?: Record<string, unknown>;
  "revealed-tiles-snapshot"?: Record<string, unknown>;
};

export function normalizeVisibilityPayload(source: unknown): {
  tileVisibility: Record<string, TileVisibilityState>;
  revealedTilesSnapshot: Record<string, unknown>;
} {
  const payload = (source && typeof source === "object" ? source : {}) as VisibilityPayload;

  const tileVisibility = normalizeKeyedMap<TileVisibilityState>(
    (payload.tile_visibility ?? payload["tile-visibility"] ?? {}) as Record<string, TileVisibilityState>
  );

  const revealedTilesSnapshot = normalizeKeyedMap(
    (payload.revealed_tiles_snapshot ?? payload["revealed-tiles-snapshot"] ?? {}) as Record<string, unknown>
  );

  return { tileVisibility, revealedTilesSnapshot };
}
