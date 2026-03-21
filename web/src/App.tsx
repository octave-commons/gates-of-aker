import { useCallback, useRef, useState } from "react";
import { Routes, Route, useNavigate } from "react-router-dom";
import { playDeathTone, playToneSequence, playToneSequenceWithVoice, getScaleFrequency, markUserInteraction, playHuntStartTone, playHuntAttackTone, playHuntKillTone } from "./audio";
import {
  FactionsPanel,
  MythPanel,
  SelectedPanel,
  SimulationCanvas,
  StatusBar,
  TickControls,
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
  ForkTalesPanel,
  ForkTalesPage,
} from "./components";
import { TraceFeed } from "./components/TraceFeed";
import { Agent, Snapshot, hasPos } from "./types";
import type { AxialCoords } from "./hex";
import type { Book } from "./components/LibraryPanel";



type SpeechBubble = {
  agentId: number;
  text: string;
  interactionType: string;
  timestamp: number;
};
import { normalizeKeyedMap } from "./utils";
import { CONFIG } from "./config/constants";
import { useWebSocket } from "./hooks/useWebSocket";
import { useSimulationState } from "./hooks/useSimulationState";
import { useSimulationInitialization } from "./hooks/useSimulationInitialization";
import { useCollapsedPanels } from "./hooks/useCollapsedPanels";
import { useWorldSizeFromMapConfig } from "./hooks/useWorldSizeFromMapConfig";
import { useExpiringTimestampList } from "./hooks/useExpiringTimestampList";
import { useGlobalSimulationShortcuts } from "./hooks/useGlobalSimulationShortcuts";
import { useSimulationSelectors } from "./hooks/useSimulationSelectors";
import { useResetDismissedOnOpen } from "./hooks/useResetDismissedOnOpen";
import { useSyncRef } from "./hooks/useSyncRef";
import { useSimulationControls } from "./hooks/useSimulationControls";
import { useSimulationMessageHandler } from "./hooks/useSimulationMessageHandler";
import { logDebug } from "./logging";

const normalizeBooks = (books: unknown): Record<string, Book> => {
  if (!books || typeof books !== "object") {
    return {};
  }

  const source = books as Record<string, unknown>;
  return Object.entries(source).reduce((acc, [bookId, value]) => {
    if (!value || typeof value !== "object") {
      return acc;
    }
    const raw = value as Record<string, unknown>;
    const id = typeof raw.id === "string" ? raw.id : String(bookId);
    const title = typeof raw.title === "string" ? raw.title : "Untitled";
    const text = typeof raw.text === "string" ? raw.text : "";
    const createdAt = typeof raw.created_at === "number" ? raw.created_at : 0;
    const createdBy = typeof raw.created_by === "string" ? raw.created_by : "unknown";
    const traceIds = Array.isArray(raw.trace_ids) ? raw.trace_ids.filter((traceId): traceId is string => typeof traceId === "string") : [];
    const readCount = typeof raw.read_count === "number" ? raw.read_count : 0;

    return {
      ...acc,
      [bookId]: {
        id,
        title,
        text,
        created_at: createdAt,
        created_by: createdBy,
        trace_ids: traceIds,
        read_count: readCount,
      },
    };
  }, {} as Record<string, Book>);
};

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

      const normalizeSnapshot = (state: unknown): Snapshot => {
        if (!state || typeof state !== "object") {
          return {};
        }
        const rawState = state as Record<string, unknown>;
        const inputAgents = Array.isArray(rawState.agents) ? rawState.agents.length : 0;
        const inputTiles = normalizeKeyedMap(rawState.tiles as Record<string, unknown> | null | undefined);
        logDebug("[App] normalizeSnapshot - input agents:", inputAgents, "tiles:", Object.keys(inputTiles).length);

       const normalizedTiles = normalizeKeyedMap(rawState.tiles as Record<string, unknown> | null | undefined);
       const normalizedItems = normalizeKeyedMap(rawState.items as Record<string, unknown> | null | undefined);
       const normalizedStockpiles = normalizeKeyedMap(rawState.stockpiles as Record<string, unknown> | null | undefined);

        const normalized: Snapshot = {
          ...rawState,
          tiles: normalizedTiles as Snapshot["tiles"],
          items: normalizedItems as Snapshot["items"],
          stockpiles: normalizedStockpiles as Snapshot["stockpiles"],
        };
       
       logDebug("[App] normalizeSnapshot - output agents:", normalized.agents?.length ?? 0, "tiles:", Object.keys(normalized.tiles ?? {}).length);
       return normalized;
     };

