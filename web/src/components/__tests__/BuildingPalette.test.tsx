import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { BuildingPalette } from '../BuildingPalette';

describe('BuildingPalette', () => {
  it('renders palette title', () => {
    render(
      <BuildingPalette
        onPlaceBuilding={vi.fn()}
        selectedCell={null}
      />
    );

    expect(screen.getByText('Building Palette')).toBeDefined();
  });

  it('shows selected cell feedback', () => {
    render(
      <BuildingPalette
        onPlaceBuilding={vi.fn()}
        selectedCell={[5, 3]}
      />
    );

    expect(screen.getByText('Selected: [5, 3]')).toBeDefined();
  });

  it('enables building placement workflow', () => {
    const mockOnPlaceBuilding = vi.fn();
    render(
      <BuildingPalette
        onPlaceBuilding={mockOnPlaceBuilding}
        selectedCell={[5, 3]}
      />
    );

    // Should show ready to place message after clicking a building
    const buildingIcons = screen.getAllByTitle(/:/);
    expect(buildingIcons.length).toBeGreaterThan(0);
    
    // Click first building
    fireEvent.click(buildingIcons[0]);
    
    // Should show a place button
    const placeButtons = screen.getAllByText(/^Place /);
    expect(placeButtons.length).toBeGreaterThan(0);
  });

  it('shows no cell selected message when no cell selected', () => {
    render(
      <BuildingPalette
        onPlaceBuilding={vi.fn()}
        selectedCell={null}
      />
    );

    expect(screen.queryByText(/Selected:/)).toBeNull();
  });
});