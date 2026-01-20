import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BuildingPalette } from '../BuildingPalette';

describe('BuildingPalette', () => {
  it('renders palette title', () => {
    render(
      <BuildingPalette
        onQueueBuild={vi.fn()}
        selectedCell={null}
      />
    );

    expect(screen.getByText('Building Palette')).toBeDefined();
  });

  it('shows selected cell feedback', () => {
    render(
      <BuildingPalette
        onQueueBuild={vi.fn()}
        selectedCell={[5, 3]}
      />
    );

    expect(screen.getByText('Selected: [5, 3]')).toBeDefined();
  });

  it('enables building placement workflow', () => {
    const mockOnPlaceBuilding = vi.fn();
    render(
      <BuildingPalette
        onQueueBuild={mockOnPlaceBuilding}
        selectedCell={[5, 3]}
      />
    );

    // Should show ready to queue message after clicking a building
    const buildingIcons = screen.getAllByTitle(/:/);
    expect(buildingIcons.length).toBeGreaterThan(0);
    
    // Click first building
    fireEvent.click(buildingIcons[0]);
    
    // Should show a queue button
    const queueButtons = screen.getAllByText(/^Queue /);
    expect(queueButtons.length).toBeGreaterThan(0);
  });

  it('shows no cell selected message when no cell selected', () => {
    render(
      <BuildingPalette
        onQueueBuild={vi.fn()}
        selectedCell={null}
      />
    );

    expect(screen.queryByText(/Selected:/)).toBeNull();
  });
});
