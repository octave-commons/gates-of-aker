import { useEffect } from "react";
import type { HexConfig } from "../hex";

type UseWorldSizeFromMapConfigOptions = {
  mapConfig: HexConfig | null;
  setWorldWidth: (value: number) => void;
  setWorldHeight: (value: number) => void;
};

export function useWorldSizeFromMapConfig({
  mapConfig,
  setWorldWidth,
  setWorldHeight,
}: UseWorldSizeFromMapConfigOptions) {
  useEffect(() => {
    if (!mapConfig || !mapConfig.bounds) return;
    const bounds = mapConfig.bounds as { shape?: string; w?: number; h?: number; r?: number };

    if (bounds.shape === "rect") {
      if (typeof bounds.w === "number") {
        setWorldWidth(bounds.w);
      }
      if (typeof bounds.h === "number") {
        setWorldHeight(bounds.h);
      }
      return;
    }

    if (bounds.shape === "radius") {
      const radius = typeof bounds.r === "number" ? bounds.r : 0;
      const size = (radius * 2) + 1;
      setWorldWidth(size);
      setWorldHeight(size);
    }
  }, [mapConfig, setWorldHeight, setWorldWidth]);
}
