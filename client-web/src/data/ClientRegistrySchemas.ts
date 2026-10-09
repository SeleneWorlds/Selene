import { z } from 'zod';

export const RegistryMetadataSchema = z.record(z.string(), z.unknown());
export type RegistryMetadata = z.infer<typeof RegistryMetadataSchema>;

const tagsSchema = z.array(z.string()).default([]);
const metadataSchema = RegistryMetadataSchema.default({});

const tileLightSchema = z.object({
  radius: z.number().finite().positive(),
  intensity: z.number().finite().nonnegative().default(1),
  red: z.number().finite().default(1),
  green: z.number().finite().default(1),
  blue: z.number().finite().default(1),
});

export const ClientTileDefinitionSchema = z.object({
  visual: z.string(),
  impassable: z.boolean().optional(),
  passableAbove: z.boolean().optional(),
  light: tileLightSchema.nullish(),
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
  occlusionFade: z.boolean().default(true),
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

const rangeSchema = z.object({
  min: z.number().finite().positive(),
  max: z.number().finite().positive(),
}).refine((value) => value.max >= value.min, { message: 'max must be greater than or equal to min' });

const transitionSchema = z.object({
  start: z.number().finite().nonnegative(),
  end: z.number().finite().nonnegative(),
}).default({ start: 1, end: 1 });

const alphaTransitionSchema = z.object({
  start: z.number().finite().min(0).max(1),
  end: z.number().finite().min(0).max(1),
}).default({ start: 1, end: 1 });

const colorTransitionSchema = z.object({
  start: z.string().regex(/^#[0-9a-fA-F]{6}$/),
  end: z.string().regex(/^#[0-9a-fA-F]{6}$/),
}).default({ start: '#ffffff', end: '#ffffff' });

const particleSpawnSchema = z.discriminatedUnion('type', [
  z.object({ type: z.literal('point') }),
  z.object({
    type: z.literal('rectangle'),
    width: z.number().finite().nonnegative(),
    height: z.number().finite().nonnegative(),
  }),
]);

/** Portable subset supported by PixiJS 8 ParticleContainer and libGDX ParticleEmitter. */
export const ClientParticleSystemDefinitionSchema = z.object({
  texture: z.string().min(1),
  lifetime: rangeSchema,
  frequency: z.number().finite().positive(),
  emitterLifetime: z.number().finite().positive().nullable().default(null),
  maxParticles: z.number().int().positive().default(100),
  speed: transitionSchema,
  speedMinimumMultiplier: z.number().finite().min(0).max(1).default(1),
  scale: transitionSchema,
  scaleMinimumMultiplier: z.number().finite().min(0).max(1).default(1),
  alpha: alphaTransitionSchema,
  color: colorTransitionSchema,
  angle: z.object({
    min: z.number().finite(),
    max: z.number().finite(),
  }).refine((value) => value.max >= value.min, {
    message: 'max must be greater than or equal to min',
  }).default({ min: 0, max: 0 }),
  rotation: z.object({
    min: z.number().finite(),
    max: z.number().finite(),
  }).refine((value) => value.max >= value.min, {
    message: 'max must be greater than or equal to min',
  }).default({ min: 0, max: 0 }),
  spawn: particleSpawnSchema.default({ type: 'point' }),
  additive: z.boolean().default(false),
  metadata: metadataSchema,
});
export type ClientParticleSystemDefinition = z.infer<typeof ClientParticleSystemDefinitionSchema>;

const timelineKeyframeSchema = z.object({
  time: z.number().finite().nonnegative(),
  value: z.union([z.number().finite(), z.string(), z.boolean()]),
  interpolation: z.enum(['linear', 'step']).default('linear'),
});

const timelineKeysSchema = z.record(z.string(), z.array(timelineKeyframeSchema)).default({});

export const VisualAnimationTimelineEventSchema = z.object({
  type: z.literal('visual_animation'),
  time: z.number().finite().nonnegative().default(0),
  visual: z.string().min(1),
  duration: z.number().finite().positive().optional(),
  position: z.string().min(1).default('position'),
  ignoresElevation: z.boolean().default(false),
  keys: timelineKeysSchema,
});
export type VisualAnimationTimelineEvent = z.infer<typeof VisualAnimationTimelineEventSchema>;

export const ParticleSystemTimelineEventSchema = z.object({
  type: z.literal('particle_system'),
  time: z.number().finite().nonnegative().default(0),
  particle: z.string().min(1),
  space: z.enum(['world', 'screen']).default('world'),
  position: z.string().min(1).default('position'),
  emissionRateMultiplier: z.string().min(1).optional(),
  keys: timelineKeysSchema,
});
export type ParticleSystemTimelineEvent = z.infer<typeof ParticleSystemTimelineEventSchema>;

export const SoundTimelineEventSchema = z.object({
  type: z.literal('sound'),
  time: z.number().finite().nonnegative().default(0),
  sound: z.string().min(1),
  volume: z.number().finite().min(0).max(1).default(1),
  pitch: z.number().finite().positive().default(1),
  keys: timelineKeysSchema,
});
export type SoundTimelineEvent = z.infer<typeof SoundTimelineEventSchema>;

export const ScreenOverlayTimelineEventSchema = z.object({
  type: z.literal('screen_overlay'),
  time: z.number().finite().nonnegative().default(0),
  duration: z.number().finite().positive().optional(),
  texture: z.string().min(1).optional(),
  alphaMultiplier: z.string().min(1).optional(),
  color: z.string().regex(/^#[0-9a-fA-F]{6}([0-9a-fA-F]{2})?$/).default('#ffffff'),
  alpha: z.number().min(0).max(1).default(1),
  keys: timelineKeysSchema,
});
export type ScreenOverlayTimelineEvent = z.infer<typeof ScreenOverlayTimelineEventSchema>;

export const ClientTimelineDefinitionSchema = z.object({
  events: z.array(z.discriminatedUnion('type', [
    VisualAnimationTimelineEventSchema,
    ParticleSystemTimelineEventSchema,
    SoundTimelineEventSchema,
    ScreenOverlayTimelineEventSchema,
  ])),
  metadata: metadataSchema,
});
export type ClientTimelineDefinition = z.infer<typeof ClientTimelineDefinitionSchema>;

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
  particles: ClientParticleSystemDefinitionSchema,
  grids: ClientGridDefinitionSchema,
  timelines: ClientTimelineDefinitionSchema,
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
