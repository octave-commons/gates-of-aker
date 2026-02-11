import { describe, expect, it } from "vitest";
import { 
  clamp01, 
  fmt, 
  safeStringify, 
  colorForRole, 
  getAgentIcon,
  getMovementSteps,
  appendBounded,
  appendManyBounded,
  applyDelta
} from "../utils";

describe("utils", () => {
  describe("clamp01", () => {
    it("clamps numbers between 0 and 1", () => {
      expect(clamp01(-0.5)).toBe(0);
      expect(clamp01(0.5)).toBe(0.5);
      expect(clamp01(1.5)).toBe(1);
      expect(clamp01(0)).toBe(0);
      expect(clamp01(1)).toBe(1);
    });
  });

  describe("fmt", () => {
    it("formats numbers with 3 decimal places", () => {
      expect(fmt(1.234567)).toBe("1.235");
      expect(fmt(1.2)).toBe("1.200");
      expect(fmt(1)).toBe("1.000");
      expect(fmt(0)).toBe("0.000");
    });

    it("converts non-numbers to string", () => {
      expect(fmt("hello")).toBe("hello");
      expect(fmt(null)).toBe("");
      expect(fmt(undefined)).toBe("");
      expect(fmt({})).toBe("[object Object]");
      expect(fmt([])).toBe("");
    });
  });

  describe("safeStringify", () => {
    it("stringifies objects without circular references", () => {
      const obj = { a: 1, b: 2 };
      expect(safeStringify(obj)).toBe(JSON.stringify(obj));
    });

    it("handles circular references", () => {
      const obj: any = { a: 1 };
      obj.self = obj;
      const result = safeStringify(obj);
      expect(result).toContain("[Circular]");
    });

    it("handles different spacing options", () => {
      const obj = { a: 1, b: 2 };
      expect(safeStringify(obj, 2)).toContain("\n");
      expect(safeStringify(obj, 0)).not.toContain("\n");
    });
  });

  describe("colorForRole", () => {
    it("returns correct colors for known roles", () => {
      expect(colorForRole("priest")).toMatch(/^#[0-9a-f]{6}$/i);
      expect(colorForRole("knight")).toMatch(/^#[0-9a-f]{6}$/i);
      expect(colorForRole("champion")).toBe("#0f766e");
      expect(colorForRole("wolf")).toBe("#795548");
      expect(colorForRole("bear")).toBe("#5d4037");
      expect(colorForRole("deer")).toBe("#8d6e63");
    });

    it("returns default color for unknown roles", () => {
      expect(colorForRole("unknown")).toBe("#111");
      expect(colorForRole("")).toBe("#111");
      expect(colorForRole(undefined)).toBe("#111");
    });
  });

  describe("getAgentIcon", () => {
    it("returns correct icons for known roles", () => {
      expect(getAgentIcon("priest")).toBe("✝");
      expect(getAgentIcon("knight")).toBe("⚔");
      expect(getAgentIcon("champion")).toBe("🜂");
      expect(getAgentIcon("wolf")).toBe("🐺");
      expect(getAgentIcon("bear")).toBe("🐻");
      expect(getAgentIcon("deer")).toBe("🦌");
    });

    it("returns default icon for unknown roles", () => {
      expect(getAgentIcon("unknown")).toBe("👤");
      expect(getAgentIcon("")).toBe("👤");
      expect(getAgentIcon(undefined)).toBe("👤");
    });
  });



  describe("getMovementSteps", () => {
    it("calculates movement steps based on dexterity", () => {
      // Default dexterity (0.4)
      const defaultMovement = getMovementSteps();
      expect(defaultMovement.base).toBe(1);
      expect(defaultMovement.road).toBe(3); // 1 + (1 + Math.floor(0.4 * 3))

      // Higher dexterity
      const highDexMovement = getMovementSteps({ dexterity: 0.8 });
      expect(highDexMovement.base).toBe(2); // 1 + Math.floor(0.8 * 2)
      expect(highDexMovement.road).toBe(5); // 2 + (1 + Math.floor(0.8 * 3))

      // Lower dexterity
      const lowDexMovement = getMovementSteps({ dexterity: 0.2 });
      expect(lowDexMovement.base).toBe(1); // 1 + Math.floor(0.2 * 2)
      expect(lowDexMovement.road).toBe(2); // 1 + (1 + Math.floor(0.2 * 3))
    });

    it("handles zero dexterity", () => {
      const zeroDexMovement = getMovementSteps({ dexterity: 0 });
      expect(zeroDexMovement.base).toBe(1);
      expect(zeroDexMovement.road).toBe(2);
    });

    it("handles maximum dexterity", () => {
      const maxDexMovement = getMovementSteps({ dexterity: 1 });
      expect(maxDexMovement.base).toBe(3);
      expect(maxDexMovement.road).toBe(7);
    });
  });

  describe("appendBounded", () => {
    it("appends while under limit", () => {
      expect(appendBounded([1, 2], 3, 5)).toEqual([1, 2, 3]);
    });

    it("drops oldest item at limit", () => {
      expect(appendBounded([1, 2, 3], 4, 3)).toEqual([2, 3, 4]);
    });

    it("returns empty array when limit is zero", () => {
      expect(appendBounded([1, 2], 3, 0)).toEqual([]);
    });
  });

  describe("appendManyBounded", () => {
    it("appends incoming items when under limit", () => {
      expect(appendManyBounded([1, 2], [3, 4], 10)).toEqual([1, 2, 3, 4]);
    });

    it("keeps only newest items when overflowing", () => {
      expect(appendManyBounded([1, 2, 3], [4, 5], 4)).toEqual([2, 3, 4, 5]);
    });

    it("keeps tail of incoming when incoming exceeds limit", () => {
      expect(appendManyBounded([1, 2], [3, 4, 5, 6], 3)).toEqual([4, 5, 6]);
    });
  });

  describe("applyDelta agent ID handling", () => {
    it("keeps unchanged string-id agents when only a subset receives deltas", () => {
      const prev = {
        tick: 1,
        agents: [
          { id: "agent-a", role: "priest", pos: [0, 0] },
          { id: "agent-b", role: "knight", pos: [1, 0] },
        ],
      };
      const delta: any = {
        global_updates: { tick: 2, temperature: 0, daylight: 1 },
        changed_agents: {
          "agent-a": { pos: [0, 1] },
        },
      };

      const next = applyDelta(prev, delta) as any;
      expect(next.agents).toHaveLength(2);
      expect(next.agents.map((a: any) => a.id)).toEqual(["agent-a", "agent-b"]);
      expect(next.agents.find((a: any) => a.id === "agent-a")?.pos).toEqual([0, 1]);
      expect(next.agents.find((a: any) => a.id === "agent-b")?.pos).toEqual([1, 0]);
    });

    it("adds non-numeric agent IDs without coercing them to NaN", () => {
      const prev = {
        tick: 1,
        agents: [],
      };
      const delta: any = {
        global_updates: { tick: 2, temperature: 0, daylight: 1 },
        changed_agents: {
          "uuid-agent-1": { role: "priest", pos: [2, 3] },
        },
      };

      const next = applyDelta(prev, delta) as any;
      expect(next.agents).toHaveLength(1);
      expect(next.agents[0].id).toBe("uuid-agent-1");
      expect(next.agents[0].role).toBe("priest");
    });
  });

  describe("applyDelta bounded arrays", () => {
    it("caps combat events to configured limit", () => {
      const prev = {
        tick: 1,
        combat_events: Array.from({ length: 50 }, (_, idx) => ({ id: idx })),
      };
      const delta: any = {
        global_updates: { tick: 2, temperature: 0, daylight: 1 },
        changed_agents: {},
        combat_events: [{ id: 50 }, { id: 51 }],
      };

      const next = applyDelta(prev, delta) as any;
      expect(next.combat_events).toHaveLength(50);
      expect(next.combat_events[0]).toEqual({ id: 2 });
      expect(next.combat_events[49]).toEqual({ id: 51 });
    });

    it("caps traces to configured limit", () => {
      const prev = {
        tick: 1,
        traces: Array.from({ length: 250 }, (_, idx) => ({ id: idx })),
      };
      const delta: any = {
        global_updates: { tick: 2, temperature: 0, daylight: 1 },
        changed_agents: {},
        traces: [{ id: 250 }, { id: 251 }, { id: 252 }],
      };

      const next = applyDelta(prev, delta) as any;
      expect(next.traces).toHaveLength(250);
      expect(next.traces[0]).toEqual({ id: 3 });
      expect(next.traces[249]).toEqual({ id: 252 });
    });
  });
});
