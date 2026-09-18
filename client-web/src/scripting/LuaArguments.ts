import type { ClientInputType } from '@/api/InputApi';
import type { Coordinate } from '@/networking/GameProtocol';

export class LuaArguments {
  constructor(private readonly context: string) {}

  function<T extends (...args: unknown[]) => unknown>(value: unknown, label: string): T {
    if (typeof value !== 'function') {
      throw new Error(`${this.context} expected ${label} to be a function.`);
    }

    return value as T;
  }

  string(value: unknown, label: string): string {
    if (typeof value !== 'string' || value.trim() === '') {
      throw new Error(`${this.context} expected ${label} to be a non-empty string.`);
    }

    return value;
  }

  inputType(value: unknown, label: string): ClientInputType {
    const stringValue = this.string(value, label).toLowerCase();

    if (stringValue !== 'keyboard' && stringValue !== 'mouse') {
      throw new Error(`${this.context} expected ${label} to be keyboard or mouse.`);
    }

    return stringValue;
  }

  serializedMap(value: unknown, label: string): Record<string, unknown> {
    if (typeof value !== 'object' || value === null || Array.isArray(value)) {
      throw new Error(`${this.context} expected ${label} to be a table.`);
    }

    return value as Record<string, unknown>;
  }

  coordinate(value: unknown, label: string): Coordinate {
    if (typeof value !== 'object' || value === null) {
      throw new Error(`${this.context} expected ${label} to be a coordinate.`);
    }

    const coordinate = value as Partial<Coordinate> & {
      getX?: () => unknown;
      getY?: () => unknown;
      getZ?: () => unknown;
    };

    return {
      x: this.integer(coordinate.x ?? coordinate.getX?.(), `${label}.x`),
      y: this.integer(coordinate.y ?? coordinate.getY?.(), `${label}.y`),
      z: this.integer(coordinate.z ?? coordinate.getZ?.(), `${label}.z`),
    };
  }

  finiteNumber(value: unknown, label: string): number {
    if (typeof value !== 'number' || !Number.isFinite(value)) {
      throw new Error(`${this.context} expected ${label} to be a finite number.`);
    }

    return value;
  }

  integer(value: unknown, label: string): number {
    const numberValue = this.finiteNumber(value, label);

    if (!Number.isInteger(numberValue)) {
      throw new Error(`${this.context} expected ${label} to be an integer.`);
    }

    return numberValue;
  }
}
