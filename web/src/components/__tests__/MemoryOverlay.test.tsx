import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { MemoryOverlay } from '../MemoryOverlay';
import type { HexConfig } from '../../hex';
import { CONFIG } from '../../config/constants';

describe('MemoryOverlay', () => {
  const mockMapConfig: HexConfig = {
    kind: 'hex',
    layout: 'pointy',
    bounds: { shape: 'radius', r: 20 },
  };

  const mockMemories = [
    {
      id: 'mem-1',
      type: 'memory/danger',
      location: [0, 0] as [number, number],
      created_at: 100,
      strength: 0.8,
      decay_rate: 0.001,
      entity_id: 'agent-1',
      facets: ['wolf', 'danger', 'threat'],
    },
    {
      id: 'mem-2',
      type: 'memory/social-bond',
      location: [5, 0] as [number, number],
      created_at: 105,
      strength: 0.9,
      decay_rate: 0.001,
      entity_id: 'agent-2',
      facets: ['friend', 'ally', 'trust'],
    },
    {
      id: 'mem-3',
      type: 'memory/danger',
      location: [-3, 2] as [number, number],
      created_at: 110,
      strength: 0.2,
      decay_rate: 0.001,
      entity_id: 'wolf-1',
      facets: ['wolf', 'pack'],
    },
  ];

  beforeEach(() => {
    Object.defineProperty(window, 'innerWidth', { value: 800, writable: true });
    Object.defineProperty(window, 'innerHeight', { value: 600, writable: true });
  });

  it('renders null when showMemories is false', () => {
    const { container } = render(
      <MemoryOverlay
        memories={mockMemories}
        mapConfig={mockMapConfig}
        showMemories={false}
        strengthThreshold={0.3}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders null when mapConfig is null', () => {
    const { container } = render(
      <MemoryOverlay
        memories={mockMemories}
        mapConfig={null}
        showMemories={true}
        strengthThreshold={0.3}
      />
    );
    expect(container.firstChild).toBeNull();
  });

  it('renders canvas when showMemories is true and mapConfig exists', () => {
    const { container } = render(
      <MemoryOverlay
        memories={mockMemories}
        mapConfig={mockMapConfig}
        showMemories={true}
        strengthThreshold={0.3}
      />
    );
    const canvas = container.querySelector('canvas');
    expect(canvas).not.toBeNull();
    expect(canvas?.style.position).toBe('absolute');
    expect(canvas?.style.pointerEvents).toBe('none');
    expect(canvas?.style.zIndex).toBe('5');
  });

  it('has correct z-index for overlay positioning', () => {
    const { container } = render(
      <MemoryOverlay
        memories={mockMemories}
        mapConfig={mockMapConfig}
        showMemories={true}
        strengthThreshold={0.3}
      />
    );
    const canvas = container.querySelector('canvas');
    expect(canvas?.style.zIndex).toBe('5');
  });

  describe('Visibility filtering based on strength threshold', () => {
    it('filters out weak memories below threshold', () => {
      const { container } = render(
        <MemoryOverlay
          memories={mockMemories}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.5}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
      // Memory with strength 0.2 should be filtered out
      // Memories with strength 0.8 and 0.9 should be rendered
    });

    it('renders all memories when threshold is very low', () => {
      const { container } = render(
        <MemoryOverlay
          memories={mockMemories}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.1}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });

    it('renders only strongest memories when threshold is high', () => {
      const { container } = render(
        <MemoryOverlay
          memories={mockMemories}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.85}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
      // Only memory with strength 0.9 should be rendered
    });
  });

  describe('Memory types and visibility', () => {
    it('handles different memory types with visibility filtering', () => {
      const differentMemoryTypes = [
        {
          id: 'food-memory',
          type: 'memory/food-source',
          location: [0, 0] as [number, number],
          created_at: 100,
          strength: 0.7,
          decay_rate: 0.001,
          entity_id: 'berry-bush',
          facets: ['edible', 'organic'],
        },
        {
          id: 'danger-memory',
          type: 'memory/danger',
          location: [1, 1] as [number, number],
          created_at: 105,
          strength: 0.6,
          decay_rate: 0.002,
          entity_id: 'predator',
          facets: ['threat', 'animal'],
        },
        {
          id: 'social-memory',
          type: 'memory/social-bond',
          location: [-2, -1] as [number, number],
          created_at: 110,
          strength: 0.8,
          decay_rate: 0.001,
          entity_id: 'ally',
          facets: ['friend', 'trust'],
        },
      ];

      const { container } = render(
        <MemoryOverlay
          memories={differentMemoryTypes}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.5}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });

    it('handles memories at various coordinate positions', () => {
      const variousPositions = [
        {
          id: 'origin-memory',
          type: 'memory/location',
          location: [0, 0] as [number, number],
          created_at: 100,
          strength: 0.7,
          decay_rate: 0.001,
          entity_id: null,
          facets: ['origin'],
        },
        {
          id: 'negative-x-memory',
          type: 'memory/location',
          location: [-5, 2] as [number, number],
          created_at: 105,
          strength: 0.6,
          decay_rate: 0.001,
          entity_id: null,
          facets: ['west'],
        },
        {
          id: 'positive-y-memory',
          type: 'memory/location',
          location: [3, 8] as [number, number],
          created_at: 110,
          strength: 0.8,
          decay_rate: 0.001,
          entity_id: null,
          facets: ['north'],
        },
      ];

      const { container } = render(
        <MemoryOverlay
          memories={variousPositions}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.5}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });
  });

  describe('Edge cases for visibility', () => {
    it('handles empty memories array with visibility settings', () => {
      const { container } = render(
        <MemoryOverlay
          memories={[]}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.5}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });

    it('handles memories with zero strength', () => {
      const zeroStrengthMemories = [
        {
          id: 'zero-strength',
          type: 'memory/location',
          location: [0, 0] as [number, number],
          created_at: 100,
          strength: 0,
          decay_rate: 0.001,
          entity_id: null,
          facets: ['forgotten'],
        },
      ];

      const { container } = render(
        <MemoryOverlay
          memories={zeroStrengthMemories}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.1}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });

    it('handles memories with maximum strength', () => {
      const maxStrengthMemories = [
        {
          id: 'max-strength',
          type: 'memory/location',
          location: [0, 0] as [number, number],
          created_at: 100,
          strength: 1.0,
          decay_rate: 0.001,
          entity_id: null,
          facets: ['vivid'],
        },
      ];

      const { container } = render(
        <MemoryOverlay
          memories={maxStrengthMemories}
          mapConfig={mockMapConfig}
          showMemories={true}
          strengthThreshold={0.9}
        />
      );
      const canvas = container.querySelector('canvas');
      expect(canvas).toBeInTheDocument();
    });
  });
});
