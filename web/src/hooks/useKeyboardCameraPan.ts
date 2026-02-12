import { useEffect, useRef } from "react";
import { CONFIG } from "../config/constants";

type CameraState = {
  offsetX: number;
  offsetY: number;
  zoom: number;
};

type MutableRef<T> = { current: T };

type UseKeyboardCameraPanOptions = {
  canvasRef: MutableRef<HTMLCanvasElement | null>;
  setCamera: (updater: (prev: CameraState) => CameraState) => void;
};

const PAN_KEYS = ["KeyW", "KeyA", "KeyS", "KeyD"] as const;

export function useKeyboardCameraPan({ canvasRef, setCamera }: UseKeyboardCameraPanOptions) {
  const keysPressed = useRef<Set<string>>(new Set());

  useEffect(() => {
    let animationFrameId: number | null = null;

    const stopCameraMovement = () => {
      if (animationFrameId !== null) {
        cancelAnimationFrame(animationFrameId);
        animationFrameId = null;
      }
    };

    const scheduleCameraMovement = () => {
      if (animationFrameId === null && keysPressed.current.size > 0) {
        animationFrameId = requestAnimationFrame(handleCameraMovement);
      }
    };

    const handleCameraMovement = () => {
      animationFrameId = null;
      const keys = Array.from(keysPressed.current);
      if (keys.length === 0) {
        return;
      }

      setCamera((prevCamera) => {
        let newOffsetX = prevCamera.offsetX;
        let newOffsetY = prevCamera.offsetY;
        const moveAmount = CONFIG.canvas.PAN_SPEED / prevCamera.zoom;

        if (keys.includes("KeyW")) {
          newOffsetY += moveAmount;
        }
        if (keys.includes("KeyS")) {
          newOffsetY -= moveAmount;
        }
        if (keys.includes("KeyA")) {
          newOffsetX += moveAmount;
        }
        if (keys.includes("KeyD")) {
          newOffsetX -= moveAmount;
        }

        if (newOffsetX !== prevCamera.offsetX || newOffsetY !== prevCamera.offsetY) {
          return { ...prevCamera, offsetX: newOffsetX, offsetY: newOffsetY };
        }
        return prevCamera;
      });

      scheduleCameraMovement();
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (PAN_KEYS.includes(e.code as (typeof PAN_KEYS)[number])) {
        e.preventDefault();
        const wasEmpty = keysPressed.current.size === 0;
        keysPressed.current.add(e.code);
        if (wasEmpty) {
          scheduleCameraMovement();
        }
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      if (PAN_KEYS.includes(e.code as (typeof PAN_KEYS)[number])) {
        e.preventDefault();
        keysPressed.current.delete(e.code);
        if (keysPressed.current.size === 0) {
          stopCameraMovement();
        }
      }
    };

    const handleWindowBlur = () => {
      keysPressed.current.clear();
      stopCameraMovement();
    };

    const canvas = canvasRef.current;
    if (canvas) {
      canvas.setAttribute("tabIndex", "0");
    }

    window.addEventListener("keydown", handleKeyDown, { passive: false });
    window.addEventListener("keyup", handleKeyUp, { passive: false });
    window.addEventListener("blur", handleWindowBlur);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      window.removeEventListener("blur", handleWindowBlur);
      stopCameraMovement();
    };
  }, [canvasRef, setCamera]);
}
