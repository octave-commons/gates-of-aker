import React from 'react';
import type { HexConfig } from '../hex';
import { axialToPixel } from '../hex';
import { CONFIG } from '../config/constants';

type Memory = {
  id: string;
  type: string;
  location: [number, number];
  created_at: number;
  strength: number;
  decay_rate: number;
  entity_id: string | null;
  facets: string[];
};

type MemoryOverlayProps = {
  memories: Memory[];
  mapConfig: HexConfig | null;
  showMemories: boolean;
  strengthThreshold: number;
};

export function MemoryOverlay({
  memories,
  mapConfig,
  showMemories,
  strengthThreshold,
}: MemoryOverlayProps) {
  if (!showMemories || !mapConfig) return null;

  const canvasRef = React.useRef<HTMLCanvasElement | null>(null);

  React.useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const size = CONFIG.canvas.HEX_SIZE + CONFIG.canvas.HEX_SPACING;

    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Filter memories by strength threshold
    const visibleMemories = memories.filter((m) => m.strength >= strengthThreshold);

    visibleMemories.forEach((memory) => {
      const [q, r] = memory.location;
      const [x, y] = axialToPixel([q, r], size);

      // Calculate opacity based on memory strength (0.05 to 1.0 range)
      const opacity = Math.min(1.0, Math.max(0.1, memory.strength));

      // Choose color based on memory type
      let color = '#ff6b6b';
      if (memory.type === 'memory/danger') {
        color = '#e74c3c';
      } else if (memory.type === 'memory/social-bond') {
        color = '#3498db';
      } else if (memory.type === 'memory/social-conflict') {
        color = '#f39c12';
      }

      // Draw circle with opacity
      ctx.globalAlpha = opacity;
      ctx.fillStyle = color;
      ctx.beginPath();
      ctx.arc(x, y, size * 0.3, 0, Math.PI * 2);
      ctx.fill();

      // Draw border
      ctx.globalAlpha = opacity * 0.8;
      ctx.strokeStyle = color;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(x, y, size * 0.3, 0, Math.PI * 2);
      ctx.stroke();
    });

    ctx.globalAlpha = 1.0;
  }, [memories, mapConfig, showMemories, strengthThreshold]);

  return (
    <canvas
      ref={canvasRef}
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        pointerEvents: 'none',
        zIndex: 5,
      }}
    />
  );
}
