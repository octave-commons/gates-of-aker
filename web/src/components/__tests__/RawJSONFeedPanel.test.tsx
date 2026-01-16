import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RawJSONFeedPanel } from "../RawJSONFeedPanel";

describe("RawJSONFeedPanel", () => {
  it("renders JSON payloads for inspection", () => {
    const data = { hello: "world", n: 3 };
    render(<RawJSONFeedPanel title="Sample" data={data} />);

    expect(screen.getByText("Sample")).toBeInTheDocument();
    expect(screen.getByText(/"hello": "world"/)).toBeInTheDocument();
  });
});
