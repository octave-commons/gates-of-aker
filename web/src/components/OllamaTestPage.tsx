import React, { useState } from "react";

type Status = "idle" | "testing" | "connected" | "error";

interface OllamaTestPageProps {
  onBack: () => void;
}

export function OllamaTestPage({ onBack }: OllamaTestPageProps) {
  const [status, setStatus] = useState<Status>("idle");
  const [latency, setLatency] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [model, setModel] = useState<string>("");

  const testOllama = async () => {
    setStatus("testing");
    setLatency(null);
    setError(null);
    setModel("");

    try {
      const backendOrigin = import.meta.env.VITE_BACKEND_ORIGIN ?? "http://localhost:3000";
      const response = await fetch(`${backendOrigin}/api/ollama/test`, {
        method: "GET",
      });

      if (response.ok) {
        const data = await response.json();
        if (data.connected) {
          setStatus("connected");
          setLatency(data.latency_ms);
          setModel(data.model);
        } else {
          setStatus("error");
          setError(data.error || "Connection failed");
          setLatency(data.latency_ms);
          setModel(data.model);
        }
      } else {
        setStatus("error");
        setError(`HTTP ${response.status}: ${response.statusText}`);
      }
    } catch (err) {
      setStatus("error");
      setError(err instanceof Error ? err.message : "Unknown error occurred");
    }
  };

  const getStatusColor = () => {
    switch (status) {
      case "connected":
        return "#4ade80";
      case "error":
        return "#f87171";
      case "testing":
        return "#fbbf24";
      default:
        return "#ffffff";
    }
  };

  const getStatusText = () => {
    switch (status) {
      case "connected":
        return "Connected";
      case "error":
        return "Connection Failed";
      case "testing":
        return "Testing...";
      default:
        return "Ready to test";
    }
  };

  return (
    <div
      style={{
        position: "fixed",
        top: 0,
        left: 0,
        width: "100vw",
        height: "100vh",
        backgroundColor: "#1a1a2e",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 1000,
        color: "#ffffff",
        fontFamily: "sans-serif",
      }}
    >
      <div
        style={{
          backgroundColor: "rgba(255, 255, 255, 0.05)",
          padding: "3rem",
          borderRadius: "16px",
          border: "1px solid rgba(255, 255, 255, 0.1)",
          display: "flex",
          flexDirection: "column",
          gap: "1.5rem",
          maxWidth: "500px",
          width: "90%",
        }}
      >
        <h2
          style={{
            margin: 0,
            fontSize: "2rem",
            textAlign: "center",
            color: "#ffd700",
            letterSpacing: "0.1em",
          }}
        >
          Ollama Connection Test
        </h2>

        <div
          style={{
            display: "flex",
            flexDirection: "column",
            gap: "1rem",
            padding: "1.5rem",
            backgroundColor: "rgba(0, 0, 0, 0.2)",
            borderRadius: "8px",
          }}
        >
          <div
            style={{
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              fontSize: "1rem",
            }}
          >
            <span style={{ opacity: 0.7 }}>Status:</span>
            <span style={{ color: getStatusColor(), fontWeight: "bold" }}>
              {getStatusText()}
            </span>
          </div>

          {latency !== null && (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                fontSize: "1rem",
              }}
            >
              <span style={{ opacity: 0.7 }}>Latency:</span>
              <span>{latency}ms</span>
            </div>
          )}

          {model && (
            <div
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: "center",
                fontSize: "1rem",
              }}
            >
              <span style={{ opacity: 0.7 }}>Model:</span>
              <span>{model}</span>
            </div>
          )}

          {error && (
            <div
              style={{
                color: "#f87171",
                fontSize: "0.9rem",
                padding: "1rem",
                backgroundColor: "rgba(248, 113, 113, 0.1)",
                borderRadius: "4px",
                wordBreak: "break-word",
              }}
            >
              {error}
            </div>
          )}
        </div>

        <div
          style={{
            display: "flex",
            gap: "1rem",
            justifyContent: "center",
          }}
        >
          <button
            onClick={testOllama}
            disabled={status === "testing"}
            style={{
              padding: "1rem 2rem",
              fontSize: "1rem",
              backgroundColor: status === "testing" ? "rgba(255, 255, 255, 0.1)" : "#ffd700",
              color: status === "testing" ? "rgba(255, 255, 255, 0.5)" : "#1a1a2e",
              border: "none",
              borderRadius: "8px",
              cursor: status === "testing" ? "not-allowed" : "pointer",
              transition: "all 0.2s ease",
              fontWeight: "bold",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            {status === "testing" ? "Testing..." : "Test Connection"}
          </button>

          <button
            onClick={onBack}
            style={{
              padding: "1rem 2rem",
              fontSize: "1rem",
              backgroundColor: "rgba(255, 255, 255, 0.1)",
              color: "#ffffff",
              border: "1px solid rgba(255, 255, 255, 0.2)",
              borderRadius: "8px",
              cursor: "pointer",
              transition: "all 0.2s ease",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            Back to Menu
          </button>
        </div>

        <div
          style={{
            fontSize: "0.8rem",
            textAlign: "center",
            opacity: 0.5,
            marginTop: "0.5rem",
          }}
        >
          Ollama should be running on localhost:11434
        </div>
      </div>
    </div>
  );
}
