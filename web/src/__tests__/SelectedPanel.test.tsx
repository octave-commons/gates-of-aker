import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { SelectedPanel } from '../components/SelectedPanel';

describe('SelectedPanel tile rendering', () => {
  it('renders structured tile rows instead of raw JSON payload', () => {
    render(
      <SelectedPanel
        selectedCell={[3, 4]}
        selectedTile={{
          biome: ':plains',
          terrain: ':ground',
          structure: ':campfire',
          resource: ':tree',
        }}
        selectedTileItems={{}}
        selectedTileAgents={[]}
        selectedAgentId={null}
        selectedAgent={null}
        selectedVisibilityAgentId={null}
        agentVisibilityMaps={{}}
        agents={[]}
        onSetVisibilityAgentId={vi.fn()}
        tileVisibility={{ '3,4': 'visible' }}
      />,
    );

    expect(screen.getByText('Coordinates')).toBeInTheDocument();
    expect(screen.getByText('(3, 4)')).toBeInTheDocument();
    expect(screen.getByText('Biome')).toBeInTheDocument();
    expect(screen.getByText('plains')).toBeInTheDocument();
    expect(screen.getByText('Structure')).toBeInTheDocument();
    expect(screen.getByText('campfire')).toBeInTheDocument();
    expect(screen.queryByText(/\{"biome":/i)).toBeNull();
  });
});
