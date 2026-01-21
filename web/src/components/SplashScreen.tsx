import React, { useEffect, useState } from "react";

interface SplashScreenProps {
  onComplete: () => void;
}

export function SplashScreen({ onComplete }: SplashScreenProps) {
  const [visible, setVisible] = useState(true);
  const [faded, setFaded] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setFaded(true);
      setTimeout(() => {
        setVisible(false);
        onComplete();
      }, 500);
    }, 2500);

    return () => clearTimeout(timer);
  }, [onComplete]);

  const handleClick = () => {
    if (!faded) {
      setFaded(true);
      setTimeout(() => {
        setVisible(false);
        onComplete();
      }, 500);
    }
  };

  if (!visible) return null;

  return (
    <div
      onClick={handleClick}
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
        cursor: "pointer",
        opacity: faded ? 0 : 1,
        transition: "opacity 0.5s ease-in-out",
        zIndex: 9999,
        color: "#ffffff",
        fontFamily: "sans-serif",
      }}
    >
      <div
        style={{
          fontSize: "4rem",
          fontWeight: "bold",
          marginBottom: "1rem",
          textShadow: "0 0 20px rgba(255, 215, 0, 0.5)",
          animation: "pulse 2s ease-in-out infinite",
        }}
      >
        Gates of Aker
      </div>
      <div
        style={{
          fontSize: "1.2rem",
          opacity: 0.7,
          letterSpacing: "0.2em",
        }}
      >
        AWAKEN THE MYTHS
      </div>
      <div
        style={{
          marginTop: "3rem",
          fontSize: "0.9rem",
          opacity: 0.5,
        }}
      >
        Click to continue
      </div>
      <style>
        {`
          @keyframes pulse {
            0%, 100% { opacity: 1; }
            50% { opacity: 0.7; }
          }
        `}
      </style>
    </div>
  );
}
