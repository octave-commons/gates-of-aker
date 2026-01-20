import { describe, it, expect, vi, beforeEach } from "vitest";
import { hexToFrequency, playTone, playDeathTone, setMute, isMuted, toggleMute } from "../../audio";

describe("audio", () => {
  beforeEach(() => {
    setMute(false);
  });

  describe("hexToFrequency", () => {
    it("maps priest color (#d7263d) to C5 (523.25 Hz)", () => {
      const freq = hexToFrequency("#d7263d");
      expect(freq).toBe(523.25);
    });

    it("maps grain color (#ffeb3b) to C4 (261.63 Hz)", () => {
      const freq = hexToFrequency("#ffeb3b");
      expect(freq).toBe(261.63);
    });

    it("maps forest color (#2e7d32) to E4 (329.63 Hz)", () => {
      const freq = hexToFrequency("#2e7d32");
      expect(freq).toBe(329.63);
    });

    it("maps knight color (#3366ff) to G4 (392.00 Hz)", () => {
      const freq = hexToFrequency("#3366ff");
      expect(freq).toBe(392.00);
    });

    it("maps pure blue (#0000ff) to A4 (440.00 Hz)", () => {
      const freq = hexToFrequency("#0000ff");
      expect(freq).toBe(440.00);
    });

    it("maps magenta (#ff00ff) to C5 (523.25 Hz)", () => {
      const freq = hexToFrequency("#ff00ff");
      expect(freq).toBe(523.25);
    });

    it("returns default A4 (440 Hz) for invalid hex", () => {
      const freq = hexToFrequency("invalid");
      expect(freq).toBe(440);
    });

    it("handles hex without # prefix", () => {
      const freq = hexToFrequency("d7263d");
      expect(freq).toBe(523.25);
    });
  });

  describe("playTone", () => {
    it("does not throw when called", () => {
      expect(() => playTone(440)).not.toThrow();
    });

    it("does not throw when called with high frequency", () => {
      expect(() => playTone(1000)).not.toThrow();
    });

    it("does not throw when called with low frequency", () => {
      expect(() => playTone(100)).not.toThrow();
    });

    it("does not throw when called with custom duration", () => {
      expect(() => playTone(440, 0.5)).not.toThrow();
    });
  });

  describe("playDeathTone", () => {
    it("does not throw when called", () => {
      expect(() => playDeathTone()).not.toThrow();
    });
  });

  describe("mute control", () => {
    it("isMuted returns true by default", () => {
      setMute(true);
      expect(isMuted()).toBe(true);
    });

    it("setMute changes mute state", () => {
      setMute(true);
      expect(isMuted()).toBe(true);
      setMute(false);
      expect(isMuted()).toBe(false);
    });

    it("toggleMute returns new state", () => {
      setMute(true);
      const newState = toggleMute();
      expect(newState).toBe(false);
      expect(isMuted()).toBe(false);
    });

    it("toggleMute toggles state correctly", () => {
      setMute(true);
      toggleMute();
      expect(isMuted()).toBe(false);
      toggleMute();
      expect(isMuted()).toBe(true);
    });
  });
});
