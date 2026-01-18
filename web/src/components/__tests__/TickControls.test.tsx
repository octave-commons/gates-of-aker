import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TickControls } from "../TickControls";

describe("TickControls", () => {
  it("invokes callbacks for tick buttons", async () => {
    const user = userEvent.setup();
    const onTick = vi.fn();
    const onReset = vi.fn();
    const onToggleRun = vi.fn();
    const noop = vi.fn();

    render(
      <TickControls
        onTick={onTick}
        onReset={onReset}
        onPlaceShrine={noop}
        onSetMouthpiece={noop}
        canPlaceShrine={false}
        canSetMouthpiece={false}
        isRunning={false}
        onToggleRun={onToggleRun}
      />
    );

    await user.click(screen.getByRole("button", { name: "Tick" }));
    await user.click(screen.getByRole("button", { name: "Tick×10" }));
    await user.click(screen.getByRole("button", { name: "Reset" }));

    expect(onTick).toHaveBeenNthCalledWith(1, 1);
    expect(onTick).toHaveBeenNthCalledWith(2, 10);
    expect(onReset).toHaveBeenCalledTimes(1);
  });

  it("disables shrine/mouthpiece actions when unavailable", () => {
    const { rerender } = render(
      <TickControls
        onTick={() => {}}
        onReset={() => {}}
        onPlaceShrine={() => {}}
        onSetMouthpiece={() => {}}
        canPlaceShrine={false}
        canSetMouthpiece={false}
        isRunning={false}
        onToggleRun={() => {}}
      />
    );

    expect(screen.getByRole("button", { name: /Place shrine/ })).toBeDisabled();
    expect(screen.getByRole("button", { name: /Set mouthpiece/ })).toBeDisabled();

    rerender(
      <TickControls
        onTick={() => {}}
        onReset={() => {}}
        onPlaceShrine={() => {}}
        onSetMouthpiece={() => {}}
        canPlaceShrine
        canSetMouthpiece
        isRunning={false}
        onToggleRun={() => {}}
      />
    );

    expect(screen.getByRole("button", { name: /Place shrine/ })).toBeEnabled();
    expect(screen.getByRole("button", { name: /Set mouthpiece/ })).toBeEnabled();
  });

  it("shows Play/Pause button correctly", () => {
    const onToggleRun = vi.fn();

    const { rerender } = render(
      <TickControls
        onTick={() => {}}
        onReset={() => {}}
        onPlaceShrine={() => {}}
        onSetMouthpiece={() => {}}
        canPlaceShrine={false}
        canSetMouthpiece={false}
        isRunning={false}
        onToggleRun={onToggleRun}
      />
    );

    expect(screen.getByText("▶ Play")).toBeInTheDocument();

    rerender(
      <TickControls
        onTick={() => {}}
        onReset={() => {}}
        onPlaceShrine={() => {}}
        onSetMouthpiece={() => {}}
        canPlaceShrine={false}
        canSetMouthpiece={false}
        isRunning
        onToggleRun={onToggleRun}
      />
    );

    expect(screen.getByText("⏸ Pause")).toBeInTheDocument();
  });
});
