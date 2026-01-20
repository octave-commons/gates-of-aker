import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { JobQueuePanel } from "../JobQueuePanel";

describe("JobQueuePanel", () => {
  it("shows message when no active jobs", () => {
    render(<JobQueuePanel jobs={[]} />);

    expect(screen.getByText(/Job Queue/)).toBeInTheDocument();
    expect(screen.getByText(/No active jobs/)).toBeInTheDocument();
  });

  it("displays job with eat type", () => {
    const jobs = [
      {
        id: "job-1",
        type: ":job/eat",
        target: [5, 10],
        worker: 1,
        progress: 0.5,
        required: 1.0,
        state: ":in-progress",
        priority: 100
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText(/Job Queue \(1\)/)).toBeInTheDocument();
    expect(screen.getByText("Eat")).toBeInTheDocument();
    expect(screen.getByText(/Agent #1/)).toBeInTheDocument();
    expect(screen.getByText(/\[5, 10\]/)).toBeInTheDocument();
  });

  it("displays job with sleep type", () => {
    const jobs = [
      {
        id: "job-2",
        type: ":job/sleep",
        target: [3, 8],
        worker: 2,
        progress: 0.3,
        required: 1.0,
        state: ":in-progress",
        priority: 90
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText("Sleep")).toBeInTheDocument();
    expect(screen.getByText(/Agent #2/)).toBeInTheDocument();
  });

  it("displays job with chop-tree type", () => {
    const jobs = [
      {
        id: "job-3",
        type: ":job/chop-tree",
        target: [10, 2],
        worker: null,
        progress: 0.0,
        required: 1.0,
        state: ":pending",
        priority: 60
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText("Chop Tree")).toBeInTheDocument();
    expect(screen.getAllByText(/Unassigned/)).toHaveLength(2);
  });

  it("displays job with haul type including from position", () => {
    const jobs = [
      {
        id: "job-4",
        type: ":job/haul",
        target: [15, 20],
        from_pos: [10, 10],
        resource: "wood",
        qty: 5,
        worker: 3,
        progress: 0.7,
        required: 1.0,
        state: ":in-progress",
        priority: 50
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText("Haul")).toBeInTheDocument();
    expect(screen.getByText(/\[15, 20\]/)).toBeInTheDocument();
    expect(screen.getByText(/←/)).toBeInTheDocument();
    expect(screen.getByText(/10, 10/)).toBeInTheDocument();
  });

  it("displays job with build-wall type", () => {
    const jobs = [
      {
        id: "job-5",
        type: ":job/build-wall",
        target: [7, 12],
        worker: 4,
        progress: 0.9,
        required: 1.0,
        state: ":in-progress",
        priority: 40
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText("Build Wall")).toBeInTheDocument();
  });

  it("filters out completed jobs", () => {
    const jobs = [
      {
        id: "job-1",
        type: ":job/eat",
        target: [5, 10],
        worker: 1,
        progress: 1.0,
        required: 1.0,
        state: ":completed",
        priority: 100
      },
      {
        id: "job-2",
        type: ":job/sleep",
        target: [3, 8],
        worker: 2,
        progress: 0.5,
        required: 1.0,
        state: ":in-progress",
        priority: 90
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText(/Job Queue \(1\)/)).toBeInTheDocument();
    expect(screen.queryByText("Eat")).not.toBeInTheDocument();
    expect(screen.getByText("Sleep")).toBeInTheDocument();
  });

  it("displays multiple active jobs", () => {
    const jobs = [
      {
        id: "job-1",
        type: ":job/eat",
        target: [5, 10],
        worker: 1,
        progress: 0.2,
        required: 1.0,
        state: ":in-progress",
        priority: 100
      },
      {
        id: "job-2",
        type: ":job/sleep",
        target: [3, 8],
        worker: 2,
        progress: 0.5,
        required: 1.0,
        state: ":in-progress",
        priority: 90
      },
      {
        id: "job-3",
        type: ":job/chop-tree",
        target: [10, 2],
        worker: null,
        progress: 0.0,
        required: 1.0,
        state: ":pending",
        priority: 60
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText(/Job Queue \(3\)/)).toBeInTheDocument();
    expect(screen.getAllByText("Eat")).toHaveLength(1);
    expect(screen.getAllByText("Sleep")).toHaveLength(1);
    expect(screen.getAllByText("Chop Tree")).toHaveLength(1);
  });

  it("handles unknown job type", () => {
    const jobs = [
      {
        id: "job-1",
        type: ":job/unknown",
        target: [5, 10],
        worker: 1,
        progress: 0.3,
        required: 1.0,
        state: ":in-progress",
        priority: 50
      }
    ];

    render(<JobQueuePanel jobs={jobs} />);

    expect(screen.getByText("unknown")).toBeInTheDocument();
  });
});
