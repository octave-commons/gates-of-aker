import { useCallback, useReducer } from "react";

export type CollapsiblePanel = "traces" | "jobs" | "thoughts" | "myth";

type PanelCollapseState = Record<CollapsiblePanel, boolean>;

type PanelCollapseAction = {
  type: "toggle";
  panel: CollapsiblePanel;
};

const initialPanelCollapseState: PanelCollapseState = {
  traces: true,
  jobs: true,
  thoughts: true,
  myth: true,
};

const panelCollapseReducer = (state: PanelCollapseState, action: PanelCollapseAction): PanelCollapseState => {
  switch (action.type) {
    case "toggle":
      return {
        ...state,
        [action.panel]: !state[action.panel],
      };
    default:
      return state;
  }
};

export function useCollapsedPanels() {
  const [collapsedPanels, dispatchCollapsedPanels] = useReducer(panelCollapseReducer, initialPanelCollapseState);

  const togglePanelCollapse = useCallback((panel: CollapsiblePanel) => {
    dispatchCollapsedPanels({ type: "toggle", panel });
  }, []);

  return {
    tracesCollapsed: collapsedPanels.traces,
    jobsCollapsed: collapsedPanels.jobs,
    thoughtsCollapsed: collapsedPanels.thoughts,
    mythCollapsed: collapsedPanels.myth,
    togglePanelCollapse,
  };
}
