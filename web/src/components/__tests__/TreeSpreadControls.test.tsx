import { fireEvent, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { TreeSpreadControls } from "../TreeSpreadControls";

describe("TreeSpreadControls", () => {
  it("renders all sliders and apply button", async () => {
    const user = userEvent.setup();
    const onSpreadProbabilityChange = vi.fn();
    const onMinIntervalChange = vi.fn();
    const onMaxIntervalChange = vi.fn();
    const onApply = vi.fn();

    render(
      <TreeSpreadControls
        spreadProbability={0.30}
        minInterval={20}
        maxInterval={160}
        onSpreadProbabilityChange={onSpreadProbabilityChange}
        onMinIntervalChange={onMinIntervalChange}
        onMaxIntervalChange={onMaxIntervalChange}
        onApply={onApply}
      />
    );

    expect(screen.getByText("Tree Spread")).toBeInTheDocument();
    expect(screen.getByText("Spread probability")).toBeInTheDocument();
    expect(screen.getByText("Min interval (ticks)")).toBeInTheDocument();
    expect(screen.getByText("Max interval (ticks)")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Apply tree spread settings/ })).toBeInTheDocument();
  });

  it("reflects slider values and triggers callbacks", async () => {
    const user = userEvent.setup();
    const onSpreadProbabilityChange = vi.fn();
    const onMinIntervalChange = vi.fn();
    const onMaxIntervalChange = vi.fn();
    const onApply = vi.fn();

    render(
      <TreeSpreadControls
        spreadProbability={0.30}
        minInterval={20}
        maxInterval={160}
        onSpreadProbabilityChange={onSpreadProbabilityChange}
        onMinIntervalChange={onMinIntervalChange}
        onMaxIntervalChange={onMaxIntervalChange}
        onApply={onApply}
      />
    );

    const spreadSlider = screen.getByLabelText(/Spread probability/);
    const minIntervalSlider = screen.getByLabelText(/Min interval/);
    const maxIntervalSlider = screen.getByLabelText(/Max interval/);

    fireEvent.change(spreadSlider, { target: { value: "0.5" } });
    expect(onSpreadProbabilityChange).toHaveBeenCalledWith(0.5);

    fireEvent.change(minIntervalSlider, { target: { value: "10" } });
    expect(onMinIntervalChange).toHaveBeenCalledWith(10);

    fireEvent.change(maxIntervalSlider, { target: { value: "200" } });
    expect(onMaxIntervalChange).toHaveBeenCalledWith(200);

    await user.click(screen.getByRole("button", { name: /Apply tree spread settings/ }));
    expect(onApply).toHaveBeenCalledTimes(1);
  });

  it("displays formatted values", () => {
    const onApply = vi.fn();

    render(
      <TreeSpreadControls
        spreadProbability={0.30}
        minInterval={20}
        maxInterval={160}
        onSpreadProbabilityChange={() => {}}
        onMinIntervalChange={() => {}}
        onMaxIntervalChange={() => {}}
        onApply={onApply}
      />
    );

    expect(screen.getByText("0.30")).toBeInTheDocument();
    expect(screen.getByText("20")).toBeInTheDocument();
    expect(screen.getByText("160")).toBeInTheDocument();
  });
});
