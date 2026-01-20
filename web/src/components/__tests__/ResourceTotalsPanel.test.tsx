import { render, screen } from "@testing-library/react";
import { ResourceTotalsPanel } from "../ResourceTotalsPanel";

describe("ResourceTotalsPanel", () => {
  it("shows empty state when no totals", () => {
    render(<ResourceTotalsPanel totals={{}} />);

    expect(screen.getByText("No stockpile resources yet.")).toBeDefined();
  });

  it("renders stockpile totals", () => {
    render(<ResourceTotalsPanel totals={{ grain: 8, fruit: 3 }} />);

    expect(screen.getByText("Grain")).toBeDefined();
    expect(screen.getByText("8")).toBeDefined();
    expect(screen.getByText("Fruit")).toBeDefined();
    expect(screen.getByText("3")).toBeDefined();
  });
});
