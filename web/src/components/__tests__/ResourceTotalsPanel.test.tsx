import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ResourceTotalsPanel } from "../ResourceTotalsPanel";

describe("ResourceTotalsPanel", () => {
  it("shows empty state when no totals", () => {
    render(<ResourceTotalsPanel totals={{}} />);
 
    expect(screen.getByText("No stockpile resources yet.")).not.toBeNull();
  });
 
  it("renders stockpile totals", () => {
    render(<ResourceTotalsPanel totals={{ grain: 8, fruit: 3 }} />);
 
    expect(screen.getByText("Grain")).not.toBeNull();
    expect(screen.getByText("8")).not.toBeNull();
    expect(screen.getByText("Fruit")).not.toBeNull();
    expect(screen.getByText("3")).not.toBeNull();
  });
});
