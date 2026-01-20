import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBar } from "../StatusBar";

describe("StatusBar", () => {
  it("shows websocket status", () => {
    render(<StatusBar status="open" />);
    expect(screen.getByText(/WS:/)).toHaveTextContent("WS: open");
  });

  it("shows healthy status with green checkmark", () => {
    render(
      <StatusBar
        status="open"
        tickHealth={{ targetMs: 100, tickMs: 50, health: "healthy" }}
      />
    );
    expect(screen.getByText(/Health:/)).toBeInTheDocument();
    expect(screen.getByText("✓")).toBeInTheDocument();
  });

  it("shows degraded status with yellow warning", () => {
    render(
      <StatusBar
        status="open"
        tickHealth={{ targetMs: 100, tickMs: 75, health: "degraded" }}
      />
    );
    expect(screen.getByText(/Health:/)).toBeInTheDocument();
    expect(screen.getByText("⚠")).toBeInTheDocument();
  });

  it("shows unhealthy status with red cross", () => {
    render(
      <StatusBar
        status="open"
        tickHealth={{ targetMs: 100, tickMs: 95, health: "unhealthy" }}
      />
    );
    expect(screen.getByText(/Health:/)).toBeInTheDocument();
    expect(screen.getByText("✗")).toBeInTheDocument();
  });

  it("displays tick duration and target interval", () => {
    render(
      <StatusBar
        status="open"
        tickHealth={{ targetMs: 66.67, tickMs: 45.32, health: "healthy" }}
      />
    );
    expect(screen.getByText(/Health:/)).toBeInTheDocument();
    expect(screen.getByText(/\(45.3ms \/ 66.7ms\)/)).toBeInTheDocument();
  });

  it("does not show health indicator when tickHealth is null", () => {
    render(<StatusBar status="open" tickHealth={null} />);
    expect(screen.queryByText(/Health:/)).not.toBeInTheDocument();
  });

  it("does not show health indicator when tickHealth is undefined", () => {
    render(<StatusBar status="open" />);
    expect(screen.queryByText(/Health:/)).not.toBeInTheDocument();
  });
});
