import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SimulationCanvas } from "../SimulationCanvas";
import type { HexConfig } from "../../hex";

const HEX_SIZE = 16;
const HEX_SPACING = 1;

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

const agentPaths: Record<number, Array<[number, number]>> = {};

describe("SimulationCanvas", () => {
  it("maps clicks to cells and surfaces agent hits", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={null}
        selectedAgentId={null}
        agentPaths={agentPaths}
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
        agentPaths={agentPaths}
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
        agentPaths={agentPaths}
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
        agentPaths={agentPaths}
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
        agentPaths={agentPaths}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    fireEvent.click(canvas, {
      clientX: rect.left + centerX,
      clientY: rect.top + centerY,
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
        agentPaths={agentPaths}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    const size = HEX_SIZE + HEX_SPACING;
    const SQRT3 = Math.sqrt(3);

    const internalX = size * SQRT3 * 0.1;
    const internalY = size * 1.5 * 0.1;

    fireEvent.click(canvas, {
      clientX: rect.left + centerX + internalX,
      clientY: rect.top + centerY + internalY,
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
        agentPaths={agentPaths}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas") as HTMLCanvasElement;
    const rect = canvas.getBoundingClientRect();

    const centerX = canvas.width / 2;
    const centerY = canvas.height / 2;

    const size = HEX_SIZE + HEX_SPACING;
    const SQRT3 = Math.sqrt(3);

    const internalX = size * SQRT3;
    const internalY = size * 1.5 * 0;

    fireEvent.click(canvas, {
      clientX: rect.left + centerX + internalX,
      clientY: rect.top + centerY + internalY,
      target: canvas,
    });

    const calledWith = onCellSelect.mock.calls[0][0];
    expect(calledWith).toEqual([1, 0]);
  });
});