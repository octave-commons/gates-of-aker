import { describe, expect, it } from "vitest";
import { 
  clamp01, 
  fmt, 
  safeStringify, 
  colorForRole, 
  getAgentIcon,
  getMovementSteps
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
});