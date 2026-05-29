import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ForkTalesPanel } from "../ForkTalesPanel";

type MockResponseBody = Record<string, unknown>;

const mockJsonResponse = (body: MockResponseBody, ok = true, status = 200) => ({
  ok,
  status,
  json: async () => body,
});

describe("ForkTalesPanel", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("loads storyteller status, chapter history, and latest chapter detail on mount", async () => {
    fetchMock
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          provider: "openai-compatible",
          model: "mistral-large-3:675b",
          chapter_count: 49,
          narrative_dir: "/tmp/fork-tales/narrative",
          latest_chapter: {
            number: 49,
            title: "Permissions of the Pack",
            path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
          },
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          chapter_count: 49,
          chapters: [
            {
              number: 49,
              title: "Permissions of the Pack",
              path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
              preview: "Intent without permission is just a threat with better spelling.",
            },
            {
              number: 48,
              title: "Another Chapter",
              path: "/tmp/fork-tales/narrative/Chapter_48_Another_Chapter.md",
              preview: "Another preview.",
            },
          ],
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          number: 49,
          title: "Permissions of the Pack",
          path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
          preview: "Intent without permission is just a threat with better spelling.",
          text: "# 49 — Permissions of the Pack\n\nIntent without permission is just a threat with better spelling.",
        })
      );

    render(<ForkTalesPanel backendOrigin="http://example.test" />);

    expect(await screen.findByText("Configured")).toBeInTheDocument();
    expect(await screen.findByText(/Chapter History/i)).toBeInTheDocument();
    expect(await screen.findByText(/Selected Chapter/i)).toBeInTheDocument();
    const matchingText = await screen.findAllByText(/Intent without permission is just a threat with better spelling\./i);
    expect(matchingText.length).toBeGreaterThan(0);

    expect(fetchMock).toHaveBeenNthCalledWith(1, "http://example.test/api/fork-tales/status");
    expect(fetchMock).toHaveBeenNthCalledWith(2, "http://example.test/api/fork-tales/history");
    expect(fetchMock).toHaveBeenNthCalledWith(3, "http://example.test/api/fork-tales/history/49");
  });

  it("submits dry-run preview requests and renders generated chapter text", async () => {
    fetchMock
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          provider: "openai-compatible",
          model: "mistral-large-3:675b",
          chapter_count: 49,
          narrative_dir: "/tmp/fork-tales/narrative",
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          chapter_count: 49,
          chapters: [
            {
              number: 49,
              title: "Permissions of the Pack",
              path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
              preview: "Intent without permission is just a threat with better spelling.",
            },
          ],
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          number: 49,
          title: "Permissions of the Pack",
          path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
          text: "# 49 — Permissions of the Pack\n\nIntent without permission is just a threat with better spelling.",
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          ok: true,
          configured: true,
          chapter_number: 50,
          title: "The Static Narrator",
          written: false,
          path: "/tmp/fork-tales/narrative/Chapter_50_The_Static_Narrator.md",
          text: "# 50 — The Static Narrator\n\nA preview chapter.",
        })
      );

    render(<ForkTalesPanel backendOrigin="http://example.test" />);

    await screen.findByText("Configured");

    fireEvent.change(screen.getByPlaceholderText(/Optional steering prompt/i), {
      target: { value: "Focus on Duct and Sei." },
    });

    fireEvent.click(screen.getByRole("button", { name: /Generate Preview/i }));

    await screen.findByText("PREVIEW");
    expect(screen.getByText(/#50\s+—\s+The Static Narrator/)).toBeInTheDocument();
    expect(screen.getByText(/A preview chapter\./)).toBeInTheDocument();

    await waitFor(() => {
      expect(fetchMock).toHaveBeenNthCalledWith(
        4,
        "http://example.test/api/fork-tales/continue",
        expect.objectContaining({
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            dry_run: true,
            user_prompt: "Focus on Duct and Sei.",
          }),
        })
      );
    });
  });

  it("loads a different chapter when history item is selected", async () => {
    fetchMock
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          provider: "openai-compatible",
          model: "mistral-large-3:675b",
          chapter_count: 49,
          narrative_dir: "/tmp/fork-tales/narrative",
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          configured: true,
          chapter_count: 49,
          chapters: [
            {
              number: 49,
              title: "Permissions of the Pack",
              path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
              preview: "Intent without permission is just a threat with better spelling.",
            },
            {
              number: 48,
              title: "Another Chapter",
              path: "/tmp/fork-tales/narrative/Chapter_48_Another_Chapter.md",
              preview: "Another preview.",
            },
          ],
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          number: 49,
          title: "Permissions of the Pack",
          path: "/tmp/fork-tales/narrative/Chapter_49_Permissions_of_the_Pack.md",
          text: "# 49 — Permissions of the Pack\n\nInitial detail.",
        })
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          number: 48,
          title: "Another Chapter",
          path: "/tmp/fork-tales/narrative/Chapter_48_Another_Chapter.md",
          text: "# 48 — Another Chapter\n\nLoaded on click.",
        })
      );

    render(<ForkTalesPanel backendOrigin="http://example.test" />);

    await screen.findByText(/Initial detail\./);
    fireEvent.click(screen.getByRole("button", { name: /#48 — Another Chapter/i }));
    await screen.findByText(/Loaded on click\./);

    expect(fetchMock).toHaveBeenNthCalledWith(4, "http://example.test/api/fork-tales/history/48");
  });
});
