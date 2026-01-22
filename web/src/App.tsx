import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { WSClient, WSMessage } from "./ws";
import { playDeathTone, playTone, playToneSequence, playToneSequenceWithVoice, getScaleFrequency, markUserInteraction, playBookCreatedTone, playHuntStartTone, playHuntAttackTone, playHuntKillTone } from "./audio";
import { applyDelta } from "./utils";
import {
  AgentList,
  FactionsPanel,
  MythPanel,
  RawJSONFeedPanel,
  SelectedPanel,
  SimulationCanvas,
  StatusBar,
  TickControls,
  BuildControls,
  BuildingPalette,
  JobQueuePanel,
  ResourceTotalsPanel,
  WorldInfoPanel,
  ThoughtsPanel,
  LibraryPanel,
  SplashScreen,
  MainMenu,
  OllamaTestPage,
  VisibilityControlPanel,
  MemoryOverlay,
  FacetControls,
} from "./components";
import { TraceFeed } from "./components/TraceFeed";
import { Agent, Trace, hasPos, PathPoint } from "./types";
import type { HexConfig, AxialCoords } from "./hex";

type AppState = "splash" | "menu" | "simulation" | "ollama-test";

type SpeechBubble = {
  agentId: number;
  text: string;
  interactionType: string;
  timestamp: number;
};
import { clamp01, fmt, colorForRole } from "./utils";
import { CONFIG } from "./config/constants";

const localFmt = (n: any) => (typeof n === "number" ? n.toFixed(3) : String(n ?? ""));

const MAX_TONE_SEQUENCES_PER_TICK = 8;
const NOTE_DURATION = 0.11;
const NOTE_GAP = 0.05;

const NEED_THRESHOLD_KEYS: Record<string, string> = {
  food: "food-hungry",
  water: "water-thirsty",
  rest: "rest-tired",
  sleep: "sleep-tired",
  warmth: "warmth-cold",
  health: "health-low",
  security: "security-unsettled",
  mood: "mood-low",
  social: "social-low",
};

const NEED_TONE_SEQUENCES: Record<string, number[]> = {
  food: [0, 2, 4],
  water: [1, 3, 5],
  rest: [2, 1, 0],
  sleep: [3, 1, 3],
  warmth: [4, 2, 0],
  health: [5, 3, 1],
  security: [2, 4, 5],
  mood: [1, 4, 2],
  social: [0, 1, 4],
};

const JOB_TONE_SEQUENCES: Record<string, number[]> = {
   ":job/eat": [0, 3, 5],
   ":job/warm-up": [4, 2, 4],
   ":job/sleep": [5, 2, 0],
   ":job/hunt": [3, 1, 4],
   ":job/chop-tree": [2, 0, 2],
   ":job/mine": [1, 3, 1],
   ":job/harvest-wood": [2, 4, 2],
   ":job/harvest-fruit": [0, 4, 1],
   ":job/harvest-grain": [1, 5, 2],
   ":job/harvest-stone": [3, 5, 3],
   ":job/farm": [0, 2, 0],
   ":job/smelt": [5, 4, 2],
   ":job/build-house": [1, 2, 3],
   ":job/improve": [4, 5, 4],
   ":job/haul": [2, 5, 2],
   ":job/deliver-food": [0, 1, 2],
   ":job/build-wall": [3, 2, 1],
   ":job/builder": [4, 3, 2],
   ":job/build-structure": [2, 3, 4],
 };

const SOCIAL_TONE_SEQUENCES: Record<string, number[]> = {
   "Small talk": [0, 2, 4],
   "Gossip": [1, 3, 5],
   "Debate": [4, 2, 0, 2, 4],
   "Ritual": [0, 2, 4, 2, 0],
   "Teaching": [2, 4, 2],
 };

const toSequence = (notes: number[], octaveShift: number = 0) =>
  notes.map((note) => getScaleFrequency(note, octaveShift));

const normalizeTileKey = (rawKey: string) => {
  const trimmed = rawKey.trim();
  if (trimmed.includes(",") && !trimmed.includes("[")) {
    return trimmed.replace(/\s+/g, "");
  }
  const match = trimmed.match(/^\[(-?\d+)\s+(-?\d+)\]$/);
  if (match) {
    return `${match[1]},${match[2]}`;
  }
  return trimmed;
};

const normalizeKeyedMap = <T,>(input: Record<string, T> | null | undefined) => {
  if (!input || typeof input !== "object") return input;
  const normalized: Record<string, T> = {};
  for (const [key, value] of Object.entries(input)) {
    normalized[normalizeTileKey(key)] = value;
  }
  return normalized;
};

