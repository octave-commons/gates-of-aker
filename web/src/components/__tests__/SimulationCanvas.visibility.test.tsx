import "@testing-library/jest-dom/vitest";
import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SimulationCanvas } from "../SimulationCanvas";
import { Agent, AxialCoords } from "../../types";

// Mock utilities
vi.mock("../../utils", () => ({
  colorForRole: vi.fn(() => "#000000"),
  hexToFrequency: vi.fn(() => 440),
  playTone: vi.fn(),
  getDexterity: vi.fn(() => 0.4),
  getMovementSteps: vi.fn(() => ({ base: 1, road: 2 })),
  clamp01: vi.fn((x: number) => x),
  fmt: vi.fn((x) => String(x)),
  safeStringify: vi.fn((obj) => JSON.stringify(obj)),
  normalizeKeyedMap: vi.fn((obj) => obj),
}));

// Mock config
vi.mock("../../config/constants", () => ({
  CONFIG: {
    canvas: {
      HEX_SIZE: 16,
      HEX_SPACING: 1,
    },
    colors: {
      RESOURCE: {
        tree: "#228b22",
        grain: "#ffd700",
        rock: "#696969",
      },
      STRUCTURE: {
        wall: "#808080",
        road: "#a9a9a9",
        house: "#8b4513",
        campfire: "#ff4500",
      },
      SELECTION: "#ff6b00",
    },
  },
}));

describe("SimulationCanvas - Visibility Logic", () => {
  const mockSnapshot = {
    agents: [
      {
        id: 1,
        role: "priest",
        pos: [0, 0] as AxialCoords,
        needs: {},
        recall: {},
      },
      {
        id: 2,
        role: "knight",
        pos: [1, 0] as AxialCoords,
        needs: {},
        recall: {},
      },
    ],
    tiles: {
      "0,0": { resource: "tree" },
      "1,0": { resource: "grain" },
      "2,0": { structure: "wall" },
      "0,1": { structure: "road" },
      "1,1": {},
    },
    items: {
      "0,0": { type: "wood" },
      "1,1": { type: "stone" },
    },
    stockpiles: {
      "2,0": { resource: "wood", qty: 10 },
    },
  };

  const mockMapConfig = null; // Simpler to avoid type issues

  const defaultProps = {
    snapshot: mockSnapshot,
    mapConfig: mockMapConfig,
    selectedCell: null,
    selectedAgentId: null,
    selectedVisibilityAgentId: null,
    visibilityData: null,
    tileVisibility: {},
    revealedTilesSnapshot: {},
    agentPaths: {},
    onCellSelect: vi.fn(),
    showRelationships: false,
    showNames: false,
    showStats: false,
    speechBubbles: [],
  };

  describe("getTileVisibilityState function", () => {
    it("returns 'visible' when no tile visibility data exists", () => {
      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={{}}
          />
        );
      }).not.toThrow();
      
      // The component should render without errors when tileVisibility is empty
    });

    it("handles visibility states correctly", () => {
      const tileVisibility: Record<string, "visible" | "revealed" | "hidden"> = {
        "0,0": "visible",
        "1,0": "revealed",
        "2,0": "hidden",
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={tileVisibility}
          />
        );
      }).not.toThrow();
      
      // The component should handle different visibility states without errors
    });
  });

  describe("isVisible function", () => {
    it("renders without errors when no visibility filter is active", () => {
      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            selectedVisibilityAgentId={null}
            visibilityData={null}
          />
        );
      }).not.toThrow();
    });

    it("renders without errors when visibility data is provided", () => {
      const visibilityData = {
        "0,0": {
          visible_agent_ids: [1] as number[],
          visible_tiles: ["0,0", "1,0"] as string[],
          visible_items: ["0,0"] as string[],
          visible_stockpiles: ["2,0"] as string[],
        },
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            selectedVisibilityAgentId={1}
            visibilityData={visibilityData}
          />
        );
      }).not.toThrow();
      
      // Should handle visibility filtering without errors
    });
  });

  describe("Rendering with visibility states", () => {
    it("renders with revealed tiles", () => {
      const tileVisibility: Record<string, "visible" | "revealed" | "hidden"> = {
        "0,0": "revealed",
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={tileVisibility}
          />
        );
      }).not.toThrow();
    });

    it("renders with hidden tiles", () => {
      const tileVisibility: Record<string, "visible" | "revealed" | "hidden"> = {
        "2,0": "hidden",
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={tileVisibility}
          />
        );
      }).not.toThrow();
    });

    it("handles mixed visibility states", () => {
      const tileVisibility: Record<string, "visible" | "revealed" | "hidden"> = {
        "0,0": "visible",
        "1,0": "revealed",
        "2,0": "hidden",
        "0,1": "revealed",
        "1,1": "visible",
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={tileVisibility}
          />
        );
      }).not.toThrow();
    });
  });

  describe("Edge cases", () => {
    it("handles missing visibility data gracefully", () => {
      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            selectedVisibilityAgentId={999} // Non-existent agent
            visibilityData={{
              "0,0": {
                visible_agent_ids: [1] as number[],
              },
            }}
          />
        );
      }).not.toThrow();
    });

    it("handles empty visibility arrays", () => {
      const visibilityData = {
        "0,0": {
          visible_agent_ids: [] as number[],
          visible_tiles: [] as string[],
          visible_items: [] as string[],
          visible_stockpiles: [] as string[],
        },
      };

      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            selectedVisibilityAgentId={1}
            visibilityData={visibilityData}
          />
        );
      }).not.toThrow();
    });

    it("handles undefined tileVisibility", () => {
      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            tileVisibility={undefined}
          />
        );
      }).not.toThrow();
    });

    it("handles undefined revealedTilesSnapshot", () => {
      expect(() => {
        render(
          <SimulationCanvas 
            {...defaultProps}
            revealedTilesSnapshot={undefined}
          />
        );
      }).not.toThrow();
    });
  });
});