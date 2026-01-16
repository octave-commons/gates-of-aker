import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AgentList } from "../AgentList";
import { Agent } from "../../types";

describe("AgentList", () => {
  it("lists each agent with id, role, and position", () => {
    const agents: Agent[] = [
      { id: 1, role: "priest", pos: [3, 4], needs: {}, recall: {} },
      { id: 2, role: "scout", pos: null, needs: {}, recall: {} },
    ];

    render(<AgentList agents={agents} />);

    expect(screen.getByText(/Agents \(2\)/)).toBeInTheDocument();
    expect(screen.getByText(/#1 — priest/)).toBeInTheDocument();
    expect(screen.getByText("(3,4)")).toBeInTheDocument();
    expect(screen.getByText(/#2 — scout/)).toBeInTheDocument();
    expect(screen.getByText("pos:n/a")).toBeInTheDocument();
  });
});
