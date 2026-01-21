import React, { useState, useEffect } from "react";

interface MainMenuProps {
  onNewGame: () => void;
  onOllamaTest: () => void;
}

export function MainMenu({ onNewGame, onOllamaTest }: MainMenuProps) {
  const [selectedIndex, setSelectedIndex] = useState(0);

  const menuItems = [
    { id: "newgame", label: "New Game", action: onNewGame },
    { id: "ollama", label: "Ollama Test", action: onOllamaTest },
    { id: "settings", label: "Settings", action: () => {} },
    { id: "credits", label: "Credits", action: () => {} },
  ];

  const handleSelect = () => {
    menuItems[selectedIndex].action();
  };

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case "ArrowUp":
          e.preventDefault();
          setSelectedIndex((prev) => (prev === 0 ? menuItems.length - 1 : prev - 1));
          break;
        case "ArrowDown":
          e.preventDefault();
          setSelectedIndex((prev) => (prev === menuItems.length - 1 ? 0 : prev + 1));
          break;
        case "Enter":
          e.preventDefault();
          handleSelect();
          break;
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [selectedIndex]);

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
      }}
    >
      <div
        style={{
          fontSize: "3rem",
          fontWeight: "bold",
          marginBottom: "3rem",
          color: "#ffd700",
          textShadow: "0 0 30px rgba(255, 215, 0, 0.6)",
          letterSpacing: "0.1em",
        }}
      >
        Gates of Aker
      </div>
      <div
        style={{
          display: "flex",
          flexDirection: "column",
          gap: "1rem",
          width: "300px",
        }}
      >
        {menuItems.map((item, index) => (
          <button
            key={item.id}
            onClick={() => {
              setSelectedIndex(index);
              item.action();
            }}
            onMouseEnter={() => setSelectedIndex(index)}
            style={{
              padding: "1rem 2rem",
              fontSize: "1.2rem",
              backgroundColor:
                index === selectedIndex
                  ? "rgba(255, 215, 0, 0.3)"
                  : "rgba(255, 255, 255, 0.1)",
              color: index === selectedIndex ? "#ffd700" : "#ffffff",
              border:
                index === selectedIndex ? "2px solid #ffd700" : "2px solid rgba(255, 255, 255, 0.2)",
              borderRadius: "8px",
              cursor: "pointer",
              transition: "all 0.2s ease",
              textTransform: "uppercase",
              letterSpacing: "0.1em",
            }}
          >
            {item.label}
          </button>
        ))}
      </div>
      <div
        style={{
          marginTop: "3rem",
          fontSize: "0.8rem",
          color: "rgba(255, 255, 255, 0.5)",
          letterSpacing: "0.1em",
        }}
      >
        Use arrow keys or mouse to navigate
      </div>
    </div>
  );
}
