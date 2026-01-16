import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SimulationCanvas } from "../SimulationCanvas";

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

describe("SimulationCanvas", () => {
  it("maps clicks to cells and surfaces agent hits", () => {
    const onCellSelect = vi.fn();

    render(
      <SimulationCanvas
        snapshot={snapshot}
        selectedCell={null}
        selectedAgentId={null}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 30, clientY: 60, target: canvas });

    expect(onCellSelect).toHaveBeenCalledTimes(1);
    expect(onCellSelect).toHaveBeenCalledWith([1, 2], 7);
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
        selectedCell={[5, 5]}
        selectedAgentId={9}
        onCellSelect={onCellSelect}
      />
    );

    const canvas = screen.getByTestId("simulation-canvas");
    fireEvent.click(canvas, { clientX: 400, clientY: 50, target: canvas });

    expect(onCellSelect).toHaveBeenCalledWith([16, 2], null);
  });
});
