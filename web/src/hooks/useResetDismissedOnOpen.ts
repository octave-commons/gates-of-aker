import { useEffect, type Dispatch, type SetStateAction } from "react";

type WebSocketStatus = "open" | "closed" | "error";

type UseResetDismissedOnOpenOptions = {
  status: WebSocketStatus;
  setDismissed: Dispatch<SetStateAction<boolean>>;
};

export function useResetDismissedOnOpen({
  status,
  setDismissed,
}: UseResetDismissedOnOpenOptions) {
  useEffect(() => {
    if (status === "open") {
      setDismissed(false);
    }
  }, [setDismissed, status]);
}
