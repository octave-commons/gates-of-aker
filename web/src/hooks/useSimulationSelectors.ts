import { useCallback, useMemo } from "react";
import type { Agent, AxialCoords, Job, Snapshot } from "../types";
import { hasPos } from "../types";

type UseSimulationSelectorsOptions = {
  snapshot: Snapshot | null;
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
};

type MythData = {
  globalFavor: number;
  deities: NonNullable<Snapshot["deities"]>;
};

export function useSimulationSelectors({
  snapshot,
  selectedCell,
  selectedAgentId,
}: UseSimulationSelectorsOptions) {
  const agents = useMemo(() => {
    if (!snapshot?.agents) return [];
    return snapshot.agents as Agent[];
  }, [snapshot?.agents]);

  const jobs = useMemo(() => {
    if (!snapshot?.jobs) return [];
    return Array.isArray(snapshot.jobs) ? snapshot.jobs : Object.values(snapshot.jobs);
  }, [snapshot?.jobs]);

  const calendar = useMemo(() => {
    if (!snapshot?.calendar) return null;
    return snapshot.calendar;
  }, [snapshot?.calendar]);

  const mythData = useMemo<MythData>(() => ({
    globalFavor: typeof snapshot?.favor === "number" ? snapshot.favor : 0,
    deities: snapshot?.deities ?? {},
  }), [snapshot?.deities, snapshot?.favor]);

  const stockpileTotals = useMemo(() => {
    const totals: Record<string, number> = {};
    const stockpiles = snapshot?.stockpiles ?? {};
    const normalizeResource = (value: unknown) => (typeof value === "string" ? value.replace(/^:/, "") : "unknown");

    for (const stockpile of Object.values(stockpiles)) {
      const stockpileRecord = stockpile as Record<string, unknown>;
      const resource = normalizeResource(stockpileRecord.resource ?? stockpileRecord[":resource"]);
      const currentQty = Number(stockpileRecord.currentQty ?? stockpileRecord["current-qty"] ?? 0) || 0;
      totals[resource] = (totals[resource] ?? 0) + currentQty;
    }

    return totals;
  }, [snapshot?.stockpiles]);

  const selectedTile = useMemo(() => {
    if (!selectedCell || !snapshot?.tiles) return null;
    const tileKey = `${selectedCell[0]},${selectedCell[1]}`;
    return snapshot.tiles[tileKey] ?? null;
  }, [selectedCell, snapshot?.tiles]);

  const selectedTileItems = useMemo(() => {
    if (!selectedCell || !snapshot?.items) return {};
    return snapshot.items[`${selectedCell[0]},${selectedCell[1]}`] ?? {};
  }, [selectedCell, snapshot?.items]);

  const selectedTileAgents = useMemo(() => {
    if (!selectedCell || agents.length === 0) return [];

    return agents.filter((agent) => {
      if (!hasPos(agent)) return false;
      const [agentQ, agentR] = agent.pos as AxialCoords;
      return agentQ === selectedCell[0] && agentR === selectedCell[1];
    });
  }, [agents, selectedCell]);

  const selectedAgent = useMemo(() => {
    if (selectedAgentId == null || agents.length === 0) return null;
    return agents.find((agent) => agent.id === selectedAgentId) ?? null;
  }, [agents, selectedAgentId]);

  const getAgentJob = useCallback((agentId: number): Job | undefined | null => {
    if (jobs.length === 0) return null;
    const targetAgent = agents.find((agent) => agent.id === agentId);
    const currentJobId = targetAgent?.current_job ?? selectedAgent?.current_job;
    if (currentJobId == null) return null;
    return jobs.find((job: Job) => job.id === currentJobId);
  }, [agents, jobs, selectedAgent]);

  return {
    agents,
    jobs,
    calendar,
    mythData,
    stockpileTotals,
    selectedTile,
    selectedTileItems,
    selectedTileAgents,
    selectedAgent,
    getAgentJob,
  };
}
