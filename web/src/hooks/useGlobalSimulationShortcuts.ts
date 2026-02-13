import { useEffect, useRef } from "react";

type UseGlobalSimulationShortcutsOptions = {
  onToggleRun: () => void;
  onMarkInteraction: () => void;
};

export function useGlobalSimulationShortcuts({
  onToggleRun,
  onMarkInteraction,
}: UseGlobalSimulationShortcutsOptions) {
  const toggleRunRef = useRef(onToggleRun);
  const markInteractionRef = useRef(onMarkInteraction);

  useEffect(() => {
    toggleRunRef.current = onToggleRun;
  }, [onToggleRun]);

  useEffect(() => {
    markInteractionRef.current = onMarkInteraction;
  }, [onMarkInteraction]);

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.code === "Space" && !event.repeat) {
        event.preventDefault();
        toggleRunRef.current();
      }
    };

    const handleWindowClick = () => {
      markInteractionRef.current();
    };

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("click", handleWindowClick);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("click", handleWindowClick);
    };
  }, []);
}
