# World Schema (Axial Hex Coordinates)

## Overview

The world is modeled as a hexagonal grid using **axial coordinates** with pointy-top orientation. All positions are stored as `[q, r]` tuples.

## Coordinate System

### Axial Coordinates

A hex position is represented as a tuple `[q, r]`:

- `q` = column-like axis (diagonal to the right)
- `r` = row-like axis (vertical)
- Third implicit axis: `s = -q - r` (for cube coordinate calculations)

### Pointy-Top Orientation

Hexes are "pointy" on the top/bottom. This is important for:
- Pixel-to-axial conversion
- Neighbor direction vectors
- Which axis is horizontal/vertical in screen space

### Direction Vectors

The six neighbors of any hex `[q, r]` are:

```clojure
[+1,  0]  ; east
[+1, -1]  ; northeast
[ 0, -1]  ; northwest
[-1,  0]  ; west
[-1, +1]  ; southwest
[ 0, +1]  ; southeast
```

## World State Structure

### Map Configuration

```clojure
:map {:kind :hex
      :layout :pointy
      :bounds {:shape :rect          ; or :radius
               :w 20                ; width (for rect)
               :h 20}              ; height (for rect)
               ; or
               ;:r 10              ; radius (for radius)
               :origin [0 0]}      ; optional axial origin
```

#### Bound Shapes

- `:rect` - Rectangular axial bounds:
  - All positions where `origin[0] ≤ q < origin[0] + w` and `origin[1] ≤ r < origin[1] + h`
  - Useful for rectangular maps

- `:radius` - Circular bounds:
  - All positions where `hex-distance(origin, [q r]) ≤ r`
  - Useful for natural-feeling maps

### Tiles (Sparse Map)

Tiles are stored as a sparse map keyed by axial coordinates:

```clojure
:tiles {"q,r" {:terrain :ground
               :structure nil
               :resource :tree}   ; or nil
        "q2,r5" {:terrain :ground
                  :structure :wall
                  :resource nil}}
```

The key is a string `"q,r"` for easy serialization over WebSocket.

#### Tile Schema

Each tile has:

- `:terrain` - `:ground` (default), `:water`, `:mountain`, etc.
- `:structure` - `nil`, `:wall`, `:door`, `:shrine`, etc.
- `:resource` - `nil`, `:tree`, `:stone`, `:iron`, etc.

### Agents

Agents have axial positions:

```clojure
{:id 1
 :pos [5 3]           ; axial [q, r]
 :role "knight"
 :needs {:hunger 0.5 :fatigue 0.2}
 :recall {...}}
```

### Shrine Position

```clojure
:shrine [10 10]  ; axial [q, r] or nil if not placed
```

## WebSocket Snapshot Payload

The snapshot sent from backend to frontend includes:

```clojure
{:tick 42
 :shrine [10 10]
 :agents [{:id 1 :pos [5 3] :role "knight"} ...]
 :levers {...}
 :map {:kind :hex :layout :pointy :bounds {:shape :rect :w 20 :h 20}}
 :tiles {"10,10" {:terrain :ground :structure :shrine}
         "5,3" {:terrain :ground :resource nil}
         ...}
 :attribution {...}}
```

## Frontend Integration

### TypeScript Types

```typescript
export type AxialCoords = [number, number];

export type HexConfig = {
  kind: "hex";
  layout: "pointy";
  bounds: {
    shape: "rect" | "radius";
    w?: number;
    h?: number;
    r?: number;
    origin?: AxialCoords;
  };
};

export type Tile = {
  terrain: string;
  structure?: string | null;
  resource?: string | null;
};
```

### Axial to Pixel Conversion

For pointy-top hexes with size `s`:

```
x = size * sqrt(3) * (q + r/2)
y = size * 3/2 * r
```

### Pixel to Axial Conversion

```
q = (2/3 * x) / size
r = (-1/3 * x + sqrt(3)/3 * y) / size
```

Then apply cube rounding to snap to nearest hex.

## Distance Calculation

Hex distance between `[q1, r1]` and `[q2, r2]`:

```clojure
(defn distance [[q1 r1] [q2 r2]]
  (let [dq (- q1 q2)
        dr (- r1 r2)
        ds (+ dq dr)]
    (/ (+ (Math/abs dq) (Math/abs dr) (Math/abs ds)) 2)))
```

## Migration from Square Grid

Previous square grid used `[x, y]` coordinates with Manhattan distance.

Migration changes:
- `[x, y]` → `[q, r]` (axial)
- 4 neighbors → 6 neighbors
- Manhattan distance → Hex distance
- `:size [20 20]` → `:map {:kind :hex ...}`
- `:trees #{[x y]}` → `:tiles {"q,r" {:resource :tree}}`

## Future Extensions

### Walls (Milestone 2)

Tiles with `:structure :wall` will block agent movement. Pathfinding will need to consider passability.

### Factions (Milestone 5)

Factions may control territory. Consider adding `:owner` to tiles:
```clojure
:tiles {"5,3" {:terrain :ground :structure :wall :owner :faction-1}}
```

### Resources and Jobs (Milestone 3)

Trees are `:resource :tree`. Future resources:
- `:stone` - from rocks
- `:iron` - from mining
- `:wood` - processed from trees (item stack)

### Champion Control (Milestone 4)

Add champion-specific state:
```clojure
:champion {:id 100
            :pos [5 3]
            :asleep? false
            :phase-action-points 2}
```

## Implementation Notes

- **Deterministic randomness**: Use seeded random for reproducible tests. `backend/src/fantasia/sim/hex.clj` provides `rand-pos` with seed support.
- **Coordinate validation**: Always check `in-bounds?` before placing or moving.
- **Sparse tiles**: Send only non-ground tiles in snapshots to keep payloads lean.
- **Frontend canvas**: Use `axialToPixel` for all drawing, `pixelToAxial` for click handling.
