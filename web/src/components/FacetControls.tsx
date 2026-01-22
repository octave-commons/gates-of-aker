import React from 'react';
import type { HexConfig } from '../hex';

type FacetControlsProps = {
  mapConfig: HexConfig | null;
  facetLimit: number;
  visionRadius: number;
  onFacetLimitChange: (limit: number) => void;
  onVisionRadiusChange: (radius: number) => void;
};

export function FacetControls({
  mapConfig,
  facetLimit,
  visionRadius,
  onFacetLimitChange,
  onVisionRadiusChange,
}: FacetControlsProps) {
  const handleFacetLimitChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value, 10);
    onFacetLimitChange(value);
  };

  const handleVisionRadiusChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = parseInt(e.target.value, 10);
    onVisionRadiusChange(value);
  };

  return (
    <div
      style={{
        position: 'absolute',
        bottom: 80,
        right: 20,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        padding: '16px',
        borderRadius: '8px',
        color: 'white',
        fontSize: '14px',
        zIndex: 10,
      }}
    >
      <h3 style={{ margin: '0 0 12px 0', fontSize: '16px', fontWeight: 'bold' }}>
        Facet Configuration
      </h3>

      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="facet-limit" style={{ display: 'block', marginBottom: '4px' }}>
          Facet Limit (per query): {facetLimit}
        </label>
        <input
          id="facet-limit"
          type="range"
          min="8"
          max="64"
          value={facetLimit}
          onChange={handleFacetLimitChange}
          style={{ width: '100%' }}
        />
        <div style={{ fontSize: '12px', color: '#ccc' }}>
          Maximum facets considered per facet query
        </div>
      </div>

      <div style={{ marginBottom: '12px' }}>
        <label htmlFor="vision-radius" style={{ display: 'block', marginBottom: '4px' }}>
          Vision Radius (tiles): {visionRadius}
        </label>
        <input
          id="vision-radius"
          type="range"
          min="5"
          max="20"
          value={visionRadius}
          onChange={handleVisionRadiusChange}
          style={{ width: '100%' }}
        />
        <div style={{ fontSize: '12px', color: '#ccc' }}>
          Radius for facet gathering from world
        </div>
      </div>

      <div style={{ fontSize: '12px', color: '#888', paddingTop: '8px' }}>
        <div style={{ marginBottom: '4px' }}>
          <strong>Facet Types:</strong> 13 entity types registered
        </div>
        <div style={{ marginBottom: '4px' }}>
          <strong>Memory Types:</strong> danger, social-bond, social-conflict
        </div>
        <div>
          <strong>Memory Decay:</strong> 0.001 per tick
        </div>
      </div>
    </div>
  );
}
