import { render, screen, fireEvent } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { AgentList } from "../AgentList";
import { Agent } from "../../types";

describe("AgentList", () => {
  it("lists each agent with id, role, and position", () => {
    const agents: Agent[] = [
      { id: 1, role: "priest", pos: [3, 4], needs: {}, recall: {}, inventory: {} },
      { id: 2, role: "scout", pos: null, needs: {}, recall: {}, inventory: {} },
    ];

    render(<AgentList agents={agents} />);

    expect(screen.getByText(/Agents \(2\)/)).toBeInTheDocument();
    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("priest")).toBeInTheDocument();
    expect(screen.getByText("#2")).toBeInTheDocument();
    expect(screen.getByText("scout")).toBeInTheDocument();
    expect(screen.getByText("pos:n/a")).toBeInTheDocument();
  });

  it("passes jobs prop to AgentCard", () => {
    const agents: any[] = [
      { id: 1, role: "priest", pos: [3, 4], needs: {}, recall: {}, inventory: {}, current_job: "job-1" },
      { id: 2, role: "knight", pos: [5, 6], needs: {}, recall: {}, inventory: {} },
    ];

    const jobs = [
      { id: "job-1", type: ":job/eat", target: [3, 4], worker: 1, progress: 0.5, required: 1.0, state: ":in-progress" }
    ];

    render(<AgentList agents={agents} jobs={jobs} />);

    expect(screen.getByText("Eating")).toBeInTheDocument();
  });

  it("shows no agents message when empty", () => {
    render(<AgentList agents={[]} />);

    expect(screen.getByText(/Agents \(0\)/)).toBeInTheDocument();
  });

  it("toggles collapse state", () => {
    const agents: Agent[] = [
      { id: 1, role: "priest", pos: [3, 4], needs: {}, recall: {}, inventory: {} }
    ];

    const { container } = render(<AgentList agents={agents} collapsible />);

    expect(screen.getByText("#1")).toBeInTheDocument();

    const header = container.querySelector('[style*="cursor: pointer"]');
    fireEvent.click(header!);
    expect(screen.queryByText("#1")).not.toBeInTheDocument();

    fireEvent.click(header!);
    expect(screen.getByText("#1")).toBeInTheDocument();
  });

  it("shows agents in scrollable container", () => {
    const agents: Agent[] = Array.from({ length: 5 }, (_, i) => ({
      id: i + 1,
      role: "peasant",
      pos: [i, i],
      needs: {},
      recall: {},
      inventory: {}
    }));

    const { container } = render(<AgentList agents={agents} />);

    const listContainer = container.querySelector('[style*="max-height: 220px"]');
    expect(listContainer).toBeInTheDocument();
    expect(listContainer).toHaveStyle({ overflowY: "auto" });
  });

  it("uses compact mode by default in non-collapsible view", () => {
    const agents: Agent[] = [
      { id: 1, role: "priest", pos: [3, 4], needs: {}, recall: {}, inventory: {} }
    ];

    const { container } = render(<AgentList agents={agents} />);

    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("priest")).toBeInTheDocument();
  });
});
