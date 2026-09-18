const textEncoder = new TextEncoder();
const textDecoder = new TextDecoder();

const PacketId = {
  Authenticate: 1,
  NameIdMappings: 2,
  MapChunk: 3,
  Entity: 4,
  SetCameraPosition: 5,
  SetCameraFollowEntity: 6,
  SetControlledEntity: 7,
  RequestMove: 8,
  MoveEntity: 9,
  RemoveEntity: 10,
  RemoveMapChunk: 11,
  UpdateMapTiles: 14,
  Preferences: 15,
  FinalizeJoin: 16,
  TurnEntity: 17,
  RequestFacing: 19,
  SetActiveGrid: 21,
  CustomPayload: 254,
} as const;

export type GamePacket =
  | NameIdMappingsPacket
  | MapChunkPacket
  | EntityPacket
  | SetCameraPositionPacket
  | SetCameraFollowEntityPacket
  | SetControlledEntityPacket
  | MoveEntityPacket
  | RemoveEntityPacket
  | RemoveMapChunkPacket
  | UpdateMapTilesPacket
  | TurnEntityPacket
  | SetActiveGridPacket
  | CustomPayloadPacket
  | UnknownGamePacket;

export interface Coordinate {
  x: number;
  y: number;
  z: number;
}

export interface NameIdMappingsPacket {
  type: 'nameIdMappings';
  scope: string;
  mappings: ReadonlyArray<readonly [string, number]>;
}

export interface UnknownGamePacket {
  type: 'unknown';
  packetId: number;
}

export interface EntityPacket {
  type: 'entity';
  networkId: number;
  entityId: number;
  coordinate: Coordinate;
  facing: number;
  components: Record<string, string>;
}

export interface AdditionalTileEntry {
  coordinate: Coordinate;
  tileId: number;
}

export interface MapChunkPacket {
  type: 'mapChunk';
  x: number;
  y: number;
  z: number;
  width: number;
  height: number;
  baseTiles: readonly number[];
  additionalTiles: readonly AdditionalTileEntry[];
}

export interface SetCameraPositionPacket {
  type: 'setCameraPosition';
  coordinate: Coordinate;
}

export interface SetCameraFollowEntityPacket {
  type: 'setCameraFollowEntity';
  networkId: number;
}

export interface SetControlledEntityPacket {
  type: 'setControlledEntity';
  networkId: number;
}

export interface MoveEntityPacket {
  type: 'moveEntity';
  networkId: number;
  start: Coordinate;
  end: Coordinate;
  facing: number;
  duration: number;
}

export interface RemoveEntityPacket {
  type: 'removeEntity';
  networkId: number;
}

export interface RemoveMapChunkPacket {
  type: 'removeMapChunk';
  x: number;
  y: number;
  z: number;
  width: number;
  height: number;
}

export interface UpdateMapTilesPacket {
  type: 'updateMapTiles';
  coordinate: Coordinate;
  baseTileId: number;
  additionalTileIds: readonly number[];
}

export interface TurnEntityPacket {
  type: 'turnEntity';
  networkId: number;
  facing: number;
}

export interface SetActiveGridPacket {
  type: 'setActiveGrid';
  identifier: string;
}

export interface CustomPayloadPacket {
  type: 'customPayload';
  payloadId: string;
  payload: string;
}

export function encodeAuthenticatePacket(token: string): ArrayBuffer {
  return encodePacket(PacketId.Authenticate, (writer) => writer.writeString(token));
}

export function encodePreferencesPacket(locale: string): ArrayBuffer {
  return encodePacket(PacketId.Preferences, (writer) => writer.writeString(locale));
}

export function encodeFinalizeJoinPacket(): ArrayBuffer {
  return encodePacket(PacketId.FinalizeJoin);
}

export function encodeRequestMovePacket(coordinate: Coordinate): ArrayBuffer {
  return encodePacket(PacketId.RequestMove, (writer) => writer.writeCoordinate(coordinate));
}

export function encodeRequestFacingPacket(angle: number): ArrayBuffer {
  return encodePacket(PacketId.RequestFacing, (writer) => writer.writeFloat(angle));
}

export function encodeCustomPayloadPacket(payloadId: string, payload: string): ArrayBuffer {
  return encodePacket(PacketId.CustomPayload, (writer) => {
    writer.writeString(payloadId);
    writer.writeString(payload);
  });
}