export type AgentVisibility = {
  id: number;
  visibleTiles: Set<string>;
};

export function App() {
  const navigate = useNavigate();
  const {
    tick,
    setTick,
    snapshot,
    setSnapshot,
    mapConfig,
    setMapConfig,
    traces,
    setTraces,
    agentPaths,
    books,
    setBooks,
    selectedBookId,
    setSelectedBookId,
    memories,
    setMemories,
    isInitializing,
    setIsInitializing,
  } = useSimulationState();
   const aliveAgentsRef = useRef<Set<number>>(new Set());
   const prevSnapshotRef = useRef<Snapshot | null>(null);
   const snapshotRef = useRef<Snapshot | null>(null);
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
      const [visibilityData, setVisibilityData] = useState<Record<string, unknown> | null>(null);
      const [tileVisibility, setTileVisibility] = useState<Record<string, "hidden" | "revealed" | "visible">>({});
      const [revealedTilesSnapshot, setRevealedTilesSnapshot] = useState<Record<string, unknown>>({});
      const [agentVisibilityMaps, setAgentVisibilityMaps] = useState<Record<number, Set<string>>>({});

  useSyncRef(snapshotRef, snapshot);

  const [buildMode] = useState(false);
    const [fps, setFps] = useState(15);
    const [facetLimit, setFacetLimit] = useState(16);
    const [visionRadius, setVisionRadius] = useState(10);
    const [showMemories, setShowMemories] = useState(false);
    const [wsErrorDismissed, setWsErrorDismissed] = useState(false);
 
   const [worldWidth, setWorldWidth] = useState<number | null>(null);
   const [worldHeight, setWorldHeight] = useState<number | null>(null);
   const [treeDensity, setTreeDensity] = useState<number>(CONFIG.data.DEFAULT_TREE_DENSITY);

  const getAliveAgents = useCallback((state: Snapshot | null) => {
    const alive = new Set<number>();
    if (!state?.agents) return alive;
    state.agents.forEach((agent: Agent) => {
      const status = agent?.status ?? {};
      const aliveFlag = status["alive?"] ?? status.alive ?? true;
      if (aliveFlag && typeof agent.id === "number") {
        alive.add(agent.id);
      }
    });
    return alive;
  }, []);

  const handleDeathTone = useCallback((nextSnapshot: Snapshot | null) => {
    if (!nextSnapshot) return;
    const previousAlive = aliveAgentsRef.current;
    const currentAlive = getAliveAgents(nextSnapshot);
    const died = [...previousAlive].some((id) => !currentAlive.has(id));
    if (died) {
      playDeathTone();
    }
    aliveAgentsRef.current = currentAlive;
  }, [getAliveAgents]);

  const handleTickAudio = useCallback((nextSnapshot: Snapshot | null) => {
     if (!nextSnapshot) return;
     const prevSnapshot = prevSnapshotRef.current;
     prevSnapshotRef.current = nextSnapshot;
     if (!prevSnapshot) return;

     const getField = (obj: Record<string, unknown> | null | undefined, key: string) =>
       obj?.[key] ?? obj?.[key.replace(/-/g, "_")] ?? obj?.[key.replace(/-(\w)/g, (_: string, c: string) => c.toUpperCase())];

      const sequences: number[][] = [];

      const prevJobs = new Map<string, Record<string, unknown>>();
      const prevJobsArray = Array.isArray(prevSnapshot.jobs) ? prevSnapshot.jobs : Object.values(prevSnapshot.jobs ?? {});
      prevJobsArray.forEach((job: Record<string, unknown>) => {
        if (job?.id) {
          prevJobs.set(String(job.id), job);
        }
      });
      const nextJobIds = new Set<string>();
      const nextJobsArray = Array.isArray(nextSnapshot.jobs) ? nextSnapshot.jobs : Object.values(nextSnapshot.jobs ?? {});
      nextJobsArray.forEach((job: Record<string, unknown>) => {
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

      const prevAgents = new Map<number, Agent>();
      (prevSnapshot.agents ?? []).forEach((agent: Agent) => {
        if (typeof agent?.id === "number") {
          prevAgents.set(agent.id, agent);
        }
      });

      (nextSnapshot.agents ?? []).forEach((agent: Agent) => {
        if (typeof agent?.id !== "number") return;
        const prevAgent = prevAgents.get(agent.id);
        if (!prevAgent) return;
        const status = agent.status ?? {};
        const alive = status["alive?"] ?? status.alive ?? true;
        if (!alive) return;
        const prevNeeds = prevAgent.needs ?? {};
        const nextNeeds = agent.needs ?? {};
        const thresholds =
          (getField(agent, "need-thresholds") ?? getField(agent, "needThresholds") ?? getField(agent, "need_thresholds") ?? {}) as Record<string, unknown>;
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

  const handleDeltaAudio = useCallback((delta: Record<string, unknown>) => {
    if (!delta) return;

    if (Array.isArray(delta.combat_events)) {
      delta.combat_events.forEach((ce) => {
        const eventType = (ce as Record<string, unknown>).type as string;
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

  const handleSocialSound = useCallback((interactionType: string, agent: Agent | Record<string, unknown> | null | undefined) => {
    const notes = SOCIAL_TONE_SEQUENCES[interactionType] ?? [0, 2, 0];
    const sequence = toSequence(notes);
    const voiceData = (agent as Record<string, unknown> | null | undefined)?.voice as Record<string, unknown> | undefined;
    const voice = voiceData ? {
      waveform: (voiceData.waveform || "sine") as OscillatorType,
      pitchOffset: typeof voiceData["pitch-offset"] === "number" ? voiceData["pitch-offset"] : 0,
      vibratoDepth: typeof voiceData["vibrato-depth"] === "number" ? voiceData["vibrato-depth"] : 0,
      attackTime: typeof voiceData["attack-time"] === "number" ? voiceData["attack-time"] : 0,
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
    setSelectedAgentId(typeof agent.id === 'number' ? agent.id : Number(agent.id));
    setSelectedCell([q, r]);
    setFocusPos([q, r]);
    setFocusTrigger((prev) => prev + 1);
  }, []);

  const findTownCenter = useCallback((state: Snapshot | null): [number, number] | null => {
    if (!state) return null;
    if (Array.isArray(state.shrine) && state.shrine.length === 2) {
      return [Number(state.shrine[0]), Number(state.shrine[1])];
    }
    const tiles = state.tiles ?? {};
    const entry = Object.entries(tiles).find(([, tile]) => {
      const structure = (tile as Record<string, unknown>)?.structure;
      return structure === "campfire";
    });
    if (!entry) return null;
    const [key] = entry;
    const [q, r] = key.split(",").map((val) => Number(val)) as [number, number];
    if (Number.isNaN(q) || Number.isNaN(r)) return null;
    return [q, r];
  }, []);

  const focusOnTownCenter = useCallback((state: Snapshot | null) => {
    const center = findTownCenter(state);
    if (!center) return;
    setSelectedAgentId(null);
    setSelectedCell(center);
    setFocusPos(center);
    setFocusTrigger((prev) => prev + 1);
  }, [findTownCenter]);

  const {
    tracesCollapsed,
    jobsCollapsed,
    thoughtsCollapsed,
    mythCollapsed,
    togglePanelCollapse,
  } = useCollapsedPanels();

  const handleSplashComplete = useCallback(() => {
    navigate("/menu");
  }, [navigate]);

  const handleNewGame = useCallback(() => {
    navigate("/sim");
  }, [navigate]);

  const handleOllamaTest = useCallback(() => {
    navigate("/ollama-test");
  }, [navigate]);

  const handleForkTales = useCallback(() => {
    navigate("/fork-tales");
  }, [navigate]);

  const handleBackToMenu = useCallback(() => {
    navigate("/menu");
  }, [navigate]);
   const [isRunning, setIsRunning] = useState(false);
   const [tickHealth, setTickHealth] = useState<{
     targetMs: number;
     tickMs: number;
     health: "healthy" | "degraded" | "unhealthy" | "unknown";
   } | null>(null);

  useExpiringTimestampList<SpeechBubble>({ setItems: setSpeechBubbles, maxAgeMs: 3000, intervalMs: 500 });


  const handleWSMessage = useSimulationMessageHandler({
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
  });

  const { client, status, reconnect } = useWebSocket({ onMessage: handleWSMessage });

  useResetDismissedOnOpen({ status, setDismissed: setWsErrorDismissed });

  useSimulationInitialization({
    client,
    snapshot,
    status,
    treeDensity,
    defaultSeedRange: CONFIG.data.DEFAULT_SEED_RANGE,
    initializationTimeout: CONFIG.ui.INITIALIZATION_TIMEOUT,
    normalizeSnapshot,
    focusOnTownCenter,
    initialFocusRef,
    prevSnapshotRef,
    setIsInitializing,
    setTick,
    setSnapshot,
    setMapConfig,
  });

  useWorldSizeFromMapConfig({ mapConfig, setWorldWidth, setWorldHeight });

  const {
    toggleRun,
    setFpsValue,
    sendTick,
    reset,
    handleFacetLimitChange,
    handleVisionRadiusChange,
    applyWorldSize,
  } = useSimulationControls({
    client,
    isRunning,
    treeDensity,
    worldWidth,
    worldHeight,
    setIsRunning,
    clearTraces: () => setTraces([]),
    setSelectedCell,
    setSelectedAgentId,
    clearSpeechBubbles: () => setSpeechBubbles([]),
    setTileVisibility,
    setRevealedTilesSnapshot,
    setFps,
    setFacetLimit,
    setVisionRadius,
    markUserInteraction,
  });

  useGlobalSimulationShortcuts({ onToggleRun: toggleRun, onMarkInteraction: markUserInteraction });

    const handleCellSelect = useCallback((cell: [number, number], agentId: number | null) => {
      if (buildMode) {
        client.sendPlaceWallGhost(cell);
      }
      setSelectedCell(cell);
      setSelectedAgentId(agentId);
    }, [buildMode, client]);

    const handleQueueBuild = (type: string, pos: [number, number], config?: { stockpile?: { resource?: string; max_qty?: number } }) => {
      client.sendQueueBuild(type, pos, config?.stockpile);
     };

  const {
    agents,
    jobs,
    calendar,
    mythData,
    stockpileTotals,
    selectedTile,
    selectedTileItems,
    selectedTileAgents,
    selectedAgent,
  } = useSimulationSelectors({
    snapshot,
    selectedCell,
    selectedAgentId,
  });

  return (
    <Routes>
      <Route path="/" element={<SplashScreen onComplete={handleSplashComplete} />} />
      <Route path="/menu" element={<MainMenu onNewGame={handleNewGame} onOllamaTest={handleOllamaTest} onForkTales={handleForkTales} />} />
      <Route path="/ollama-test" element={<OllamaTestPage onBack={handleBackToMenu} />} />
      <Route path="/fork-tales" element={<ForkTalesPage onBack={handleBackToMenu} />} />
      <Route path="/sim" element={(
    <div
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

        {status === "error" && !wsErrorDismissed && (
          <div
            role="alert"
            style={{
              border: "1px solid #fecaca",
              backgroundColor: "#fef2f2",
              color: "#991b1b",
              borderRadius: 8,
              padding: 10,
              fontSize: 12,
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              gap: 8,
            }}
          >
            <span>WebSocket connection error. The client will keep retrying in the background.</span>
            <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
              <button
                type="button"
                onClick={() => reconnect()}
                style={{
                  border: "1px solid #991b1b",
                  backgroundColor: "#fff",
                  color: "#991b1b",
                  borderRadius: 4,
                  padding: "2px 8px",
                  cursor: "pointer",
                  fontSize: 12,
                }}
              >
                Retry now
              </button>
              <button
                type="button"
                onClick={() => setWsErrorDismissed(true)}
                style={{
                  border: "1px solid #991b1b",
                  backgroundColor: "#fff",
                  color: "#991b1b",
                  borderRadius: 4,
                  padding: "2px 8px",
                  cursor: "pointer",
                  fontSize: 12,
                }}
              >
                Dismiss
              </button>
            </div>
          </div>
        )}

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
               selectedAgentId={selectedVisibilityAgentId}
               selectedAgent={selectedAgent}
               selectedVisibilityAgentId={selectedVisibilityAgentId}
               agentVisibilityMaps={agentVisibilityMaps}
               agents={agents}
               onSetVisibilityAgentId={setSelectedVisibilityAgentId}
               tileVisibility={tileVisibility}
             />
         </div>

        {/* Factions Panel */}
        <div style={{ padding: 12, border: "1px solid #aaa", borderRadius: 8, flex: 1, overflow: "auto", backgroundColor: "rgba(255,255,255,0.98)", minHeight: 200 }}>
          <FactionsPanel agents={agents} jobs={jobs} collapsible onFocusAgent={focusOnAgent} />
        </div>
         <JobQueuePanel jobs={jobs} collapsed={jobsCollapsed} onToggleCollapse={() => togglePanelCollapse("jobs")} />
       </div>

        <div style={{ height: "calc(100vh - 40px)", overflow: "auto", paddingRight: 8 }}>
          <MythPanel
            deities={mythData.deities ?? {}}
            globalFavor={mythData.globalFavor}
            collapsed={mythCollapsed}
            onToggleCollapse={() => togglePanelCollapse("myth")}
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
              <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
                <input
                  type="checkbox"
                  checked={showMemories}
                  onChange={(e) => setShowMemories(e.target.checked)}
                />
                Memory overlay
              </label>
            </div>
          </div>

          <VisibilityControlPanel
            agents={agents}
            selectedVisibilityAgentId={selectedVisibilityAgentId}
            onSelectVisibilityAgent={setSelectedVisibilityAgentId}
          />

          <ForkTalesPanel />

          <div style={{ marginTop: 12, padding: 12, border: "1px solid #aaa", borderRadius: 8 }}>
           <h3 style={{ margin: "0 0 8px 0", fontSize: 14 }}>World Size</h3>
            <div style={{ display: "flex", gap: 8, alignItems: "center" }}>
              <label htmlFor="world-width" style={{ fontSize: 12 }}>Width:</label>
                <input
                  id="world-width"
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
                <label htmlFor="world-height" style={{ fontSize: 12 }}>Height:</label>
                <input
                  id="world-height"
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
                type="button"
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
              <label htmlFor="tree-density" style={{ fontSize: 12 }}>
                {(treeDensity * 100).toFixed(1)}%:
              </label>
               <input
                 id="tree-density"
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
            <button
              type="button"
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                cursor: "pointer",
                padding: "8px 0",
                borderBottom: tracesCollapsed ? "1px solid #ddd" : "none",
                width: "100%",
                background: "transparent",
                border: "none",
              }}
               onClick={() => togglePanelCollapse("traces")}
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
              </button>

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
           onToggleCollapse={() => togglePanelCollapse("thoughts")}
         />
        </div>
      </div>
    )} />
    </Routes>
  );
}
