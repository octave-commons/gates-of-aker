import { useCallback } from "react";
import { Agent, Snapshot, hasPos } from "../types";
import type { AxialCoords } from "../hex";

type VisibilityEntityType = "agent" | "tile" | "item" | "stockpile";

type UseEntityVisibilityOptions = {
  selectedVisibilityAgentId: number | null;
  visibilityData: Record<string, unknown> | null;
  snapshot: Snapshot | null;
};

const asRecord = (value: unknown): Record<string, unknown> | null =>
  value && typeof value === "object" ? (value as Record<string, unknown>) : null;

const asStringArray = (value: unknown): string[] =>
  Array.isArray(value) ? value.filter((item): item is string => typeof item === "string") : [];

const asNumberArray = (value: unknown): number[] =>
  Array.isArray(value) ? value.filter((item): item is number => typeof item === "number") : [];

export function useEntityVisibility({
  selectedVisibilityAgentId,
  visibilityData,
  snapshot,
}: UseEntityVisibilityOptions) {
  return useCallback(
    (entity: unknown, type: VisibilityEntityType) => {
      if (!selectedVisibilityAgentId || !visibilityData || !snapshot) {
        return true;
      }

      const selectedAgent = (snapshot.agents ?? []).find((a: Agent) => a.id === selectedVisibilityAgentId);
      if (!selectedAgent || !hasPos(selectedAgent)) {
        return true;
      }

      const viewerPos = selectedAgent.pos as AxialCoords;
      const viewerPosStr = `${viewerPos[0]},${viewerPos[1]}`;
      const visibilityMap = asRecord(visibilityData[viewerPosStr]);

      if (!visibilityMap) {
        return true;
      }

      switch (type) {
        case "agent": {
          const visibleAgentIds = asNumberArray(visibilityMap.visible_agent_ids);
          const entityId = asRecord(entity)?.id;
          return typeof entityId === "number" && visibleAgentIds.includes(entityId);
        }
        case "tile": {
          const visibleTiles = asStringArray(visibilityMap.visible_tiles);
          const entityRecord = asRecord(entity);
          const tileKey =
            entityRecord && typeof entityRecord.q === "number" && typeof entityRecord.r === "number"
              ? `${entityRecord.q},${entityRecord.r}`
              : String(entity);
          return visibleTiles.includes(tileKey);
        }
        case "item": {
          const visibleItems = asStringArray(visibilityMap.visible_items);
          const entityRecord = asRecord(entity);
          const itemKey =
            entityRecord && typeof entityRecord.q === "number" && typeof entityRecord.r === "number"
              ? `${entityRecord.q},${entityRecord.r}`
              : String(entity);
          return visibleItems.includes(itemKey);
        }
        case "stockpile": {
          const visibleStockpiles = asStringArray(visibilityMap.visible_stockpiles);
          const entityRecord = asRecord(entity);
          const stockpileKey =
            entityRecord && typeof entityRecord.q === "number" && typeof entityRecord.r === "number"
              ? `${entityRecord.q},${entityRecord.r}`
              : String(entity);
          return visibleStockpiles.includes(stockpileKey);
        }
        default:
          return true;
      }
    },
    [selectedVisibilityAgentId, visibilityData, snapshot]
  );
}
