import { z } from 'zod';

export const RegistryMetadataSchema = z.record(z.string(), z.unknown());
export type RegistryMetadata = z.infer<typeof RegistryMetadataSchema>;

const tagsSchema = z.array(z.string()).default([]);
const metadataSchema = RegistryMetadataSchema.default({});

export const ClientTileDefinitionSchema = z.object({
  visual: z.string(),
  impassable: z.boolean().optional(),
  passableAbove: z.boolean().optional(),
  metadata: metadataSchema,
  tags: tagsSchema,
});
export type ClientTileDefinition = z.infer<typeof ClientTileDefinitionSchema>;

export const ClientEntityDefinitionSchema = z.object({
  components: z.record(z.string(), z.unknown()).default({}),
  metadata: metadataSchema,
  tags: tagsSchema,
});
export type ClientEntityDefinition = z.infer<typeof ClientEntityDefinitionSchema>;

export const ClientVisualAnimationSchema = z.object({
  textures: z.array(z.string()),
  duration: z.number().finite().optional(),
  speed: z.number().finite().optional(),
  offsetX: z.number().finite().optional(),
  offsetY: z.number().finite().optional(),
  flipX: z.boolean().optional(),
  flipY: z.boolean().optional(),
});
export type ClientVisualAnimation = z.infer<typeof ClientVisualAnimationSchema>;

export const ClientVisualDefinitionSchema = z.object({
  type: z.enum(['simple', 'variants', 'animated', 'animator', 'text']),
  texture: z.string().optional(),
  textures: z.array(z.string()).optional(),
  text: z.string().optional(),
  align: z.string().optional(),
  animator: z.string().optional(),
  animations: z.record(z.string(), ClientVisualAnimationSchema).optional(),
  duration: z.number().finite().optional(),
  instanced: z.boolean().optional(),
  sortLayerOffset: z.number().finite().optional(),
  surfaceOffsetY: z.number().finite().optional(),
  offsetX: z.number().finite().optional(),
  offsetY: z.number().finite().optional(),
  flipX: z.boolean().optional(),
  flipY: z.boolean().optional(),
  metadata: metadataSchema,
}).superRefine((value, context) => {
  const requireField = (field: 'texture' | 'textures' | 'text' | 'animator' | 'animations') => {
    if (value[field] === undefined) {
      context.addIssue({ code: 'custom', path: [field], message: `${field} is required for ${value.type} visuals` });
    }
  };
  if (value.type === 'simple') requireField('texture');
  if (value.type === 'variants' || value.type === 'animated') requireField('textures');
  if (value.type === 'text') requireField('text');
  if (value.type === 'animator') {
    requireField('animator');
    requireField('animations');
  }
});
export type ClientVisualDefinition = z.infer<typeof ClientVisualDefinitionSchema>;

export const ClientSoundDefinitionSchema = z.object({
  audio: z.string(),
  metadata: metadataSchema,
  tags: tagsSchema,
});
export type ClientSoundDefinition = z.infer<typeof ClientSoundDefinitionSchema>;

export const ClientAudioDefinitionSchema = z.object({
  type: z.enum(['simple', 'music']),
  file: z.string(),
  volume: z.number().finite().optional(),
  pitch: z.number().finite().optional(),
  loop: z.boolean().optional(),
  metadata: metadataSchema,
});
export type ClientAudioDefinition = z.infer<typeof ClientAudioDefinitionSchema>;

export const ClientGridDirectionDefinitionSchema = z.object({
  name: z.string().min(1),
  x: z.number().int(),
  y: z.number().int(),
  z: z.number().int(),
  angle: z.number().finite(),
});
export type ClientGridDirectionDefinition = z.infer<typeof ClientGridDirectionDefinitionSchema>;

export const ClientGridDefinitionSchema = z.object({
  layout: z.string().optional(),
  directions: z.array(ClientGridDirectionDefinitionSchema),
  metadata: metadataSchema,
});
export type ClientGridDefinition = z.infer<typeof ClientGridDefinitionSchema>;

export const clientRegistryEntrySchemas = {
  tiles: ClientTileDefinitionSchema,
  entities: ClientEntityDefinitionSchema,
  visuals: ClientVisualDefinitionSchema,
  sounds: ClientSoundDefinitionSchema,
  audio: ClientAudioDefinitionSchema,
  grids: ClientGridDefinitionSchema,
} satisfies Record<string, z.ZodType>;

export type ClientRegistryEntryMap = {
  [K in keyof typeof clientRegistryEntrySchemas]: z.infer<(typeof clientRegistryEntrySchemas)[K]>;
};
export type EngineRegistryName = keyof ClientRegistryEntryMap;

export function validateRegistryEntries(registryName: string, value: unknown): Record<string, unknown> {
  const entries = z.record(z.string(), z.unknown()).parse(value);
  const engineName = registryName.replace(/^selene:/, '');
  if (!isEngineRegistryName(engineName)) return entries;

  try {
    return z.record(z.string(), clientRegistryEntrySchemas[engineName]).parse(entries);
  } catch (error) {
    if (error instanceof z.ZodError) {
      throw new Error(`Invalid client registry data in ${registryName}:\n${z.prettifyError(error)}`);
    }
    throw error;
  }
}

function isEngineRegistryName(value: string): value is EngineRegistryName {
  return Object.prototype.hasOwnProperty.call(clientRegistryEntrySchemas, value);
}
