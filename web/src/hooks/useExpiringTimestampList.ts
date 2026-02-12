import { useEffect, type Dispatch, type SetStateAction } from "react";

type Timestamped = {
  timestamp: number;
};

type UseExpiringTimestampListOptions<T extends Timestamped> = {
  setItems: Dispatch<SetStateAction<T[]>>;
  maxAgeMs?: number;
  intervalMs?: number;
};

export function useExpiringTimestampList<T extends Timestamped>({
  setItems,
  maxAgeMs = 3000,
  intervalMs = 500,
}: UseExpiringTimestampListOptions<T>) {
  useEffect(() => {
    const interval = setInterval(() => {
      const now = Date.now();
      setItems((prev) => prev.filter((item) => now - item.timestamp < maxAgeMs));
    }, intervalMs);

    return () => clearInterval(interval);
  }, [intervalMs, maxAgeMs, setItems]);
}
