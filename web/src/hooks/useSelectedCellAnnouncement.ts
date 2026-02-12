import { useMemo } from "react";
import type { Agent } from "../types";

type UseSelectedCellAnnouncementOptions = {
  selectedCell: [number, number] | null;
  selectedAgentId: number | null;
  agents: Agent[] | undefined;
};

export function useSelectedCellAnnouncement({
  selectedCell,
  selectedAgentId,
  agents,
}: UseSelectedCellAnnouncementOptions) {
  return useMemo(() => {
    if (!selectedCell) {
      return "No tile selected.";
    }

    const [q, r] = selectedCell;
    const selectedAgent = selectedAgentId != null
      ? (agents ?? []).find((agent: Agent) => agent.id === selectedAgentId)
      : null;

    if (selectedAgentId != null && selectedAgent) {
      const role = typeof selectedAgent.role === "string" ? selectedAgent.role : "agent";
      return `Selected tile ${q}, ${r}. Agent ${selectedAgentId}, role ${role}.`;
    }

    return `Selected tile ${q}, ${r}.`;
  }, [agents, selectedAgentId, selectedCell]);
}
