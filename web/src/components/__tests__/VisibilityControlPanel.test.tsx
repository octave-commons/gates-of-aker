import "@testing-library/jest-dom/vitest";
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { VisibilityControlPanel } from "../VisibilityControlPanel";

describe("VisibilityControlPanel", () => {
  const mockAgents = [
    {
      id: 1,
      name: "Alice",
      role: "priest",
      faction: "player",
      pos: [0, 0],
      needs: {},
      recall: {},
    },
    {
      id: 2,
      name: "Bob",
      role: "knight",
      faction: "player",
      pos: [1, 0],
      needs: {},
      recall: {},
    },
    {
      id: 3,
      name: "Charlie",
      role: "wolf",
      faction: "enemy",
      pos: [2, 0],
      needs: {},
      recall: {},
    },
    {
      id: 4,
      role: "deer",
      faction: "neutral",
      pos: [0, 1],
      needs: {},
      recall: {},
    },
  ];

  const defaultProps = {
    agents: mockAgents,
    selectedVisibilityAgentId: null,
    onSelectVisibilityAgent: vi.fn(),
  };

  describe("Visibility filtering", () => {
    it("shows only player faction agents", () => {
      render(<VisibilityControlPanel {...defaultProps} />);
      
      // Should show Alice and Bob (player faction)
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("Bob")).toBeInTheDocument();
      
      // Should not show Charlie (enemy faction) and deer (neutral)
      expect(screen.queryByText("Charlie")).not.toBeInTheDocument();
      expect(screen.queryByText("Agent 4")).not.toBeInTheDocument();
    });

    it("shows 'All visible' option", () => {
      render(<VisibilityControlPanel {...defaultProps} />);
      
      const allVisibleOption = screen.getByLabelText("All visible (no filter)");
      expect(allVisibleOption).toBeInTheDocument();
      expect(allVisibleOption).toBeChecked();
    });

    it("filters agents by player faction correctly", () => {
      const agentsWithMixedFactions = [
        ...mockAgents,
        {
          id: 5,
          name: "Dave",
          role: "champion",
          faction: "player",
          pos: [3, 0],
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithMixedFactions}
        />
      );
      
      // Should show all player faction agents
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("Bob")).toBeInTheDocument();
      expect(screen.getByText("Dave")).toBeInTheDocument();
      
      // Should not show non-player agents
      expect(screen.queryByText("Charlie")).not.toBeInTheDocument();
      expect(screen.queryByText("Agent 4")).not.toBeInTheDocument();
    });
  });

  describe("Agent selection", () => {
    it("shows agent role and position", () => {
      render(<VisibilityControlPanel {...defaultProps} />);
      
      expect(screen.getByText("Alice")).toBeInTheDocument();
      expect(screen.getByText("(priest at [0, 0])")).toBeInTheDocument();
      
      expect(screen.getByText("Bob")).toBeInTheDocument();
      expect(screen.getByText("(knight at [1, 0])")).toBeInTheDocument();
    });

    it("shows fallback name when agent name is missing", () => {
      const agentsWithoutNames = [
        {
          id: 7,
          role: "bear",
          faction: "player",
          pos: [0, 0],
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithoutNames}
        />
      );
      
      expect(screen.getByText("Agent 7")).toBeInTheDocument();
      expect(screen.getByText("(bear at [0, 0])")).toBeInTheDocument();
    });

    it("shows fallback role when agent role is missing", () => {
      const agentsWithoutRoles = [
        {
          id: 8,
          name: "Mystery",
          faction: "player",
          pos: [0, 0],
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithoutRoles}
        />
      );
      
      expect(screen.getByText("Mystery")).toBeInTheDocument();
      expect(screen.getByText("(unknown at [0, 0])")).toBeInTheDocument();
    });

    it("handles agents without position", () => {
      const agentsWithoutPosition = [
        {
          id: 9,
          name: "Ghost",
          role: "priest",
          faction: "player",
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithoutPosition}
        />
      );
      
      expect(screen.getByText("Ghost")).toBeInTheDocument();
      expect(screen.getByText("(priest at [, ])")).toBeInTheDocument();
    });
  });

  describe("Selected visibility state", () => {
    it("selects 'All visible' when selectedVisibilityAgentId is null", () => {
      render(
        <VisibilityControlPanel 
          {...defaultProps}
          selectedVisibilityAgentId={null}
        />
      );
      
      const allVisibleOption = screen.getByLabelText("All visible (no filter)");
      expect(allVisibleOption).toBeChecked();
    });

    it("selects specific agent when selectedVisibilityAgentId is set", () => {
      render(
        <VisibilityControlPanel 
          {...defaultProps}
          selectedVisibilityAgentId={2}
        />
      );
      
      const allVisibleOption = screen.getByLabelText("All visible (no filter)");
      expect(allVisibleOption).not.toBeChecked();
      
      const bobOption = screen.getByText("Bob");
      expect(bobOption).toBeInTheDocument();
    });

    it("calls onSelectVisibilityAgent when 'All visible' is selected", () => {
      const mockOnSelect = vi.fn();
      
      render(
        <VisibilityControlPanel 
          {...defaultProps}
          selectedVisibilityAgentId={2}
          onSelectVisibilityAgent={mockOnSelect}
        />
      );
      
      const allVisibleOption = screen.getByLabelText("All visible (no filter)");
      allVisibleOption.click();
      
      expect(mockOnSelect).toHaveBeenCalledWith(null);
    });

    it("calls onSelectVisibilityAgent when agent is selected", () => {
      const mockOnSelect = vi.fn();
      
      render(
        <VisibilityControlPanel 
          {...defaultProps}
          selectedVisibilityAgentId={null}
          onSelectVisibilityAgent={mockOnSelect}
        />
      );
      
      const aliceOption = screen.getByText("Alice");
      aliceOption.click();
      
      expect(mockOnSelect).toHaveBeenCalledWith(1);
    });
  });

  describe("Empty states", () => {
    it("shows message when no player agents exist", () => {
      const agentsWithoutPlayerFaction = [
        {
          id: 1,
          name: "Wolf1",
          role: "wolf",
          faction: "enemy",
          pos: [0, 0],
          needs: {},
          recall: {},
        },
        {
          id: 2,
          name: "Deer1",
          role: "deer",
          faction: "neutral",
          pos: [1, 0],
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithoutPlayerFaction}
        />
      );
      
      expect(screen.getByText("No player agents available")).toBeInTheDocument();
      expect(screen.queryByText("Wolf1")).not.toBeInTheDocument();
      expect(screen.queryByText("Deer1")).not.toBeInTheDocument();
    });

    it("shows 'All visible' option even when no player agents exist", () => {
      const agentsWithoutPlayerFaction = [
        {
          id: 1,
          role: "wolf",
          faction: "enemy",
          pos: [0, 0],
          needs: {},
          recall: {},
        },
      ];

      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={agentsWithoutPlayerFaction}
        />
      );
      
      expect(screen.getByLabelText("All visible (no filter)")).toBeInTheDocument();
      expect(screen.getByText("No player agents available")).toBeInTheDocument();
    });

    it("handles empty agents array", () => {
      render(
        <VisibilityControlPanel 
          {...defaultProps}
          agents={[]}
        />
      );
      
      expect(screen.getByText("No player agents available")).toBeInTheDocument();
      expect(screen.getByLabelText("All visible (no filter)")).toBeInTheDocument();
    });
  });

  describe("Component rendering", () => {
    it("has proper title and styling", () => {
      const { container } = render(<VisibilityControlPanel {...defaultProps} />);
      
      expect(screen.getByText("Visibility (LoS)")).toBeInTheDocument();
      expect(container.firstChild).toHaveStyle({
        padding: "12px",
        border: "1px solid #aaa",
        borderRadius: "8px",
      });
    });

    it("renders agents in grid layout", () => {
      const { container } = render(<VisibilityControlPanel {...defaultProps} />);
      
      expect(container.querySelector('[style*="display: grid"]')).toBeInTheDocument();
    });

    it("applies correct styling to agent labels", () => {
      render(<VisibilityControlPanel {...defaultProps} />);
      
      const aliceElement = screen.getByText("Alice");
      expect(aliceElement.parentElement).toHaveStyle({
        display: "flex",
        alignItems: "center",
        gap: "6px",
        cursor: "pointer",
      });
    });
  });
});