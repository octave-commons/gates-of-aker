import { useCallback, type Dispatch, type SetStateAction } from "react";
import { playBookCreatedTone, playHuntAttackTone, playHuntKillTone, playHuntStartTone, playTone } from "../audio";
import { CONFIG } from "../config/constants";
import { logDebug, logWarn } from "../logging";
import type { Book } from "../components/LibraryPanel";
import type { Memory } from "../components/MemoryOverlay";
import type { Agent, Snapshot, Trace } from "../types";
import { appendBounded, applyDelta, normalizeKeyedMap } from "../utils";
import { normalizeVisibilityPayload } from "../visibilityPayload";
import type { WSMessage } from "../ws";
import type { HexConfig } from "../hex";

type TickHealth = {
  targetMs: number;
  tickMs: number;
  health: "healthy" | "degraded" | "unhealthy" | "unknown";
};

type MutableRef<T> = { current: T };

export type SpeechBubble = {
  agentId: number;
  text: string;
  interactionType: string;
  timestamp: number;
};

type UseSimulationMessageHandlerOptions = {
  initialFocusRef: MutableRef<boolean>;
  aliveAgentsRef: MutableRef<Set<number>>;
  prevSnapshotRef: MutableRef<Snapshot | null>;
  snapshotRef: MutableRef<Snapshot | null>;
  prevBookCountRef: MutableRef<number>;
  setTick: (value: number) => void;
  setSnapshot: Dispatch<SetStateAction<Snapshot | null>>;
  setMapConfig: (value: HexConfig | null) => void;
  setTileVisibility: (value: Record<string, "hidden" | "revealed" | "visible">) => void;
  setRevealedTilesSnapshot: (value: Record<string, unknown>) => void;
  setAgentVisibilityMaps: Dispatch<SetStateAction<Record<number, Set<string>>>>;
  setMemories: (value: Memory[]) => void;
  setVisibilityData: (value: Record<string, unknown> | null) => void;
  setTraces: Dispatch<SetStateAction<Trace[]>>;
  setBooks: (value: Record<string, Book>) => void;
  setSelectedCell: (value: [number, number] | null) => void;
  setSelectedAgentId: (value: number | null) => void;
  setSpeechBubbles: Dispatch<SetStateAction<SpeechBubble[]>>;
  setIsRunning: (value: boolean) => void;
  setFps: (value: number) => void;
  setTickHealth: (value: TickHealth | null) => void;
  normalizeSnapshot: (input: unknown) => Snapshot;
  normalizeBooks: (input: unknown) => Record<string, Book>;
  handleDeathTone: (nextSnapshot: Snapshot | null) => void;
  handleTickAudio: (nextSnapshot: Snapshot | null) => void;
  handleDeltaAudio: (delta: Record<string, unknown>) => void;
  handleSocialSound: (
    interactionType: string,
    agent: Agent | Record<string, unknown> | null | undefined
  ) => void;
  focusOnTownCenter: (state: Snapshot | null) => void;
  getAliveAgents: (state: Snapshot | null) => Set<number>;
};

