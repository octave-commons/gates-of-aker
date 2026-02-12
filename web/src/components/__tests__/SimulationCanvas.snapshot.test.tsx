import { render } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { SimulationCanvas } from "../SimulationCanvas";
import type { HexConfig } from "../../hex";

const snapshot = {
  tick: 1,
  shrine: [0, 0],
  agents: [{ id: 1, pos: [0, 0], role: "priest", name: "Aset" }],
  tiles: {
    "0,0": { biome: "forest" },
    "1,0": { biome: "field" },
  },
  items: {},
  stockpiles: {},
};

const mapConfig: HexConfig = {
  kind: "hex",
  layout: "pointy",
  bounds: {
    shape: "rect",
    w: 3,
    h: 3,
    origin: [0, 0],
  },
};

describe("SimulationCanvas snapshot", () => {
  it("matches stable render snapshot", () => {
    const { asFragment } = render(
      <SimulationCanvas
        snapshot={snapshot}
        mapConfig={mapConfig}
        selectedCell={[0, 0]}
        selectedAgentId={1}
        agentPaths={{}}
        onCellSelect={vi.fn()}
        showRelationships={false}
        showNames
        showStats={false}
        speechBubbles={[]}
        visibilityData={null}
        selectedVisibilityAgentId={null}
        tileVisibility={{ "0,0": "visible", "1,0": "visible" }}
        revealedTilesSnapshot={{}}
      />
    );

    expect(asFragment()).toMatchSnapshot();
  });
});