const normalizeSnapshot = (state: any) => {
  if (!state || typeof state !== "object") return state;
  return {
    ...state,
    tiles: normalizeKeyedMap(state.tiles),
    items: normalizeKeyedMap(state.items),
    stockpiles: normalizeKeyedMap(state.stockpiles),
  };
};

export function App() {
  const [appState, setAppState] = useState<AppState>("splash");
   const [status, setStatus] = useState<"open" | "closed" | "error">("closed");
   const [tick, setTick] = useState(0);
    const [snapshot, setSnapshot] = useState<any>(null);
      const [mapConfig, setMapConfig] = useState<HexConfig | null>(null);
   const [traces, setTraces] = useState<Trace[]>([]);
   const [agentPaths, setAgentPaths] = useState<Record<number, PathPoint[]>>({});
   const [books, setBooks] = useState<Record<string, any>>({});
   const [selectedBookId, setSelectedBookId] = useState<string | undefined>(undefined);
   const aliveAgentsRef = useRef<Set<number>>(new Set());
   const prevSnapshotRef = useRef<any>(null);
   const initialFocusRef = useRef(false);
   const prevBookCountRef = useRef<number>(0);
  const [focusPos, setFocusPos] = useState<[number, number] | null>(null);
  const [focusTrigger, setFocusTrigger] = useState(0);

  const [showRelationships, setShowRelationships] = useState(true);
  const [showNames, setShowNames] = useState(true);
  const [showStats, setShowStats] = useState(true);

    const [selectedCell, setSelectedCell] = useState<[number, number] | null>(null);
    const [selectedAgentId, setSelectedAgentId] = useState<number | null>(null);
    const [speechBubbles, setSpeechBubbles] = useState<SpeechBubble[]>([]);
     const [selectedVisibilityAgentId, setSelectedVisibilityAgentId] = useState<number | null>(null);
      const [visibilityData, setVisibilityData] = useState<Record<string, any> | null>(null);
   const [tileVisibility, setTileVisibility] = useState<Record<string, "hidden" | "revealed" | "visible">>({});
   const [revealedTilesSnapshot, setRevealedTilesSnapshot] = useState<Record<string, any>>({});

     useEffect(() => {
     }, [snapshot]);

  const getAgentPath = (agentId: number): PathPoint[] => {
    return agentPaths[agentId] ?? [];
  };

  const [buildMode, setBuildMode] = useState(false);
  const [stockpileMode, setStockpileMode] = useState(false);
    const [fps, setFps] = useState(15);
    const [memories, setMemories] = useState<any[]>([]);
    const [facetLimit, setFacetLimit] = useState(16);
    const [visionRadius, setVisionRadius] = useState(10);
    const [showMemories, setShowMemories] = useState(false);
 
   const [worldWidth, setWorldWidth] = useState<number | null>(null);
   const [worldHeight, setWorldHeight] = useState<number | null>(null);
   const [treeDensity, setTreeDensity] = useState<number>(CONFIG.data.DEFAULT_TREE_DENSITY);

  const getAliveAgents = useCallback((state: any) => {
    const alive = new Set<number>();
    if (!state?.agents) return alive;
    state.agents.forEach((agent: any) => {
      const status = agent?.status ?? {};
      const aliveFlag = status["alive?"] ?? status.alive ?? true;
      if (aliveFlag && typeof agent.id === "number") {
        alive.add(agent.id);
      }
    });
    return alive;
  }, []);

  const handleDeathTone = useCallback((nextSnapshot: any) => {
    if (!nextSnapshot) return;
    const previousAlive = aliveAgentsRef.current;
    const currentAlive = getAliveAgents(nextSnapshot);
    const died = [...previousAlive].some((id) => !currentAlive.has(id));
    if (died) {
      playDeathTone();
    }
    aliveAgentsRef.current = currentAlive;
  }, [getAliveAgents]);

   const handleTickAudio = useCallback((nextSnapshot: any) => {
     if (!nextSnapshot) return;
     const prevSnapshot = prevSnapshotRef.current;
     prevSnapshotRef.current = nextSnapshot;
     if (!prevSnapshot) return;

     const getField = (obj: any, key: string) =>
       obj?.[key] ?? obj?.[key.replace(/-/g, "_")] ?? obj?.[key.replace(/-(\w)/g, (_: string, c: string) => c.toUpperCase())];

     const sequences: number[][] = [];

     const prevJobs = new Map<string, any>();
     (prevSnapshot.jobs ?? []).forEach((job: any) => {
       if (job?.id) {
         prevJobs.set(String(job.id), job);
       }
     });
     const nextJobIds = new Set<string>();
     (nextSnapshot.jobs ?? []).forEach((job: any) => {
       if (job?.id) {
         nextJobIds.add(String(job.id));
       }
     });
      prevJobs.forEach((job, jobId) => {
        if (!nextJobIds.has(jobId)) {
          const jobType = String(job?.type ?? ":job/unknown");
          const notes = JOB_TONE_SEQUENCES[jobType] ?? [1, 0, 1];
          sequences.push(toSequence(notes, 0));
        }
      });

      const prevAgents = new Map<number, any>();
      (prevSnapshot.agents ?? []).forEach((agent: any) => {
        if (typeof agent?.id === "number") {
          prevAgents.set(agent.id, agent);
        }
      });

      (nextSnapshot.agents ?? []).forEach((agent: any) => {
        if (typeof agent?.id !== "number") return;
        const prevAgent = prevAgents.get(agent.id);
        if (!prevAgent) return;
        const status = agent.status ?? {};
        const alive = status["alive?"] ?? status.alive ?? true;
        if (!alive) return;
        const prevNeeds = prevAgent.needs ?? {};
        const nextNeeds = agent.needs ?? {};
        const thresholds =
          getField(agent, "need-thresholds") ?? getField(agent, "needThresholds") ?? getField(agent, "need_thresholds") ?? {};
        Object.entries(NEED_THRESHOLD_KEYS).forEach(([needKey, thresholdKey]) => {
          const threshold = getField(thresholds, thresholdKey);
          const prevValue = getField(prevNeeds, needKey);
          const nextValue = getField(nextNeeds, needKey);
          if (typeof threshold !== "number" || typeof prevValue !== "number" || typeof nextValue !== "number") {
            return;
          }
          if (prevValue >= threshold && nextValue < threshold) {
            const notes = NEED_TONE_SEQUENCES[needKey] ?? [1, 0, 1];
            sequences.push(toSequence(notes, 0));
          }
        });
      });

      sequences.slice(0, MAX_TONE_SEQUENCES_PER_TICK).forEach((sequence, index) => {
       playToneSequence(sequence, {
         noteDuration: NOTE_DURATION,
         gap: NOTE_GAP,
         startDelay: index * 0.08,
         gain: 0.9,
       });
     });
   }, []);

  const handleDeltaAudio = useCallback((delta: any) => {
    if (!delta) return;

    if (delta.combat_events) {
      delta.combat_events.forEach((ce: any) => {
        const eventType = ce.type as string;
        if (eventType === "hunt-start") {
          playHuntStartTone();
        } else if (eventType === "hunt-attack") {
          playHuntAttackTone();
        } else if (eventType === "hunt-kill") {
          playHuntKillTone();
        }
      });
    }
  }, []);

  const handleSocialSound = useCallback((interactionType: string, agent: any) => {
    const notes = SOCIAL_TONE_SEQUENCES[interactionType] ?? [0, 2, 0];
    const sequence = toSequence(notes);
    const voiceData = agent?.voice;
    const voice = voiceData ? {
      waveform: (voiceData.waveform || "sine") as OscillatorType,
      pitchOffset: voiceData["pitch-offset"] ?? 0,
      vibratoDepth: voiceData["vibrato-depth"] ?? 0,
      attackTime: voiceData["attack-time"] ?? 0,
    } : undefined;
    playToneSequenceWithVoice(sequence, {
      noteDuration: NOTE_DURATION,
      gap: NOTE_GAP,
      gain: 0.6,
      voice,
    });
  }, []);

  const focusOnAgent = useCallback((agent: Agent) => {
    if (!hasPos(agent)) return;
    const [q, r] = agent.pos as AxialCoords;
    setSelectedAgentId(agent.id);
    setSelectedCell([q, r]);
    setFocusPos([q, r]);
    setFocusTrigger((prev) => prev + 1);
  }, []);

  const findTownCenter = useCallback((state: any): [number, number] | null => {
    if (!state) return null;
    if (Array.isArray(state.shrine) && state.shrine.length === 2) {
      return [Number(state.shrine[0]), Number(state.shrine[1])];
    }
    const tiles = state.tiles ?? {};
    const entry = Object.entries(tiles).find(([, tile]) => {
      const structure = (tile as any)?.structure;
      return structure === "campfire";
    });
    if (!entry) return null;
    const [key] = entry;
    const [q, r] = key.split(",").map((val) => Number(val)) as [number, number];
    if (Number.isNaN(q) || Number.isNaN(r)) return null;
    return [q, r];
  }, []);

  const focusOnTownCenter = useCallback((state: any) => {
    const center = findTownCenter(state);
    if (!center) return;
    setSelectedAgentId(null);
    setSelectedCell(center);
    setFocusPos(center);
    setFocusTrigger((prev) => prev + 1);
  }, [findTownCenter]);

    const [tracesCollapsed, setTracesCollapsed] = useState(true);
    const [jobsCollapsed, setJobsCollapsed] = useState(true);
    const [thoughtsCollapsed, setThoughtsCollapsed] = useState(true);
    const [mythCollapsed, setMythCollapsed] = useState(true);
    const [isInitializing, setIsInitializing] = useState(false);

  const handleSplashComplete = useCallback(() => {
    setAppState("menu");
  }, []);

  const handleNewGame = useCallback(() => {
    setAppState("simulation");
  }, []);

  const handleOllamaTest = useCallback(() => {
    setAppState("ollama-test");
  }, []);

  const handleBackToMenu = useCallback(() => {
    setAppState("menu");
  }, []);
   const [isRunning, setIsRunning] = useState(false);
   const [tickHealth, setTickHealth] = useState<{
     targetMs: number;
     tickMs: number;
     health: "healthy" | "degraded" | "unhealthy" | "unknown";
   } | null>(null);

  useEffect(() => {
     const handleKeyDown = (e: KeyboardEvent) => {
       if (e.code === "Space" && !e.repeat) {
         e.preventDefault();
         toggleRun();
       }
     };

     window.addEventListener("keydown", handleKeyDown);
     window.addEventListener("click", markUserInteraction);
     return () => {
       window.removeEventListener("keydown", handleKeyDown);
       window.removeEventListener("click", markUserInteraction);
     };
   }, [isRunning]);

   useEffect(() => {
     const interval = setInterval(() => {
       const now = Date.now();
       setSpeechBubbles((prev) => prev.filter((bubble) => now - bubble.timestamp < 3000));
     }, 500);
     return () => clearInterval(interval);
   }, []);


  const client = useMemo(() => {
    const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
    const wsUrl = backendOrigin.replace(/^http/, "ws").replace(/\/$/, "") + "/ws";
    return new WSClient(
      wsUrl,
      (m: WSMessage) => {
         if (m.op === "hello") {
           const state = normalizeSnapshot(m.state ?? {});
           const tv = state?.tile_visibility ?? state?.["tile-visibility"] ?? {};
           const rts = state?.revealed_tiles_snapshot ?? state?.["revealed-tiles-snapshot"] ?? {};
           setTick(state.tick ?? 0);
           setSnapshot(state);
           setTileVisibility(tv);
           setRevealedTilesSnapshot(rts);
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
            console.log("[tick] Received tick:", m.data?.tick);
            setTick(m.data?.tick ?? 0);
            const nextSnapshot = normalizeSnapshot(m.data?.snapshot ?? null);
            console.log("[tick] Setting snapshot with", nextSnapshot?.agents?.length, "agents");
            setSnapshot(nextSnapshot);
            playTone(440, 0.08);
            handleDeathTone(nextSnapshot);
            handleTickAudio(nextSnapshot);
          }
              if (m.op === "tick_delta") {
                const delta = m.data as any;
                const tv = delta?.tile_visibility ?? delta?.["tile-visibility"] ?? {};
                const rts = delta?.revealed_tiles_snapshot ?? delta?.["revealed-tiles-snapshot"] ?? {};
                setTick(delta?.tick ?? 0);
                setSnapshot((prev: any) => applyDelta(prev, delta));
                setVisibilityData(delta?.visibility ?? null);
                setTileVisibility(tv);
                setRevealedTilesSnapshot(rts);
                handleDeltaAudio(delta);
              }
        if (m.op === "trace") {
            const incoming = m.data as Trace;
            setTraces((prev) => {
              const next = [...prev, incoming];
              return next.slice(Math.max(0, next.length - CONFIG.data.MAX_TRACES));
            });
         }
         if (m.op === "books") {
           const newBooks = m.data?.books ?? {};
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
               const tv = state?.tile_visibility ?? state?.["tile-visibility"] ?? {};
               const rts = state?.revealed_tiles_snapshot ?? state?.["revealed-tiles-snapshot"] ?? {};
               setSnapshot(state);
               setTileVisibility(tv);
               setRevealedTilesSnapshot(rts);
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
           const si = m.data as any;
           if (si && typeof si.agent_1_id === "number" && typeof si.agent_2_id === "number") {
             const interactionName = (si.interaction_type || "social") as string;
             const agents = snapshot?.agents ?? [];
             const agent1 = agents.find((a: any) => a.id === si.agent_1_id);
             const agent2 = agents.find((a: any) => a.id === si.agent_2_id);
             if (agent1) handleSocialSound(interactionName, agent1);
             if (agent2) handleSocialSound(interactionName, agent2);
             setSpeechBubbles((prev) => [
               ...prev,
               {
                 agentId: si.agent_1_id,
                 text: interactionName,
                 interactionType: interactionName,
                 timestamp: Date.now()
               },
               {
                 agentId: si.agent_2_id,
                 text: interactionName,
                 interactionType: interactionName,
                 timestamp: Date.now()
               }
             ]);
           }
          }
        if (m.op === "tiles") {
           setSnapshot((prev: any) => {
             if (!prev) return prev;
             return { ...prev, tiles: normalizeKeyedMap(m.tiles) };
           });
         }
         if (m.op === "stockpiles") {
           setSnapshot((prev: any) => {
             if (!prev) return prev;
             return { ...prev, stockpiles: normalizeKeyedMap(m.stockpiles) };
           });
         }
         if (m.op === "agent_path") {
           setSnapshot((prev: any) => {
             if (!prev) return prev;
             const agentPath = { [m.agent_id]: m.path };
             return { ...prev, agentPath };
           });
         }
         if (m.op === "jobs") {
           setSnapshot((prev: any) => {
             if (!prev) return prev;
             return { ...prev, jobs: m.jobs };
           });
         }
        if (m.op === "runner_state") {
           setIsRunning(m.running);
           setFps(m.fps);
         }
         if (m.op === "tick_health") {
            const data = m.data ?? {};
            const targetMs = typeof data.targetMs === "number"
              ? data.targetMs
              : typeof data["target-ms"] === "number"
                ? data["target-ms"]
                : undefined;
            const tickMs = typeof data.tickMs === "number"
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
      (s) => setStatus(s)
    );
  }, []);

  useEffect(() => {
    client.connect();
    return () => client.close();
  }, [client]);

  // Initialize snapshot - fetch latest or create new one
  useEffect(() => {
    const initializeSnapshot = async () => {
      setIsInitializing(true);
      try {
        const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
        const response = await fetch(`${backendOrigin}/sim/state`);

          if (response.ok) {
            const state = normalizeSnapshot(await response.json());

          // Check if the state has meaningful data
          const hasData = state && (
            (state.tick && state.tick > 0) ||
            (state.agents && state.agents.length > 0) ||
            (state.ledger && Object.keys(state.ledger).length > 0)
          );

            if (hasData) {
                setTick(state.tick ?? 0);
                setSnapshot(state);
                prevSnapshotRef.current = state;
               if (state.map) {
                 setMapConfig(state.map as HexConfig);
               }
             if (!initialFocusRef.current) {
               focusOnTownCenter(state);
               initialFocusRef.current = true;
             }
            } else {
              createNewSnapshot();
            }
         } else {
           createNewSnapshot();
         }
       } catch (error) {
         createNewSnapshot();
      } finally {
        setIsInitializing(false);
      }
    };

    const createNewSnapshot = () => {
      const defaultSeed = Math.floor(Math.random() * CONFIG.data.DEFAULT_SEED_RANGE);
      client.send({ op: "reset", seed: defaultSeed, tree_density: treeDensity });
    };

    // Initialize snapshot when component mounts
    const timeoutId = setTimeout(() => {
      // Only initialize if WebSocket hasn't provided data yet
      if (!snapshot && status === "open") {
        initializeSnapshot();
      }
    }, CONFIG.ui.INITIALIZATION_TIMEOUT); // Wait for WebSocket "hello" message

    return () => clearTimeout(timeoutId);
    }, [client, snapshot, status]);

  useEffect(() => {
    if (!mapConfig || !mapConfig.bounds) return;
    const b = mapConfig.bounds as any;
    if (b.shape === "rect") {
      setWorldWidth(b.w);
      setWorldHeight(b.h);
    } else if (b.shape === "radius") {
      const size = (b.r * 2) + 1;
      setWorldWidth(size);
      setWorldHeight(size);
    }
  }, [mapConfig]);

   const toggleRun = () => {
     markUserInteraction();
     if (isRunning) {
       client.send({ op: "stop_run" });
       setIsRunning(false);
     } else {
       client.send({ op: "start_run" });
     }
   };

  const setFpsValue = (value: number) => {
    client.sendSetFps(value);
    setFps(value);
  };

   const sendTick = (n: number) => {
     if (isRunning) {
       console.log("[sendTick] Simulation is running, ignoring tick request");
       return;
     }
     client.send({ op: "tick", n });
   };
   const reset = (seed: number, bounds?: { w: number; h: number }, treeDensity?: number) => {
     console.log("[App] Resetting world with seed:", seed);
     markUserInteraction();
     const payload: { op: string; seed: number; bounds?: { w: number; h: number }; tree_density?: number } = { op: "reset", seed };
     if (bounds) {
       payload.bounds = bounds;
     }
     if (treeDensity !== undefined) {
       payload.tree_density = treeDensity;
     }
     setIsRunning(false);
     setTraces([]);
     setSelectedCell(null);
     setSelectedAgentId(null);
     setSpeechBubbles([]);
     setTileVisibility({});
     setRevealedTilesSnapshot({});
     client.send(payload);
   };

    const handleCellSelect = (cell: [number, number], agentId: number | null) => {
      if (buildMode) {
        client.sendPlaceWallGhost(cell);
      }
      setSelectedCell(cell);
      setSelectedAgentId(agentId);
    };

   const handleQueueBuild = (type: string, pos: [number, number], config?: { stockpile?: { resource?: string; max_qty?: number } }) => {
      client.sendQueueBuild(type, pos, config?.stockpile);
    };
  const agents = useMemo(() => {
    if (!snapshot?.agents) return [];
    return snapshot.agents as Agent[];
  }, [snapshot?.agents]);
   const jobs = useMemo(() => {
     if (!snapshot?.jobs) return [];
     return snapshot.jobs as any[];
   }, [snapshot?.jobs]);
  const calendar = useMemo(() => {
     if (!snapshot?.calendar) return null;
     return snapshot.calendar;
  }, [snapshot?.calendar]);

  const mythData = useMemo(() => {
    return {
      globalFavor: typeof snapshot?.favor === "number" ? snapshot.favor : 0.0,
      deities: snapshot?.deities ?? {}
    };
  }, [snapshot?.favor, snapshot?.deities]);

  const stockpileTotals = useMemo(() => {
    const totals: Record<string, number> = {};
    const stockpiles = snapshot?.stockpiles ?? {};
    const normalizeResource = (val: unknown) => (typeof val === "string" ? val.replace(/^:/, "") : "unknown");
    for (const stockpile of Object.values(stockpiles)) {
      const spRaw = stockpile as Record<string, unknown>;
      const resource = normalizeResource(spRaw.resource ?? spRaw[":resource"]);
      const currentQty = Number(spRaw.currentQty ?? spRaw["current-qty"] ?? 0) || 0;
      totals[resource] = (totals[resource] ?? 0) + currentQty;
    }
    return totals;
  }, [snapshot?.stockpiles]);

  const selectedTile = useMemo(() => {
    try {
      if (!selectedCell || !snapshot?.tiles) return null;
      return snapshot.tiles[`${selectedCell[0]},${selectedCell[1]}`] ?? null;
    } catch (error) {
      console.error("Error in selectedTile useMemo:", error);
      return null;
    }
  }, [selectedCell, snapshot?.tiles]);

  const selectedTileItems = useMemo(() => {
    if (!selectedCell || !snapshot?.items) return {};
    return snapshot.items[`${selectedCell[0]},${selectedCell[1]}`] ?? {};
  }, [selectedCell, snapshot?.items]);

  const selectedTileAgents = useMemo(() => {
    try {
      if (!selectedCell || !agents || agents.length === 0) return [];
      return agents.filter((a: Agent) => {
        try {
          if (!hasPos(a)) return false;
          const [aq, ar] = a.pos as AxialCoords;
          return aq === selectedCell[0] && ar === selectedCell[1];
        } catch (error) {
          console.error("Error filtering agent:", error, a);
          return false;
        }
      });
    } catch (error) {
      console.error("Error in selectedTileAgents useMemo:", error);
      return [];
    }
  }, [selectedCell, agents]);

  const selectedAgent = useMemo(() => {
    try {
      if (selectedAgentId == null || !agents || agents.length === 0) return null;
      return agents.find((a: Agent) => a.id === selectedAgentId) ?? null;
    } catch (error) {
      console.error("Error in selectedAgent useMemo:", error);
      return null;
    }
  }, [selectedAgentId, agents]);

  const getAgentJob = useCallback((agentId: number) => {
    try {
      if (!jobs || jobs.length === 0) return null;
      const targetAgent = agents?.find((a: Agent) => a.id === agentId);
      const currentJobId = targetAgent?.current_job ?? selectedAgent?.current_job;
      if (currentJobId == null) return null;
      return jobs.find((j: any) => j.id === currentJobId);
    } catch (error) {
      console.error("Error in getAgentJob:", error);
      return null;
    }
  }, [agents, jobs, selectedAgent]);

  const applyWorldSize = () => {
      if (worldWidth == null || worldHeight == null) return;
      reset(1, { w: worldWidth, h: worldHeight }, treeDensity);
    };

  if (appState === "splash") {
    return <SplashScreen onComplete={handleSplashComplete} />;
  }

  if (appState === "menu") {
    return <MainMenu onNewGame={handleNewGame} onOllamaTest={handleOllamaTest} />;
  }

  if (appState === "ollama-test") {
    return <OllamaTestPage onBack={handleBackToMenu} />;
  }

  return (
    <div
      onClick={() => markUserInteraction()}
      style={{ display: "grid", gridTemplateColumns: "1fr 320px 320px", overflow: "hidden", margin: 0 }}
    >
      <div style={{ height: "calc(100vh - 40px)", overflow: "auto", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center", position: "relative" }}>
        {/* Loading overlay for snapshot initialization */}
        {isInitializing && !snapshot && (
          <div style={{
            position: "absolute",
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: "rgba(255, 255, 255, 0.9)",
            display: "flex",
            flexDirection: "column",
            alignItems: "center",
            justifyContent: "center",
            zIndex: 1000,
            gap: 16
          }}>
            <div style={{ fontSize: "1.2em", fontWeight: "bold", color: "#333" }}>
              Initializing Simulation...
            </div>
            <div style={{ fontSize: "0.9em", color: "#666" }}>
              {status === "open" ? "Fetching latest snapshot..." : "Connecting to server..."}
            </div>
          </div>
        )}

        <SimulationCanvas
          snapshot={snapshot}
          mapConfig={mapConfig}
          selectedCell={selectedCell}
          selectedAgentId={selectedAgentId}
          agentPaths={agentPaths}
          onCellSelect={handleCellSelect}
          focusPos={focusPos}
          focusTrigger={focusTrigger}
          showRelationships={showRelationships}
          showNames={showNames}
          showStats={showStats}
          speechBubbles={speechBubbles}
          visibilityData={visibilityData}
          selectedVisibilityAgentId={selectedVisibilityAgentId}
          tileVisibility={tileVisibility}
          revealedTilesSnapshot={revealedTilesSnapshot}
        />

        <MemoryOverlay
          memories={memories}
          mapConfig={mapConfig}
          showMemories={showMemories}
          strengthThreshold={0.3}
        />

        <FacetControls
          mapConfig={mapConfig}
          facetLimit={facetLimit}
          visionRadius={visionRadius}
          onFacetLimitChange={handleFacetLimitChange}
          onVisionRadiusChange={handleVisionRadiusChange}
        />
      </div>

      <div style={{ height: "calc(100vh - 40px)", overflow: "auto", display: "flex", flexDirection: "column", gap: 12 }}>
        <StatusBar status={status} tickHealth={tickHealth} />

        <WorldInfoPanel calendar={calendar} />

        <ResourceTotalsPanel totals={stockpileTotals} />

         {/* Time controls */}
         <TickControls
            onTick={sendTick}
            onReset={() => reset(1, undefined, treeDensity)}
            isRunning={isRunning}
            onToggleRun={toggleRun}
            tick={tick}
            fps={fps}
            onSetFps={setFpsValue}
          />

          {/* Selected Panel */}
          <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8, flex: 1, overflow: "auto", backgroundColor: "rgba(255,255,255,0.98)", minHeight: 200 }}>
            <SelectedPanel
              selectedCell={selectedCell}
              selectedTile={selectedTile}
              selectedTileItems={selectedTileItems}
              selectedTileAgents={selectedTileAgents}
              selectedAgentId={selectedAgentId}
              selectedAgent={selectedAgent}
            />
         </div>

        {/* Factions Panel */}
        <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8, flex: 1, overflow: "auto", backgroundColor: "rgba(255,255,255,0.98)", minHeight: 200 }}>
          <FactionsPanel agents={agents} jobs={jobs} collapsible onFocusAgent={focusOnAgent} />
        </div>
         <JobQueuePanel jobs={jobs} collapsed={jobsCollapsed} onToggleCollapse={() => setJobsCollapsed(!jobsCollapsed)} />
       </div>

        <div style={{ height: "calc(100vh - 40px)", overflow: "auto", paddingRight: 8 }}>
          <MythPanel
            deities={mythData.deities}
            globalFavor={mythData.globalFavor}
            collapsed={mythCollapsed}
            onToggleCollapse={() => setMythCollapsed(!mythCollapsed)}
          />

          <BuildingPalette
            onQueueBuild={handleQueueBuild}
            selectedCell={selectedCell}
          />

          <div style={{ marginTop: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
            <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>Overlays</h3>
            <div style={{ display: "grid", gap: 8, fontSize: 12 }}>
              <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  checked={showRelationships}
                  onChange={(e) => setShowRelationships(e.target.checked)}
                />
                Relationship links
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  checked={showNames}
                  onChange={(e) => setShowNames(e.target.checked)}
                />
                Name labels
              </label>
              <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  checked={showStats}
                  onChange={(e) => setShowStats(e.target.checked)}
                />
                Stat pips
              </label>
            </div>
          </div>

          <VisibilityControlPanel
            agents={agents}
            selectedVisibilityAgentId={selectedVisibilityAgentId}
            onSelectVisibilityAgent={setSelectedVisibilityAgentId}
          />

          <div style={{ marginTop: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
           <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>World Size</h3>
            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
             <label style={{ fontSize: 12 }}>Width:</label>
               <input
                 type="number"
                 min={0}
                 max={CONFIG.data.MAX_WORLD_WIDTH}
                 value={worldWidth ?? 0}
                 onChange={(e) => {
                   const val = parseInt(e.target.value, 10);
                   if (!isNaN(val)) setWorldWidth(val);
                 }}
                 style={{ width: 60 }}
               />
               <label style={{ fontSize: 12 }}>Height:</label>
               <input
                 type="number"
                 min={0}
                 max={CONFIG.data.MAX_WORLD_HEIGHT}
                 value={worldHeight ?? 0}
                 onChange={(e) => {
                   const val = parseInt(e.target.value, 10);
                   if (!isNaN(val)) setWorldHeight(val);
                 }}
                 style={{ width: 60 }}
               />
             <button
               onClick={applyWorldSize}
               style={{ padding: "4px 8px", fontSize: 12 }}
             >
               Apply
             </button>
           </div>
         </div>

         <div style={{ marginTop: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
           <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>Tree Density</h3>
           <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
             <label style={{ fontSize: 12 }}>
               {(treeDensity * 100).toFixed(1)}%:
             </label>
              <input
                type="range"
                min={0}
                max={CONFIG.data.MAX_TREE_DENSITY}
                step={0.01}
                value={treeDensity}
               onChange={(e) => {
                 const val = parseFloat(e.target.value);
                 if (!isNaN(val)) setTreeDensity(val);
               }}
               style={{ flex: 1 }}
             />
           </div>
           <div style={{ fontSize: 11, opacity: 0.7, marginTop: 4 }}>
             ~{Math.floor((worldWidth ?? 0) * (worldHeight ?? 0) * treeDensity)} trees expected
           </div>
         </div>



         <div style={{ marginTop: 12 }}>
           <div
             style={{
               display: "flex",
               justifyContent: "space-between",
               alignItems: "center",
               cursor: "pointer",
               padding: "8px 0",
               borderBottom: tracesCollapsed ? "1px solid #ddd" : "none"
             }}
             onClick={() => setTracesCollapsed(!tracesCollapsed)}
           >
             <strong style={{ margin: 0 }}>Traces</strong>
             <span style={{ opacity: 0.7, marginRight: 8 }}>({traces.length})</span>
             <span style={{
               fontSize: "1.2em",
               color: "#666",
               transition: "transform 0.2s ease",
               transform: tracesCollapsed ? "rotate(-90deg)" : "rotate(0deg)"
             }}>
               ▼
             </span>
             </div>

            {!tracesCollapsed && (
              <div style={{ marginTop: 8 }}>
                <TraceFeed traces={traces} />
              </div>
            )}

            <LibraryPanel
              books={books}
              selectedBookId={selectedBookId}
              onSelectBook={setSelectedBookId}
            />
           </div> 

          <ThoughtsPanel
           agents={agents}
           selectedAgent={selectedAgent}
           collapsible
           collapsed={thoughtsCollapsed}
           onToggleCollapse={() => setThoughtsCollapsed(!thoughtsCollapsed)}
         />
       </div>
     </div>
   );
 }
