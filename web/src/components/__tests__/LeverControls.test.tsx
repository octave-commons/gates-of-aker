import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { LeverControls } from "../LeverControls";

describe("LeverControls", () => {
  it("reflects tick + slider values and triggers callbacks", async () => {
    const user = userEvent.setup();
    const onFireChange = vi.fn();
    const onApply = vi.fn();

    render(
      <LeverControls
        tick={42}
        fireToPatron={0.8}
        lightningToStorm={0.5}
        stormToDeity={0.2}
        onFireChange={onFireChange}
        onLightningChange={() => {}}
        onStormChange={() => {}}
        onApply={onApply}
      />
    );

    const tickRow = screen.getByText(/Tick:/).parentElement;
    expect(tickRow).toHaveTextContent("Tick: 42");
    fireEvent.change(screen.getByLabelText(/fire/), { target: { value: "0.9" } });
    expect(onFireChange).toHaveBeenCalledWith(0.9);

    await user.click(screen.getByRole("button", { name: /Apply levers/ }));
    expect(onApply).toHaveBeenCalledTimes(1);
  });
});