export function decodeGamePacket(data: ArrayBuffer): GamePacket {
  const reader = new PacketReader(data);
  const packetId = reader.readUnsignedByte();

  switch (packetId) {
    case PacketId.NameIdMappings:
      return decodeNameIdMappingsPacket(reader);
    case PacketId.MapChunk:
      return decodeMapChunkPacket(reader);
    case PacketId.Entity:
      return decodeEntityPacket(reader);
    case PacketId.SetCameraPosition:
      return decodeSetCameraPositionPacket(reader);
    case PacketId.SetCameraFollowEntity:
      return decodeSetCameraFollowEntityPacket(reader);
    case PacketId.SetControlledEntity:
      return decodeSetControlledEntityPacket(reader);
    case PacketId.MoveEntity:
      return decodeMoveEntityPacket(reader);
    case PacketId.RemoveEntity:
      return decodeRemoveEntityPacket(reader);
    case PacketId.RemoveMapChunk:
      return decodeRemoveMapChunkPacket(reader);
    case PacketId.UpdateMapTiles:
      return decodeUpdateMapTilesPacket(reader);
    case PacketId.TurnEntity:
      return decodeTurnEntityPacket(reader);
    case PacketId.SetActiveGrid:
      return decodeSetActiveGridPacket(reader);
    case PacketId.CustomPayload:
      return decodeCustomPayloadPacket(reader);
    default:
      return { type: 'unknown', packetId };
  }
}

function encodePacket(packetId: number, writeBody?: (writer: PacketWriter) => void): ArrayBuffer {
  const writer = new PacketWriter();
  writer.writeUnsignedByte(packetId);
  writeBody?.(writer);
  return writer.toArrayBuffer();
}

function decodeNameIdMappingsPacket(reader: PacketReader): NameIdMappingsPacket {
  const scope = reader.readString();
  const entryCount = reader.readInt();
  if (entryCount < 0) {
    throw new Error(`Invalid Selene id mappings entry count: ${entryCount}.`);
  }

  const mappings: Array<readonly [string, number]> = [];

  for (let index = 0; index < entryCount; index += 1) {
    mappings.push([reader.readString(), reader.readInt()]);
  }

  reader.assertFullyRead();
  return { type: 'nameIdMappings', scope, mappings };
}

function decodeMapChunkPacket(reader: PacketReader): MapChunkPacket {
  const x = reader.readInt();
  const y = reader.readInt();
  const z = reader.readInt();
  const width = reader.readInt();
  const height = reader.readInt();
  if (width < 0 || height < 0) {
    throw new Error(`Invalid Selene map chunk size: ${width}x${height}.`);
  }

  const tileCount = width * height;
  const baseTiles: number[] = [];
  for (let index = 0; index < tileCount; index += 1) {
    baseTiles.push(reader.readInt());
  }

  const additionalTileCount = reader.readShort();
  if (additionalTileCount < 0) {
    throw new Error(`Invalid Selene map chunk additional tile count: ${additionalTileCount}.`);
  }

  const additionalTiles: AdditionalTileEntry[] = [];
  for (let index = 0; index < additionalTileCount; index += 1) {
    additionalTiles.push({
      coordinate: reader.readRelativeCoordinate(x, y),
      tileId: reader.readInt(),
    });
  }

  reader.assertFullyRead();
  return { type: 'mapChunk', x, y, z, width, height, baseTiles, additionalTiles };
}

function decodeEntityPacket(reader: PacketReader): EntityPacket {
  const networkId = reader.readInt();
  const entityId = reader.readInt();
  const coordinate = reader.readCoordinate();
  const facing = reader.readFloat();
  const componentCount = reader.readInt();
  if (componentCount < 0) {
    throw new Error(`Invalid Selene entity component count: ${componentCount}.`);
  }

  const components: Record<string, string> = {};
  for (let index = 0; index < componentCount; index += 1) {
    components[reader.readString()] = reader.readString();
  }

  reader.assertFullyRead();
  return { type: 'entity', networkId, entityId, coordinate, facing, components };
}

function decodeSetCameraPositionPacket(reader: PacketReader): SetCameraPositionPacket {
  const coordinate = reader.readCoordinate();

  reader.assertFullyRead();
  return { type: 'setCameraPosition', coordinate };
}

function decodeSetCameraFollowEntityPacket(reader: PacketReader): SetCameraFollowEntityPacket {
  const networkId = reader.readInt();

  reader.assertFullyRead();
  return { type: 'setCameraFollowEntity', networkId };
}

function decodeSetControlledEntityPacket(reader: PacketReader): SetControlledEntityPacket {
  const networkId = reader.readInt();

  reader.assertFullyRead();
  return { type: 'setControlledEntity', networkId };
}

function decodeMoveEntityPacket(reader: PacketReader): MoveEntityPacket {
  const networkId = reader.readInt();
  const start = reader.readCoordinate();
  const end = reader.readCoordinate();
  const facing = reader.readFloat();
  const duration = reader.readFloat();

  reader.assertFullyRead();
  return { type: 'moveEntity', networkId, start, end, facing, duration };
}

function decodeRemoveEntityPacket(reader: PacketReader): RemoveEntityPacket {
  const networkId = reader.readInt();

  reader.assertFullyRead();
  return { type: 'removeEntity', networkId };
}

