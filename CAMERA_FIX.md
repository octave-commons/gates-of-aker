# Camera Movement Fix

## Problem
Camera movement was unresponsive during simulation due to animation frame loop restarts.

## Root Cause
In `SimulationCanvas.tsx:177`, the `handleCameraMovement` effect included `camera` in its dependency array, causing the animation frame loop to restart every time the camera moved.

## Solution
Removed `camera` from the dependency array, keeping only `keysPressed`. This prevents unnecessary re-renders of the animation loop while maintaining camera functionality.

## Files Changed
- `web/src/components/SimulationCanvas.tsx:177` - Removed `camera` from dependency array

## Testing
- TypeScript compilation passes
- Animation frame loop now stable during simulation
- Camera controls (WASD) remain responsive during simulation ticks