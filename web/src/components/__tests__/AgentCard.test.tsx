import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AgentCard } from "../AgentCard";
import { Agent } from "../../types";

describe("AgentCard", () => {
  const baseAgent: Agent = {
    id: 1,
    role: "priest",
    pos: [3, 4],
    needs: {},
    recall: {}
  };

  it("renders agent id and role", () => {
    render(<AgentCard agent={baseAgent} />);

    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("priest")).toBeInTheDocument();
  });

  it("renders agent position when has pos", () => {
    render(<AgentCard agent={baseAgent} />);

    expect(screen.getByText("(3, 4)")).toBeInTheDocument();
  });

  it("renders 'pos:n/a' when agent has no position", () => {
    const agentNoPos: Agent = { ...baseAgent, pos: null };
    
    render(<AgentCard agent={agentNoPos} />);

    expect(screen.getByText("pos:n/a")).toBeInTheDocument();
  });

  it("renders beliefs count", () => {
    const agentWithBeliefs: Agent = { ...baseAgent, beliefs: { belief1: 0.5, belief2: 0.3 } };
    
    render(<AgentCard agent={agentWithBeliefs} />);

    expect(screen.getByText("beliefs: 2")).toBeInTheDocument();
  });

  it("renders facets count", () => {
    const agentWithFacets: Agent = {
      ...baseAgent,
      "top-facets": [{ facet: "fire", a: 0.5 }, { facet: "judgment", a: 0.3 }]
    };
    
    render(<AgentCard agent={agentWithFacets} />);

    expect(screen.getByText("facets: 2")).toBeInTheDocument();
  });

  it("renders top facets when not compact and facets exist", () => {
    const agentWithFacets: Agent = {
      ...baseAgent,
      "top-facets": [{ facet: "fire", a: 0.5 }, { facet: "winter", a: 0.3 }]
    };
    
    render(<AgentCard agent={agentWithFacets} />);

    expect(screen.getByText("fire")).toBeInTheDocument();
    expect(screen.getByText("winter")).toBeInTheDocument();
  });

  it("does not render facets when compact", () => {
    const agentWithFacets: Agent = {
      ...baseAgent,
      "top-facets": [{ facet: "fire", a: 0.5 }]
    };
    
    render(<AgentCard agent={agentWithFacets} compact />);

    expect(screen.queryByText("fire")).not.toBeInTheDocument();
  });

  it("shows '+X more' when facets exceed display limit", () => {
    const agentWithManyFacets: Agent = {
      ...baseAgent,
      "top-facets": Array.from({ length: 10 }, (_, i) => ({ facet: `facet${i}`, a: 0.1 }))
    };
    
    render(<AgentCard agent={agentWithManyFacets} compact={false} />);

    expect(screen.getByText("+2 more")).toBeInTheDocument();
  });

  describe("Needs Display", () => {
    it("renders food need bar", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.5, sleep: 0.8, warmth: 0.6 } };
      
      render(<AgentCard agent={agentWithNeeds} />);

      expect(screen.getByText("food:")).toBeInTheDocument();
    });

    it("renders sleep need bar", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.8, sleep: 0.5, warmth: 0.6 } };
      
      render(<AgentCard agent={agentWithNeeds} />);

      expect(screen.getByText("sleep:")).toBeInTheDocument();
    });

    it("renders warmth need bar", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.8, sleep: 0.8, warmth: 0.4 } };
      
      render(<AgentCard agent={agentWithNeeds} />);

      expect(screen.getByText("warmth:")).toBeInTheDocument();
    });

    it("uses high color when need > 0.7", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.8, sleep: 0.8, warmth: 0.9 } };

      const { container } = render(<AgentCard agent={agentWithNeeds} />);

      const foodLabel = screen.getByText("food:");
      const row = foodLabel.parentElement as HTMLElement | null;
      const barContainer = row?.querySelector("div");
      const foodProgressBar = barContainer?.querySelector("div") as HTMLElement | null;
      expect(foodProgressBar).toHaveStyle({ backgroundColor: "#4CAF50" });
    });

    it("uses low color when need < 0.3", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.2, sleep: 0.2, warmth: 0.2 } };

      const { container } = render(<AgentCard agent={agentWithNeeds} />);

      const foodLabel = screen.getByText("food:");
      const row = foodLabel.parentElement as HTMLElement | null;
      const barContainer = row?.querySelector("div");
      const foodProgressBar = barContainer?.querySelector("div") as HTMLElement | null;
      expect(foodProgressBar).toHaveStyle({ backgroundColor: "#f44336" });
    });

    it("uses mid color when need is between 0.3 and 0.7", () => {
      const agentWithNeeds = { ...baseAgent, needs: { food: 0.5, sleep: 0.5, warmth: 0.5 } };

      const { container } = render(<AgentCard agent={agentWithNeeds} />);

      const foodLabel = screen.getByText("food:");
      const row = foodLabel.parentElement as HTMLElement | null;
      const barContainer = row?.querySelector("div");
      const foodProgressBar = barContainer?.querySelector("div") as HTMLElement | null;
      expect(foodProgressBar).toHaveStyle({ backgroundColor: "#FFC107" });
    });
  });

  describe("Inventory Display", () => {
    it("renders inventory when items exist", () => {
      const agentWithInventory: Agent = {
        ...baseAgent,
        inventory: { wood: 5, food: 2 }
      };
      
      render(<AgentCard agent={agentWithInventory} />);

      expect(screen.getByText("Inventory:")).toBeInTheDocument();
      expect(screen.getByText("wood:5, food:2")).toBeInTheDocument();
    });

    it("renders inventory with zero items", () => {
      const agentWithInventory: Agent = {
        ...baseAgent,
        inventory: { wood: 0, food: 0 }
      };
      
      render(<AgentCard agent={agentWithInventory} />);

      expect(screen.getByText("wood:0, food:0")).toBeInTheDocument();
    });

    it("does not render inventory label when empty", () => {
      const agentNoInventory: Agent = {
        ...baseAgent,
        inventory: {}
      };
      
      render(<AgentCard agent={agentNoInventory} />);

      expect(screen.queryByText("Inventory:")).not.toBeInTheDocument();
    });
  });

  describe("Job Display", () => {
    it("shows job type when current job provided", () => {
      const currentJob = {
        type: ":job/eat",
        target: [5, 10],
        worker: 1,
        progress: 0.3,
        required: 1.0,
        state: ":in-progress"
      };
      
      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("Eating")).toBeInTheDocument();
    });

    it("shows chopping job type", () => {
      const currentJob = {
        type: ":job/chop-tree",
        target: [10, 5]
      };
      
      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("Chopping")).toBeInTheDocument();
    });

    it("shows sleeping job type", () => {
      const currentJob = {
        type: ":job/sleep",
        target: [3, 8]
      };
      
      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("Sleeping")).toBeInTheDocument();
    });

    it("shows hauling job type", () => {
      const currentJob = {
        type: ":job/haul",
        target: [15, 20]
      };
      
      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("Hauling")).toBeInTheDocument();
    });

    it("shows building job type", () => {
      const currentJob = {
        type: ":job/build-wall",
        target: [7, 12]
      };
      
      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("Building")).toBeInTheDocument();
    });

    it("does not show job when currentJob not provided", () => {
      render(<AgentCard agent={baseAgent} />);

      expect(screen.queryByText("Eating")).not.toBeInTheDocument();
      expect(screen.queryByText("Chopping")).not.toBeInTheDocument();
    });

    it("handles unknown job type", () => {
      const currentJob = {
        type: ":job/unknown",
        target: [5, 10]
      };

      render(<AgentCard agent={baseAgent} currentJob={currentJob} />);

      expect(screen.getByText("unknown")).toBeInTheDocument();
    });
  });

  describe("Sleep State", () => {
    it("shows sleep emoji when agent is asleep", () => {
      const sleepingAgent = { ...baseAgent, asleep: true };
      
      render(<AgentCard agent={sleepingAgent} />);

      expect(screen.getByText("💤")).toBeInTheDocument();
    });

    it("has different background when asleep", () => {
      const sleepingAgent = { ...baseAgent, asleep: true };
      
      const { container } = render(<AgentCard agent={sleepingAgent} />);
      
      expect(container.firstChild).toHaveStyle({ backgroundColor: "#e8f0fe", border: "1px solid #4285f4" });
      expect(container.firstChild).toHaveStyle({ opacity: "0.8" });
    });

    it("does not show sleep emoji when agent is awake", () => {
      render(<AgentCard agent={baseAgent} />);

      expect(screen.queryByText("💤")).not.toBeInTheDocument();
    });
  });

  describe("Compact Mode", () => {
    it("has smaller margins when compact", () => {
      const { container } = render(<AgentCard agent={baseAgent} compact />);

      const card = container.firstChild as HTMLElement;
      expect(card).toHaveStyle({ marginBottom: "4px", padding: "8px" });
    });

    it("has larger margins when not compact", () => {
      const { container } = render(<AgentCard agent={baseAgent} compact={false} />);

      const card = container.firstChild as HTMLElement;
      expect(card).toHaveStyle({ marginBottom: "8px", padding: "8px" });
    });
  });
});
