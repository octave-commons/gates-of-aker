export type AxialCoords = [number, number];

export type HexMapBounds = {
  shape: "rect" | "radius";
} & (
  | { shape: "rect"; w: number; h: number }
  | { shape: "radius"; r: number }
) & { origin?: AxialCoords };

export type HexConfig = {
  kind: "hex";
  layout: "pointy";
  bounds: HexMapBounds;
};

const SQRT3 = Math.sqrt(3);

export const pointyDirs: AxialCoords[] = [
  [1, 0],
  [1, -1],
  [0, -1],
  [-1, 0],
  [-1, 1],
  [0, 1],
];

export function addAxial([aq, ar]: AxialCoords, [bq, br]: AxialCoords): AxialCoords {
  return [aq + bq, ar + br];
}

export function neighborsAxial(pos: AxialCoords): AxialCoords[] {
  return pointyDirs.map(dir => addAxial(pos, dir));
}

export function distanceAxial(a: AxialCoords, b: AxialCoords): number {
  const [aq, ar] = a;
  const [bq, br] = b;
  const dq = aq - bq;
  const dr = ar - br;
  const ds = dq + dr;
  return (Math.abs(dq) + Math.abs(dr) + Math.abs(ds)) / 2;
}

export function inBounds(pos: AxialCoords, bounds: HexMapBounds): boolean {
  const origin = bounds.origin ?? [0, 0];

  if (bounds.shape === "rect") {
    const { w, h } = bounds;
    return pos[0] >= origin[0] && pos[0] < origin[0] + w && pos[1] >= origin[1] && pos[1] < origin[1] + h;
  } else if (bounds.shape === "radius") {
    const { r } = bounds;
    return distanceAxial(origin, pos) <= r;
  }
  return false;
}

export function randAxial(bounds: HexMapBounds, seed?: number): AxialCoords {
  const origin = bounds.origin ?? [0, 0];
  const rng = seed !== undefined ? (() => {
    let s = seed;
    return () => {
      s = (s * 9301 + 49297) % 233280;
      return s / 233280;
    };
  })() : Math.random;

  if (bounds.shape === "rect") {
    const { w, h } = bounds;
    return [origin[0] + Math.floor(rng() * w), origin[1] + Math.floor(rng() * h)] as AxialCoords;
  } else if (bounds.shape === "radius") {
    const { r } = bounds;
    const span = 2 * r + 1;
    while (true) {
      const q = Math.floor(rng() * span) - r;
      const rCoord = Math.floor(rng() * span) - r;
      const pos: AxialCoords = [origin[0] + q, origin[1] + rCoord];
      if (inBounds(pos, bounds)) {
        return pos;
      }
    }
  }
  return origin;
}

export function axialToPixel([q, r]: AxialCoords, size: number): [number, number] {
  const x = size * SQRT3 * (q + r / 2);
  const y = size * 3 / 2 * r;
  return [x, y];
}

export function pixelToAxial(x: number, y: number, size: number): AxialCoords {
  const q = (SQRT3 / 3 * x - 1 / 3 * y) / size;
  const r = (2 / 3 * y) / size;
  return axialRound(q, r);
}

function axialRound(q: number, r: number): AxialCoords {
  const s = -q - r;
  let rq = Math.round(q);
  let rr = Math.round(r);
  let rs = Math.round(s);

  const qDiff = Math.abs(rq - q);
  const rDiff = Math.abs(rr - r);
  const sDiff = Math.abs(rs - s);

  if (qDiff > rDiff && qDiff > sDiff) {
    rq = -rr - rs;
  } else if (rDiff > sDiff) {
    rr = -rq - rs;
  }
  return [rq, rr];
}

export function hexCorner(center: [number, number], size: number, i: number): [number, number] {
  const angleDeg = 60 * i - 30;
  const angleRad = Math.PI / 180 * angleDeg;
  return [
    center[0] + size * Math.cos(angleRad),
    center[1] + size * Math.sin(angleRad),
  ];
}

export function getMapBoundsInPixels(bounds: HexMapBounds, size: number): { width: number; height: number } {
  if (bounds.shape === "rect") {
    const { w, h } = bounds;
    const hexWidth = size * SQRT3;
    const hexHeight = size * 2;
    const width = hexWidth * (w + h / 2);
    const height = hexHeight * h * 3 / 4;
    return { width: width + hexWidth, height: height + hexHeight / 2 };
  } else if (bounds.shape === "radius") {
    const { r } = bounds;
    const hexWidth = size * SQRT3;
    const hexHeight = size * 2;
    const width = hexWidth * (2 * r + 1.5);
    const height = hexHeight * r * 1.5 + hexHeight / 2;
    return { width, height };
  }
  return { width: 0, height: 0 };
}
