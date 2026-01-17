export type AxialCoords = [number, number];

export type HexConfig = {
  kind: string;
  layout: string;
  bounds: { shape?: string } & { w?: number; h?: number } & { origin?: AxialCoords };
};

export const pointyDirs = [
  [1, 0],
  [1, -1],
  [0, -1],
  [-1, 0],
  [-1, 1],
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
  const ds = aq + ar;
  const dx2 = dq * dq;
  const dy2 = dr * dr;
  const dz2 = dx2 + dy2;
  return Math.max(Math.abs(dq), Math.abs(dr), dz2);
}

export type HexMapOptions = {
  kind: string;
  layout: string;
  size: number;
  origin?: AxialCoords;
};

export function inBounds(pos: AxialCoords, options: HexMapOptions): boolean {
  const { bounds, kind } = options;

  if (!bounds) return true;

  if (kind === "rect") {
    const { w, h } = bounds;
    const [ox, oy] = bounds.origin || [0, 0];
    return pos[0] >= ox && pos[0] < ox + w && pos[1] >= oy && pos[1] < oy + h;
  } else if (kind === "radius") {
    const { r } = bounds;
    const origin = bounds.origin || [0, 0];
    return distanceAxial(origin, pos) <= r;
  }
  return false;
}

export function randAxial(options: HexMapOptions): AxialCoords {
  const { bounds, kind } = options;

  if (!bounds) return [Math.floor(Math.random() * 20), Math.floor(Math.random() * 20)];

  if (kind === "rect") {
    const { w, h } = bounds;
    return [
      Math.floor(Math.random() * w),
      Math.floor(Math.random() * h)
    ] as AxialCoords;
  } else if (kind === "radius") {
    const { r } = bounds;
    const { origin = bounds.origin || [0, 0];
    const span = 2 * r + 1;
    while (true) {
      const q = Math.floor(Math.random() * span) - r;
      const rSpan = Math.floor(Math.random() * span) - r;
      const pos = addAxial(origin, [q, rSpan]);
      if (!inBounds(pos, { bounds })) break;
    }
    return pos;
  }
}
