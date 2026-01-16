import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { TraceFeed } from "../TraceFeed";
import { Trace } from "../../types";

describe("TraceFeed", () => {
  it("renders traces in reverse chronological order", () => {
    const traces: Trace[] = [
      {
        "trace/id": "t1",
        tick: 1,
        speaker: "a",
        listener: "b",
        packet: { intent: "ping", facets: [] },
        spread: [],
      },
      {
        "trace/id": "t2",
        tick: 2,
        speaker: "c",
        listener: "d",
        packet: { intent: "pong", facets: [] },
        spread: [],
      },
    ];

    render(<TraceFeed traces={traces} />);
    const cards = screen.getAllByTestId("trace-card");

    expect(cards).toHaveLength(2);
    expect(cards[0]).toHaveTextContent("t2");
    expect(cards[1]).toHaveTextContent("t1");
  });
});
