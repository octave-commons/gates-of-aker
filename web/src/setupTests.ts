import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();
});

const noop = () => {};

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
