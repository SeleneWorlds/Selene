export interface LightVertex { x: number; y: number; red: number; green: number; blue: number }
export interface LightRect { minX: number; minY: number; maxX: number; maxY: number }

/** Clip a shared lighting triangulation to artwork bounds, preserving its interpolated colors. */
export function buildSharedLightMesh(
  bounds: LightRect, stepX: number, stepY: number,
  sample: (x: number, y: number) => Omit<LightVertex, 'x' | 'y'>,
): LightVertex[] {
  const result: LightVertex[] = [];
  for (let y = Math.floor(bounds.minY / stepY) * stepY; y < bounds.maxY; y += stepY) {
    for (let x = Math.floor(bounds.minX / stepX) * stepX; x < bounds.maxX; x += stepX) {
      const a = { x, y, ...sample(x, y) };
      const b = { x: x + stepX, y, ...sample(x + stepX, y) };
      const c = { x: x + stepX, y: y + stepY, ...sample(x + stepX, y + stepY) };
      const d = { x, y: y + stepY, ...sample(x, y + stepY) };
      for (const triangle of [[a, b, c], [a, c, d]]) {
        let polygon = triangle;
        for (const [axis, edge, sign] of [
          ['x', bounds.minX, 1], ['x', bounds.maxX, -1],
          ['y', bounds.minY, 1], ['y', bounds.maxY, -1],
        ] as const) {
          const clipped: LightVertex[] = [];
          for (let i = 0; i < polygon.length; i += 1) {
            const start = polygon[i];
            const end = polygon[(i + 1) % polygon.length];
            const insideStart = (start[axis] - edge) * sign >= 0;
            const insideEnd = (end[axis] - edge) * sign >= 0;
            if (insideStart) clipped.push(start);
            if (insideStart !== insideEnd) {
              const t = (edge - start[axis]) / (end[axis] - start[axis]);
              clipped.push({
                x: start.x + (end.x - start.x) * t,
                y: start.y + (end.y - start.y) * t,
                red: start.red + (end.red - start.red) * t,
                green: start.green + (end.green - start.green) * t,
                blue: start.blue + (end.blue - start.blue) * t,
              });
            }
          }
          polygon = clipped;
        }
        for (let i = 1; i + 1 < polygon.length; i += 1) {
          const [a, b, c] = [polygon[0], polygon[i], polygon[i + 1]];
          if (Math.abs((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)) > 1e-6) {
            result.push(a, b, c);
          }
        }
      }
    }
  }
  return result;
}
