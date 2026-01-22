import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import React from 'react';
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
});
