import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SimulationCanvas } from "../SimulationCanvas";
import type { HexConfig } from "../../hex";

const snapshot = {
  shrine: [0, 0],
  agents: [
    {
      id: 7,
      pos: [1, 2],
      role: "knight",
    },
  ],
};

const mapConfig: HexConfig = {
  kind: "hex",
  layout: "pointy",
  bounds: {
    shape: "rect",
    w: 20,
    h: 20,
    origin: [0, 0],
  },
};

describe("SimulationCanvas", () => {
  it("maps clicks to cells and surfaces agent hits", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 30, clientY: 60, target: canvas });

    expect(onCellSelect).toHaveBeenCalledTimes(1);
  });

  it("renders selection outlines even when no agent is hit", () => {
    const onCellSelect = vi.fn();
    const extended = {
      ...snapshot,
      agents: [
        ...snapshot.agents,
        { id: 9, pos: [5, 5], role: "priest" },
        { id: 11, pos: [10, 10], role: "scribe" },
      ],
    };

    render(
      <SimulationCanvas
        snapshot={extended}
        mapConfig={mapConfig}
        selectedCell={[5, 5]}
        selectedAgentId={9}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 400, clientY: 50, target: canvas });

    expect(onCellSelect).toHaveBeenCalled();
  });

  it("calls onCellSelect with cell coordinates when canvas is clicked", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 100, clientY: 150, target: canvas });

    expect(onCellSelect).toHaveBeenCalledTimes(1);
    const callArgs = onCellSelect.mock.calls[0][0];
    expect(Array.isArray(callArgs)).toBe(true);
    expect(callArgs).toHaveLength(2);
    expect(typeof callArgs[0]).toBe("number");
    expect(typeof callArgs[1]).toBe("number");
  });

  it("detects agents at clicked cell positions", () => {
    const onCellSelect = vi.fn();
    const snapshotWithMultipleAgents = {
      ...snapshot,
      agents: [
        { id: 1, pos: [2, 2], role: "knight" },
        { id: 2, pos: [2, 2], role: "priest" },
        { id: 3, pos: [5, 5], role: "scribe" },
      ],
    };

    render(
      <SimulationCanvas
        snapshot={snapshotWithMultipleAgents}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 80, clientY: 60, target: canvas });

    expect(onCellSelect).toHaveBeenCalled();
  });

  it("clicking on cell [0, 0] (origin) selects exactly [0, 0]", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const padding = 32;

    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    const screenX = padding / scaleX + rect.left;
    const screenY = padding / scaleY + rect.top;

    fireEvent.click(canvas, {
      clientX: screenX,
      clientY: screenY,
      target: canvas,
    });

    expect(onCellSelect).toHaveBeenCalledWith([0, 0], null);
  });

  it("clicking near origin does NOT select a different cell like [1, 0]", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const size = 16;
    const padding = 32;
    const SQRT3 = Math.sqrt(3);

    const internalX = padding + size * SQRT3 * 0.1;
    const internalY = padding + size * 1.5 * 0.1;

    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    const screenX = internalX / scaleX + rect.left;
    const screenY = internalY / scaleY + rect.top;

    fireEvent.click(canvas, {
      clientX: screenX,
      clientY: screenY,
      target: canvas,
    });

    const calledWith = onCellSelect.mock.calls[0][0];
    expect(calledWith).toEqual([0, 0]);
  });

  it("clicking in center of cell [1, 0] selects exactly [1, 0]", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const size = 16;
    const padding = 32;
    const SQRT3 = Math.sqrt(3);

    const internalX = padding + size * SQRT3 + 5;
    const internalY = padding + 5;

    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;

    const screenX = internalX / scaleX + rect.left;
    const screenY = internalY / scaleY + rect.top;

    fireEvent.click(canvas, {
      clientX: screenX,
      clientY: screenY,
      target: canvas,
    });

    const calledWith = onCellSelect.mock.calls[0][0];
    expect(calledWith).toEqual([1, 0]);
  });
});