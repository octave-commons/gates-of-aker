import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";

afterEach(() => {
  cleanup();
});

const noop = () => {};
const originalLog = console.log;
const originalWarn = console.warn;
const originalError = console.error;
const originalInfo = console.info;
const originalDebug = console.debug;

const logLevel = import.meta.env.VITE_LOG_LEVEL || 'warn';
const logLevels = { error: 0, warn: 1, info: 2, debug: 3 };
const currentLevel = logLevels[logLevel as keyof typeof logLevels] || 1;

function shouldLog(level: number): boolean {
  return level <= currentLevel;
}

global.console = {
  ...console,
  log: (...args: unknown[]) => {
    if (shouldLog(2)) originalLog('[LOG]', ...args);
  },
  warn: (...args: unknown[]) => {
    if (shouldLog(1)) originalWarn('[WARN]', ...args);
  },
  error: (...args: unknown[]) => {
    if (shouldLog(0)) originalError('[ERROR]', ...args);
  },
  info: (...args: unknown[]) => {
    if (shouldLog(2)) originalInfo('[INFO]', ...args);
  },
  debug: (...args: unknown[]) => {
    if (shouldLog(3)) originalDebug('[DEBUG]', ...args);
  },
};

if (typeof HTMLCanvasElement !== "undefined") {
  HTMLCanvasElement.prototype.getContext = (function getContext(this: HTMLCanvasElement, contextId: string) {
    if (contextId !== "2d") {
      return null;
    }

    return {
      canvas: this,
      beginPath: noop,
      clearRect: noop,
      fill: noop,
      fillRect: noop,
      strokeRect: noop,
      arc: noop,
      stroke: noop,
      strokeStyle: "#111",
      fillStyle: "#111",
      globalAlpha: 1,
    } as unknown as CanvasRenderingContext2D;
  } as unknown as HTMLCanvasElement["getContext"]);

  HTMLCanvasElement.prototype.getBoundingClientRect = () => ({
    x: 0,
    y: 0,
    width: 480,
    height: 480,
    top: 0,
    left: 0,
    right: 480,
    bottom: 480,
    toJSON() {
      return {};
    },
  });
}

if (typeof AudioContext !== "undefined") {
  const mockAudioContext = {
    createOscillator: () => ({
      type: "sine",
      frequency: { value: 440 },
      connect: noop,
      disconnect: noop,
      start: noop,
      stop: noop,
      onended: null,
    }),
    createGain: () => ({
      gain: { value: 0.3 },
      connect: noop,
      disconnect: noop,
    }),
    destination: {},
    state: "running",
    resume: noop,
    currentTime: 0,
  };

  (globalThis as any).AudioContext = vi.fn(() => mockAudioContext);
}
