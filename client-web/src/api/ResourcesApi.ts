export interface ResourcesApi {
  listFiles(bundle: string, filter: string): string[];
  loadAsString(path: string): Promise<string>;
  fileExists(path: string): boolean;
}
