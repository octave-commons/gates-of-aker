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

     render(
       <TickControls
         onTick={onTick}
         onReset={onReset}
         isRunning={false}
         onToggleRun={onToggleRun}
         tick={0}
         fps={15}
         onSetFps={() => {}}
       />
     );

     await user.click(screen.getByRole("button", { name: "Tick" }));
     await user.click(screen.getByRole("button", { name: "Tick×10" }));
     await user.click(screen.getByRole("button", { name: "Reset" }));

     expect(onTick).toHaveBeenNthCalledWith(1, 1);
     expect(onTick).toHaveBeenNthCalledWith(2, 10);
     expect(onReset).toHaveBeenCalledTimes(1);
   });

   it("shows Play/Pause button correctly", () => {
     const onToggleRun = vi.fn();

     const { rerender } = render(
       <TickControls
         onTick={() => {}}
         onReset={() => {}}
         isRunning={false}
         onToggleRun={onToggleRun}
         tick={0}
         fps={15}
         onSetFps={() => {}}
       />
     );

     expect(screen.getByText("▶ Play")).toBeInTheDocument();

     rerender(
       <TickControls
         onTick={() => {}}
         onReset={() => {}}
         isRunning={true}
         onToggleRun={onToggleRun}
         tick={0}
         fps={15}
         onSetFps={() => {}}
       />
     );

     expect(screen.getByText("⏸ Pause")).toBeInTheDocument();
   });

   it("invokes onToggleRun callback when play/pause button is clicked", async () => {
     const user = userEvent.setup();
     const onToggleRun = vi.fn();

     render(
       <TickControls
         onTick={() => {}}
         onReset={() => {}}
         isRunning={false}
         onToggleRun={onToggleRun}
         tick={0}
         fps={15}
         onSetFps={() => {}}
       />
     );

     await user.click(screen.getByRole("button", { name: "▶ Play" }));
     expect(onToggleRun).toHaveBeenCalledTimes(1);
   });

   it("invokes onToggleRun callback when running and pause button is clicked", async () => {
     const user = userEvent.setup();
     const onToggleRun = vi.fn();

     render(
       <TickControls
         onTick={() => {}}
         onReset={() => {}}
         isRunning={true}
         onToggleRun={onToggleRun}
         tick={0}
         fps={15}
         onSetFps={() => {}}
       />
     );

     await user.click(screen.getByRole("button", { name: "⏸ Pause" }));
     expect(onToggleRun).toHaveBeenCalledTimes(1);
   });
 });
