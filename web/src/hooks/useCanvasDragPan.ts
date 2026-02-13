import { useCallback, useState, type MouseEvent as ReactMouseEvent } from "react";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type UseCanvasDragPanOptions = {
  camera: CameraState;
  setCamera: (next: CameraState) => void;
};

export function useCanvasDragPan({ camera, setCamera }: UseCanvasDragPanOptions) {
  const [isDragging, setIsDragging] = useState(false);
  const [dragStart, setDragStart] = useState<[number, number] | null>(null);
  const [cameraStart, setCameraStart] = useState<CameraState | null>(null);

  const handleMouseDown = useCallback(
    (event: ReactMouseEvent<HTMLCanvasElement>) => {
      if (event.button === 1) {
        event.preventDefault();
        setIsDragging(true);
        setDragStart([event.clientX, event.clientY]);
        setCameraStart({ ...camera });
      }
    },
    [camera]
  );

  const handleMouseMove = useCallback(
    (event: ReactMouseEvent<HTMLCanvasElement>) => {
      if (!isDragging || !dragStart || !cameraStart) {
        return;
      }

      const dx = event.clientX - dragStart[0];
      const dy = event.clientY - dragStart[1];

      const newOffsetX = cameraStart.offsetX + dx / camera.zoom;
      const newOffsetY = cameraStart.offsetY + dy / camera.zoom;

      setCamera({ ...camera, offsetX: newOffsetX, offsetY: newOffsetY });
    },
    [camera, cameraStart, dragStart, isDragging, setCamera]
  );

  const handleMouseUp = useCallback(() => {
    setIsDragging(false);
    setDragStart(null);
    setCameraStart(null);
  }, []);

  return {
    isDragging,
    handleMouseDown,
    handleMouseMove,
    handleMouseUp,
  };
}
