import { useCallback, type MouseEvent as ReactMouseEvent } from "react";

export function usePreventContextMenu<T extends HTMLElement>() {
  return useCallback((event: ReactMouseEvent<T>) => {
    event.preventDefault();
  }, []);
}
