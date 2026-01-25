import { describe, expect, it, vi } from "vitest";
import { normalizeKeyedMap } from "../utils";

describe("Snapshot Processing Utils", () => {
  describe("normalizeKeyedMap", () => {
    it("normalizes null/undefined input", () => {
      expect(normalizeKeyedMap(null)).toEqual({});
      expect(normalizeKeyedMap(undefined)).toEqual({});
    });

    it("normalizes empty object", () => {
      expect(normalizeKeyedMap({})).toEqual({});
    });

    it("normalizes string-keyed object", () => {
      const input = {
        "0,0": { resource: "tree" },
        "1,0": { resource: "grain" },
        "2,0": { structure: "wall" },
      };
      
      const result = normalizeKeyedMap(input);
      expect(result).toEqual(input);
    });

    it("normalizes number-keyed object", () => {
      const input = {
        0: { resource: "tree" },
        1: { resource: "grain" },
        2: { structure: "wall" },
      };
      
      const result = normalizeKeyedMap(input);
      expect(result).toEqual({
        "0": { resource: "tree" },
        "1": { resource: "grain" },
        "2": { structure: "wall" },
      });
    });

    it("handles mixed key types", () => {
      const input = {
        "0,0": { resource: "tree" },
        1: { resource: "grain" },
        2: { structure: "wall" },
      };
      
      const result = normalizeKeyedMap(input);
      expect(result).toEqual({
        "0,0": { resource: "tree" },
        "1": { resource: "grain" },
        "2": { structure: "wall" },
      });
    });

    it("preserves nested objects", () => {
      const input = {
        "0,0": { 
          resource: "tree",
          nested: { value: 123, items: ["a", "b"] }
        },
      };
      
      const result = normalizeKeyedMap(input);
      expect(result).toEqual(input);
    });
  });
});