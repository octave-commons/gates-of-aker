import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";

export type ForkTalesChapterSummary = {
  number?: number;
  title?: string;
  path?: string;
  preview?: string;
};

export type ForkTalesChapterDetail = ForkTalesChapterSummary & {
  text?: string;
};

export type ForkTalesStatus = {
  configured?: boolean;
  provider?: string;
  model?: string;
  narrative_dir?: string;
  narrative_exists?: boolean;
  chapter_count?: number;
  latest_chapter?: {
    number?: number;
    title?: string;
    path?: string;
  };
};

export type ForkTalesHistoryResponse = {
  configured?: boolean;
  chapter_count?: number;
  chapters?: ForkTalesChapterSummary[];
};

export type ForkTalesResult = {
  ok?: boolean;
  configured?: boolean;
  chapter_number?: number;
  title?: string;
  path?: string;
  written?: boolean;
  text?: string;
  error?: string;
};

type ForkTalesPanelProps = {
  backendOrigin?: string;
};

const panelStyle: React.CSSProperties = {
  marginTop: 12,
  padding: 12,
  border: "1px solid #aaa",
  borderRadius: 8,
  backgroundColor: "rgba(255,255,255,0.98)",
  color: "#111827",
};

const compactLabelStyle: React.CSSProperties = {
  fontSize: 11,
  opacity: 0.7,
  textTransform: "uppercase",
  letterSpacing: "0.06em",
};

const sectionCardStyle: React.CSSProperties = {
  border: "1px solid #ddd",
  borderRadius: 6,
  padding: 8,
  backgroundColor: "#fafafa",
};