export function useSimulationMessageHandler({
  initialFocusRef,
  aliveAgentsRef,
  prevSnapshotRef,
  snapshotRef,
  prevBookCountRef,
  setTick,
  setSnapshot,
  setMapConfig,
  setTileVisibility,
  setRevealedTilesSnapshot,
  setAgentVisibilityMaps,
  setMemories,
  setVisibilityData,
  setTraces,
  setBooks,
  setSelectedCell,
  setSelectedAgentId,
  setSpeechBubbles,
  setIsRunning,
  setFps,
  setTickHealth,
  normalizeSnapshot,
  normalizeBooks,
  handleDeathTone,
  handleTickAudio,
  handleDeltaAudio,
  handleSocialSound,
  focusOnTownCenter,
  getAliveAgents,
}: UseSimulationMessageHandlerOptions) {
  return useCallback(
    (m: WSMessage) => {
      if (m.op === "hello") {
        logDebug("[App] Processing hello message");
        const state = normalizeSnapshot(m.state ?? {});
        const {
          tileVisibility: nextTileVisibility,
          revealedTilesSnapshot: nextRevealedTilesSnapshot,
        } = normalizeVisibilityPayload(state);
        const avm = (state?.agent_visibility ?? {}) as Record<number, Set<string>>;
        logDebug("[App] Setting initial state - agents:", state.agents?.length ?? 0);
        setTick(state.tick ?? 0);
        setSnapshot(state);
        snapshotRef.current = state;
        setTileVisibility(nextTileVisibility);
        setRevealedTilesSnapshot(nextRevealedTilesSnapshot);
        setAgentVisibilityMaps(avm);
        prevSnapshotRef.current = state;
        if (state.map) {
          setMapConfig(state.map as HexConfig);
        }
        handleDeathTone(state);
        if (!initialFocusRef.current) {
          focusOnTownCenter(state);
          initialFocusRef.current = true;
        }
      }

      if (m.op === "tick") {
        logDebug("[App] Processing tick message");
        logDebug("[App] Full tick data:", m.data);
        setTick(m.data?.tick ?? 0);
        if (!m.data?.snapshot) {
          logWarn("[App] Tick message has no snapshot data, keeping current state");
          playTone(440, 0.08);
          return;
        }
        const nextSnapshot = normalizeSnapshot(m.data?.snapshot ?? null);
        logDebug("[App] Tick update - agents:", nextSnapshot.agents?.length ?? 0);
        logDebug("[App] First agent:", nextSnapshot.agents?.[0]);
        setSnapshot(nextSnapshot);
        snapshotRef.current = nextSnapshot;
        setMemories(Array.isArray(nextSnapshot?.memories) ? (nextSnapshot.memories as Memory[]) : []);
        playTone(440, 0.08);
        handleDeathTone(nextSnapshot);
        handleTickAudio(nextSnapshot);
      }

      if (m.op === "tick_delta") {
        logDebug("[App] Processing tick_delta message");
        const delta = m.data as Parameters<typeof applyDelta>[1];
        const {
          tileVisibility: nextTileVisibility,
          revealedTilesSnapshot: nextRevealedTilesSnapshot,
        } = normalizeVisibilityPayload(delta);
        const avm = (delta as Record<string, unknown>)?.agent_visibility ?? delta?.changed_agent_visibility;
        if (delta && Object.keys(nextTileVisibility).length > 0 && Object.keys(nextTileVisibility).length < 5) {
          logDebug(
            "[App] tick_delta received, tileVisibility sample:",
            Object.entries(nextTileVisibility).slice(0, 3)
          );
        }
        setTick(typeof delta?.tick === "number" ? delta.tick : 0);
        setSnapshot((prev: Snapshot | null) => {
          const result = applyDelta(prev, delta) as Snapshot;
          snapshotRef.current = result;
          logDebug(
            "[App] Delta applied - agents before:",
            prev?.agents?.length ?? 0,
            "after:",
            result?.agents?.length ?? 0
          );
          return result;
        });
        setVisibilityData(delta?.visibility ?? null);
        setTileVisibility(nextTileVisibility);
        setRevealedTilesSnapshot(nextRevealedTilesSnapshot);
        if (avm && typeof avm === "object") {
          setAgentVisibilityMaps((prev: Record<number, Set<string>>) => ({
            ...prev,
            ...Object.entries(avm).reduce((acc, [agentId, tiles]) => {
              const numId = parseInt(String(agentId), 10);
              const tilesArray = Array.isArray(tiles) ? tiles : [];
              return { ...acc, [numId]: new Set(tilesArray) };
            }, {}),
          }));
        }
        handleDeltaAudio(delta);
      }

      if (m.op === "trace") {
        const incoming = m.data as Trace;
        setTraces((prev) => appendBounded(prev, incoming, CONFIG.data.MAX_TRACES));
      }

      if (m.op === "books") {
        const newBooks = normalizeBooks(m.data?.books ?? {});
        const newBookCount = Object.keys(newBooks).length;
        if (newBookCount > prevBookCountRef.current) {
          playBookCreatedTone();
        }
        prevBookCountRef.current = newBookCount;
        setBooks(newBooks);
      }

      if (m.op === "reset") {
        setTraces([]);
        setSelectedCell(null);
        setSelectedAgentId(null);
        setSpeechBubbles([]);
        setTileVisibility({});
        setRevealedTilesSnapshot({});
        const state = normalizeSnapshot(m.state ?? {});
        const {
          tileVisibility: nextTileVisibility,
          revealedTilesSnapshot: nextRevealedTilesSnapshot,
        } = normalizeVisibilityPayload(state);
        setSnapshot(state);
        snapshotRef.current = state;
        setTileVisibility(nextTileVisibility);
        setRevealedTilesSnapshot(nextRevealedTilesSnapshot);
        prevSnapshotRef.current = state;
        if (state.map) {
          setMapConfig(state.map as HexConfig);
        }
        aliveAgentsRef.current = getAliveAgents(state);
        initialFocusRef.current = false;
        focusOnTownCenter(state);
        initialFocusRef.current = true;
      }

      if (m.op === "social_interaction") {
        const si = m.data;
        if (si && typeof si.agent_1_id === "number" && typeof si.agent_2_id === "number") {
          const interactionName = (si.interaction_type || "social") as string;
          const agents = snapshotRef.current?.agents ?? [];
          const agent1 = agents.find((a: Agent) => a.id === si.agent_1_id);
          const agent2 = agents.find((a: Agent) => a.id === si.agent_2_id);
          if (agent1) {
            handleSocialSound(interactionName, agent1);
          }
          if (agent2) {
            handleSocialSound(interactionName, agent2);
          }
          setSpeechBubbles((prev) => [
            ...prev,
            {
              agentId: si.agent_1_id,
              text: interactionName,
              interactionType: interactionName,
              timestamp: Date.now(),
            },
            {
              agentId: si.agent_2_id,
              text: interactionName,
              interactionType: interactionName,
              timestamp: Date.now(),
            },
          ]);
        }
      }

      if (m.op === "tiles") {
        setSnapshot((prev: Snapshot | null) => {
          if (!prev) {
            return prev;
          }
          return { ...prev, tiles: normalizeKeyedMap(m.tiles) as Snapshot["tiles"] };
        });
      }

      if (m.op === "stockpiles") {
        setSnapshot((prev: Snapshot | null) => {
          if (!prev) {
            return prev;
          }
          return { ...prev, stockpiles: normalizeKeyedMap(m.stockpiles) as Snapshot["stockpiles"] };
        });
      }

      if (m.op === "agent_path") {
        setSnapshot((prev: Snapshot | null) => {
          if (!prev) {
            return prev;
          }
          const agentPath = { [m.agent_id]: m.path };
          return { ...prev, agentPath };
        });
      }

      if (m.op === "jobs") {
        setSnapshot((prev: Snapshot | null) => {
          if (!prev) {
            return prev;
          }
          return { ...prev, jobs: m.jobs as Snapshot["jobs"] };
        });
      }

      if (m.op === "runner_state") {
        setIsRunning(m.running);
        setFps(m.fps);
      }

      if (m.op === "tick_health") {
        const data = m.data ?? {};
        const targetMs =
          typeof data.targetMs === "number"
            ? data.targetMs
            : typeof data["target-ms"] === "number"
              ? data["target-ms"]
              : undefined;
        const tickMs =
          typeof data.tickMs === "number"
            ? data.tickMs
            : typeof data["tick-ms"] === "number"
              ? data["tick-ms"]
              : undefined;
        const health = (data.health as "healthy" | "degraded" | "unhealthy" | "unknown") ?? "unknown";
        if (targetMs != null && tickMs != null) {
          setTickHealth({ targetMs, tickMs, health });
        } else {
          setTickHealth(null);
        }
      }

      if (m.op === "combat_event") {
        const ce = m.data ?? {};
        const eventType = ce.type as string;
        if (eventType === "hunt-start") {
          playHuntStartTone();
        } else if (eventType === "hunt-attack") {
          playHuntAttackTone();
        } else if (eventType === "hunt-kill") {
          playHuntKillTone();
        }
      }
    },
    [
      focusOnTownCenter,
      getAliveAgents,
      handleDeathTone,
      handleDeltaAudio,
      handleSocialSound,
      handleTickAudio,
      aliveAgentsRef,
      initialFocusRef,
      normalizeBooks,
      normalizeSnapshot,
      prevBookCountRef,
      prevSnapshotRef,
      setAgentVisibilityMaps,
      setBooks,
      setFps,
      setIsRunning,
      setMapConfig,
      setMemories,
      setRevealedTilesSnapshot,
      setSelectedAgentId,
      setSelectedCell,
      setSnapshot,
      setSpeechBubbles,
      setTick,
      setTickHealth,
      setTileVisibility,
      setTraces,
      setVisibilityData,
      snapshotRef,
    ]
  );
}
