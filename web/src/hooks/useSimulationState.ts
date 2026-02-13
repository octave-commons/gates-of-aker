import { useState } from "react";
import type { Book } from "../components/LibraryPanel";
import type { Memory } from "../components/MemoryOverlay";
import type { HexConfig } from "../hex";
import type { PathPoint, Snapshot, Trace } from "../types";

export function useSimulationState() {
  const [tick, setTick] = useState(0);
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [mapConfig, setMapConfig] = useState<HexConfig | null>(null);
  const [traces, setTraces] = useState<Trace[]>([]);
  const [agentPaths, setAgentPaths] = useState<Record<number, PathPoint[]>>({});
  const [books, setBooks] = useState<Record<string, Book>>({});
  const [selectedBookId, setSelectedBookId] = useState<string | undefined>(undefined);
  const [memories, setMemories] = useState<Memory[]>([]);
  const [isInitializing, setIsInitializing] = useState(false);

  return {
    tick,
    setTick,
    snapshot,
    setSnapshot,
    mapConfig,
    setMapConfig,
    traces,
    setTraces,
    agentPaths,
    setAgentPaths,
    books,
    setBooks,
    selectedBookId,
    setSelectedBookId,
    memories,
    setMemories,
    isInitializing,
    setIsInitializing,
  };
}
