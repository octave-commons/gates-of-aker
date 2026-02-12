import { useEffect } from "react";
import { CONFIG } from "../config/constants";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type MutableRef<T> = { current: T };

type UseCanvasWheelZoomOptions = {
  canvasRef: MutableRef<HTMLCanvasElement | null>;
  setCamera: (updater: (prev: CameraState) => CameraState) => void;
};

export function useCanvasWheelZoom({ canvasRef, setCamera }: UseCanvasWheelZoomOptions) {
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) {
      return;
    }

    const handleWheel = (event: globalThis.WheelEvent) => {
      event.preventDefault();
      const rect = canvas.getBoundingClientRect();
      const mouseX = event.clientX - rect.left;
      const mouseY = event.clientY - rect.top;

      if (event.deltaY === 0) {
        return;
      }

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

    return () => {
      canvas.removeEventListener("wheel", handleWheel);
    };
  }, [canvasRef, setCamera]);
}
