import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { StatusBar } from "../StatusBar";

describe("StatusBar", () => {
  it("shows the websocket status", () => {
    render(<StatusBar status="open" />);
    expect(screen.getByText(/WS:/)).toHaveTextContent("WS: open");
  });
});
