import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach } from "vitest";

afterEach(() => {
  cleanup();
});

const noop = () => {};

if (typeof HTMLCanvasElement !== "undefined") {
  HTMLCanvasElement.prototype.getContext = function getContext() {
    return {
      canvas: this,
      beginPath: noop,
      clearRect: noop,
      fill: noop,
      fillRect: noop,
      strokeRect: noop,
      arc: noop,
      stroke: noop,
      set globalAlpha(_: number) {},
      get globalAlpha() {
        return 1;
      },
      set strokeStyle(_: string | CanvasGradient | CanvasPattern) {},
      get strokeStyle() {
        return "#111";
      },
      set fillStyle(_: string | CanvasGradient | CanvasPattern) {},
      get fillStyle() {
        return "#111";
      },
    } as CanvasRenderingContext2D;
  };

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
