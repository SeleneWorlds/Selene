import type { EntitiesApi, EntityApi } from '@/api/EntitiesApi';
import { LuaArguments } from './LuaArguments';
import { toLuaCoordinate } from './LuaObjects';
import type { LuaRuntime } from './LuaRuntime';
import type { RegistriesApi } from '@/api/RegistriesApi';
import type { VisualsApi } from '@/api/VisualsApi';

export interface EntityLuaApis { registries: RegistriesApi; visuals: VisualsApi }

const luaEntities = new WeakMap<EntityApi, ReturnType<typeof createLuaEntity>>();

export async function registerEntitiesLuaModule(runtime: LuaRuntime, entities: EntitiesApi, apis: EntityLuaApis): Promise<void> {
  const create = (entityDefinition: unknown) => {
    if (typeof entityDefinition !== 'string' && (typeof entityDefinition !== 'object' || entityDefinition === null)) {
      throw new Error('selene.entities.create expected entityDefinition to be a string or entity definition.');
    }

    return toLuaEntity(entities.create(entityDefinition as Parameters<EntitiesApi['create']>[0]), apis);
  };
  const getEntitiesAt = (coordinateValue: unknown) => {
    const args = new LuaArguments('selene.entities.getEntitiesAt');
    const coordinate = args.coordinate(coordinateValue, 'coordinate');

    return entities.getEntitiesAt(coordinate).map((entity) => toLuaEntity(entity, apis));
  };
  const findEntitiesAt = (coordinateValue: unknown, criteriaValue?: unknown) => {
    const args = new LuaArguments('selene.entities.findEntitiesAt');
    const coordinate = args.coordinate(coordinateValue, 'coordinate');
    const criteria = parseCriteria(criteriaValue);

    return entities.findEntitiesAt(coordinate, criteria).map((entity) => toLuaEntity(entity, apis));
  };

  await runtime.preloadModule('selene.entities', () => ({
    create,
    getEntitiesAt,
    findEntitiesAt,
  }));
}

export function toLuaEntity(entity: EntityApi, apis: EntityLuaApis): {
  getNetworkId: () => number;
  getCoordinate: () => ReturnType<typeof toLuaCoordinate>;
  getFacing: () => number;
  spawn: () => void;
  despawn: () => void;
  setCoordinate: (coordinate: unknown) => void;
  addComponent: (name: unknown, componentData: unknown) => void;
  getComponent: (name: unknown) => unknown;
  getDefinition: () => unknown;
} {
  let luaEntity = luaEntities.get(entity);
  if (!luaEntity) {
    luaEntity = createLuaEntity(entity, apis);
    luaEntities.set(entity, luaEntity);
  }
  return luaEntity;
}

function createLuaEntity(entity: EntityApi, apis: EntityLuaApis): {
  getNetworkId: () => number;
  getCoordinate: () => ReturnType<typeof toLuaCoordinate>;
  getFacing: () => number;
  spawn: () => void;
  despawn: () => void;
  setCoordinate: (coordinate: unknown) => void;
  addComponent: (name: unknown, componentData: unknown) => void;
  getComponent: (name: unknown) => unknown;
  getDefinition: () => unknown;
} {
  return {
    getNetworkId: () => entity.getNetworkId(),
    getCoordinate: () => toLuaCoordinate(entity.getCoordinate()),
    getFacing: () => entity.getFacing(),
    spawn: () => entity.spawn(),
    despawn: () => entity.despawn(),
    setCoordinate: (coordinateValue) => {
      const args = new LuaArguments('Entity.setCoordinate');

      entity.setCoordinate(args.coordinate(coordinateValue, 'coordinate'));
    },
    addComponent: (name, componentData) => {
      const args = new LuaArguments('Entity.addComponent');

      entity.addComponent(args.string(name, 'name'), componentData);
    },
    getComponent: (name) => {
      const args = new LuaArguments('Entity.getComponent');

      const componentName = args.string(name, 'name');
      const component = entity.getComponent(componentName);

      return toLuaComponent(component, apis.visuals, (property, value) => {
        if (property === 'alpha') entity.setComponentAlpha(componentName, value);
        else entity.setComponentProperty(componentName, property, value);
      });
    },
    getDefinition: () => apis.registries.findByName('entities', entity.getDefinitionName()),
  };
}

function toLuaComponent(
  component: unknown,
  visuals: VisualsApi,
  update: (property: string, value: number) => void,
): unknown {
  if (!isRecord(component)) {
    return component;
  }

  const set = (property: string, value: number) => {
    component[property] = value;
    update(property, value);
  };
  return {
    ...component,
    getVisual: () => typeof component.visual === 'string'
      ? visuals.create(component.visual)
      : component.visual ?? null,
    getRed: () => componentNumber(component.red),
    setRed: (...values: unknown[]) => set('red', componentArgument(values, 'red')),
    getGreen: () => componentNumber(component.green),
    setGreen: (...values: unknown[]) => set('green', componentArgument(values, 'green')),
    getBlue: () => componentNumber(component.blue),
    setBlue: (...values: unknown[]) => set('blue', componentArgument(values, 'blue')),
    getAlpha: () => componentNumber(component.alpha),
    setAlpha: (...values: unknown[]) => {
      set('alpha', componentArgument(values, 'alpha'));
    },
  };
}

function componentNumber(value: unknown): number {
  return typeof value === 'number' && Number.isFinite(value) ? value : 1;
}

function componentArgument(values: unknown[], property: string): number {
  const args = new LuaArguments(`Component.set${property[0].toUpperCase()}${property.slice(1)}`);
  return args.finiteNumber(values[values.length - 1], property);
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function parseCriteria(criteriaValue: unknown): { tag?: string } | undefined {
  if (criteriaValue === undefined || criteriaValue === null) {
    return undefined;
  }

  if (typeof criteriaValue !== 'object') {
    throw new Error('selene.entities.findEntitiesAt expected criteria to be a table.');
  }

  const criteria = criteriaValue as { tag?: unknown };
  if (criteria.tag === undefined || criteria.tag === null) {
    return undefined;
  }

  const args = new LuaArguments('selene.entities.findEntitiesAt');
  return {
    tag: args.string(criteria.tag, 'criteria.tag'),
  };
}
