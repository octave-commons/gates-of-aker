import { useEffect } from "react";
import { CONFIG } from "../config/constants";
import { axialToPixel, type AxialCoords } from "../hex";
import type { HexConfig } from "../hex";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type UseFocusCameraPositionOptions = {
  mapConfig: HexConfig | null;
  focusPos?: AxialCoords | null;
  focusTrigger?: number;
  setCamera: (updater: (prev: CameraState) => CameraState) => void;
};

export function useFocusCameraPosition({
  mapConfig,
  focusPos,
  focusTrigger,
  setCamera,
}: UseFocusCameraPositionOptions) {
  useEffect(() => {
    void focusTrigger;
    if (!mapConfig || !focusPos) {
      return;
    }

    const size = CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING;
    const [px, py] = axialToPixel(focusPos, size);
    setCamera((prev) => ({ ...prev, offsetX: -px, offsetY: -py }));
  }, [focusTrigger, focusPos, mapConfig, setCamera]);
}