function decodeRemoveMapChunkPacket(reader: PacketReader): RemoveMapChunkPacket {
  const x = reader.readInt();
  const y = reader.readInt();
  const z = reader.readInt();
  const width = reader.readInt();
  const height = reader.readInt();

  reader.assertFullyRead();
  return { type: 'removeMapChunk', x, y, z, width, height };
}

function decodeUpdateMapTilesPacket(reader: PacketReader): UpdateMapTilesPacket {
  const coordinate = reader.readCoordinate();
  const baseTileId = reader.readInt();
  const additionalTileCount = reader.readInt();
  if (additionalTileCount < 0) {
    throw new Error(`Invalid Selene update map tiles additional tile count: ${additionalTileCount}.`);
  }

  const additionalTileIds: number[] = [];
  for (let index = 0; index < additionalTileCount; index += 1) {
    additionalTileIds.push(reader.readInt());
  }

  reader.assertFullyRead();
  return { type: 'updateMapTiles', coordinate, baseTileId, additionalTileIds };
}

function decodeTurnEntityPacket(reader: PacketReader): TurnEntityPacket {
  const networkId = reader.readInt();
  const facing = reader.readFloat();

  reader.assertFullyRead();
  return { type: 'turnEntity', networkId, facing };
}

function decodeSetActiveGridPacket(reader: PacketReader): SetActiveGridPacket {
  const identifier = reader.readString();

  reader.assertFullyRead();
  return { type: 'setActiveGrid', identifier };
}

function decodeCustomPayloadPacket(reader: PacketReader): CustomPayloadPacket {
  const payloadId = reader.readString();
  const payload = reader.readString();

  reader.assertFullyRead();
  return { type: 'customPayload', payloadId, payload };
}

class PacketWriter {
  private readonly bytes: number[] = [];

  writeUnsignedByte(value: number): void {
    this.bytes.push(value & 0xff);
  }

  writeShort(value: number): void {
    this.bytes.push((value >>> 8) & 0xff, value & 0xff);
  }

  writeInt(value: number): void {
    this.bytes.push((value >>> 24) & 0xff, (value >>> 16) & 0xff, (value >>> 8) & 0xff, value & 0xff);
  }

  writeFloat(value: number): void {
    const buffer = new ArrayBuffer(4);
    new DataView(buffer).setFloat32(0, value);
    this.bytes.push(...new Uint8Array(buffer));
  }

  writeCoordinate(coordinate: Coordinate): void {
    this.writeInt(coordinate.x);
    this.writeInt(coordinate.y);
    this.writeInt(coordinate.z);
  }

  writeString(value: string): void {
    const encoded = textEncoder.encode(value);

    if (encoded.byteLength > 0x7fff) {
      throw new Error(`String is too long for Selene packet encoding: ${encoded.byteLength} bytes.`);
    }

    this.writeShort(encoded.byteLength);
    this.bytes.push(...encoded);
  }

  toArrayBuffer(): ArrayBuffer {
    return new Uint8Array(this.bytes).buffer;
  }
}

class PacketReader {
  private readonly view: DataView;
  private offset = 0;

  constructor(data: ArrayBuffer) {
    this.view = new DataView(data);
  }

  readUnsignedByte(): number {
    this.assertReadable(1);
    const value = this.view.getUint8(this.offset);
    this.offset += 1;
    return value;
  }

  readByte(): number {
    this.assertReadable(1);
    const value = this.view.getInt8(this.offset);
    this.offset += 1;
    return value;
  }

  readShort(): number {
    this.assertReadable(2);
    const value = this.view.getInt16(this.offset);
    this.offset += 2;
    return value;
  }

  readInt(): number {
    this.assertReadable(4);
    const value = this.view.getInt32(this.offset);
    this.offset += 4;
    return value;
  }

  readFloat(): number {
    this.assertReadable(4);
    const value = this.view.getFloat32(this.offset);
    this.offset += 4;
    return value;
  }

  readCoordinate(): Coordinate {
    return {
      x: this.readInt(),
      y: this.readInt(),
      z: this.readInt(),
    };
  }

  readRelativeCoordinate(baseX: number, baseY: number): Coordinate {
    return {
      x: baseX + this.readByte(),
      y: baseY + this.readByte(),
      z: this.readInt(),
    };
  }

  readString(): string {
    const length = this.readShort();
    if (length < 0) {
      throw new Error(`Invalid Selene packet string length: ${length}.`);
    }

    this.assertReadable(length);
    const bytes = new Uint8Array(this.view.buffer, this.view.byteOffset + this.offset, length);
    this.offset += length;
    return textDecoder.decode(bytes);
  }

  assertFullyRead(): void {
    if (this.offset !== this.view.byteLength) {
      throw new Error(`Selene packet has ${this.view.byteLength - this.offset} unread bytes.`);
    }
  }

  private assertReadable(byteLength: number): void {
    if (this.offset + byteLength > this.view.byteLength) {
      throw new Error('Selene packet ended before it could be decoded.');
    }
  }
}
