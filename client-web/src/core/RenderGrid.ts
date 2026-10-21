import { ClientRenderGridDefinitionSchema, type ClientRenderGridDefinition } from '@/data/ClientRegistrySchemas';

const defaults = ClientRenderGridDefinitionSchema.parse({});
export let TILE_WIDTH = defaults.tileWidth;
export let TILE_HEIGHT = defaults.tileHeight;
export let TILE_STEP_X = defaults.tileStepX;
export let TILE_STEP_Y = defaults.tileStepY;
export let TILE_STEP_Z = defaults.tileStepZ;
export let Z_SORT_SCALE = defaults.zSortScale;
export let ROW_SORT_SCALE = defaults.rowSortScale;

export function applyRenderGrid(definition: ClientRenderGridDefinition): void {
  TILE_WIDTH = definition.tileWidth;
  TILE_HEIGHT = definition.tileHeight;
  TILE_STEP_X = definition.tileStepX;
  TILE_STEP_Y = definition.tileStepY;
  TILE_STEP_Z = definition.tileStepZ;
  Z_SORT_SCALE = definition.zSortScale;
  ROW_SORT_SCALE = definition.rowSortScale;
}
