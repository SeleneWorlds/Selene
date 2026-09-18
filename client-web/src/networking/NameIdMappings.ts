export interface NameIdMappings {
  getId: (scope: string, name: string) => number | undefined;
  getName: (scope: string, id: number) => string | undefined;
  isScopeComplete: (scope: string) => boolean;
  snapshot: (scope: string) => ReadonlyMap<string, number>;
  scopes: () => readonly string[];
}

export class MutableNameIdMappings implements NameIdMappings {
  private readonly idsByNameByScope = new Map<string, Map<string, number>>();
  private readonly namesByIdByScope = new Map<string, Map<number, string>>();
  private readonly completedScopes = new Set<string>();

  getId(scope: string, name: string): number | undefined {
    return this.idsByNameByScope.get(scope)?.get(name);
  }

  getName(scope: string, id: number): string | undefined {
    return this.namesByIdByScope.get(scope)?.get(id);
  }

  isScopeComplete(scope: string): boolean {
    return this.completedScopes.has(scope);
  }

  snapshot(scope: string): ReadonlyMap<string, number> {
    return new Map(this.idsByNameByScope.get(scope));
  }

  scopes(): readonly string[] {
    return [...new Set([...this.idsByNameByScope.keys(), ...this.completedScopes])];
  }

  applyPacket(scope: string, mappings: ReadonlyArray<readonly [string, number]>): void {
    if (mappings.length === 0) {
      this.completedScopes.add(scope);
      return;
    }

    const idsByName = this.getOrCreateIdsByName(scope);
    const namesById = this.getOrCreateNamesById(scope);

    for (const [name, id] of mappings) {
      idsByName.set(name, id);
      namesById.set(id, name);
    }
  }

  clear(): void {
    this.idsByNameByScope.clear();
    this.namesByIdByScope.clear();
    this.completedScopes.clear();
  }

  private getOrCreateIdsByName(scope: string): Map<string, number> {
    let idsByName = this.idsByNameByScope.get(scope);
    if (!idsByName) {
      idsByName = new Map();
      this.idsByNameByScope.set(scope, idsByName);
    }
    return idsByName;
  }

  private getOrCreateNamesById(scope: string): Map<number, string> {
    let namesById = this.namesByIdByScope.get(scope);
    if (!namesById) {
      namesById = new Map();
      this.namesByIdByScope.set(scope, namesById);
    }
    return namesById;
  }
}
