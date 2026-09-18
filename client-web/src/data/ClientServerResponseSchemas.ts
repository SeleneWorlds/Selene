import { z } from 'zod';

const nonEmptyString = z.string().min(1);
const serverPath = z.string().refine(
  (value) => value.startsWith('/') && !value.startsWith('//'),
  'Expected a server-relative path beginning with a single slash.',
);

export const ClientAssetManifestSchema = z.object({
  assets: z.record(z.string(), serverPath),
});

const ClientLuaEntrypointIndexEntrySchema = z.object({
  bundle: nonEmptyString,
  path: nonEmptyString,
  module: nonEmptyString,
  url: serverPath,
});

const ClientLuaModuleIndexEntrySchema = z.object({
  hash: nonEmptyString,
  url: serverPath,
});

export const ClientLuaIndexResponseSchema = z.object({
  hash: nonEmptyString,
  entrypoints: z.array(ClientLuaEntrypointIndexEntrySchema),
  modules: z.record(z.string().min(1), ClientLuaModuleIndexEntrySchema),
}).superRefine((index, context) => {
  index.entrypoints.forEach((entrypoint, entrypointIndex) => {
    if (!(entrypoint.module in index.modules)) {
      context.addIssue({
        code: 'custom',
        path: ['entrypoints', entrypointIndex, 'module'],
        message: `Entrypoint references missing module ${entrypoint.module}.`,
      });
    }
  });
});

export const ClientLuaModuleSourceResponseSchema = z.object({
  module: nonEmptyString,
  bundle: nonEmptyString,
  path: nonEmptyString,
  hash: nonEmptyString,
  source: z.string(),
});

export const ClientUiIndexResponseSchema = z.object({
  entrypoints: z.array(z.object({
    bundle: nonEmptyString,
    id: nonEmptyString,
    url: serverPath,
  })),
});

export type ClientAssetManifestResponse = z.infer<typeof ClientAssetManifestSchema>;
export type ClientLuaIndexResponse = z.infer<typeof ClientLuaIndexResponseSchema>;
export type ClientLuaModuleSourceResponse = z.infer<typeof ClientLuaModuleSourceResponseSchema>;
export type ClientUiIndexResponse = z.infer<typeof ClientUiIndexResponseSchema>;
export type ClientUiEntrypoint = ClientUiIndexResponse['entrypoints'][number];

export function parseServerResponse<T>(schema: z.ZodType<T>, value: unknown, label: string): T {
  const result = schema.safeParse(value);
  if (result.success) return result.data;

  const issue = result.error.issues[0];
  const location = issue?.path.length ? ` at ${issue.path.join('.')}` : '';
  throw new Error(`Invalid ${label}${location}: ${issue?.message ?? 'response did not match the expected schema'}`);
}
