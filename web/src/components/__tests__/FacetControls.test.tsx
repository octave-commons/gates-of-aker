import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { FacetControls } from '../FacetControls';
import type { HexConfig } from '../../hex';

describe('FacetControls', () => {
  const mockMapConfig: HexConfig = {
    kind: 'hex',
    layout: 'pointy',
    bounds: { shape: 'radius', r: 20 },
  };

  const mockOnFacetLimitChange = vi.fn();
  const mockOnVisionRadiusChange = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders component with all controls', () => {
    render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    expect(screen.getByText('Facet Configuration')).not.toBeNull();
    expect(screen.getByText(/Facet Limit/)).not.toBeNull();
    expect(screen.getByText(/Vision Radius/)).not.toBeNull();
    expect(screen.getByText('Maximum facets considered per facet query')).not.toBeNull();
    expect(screen.getByText('Radius for facet gathering from world')).not.toBeNull();
  });

  it('displays facet limit value correctly', () => {
    render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={32}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    expect(screen.getByText(/32/)).not.toBeNull();
  });

  it('displays vision radius value correctly', () => {
    render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={15}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    expect(screen.getByText(/15/)).not.toBeNull();
  });

  it('renders facet limit slider with correct attributes', () => {
    render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    const facetLimitSlider = screen.getByLabelText(/Facet Limit/i) as HTMLInputElement;
    expect(facetLimitSlider.type).toBe('range');
    expect(Number(facetLimitSlider.min)).toBe(8);
    expect(Number(facetLimitSlider.max)).toBe(64);
    expect(Number(facetLimitSlider.value)).toBe(16);
  });

  it('renders vision radius slider with correct attributes', () => {
    render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    const visionRadiusSlider = screen.getByLabelText(/Vision Radius/i) as HTMLInputElement;
    expect(visionRadiusSlider.type).toBe('range');
    expect(Number(visionRadiusSlider.min)).toBe(5);
    expect(Number(visionRadiusSlider.max)).toBe(20);
    expect(Number(visionRadiusSlider.value)).toBe(10);
  });

  it('positions controls correctly on screen', () => {
    const { container } = render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    const panel = container.firstChild as HTMLElement;
    expect(panel.style.position).toBe('absolute');
    expect(panel.style.bottom).toBe('80px');
    expect(panel.style.right).toBe('20px');
    expect(panel.style.zIndex).toBe('10');
  });

  it('has proper styling for visibility', () => {
    const { container } = render(
      <FacetControls
        mapConfig={mockMapConfig}
        facetLimit={16}
        visionRadius={10}
        onFacetLimitChange={mockOnFacetLimitChange}
        onVisionRadiusChange={mockOnVisionRadiusChange}
      />
    );
    
    const panel = container.firstChild as HTMLElement;
    expect(panel.style.backgroundColor).toBe('rgba(0, 0, 0, 0.8)');
    expect(panel.style.padding).toBe('16px');
    expect(panel.style.borderRadius).toBe('8px');
    expect(panel.style.color).toBe('white');
  });
});