export function ForkTalesPanel({
  backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000",
}: ForkTalesPanelProps) {
  const [status, setStatus] = useState<ForkTalesStatus | null>(null);
  const [history, setHistory] = useState<ForkTalesChapterSummary[]>([]);
  const [selectedChapter, setSelectedChapter] = useState<ForkTalesChapterDetail | null>(null);
  const [selectedChapterNumber, setSelectedChapterNumber] = useState<number | null>(null);
  const [result, setResult] = useState<ForkTalesResult | null>(null);
  const [userPrompt, setUserPrompt] = useState("");
  const [dryRun, setDryRun] = useState(true);
  const [loadingStatus, setLoadingStatus] = useState(false);
  const [loadingHistory, setLoadingHistory] = useState(false);
  const [loadingChapter, setLoadingChapter] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const autoSelectedChapterRef = useRef(false);

  const statusUrl = useMemo(
    () => `${backendOrigin.replace(/\/$/, "")}/api/fork-tales/status`,
    [backendOrigin]
  );
  const historyUrl = useMemo(
    () => `${backendOrigin.replace(/\/$/, "")}/api/fork-tales/history`,
    [backendOrigin]
  );
  const continueUrl = useMemo(
    () => `${backendOrigin.replace(/\/$/, "")}/api/fork-tales/continue`,
    [backendOrigin]
  );

  const loadStatus = useCallback(async () => {
    setLoadingStatus(true);
    setError(null);
    try {
      const response = await fetch(statusUrl);
      const data = (await response.json()) as ForkTalesStatus;
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      setStatus(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load Fork Tales status");
    } finally {
      setLoadingStatus(false);
    }
  }, [statusUrl]);

  const loadChapter = useCallback(
    async (chapterNumber: number, options?: { silent?: boolean }) => {
      if (!Number.isFinite(chapterNumber)) {
        return;
      }
      if (!options?.silent) {
        setLoadingChapter(true);
      }
      try {
        const response = await fetch(`${historyUrl}/${chapterNumber}`);
        const data = (await response.json()) as ForkTalesChapterDetail & { error?: string };
        if (!response.ok) {
          throw new Error(data.error || `HTTP ${response.status}`);
        }
        setSelectedChapter(data);
        setSelectedChapterNumber(chapterNumber);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load chapter detail");
      } finally {
        if (!options?.silent) {
          setLoadingChapter(false);
        }
      }
    },
    [historyUrl]
  );

  const loadHistory = useCallback(async () => {
    setLoadingHistory(true);
    setError(null);
    try {
      const response = await fetch(historyUrl);
      const data = (await response.json()) as ForkTalesHistoryResponse;
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const chapters = data.chapters ?? [];
      setHistory(chapters);
      if (chapters.length > 0 && !autoSelectedChapterRef.current) {
        autoSelectedChapterRef.current = true;
        const latest = chapters[0];
        if (typeof latest.number === "number") {
          void loadChapter(latest.number, { silent: true });
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load Fork Tales history");
    } finally {
      setLoadingHistory(false);
    }
  }, [historyUrl, loadChapter]);

  useEffect(() => {
    void loadStatus();
    void loadHistory();
  }, [loadHistory, loadStatus]);

  const handleGenerate = useCallback(async () => {
    setSubmitting(true);
    setError(null);
    setResult(null);
    try {
      const response = await fetch(continueUrl, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          dry_run: dryRun,
          user_prompt: userPrompt.trim() || undefined,
        }),
      });
      const data = (await response.json()) as ForkTalesResult;
      setResult(data);
      if (!response.ok || data.ok === false) {
        throw new Error(data.error || `HTTP ${response.status}`);
      }
      if (data.written) {
        await loadStatus();
        await loadHistory();
        if (typeof data.chapter_number === "number") {
          await loadChapter(data.chapter_number);
        }
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate chapter");
    } finally {
      setSubmitting(false);
    }
  }, [continueUrl, dryRun, loadChapter, loadHistory, loadStatus, userPrompt]);

  const latestChapter = status?.latest_chapter;
  const actionLabel = dryRun ? "Generate Preview" : "Write Next Chapter";
  const statusLabel = status?.configured ? "Configured" : "Missing Config";
  const statusColor = status?.configured ? "#2e7d32" : "#b71c1c";

  return (
    <div style={panelStyle}>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
        <h3 style={{ margin: 0, fontSize: 14 }}>Fork Tales Story Engine</h3>
        <button
          type="button"
          onClick={() => {
            void loadStatus();
            void loadHistory();
          }}
          disabled={loadingStatus || loadingHistory}
          style={{ padding: "4px 8px", fontSize: 12 }}
        >
          {loadingStatus || loadingHistory ? "Refreshing…" : "Refresh"}
        </button>
      </div>

      <div style={{ display: "grid", gap: 8, fontSize: 12 }}>
        <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <span style={compactLabelStyle}>Status</span>
          <span style={{ color: statusColor, fontWeight: 700 }}>{statusLabel}</span>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
          <span style={compactLabelStyle}>Provider</span>
          <span>{status?.provider ?? "openai-compatible"}</span>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
          <span style={compactLabelStyle}>Model</span>
          <span>{status?.model ?? "unknown"}</span>
        </div>

        <div style={{ display: "flex", justifyContent: "space-between", gap: 8 }}>
          <span style={compactLabelStyle}>Chapters</span>
          <span>{status?.chapter_count ?? 0}</span>
        </div>

        <div>
          <div style={compactLabelStyle}>Narrative Directory</div>
          <div style={{ fontFamily: "monospace", fontSize: 11, marginTop: 4, wordBreak: "break-all" }}>
            {status?.narrative_dir ?? "unknown"}
          </div>
        </div>

        {latestChapter && (
          <div style={sectionCardStyle}>
            <div style={compactLabelStyle}>Latest Chapter</div>
            <div style={{ marginTop: 4, fontWeight: 600 }}>
              #{latestChapter.number ?? "?"} — {latestChapter.title ?? "Untitled"}
            </div>
            {latestChapter.path && (
              <div style={{ fontFamily: "monospace", fontSize: 11, marginTop: 4, wordBreak: "break-all" }}>
                {latestChapter.path}
              </div>
            )}
          </div>
        )}

        <label style={{ display: "flex", alignItems: "center", gap: 8, fontSize: 12 }}>
          <input type="checkbox" checked={dryRun} onChange={(e) => setDryRun(e.target.checked)} />
          Start with dry run preview
        </label>

        <label style={{ display: "grid", gap: 6 }}>
          <span style={compactLabelStyle}>Operator Prompt</span>
          <textarea
            value={userPrompt}
            onChange={(e) => setUserPrompt(e.target.value)}
            rows={4}
            placeholder="Optional steering prompt for the next chapter..."
            style={{ width: "100%", resize: "vertical", fontSize: 12 }}
          />
        </label>

        <button
          type="button"
          onClick={() => void handleGenerate()}
          disabled={submitting || !status?.configured}
          style={{ padding: "8px 10px", fontSize: 12, fontWeight: 600 }}
        >
          {submitting ? "Generating…" : actionLabel}
        </button>

        {error && (
          <div role="alert" style={{ color: "#b71c1c", backgroundColor: "#ffebee", padding: 8, borderRadius: 6, fontSize: 12 }}>
            {error}
          </div>
        )}

        {result && (
          <div style={{ ...sectionCardStyle, backgroundColor: result.ok ? "#f8fff8" : "#fff8f8" }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8 }}>
              <strong>
                #{result.chapter_number ?? "?"} — {result.title ?? "Untitled"}
              </strong>
              <span style={{ fontSize: 11, fontWeight: 700, color: result.written ? "#2e7d32" : "#6a1b9a" }}>
                {result.written ? "WRITTEN" : "PREVIEW"}
              </span>
            </div>

            {result.path && (
              <div style={{ fontFamily: "monospace", fontSize: 11, marginTop: 6, wordBreak: "break-all" }}>
                {result.path}
              </div>
            )}

            {result.text && (
              <pre
                style={{
                  marginTop: 8,
                  maxHeight: 260,
                  overflow: "auto",
                  whiteSpace: "pre-wrap",
                  backgroundColor: "#111",
                  color: "#f5f5f5",
                  padding: 10,
                  borderRadius: 6,
                  fontSize: 12,
                }}
              >
                {result.text}
              </pre>
            )}
          </div>
        )}

        <div style={{ display: "grid", gap: 12 }}>
          <div style={sectionCardStyle}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8, marginBottom: 8 }}>
              <span style={compactLabelStyle}>Chapter History</span>
              <span style={{ fontSize: 11, opacity: 0.7 }}>{loadingHistory ? "Loading…" : `${history.length} loaded`}</span>
            </div>
            {history.length === 0 ? (
              <div style={{ fontSize: 12, opacity: 0.7 }}>No chapters found yet.</div>
            ) : (
              <div style={{ display: "grid", gap: 8, maxHeight: 220, overflowY: "auto" }}>
                {history.map((chapter) => {
                  const isSelected = chapter.number === selectedChapterNumber;
                  return (
                    <button
                      key={chapter.number ?? chapter.path ?? chapter.title}
                      type="button"
                      onClick={() => {
                        if (typeof chapter.number === "number") {
                          void loadChapter(chapter.number);
                        }
                      }}
                      style={{
                        textAlign: "left",
                        border: isSelected ? "2px solid #4f46e5" : "1px solid #ddd",
                        backgroundColor: isSelected ? "#eef2ff" : "#fff",
                        borderRadius: 6,
                        padding: 8,
                        cursor: "pointer",
                        display: "grid",
                        gap: 4,
                      }}
                    >
                      <strong>
                        #{chapter.number ?? "?"} — {chapter.title ?? "Untitled"}
                      </strong>
                      {chapter.preview && <span style={{ fontSize: 12, opacity: 0.8 }}>{chapter.preview}</span>}
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          <div style={sectionCardStyle}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8, marginBottom: 8 }}>
              <span style={compactLabelStyle}>Selected Chapter</span>
              <span style={{ fontSize: 11, opacity: 0.7 }}>{loadingChapter ? "Loading…" : selectedChapter ? "Ready" : "None"}</span>
            </div>
            {selectedChapter ? (
              <div style={{ display: "grid", gap: 8 }}>
                <strong>
                  #{selectedChapter.number ?? "?"} — {selectedChapter.title ?? "Untitled"}
                </strong>
                {selectedChapter.path && (
                  <div style={{ fontFamily: "monospace", fontSize: 11, wordBreak: "break-all" }}>
                    {selectedChapter.path}
                  </div>
                )}
                {selectedChapter.text && (
                  <pre
                    style={{
                      margin: 0,
                      maxHeight: 320,
                      overflow: "auto",
                      whiteSpace: "pre-wrap",
                      backgroundColor: "#111",
                      color: "#f5f5f5",
                      padding: 10,
                      borderRadius: 6,
                      fontSize: 12,
                    }}
                  >
                    {selectedChapter.text}
                  </pre>
                )}
              </div>
            ) : (
              <div style={{ fontSize: 12, opacity: 0.7 }}>Select a chapter from history to inspect it.</div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
