import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';
import ts from 'typescript';
import { Sprite, Texture } from 'pixi.js';

function moduleUrl(path, replacements = {}) {
  const source = readFileSync(new URL(path, import.meta.url), 'utf8');
  let js = ts.transpileModule(source, { compilerOptions: {
    module: ts.ModuleKind.ESNext, target: ts.ScriptTarget.ES2022,
  } }).outputText;
  for (const [from, to] of Object.entries(replacements)) js = js.replaceAll(from, to);
  return `data:text/javascript;base64,${Buffer.from(js).toString('base64')}`;
}
const meshUrl = moduleUrl('../src/renderer/tiles/SharedLightMesh.ts');
const { buildSharedLightMesh } = await import(meshUrl);
const { LitTileSprite, LitTileBatcher, LitSpritePipe } = await import(moduleUrl('../src/renderer/tiles/LitTileSprite.ts', {
  "'pixi.js'": JSON.stringify(import.meta.resolve('pixi.js')),
  "'./SharedLightMesh'": JSON.stringify(meshUrl),
  "import { TILE_STEP_X, TILE_STEP_Y } from '@/core/RenderGrid';": 'const TILE_STEP_X = 38, TILE_STEP_Y = 19;',
  "import { projectCoordinate } from '../IsoProjection';": 'const projectCoordinate = ({x,y,z}) => ({x:(x+y)*38,y:-(x-y)*19-z*114});',
}));

function interpolate(mesh, x, y) {
  for (let i = 0; i < mesh.length; i += 3) {
    const [a,b,c] = mesh.slice(i, i+3);
    const determinant = (b.y-c.y)*(a.x-c.x)+(c.x-b.x)*(a.y-c.y);
    const u = ((b.y-c.y)*(x-c.x)+(c.x-b.x)*(y-c.y))/determinant;
    const v = ((c.y-a.y)*(x-c.x)+(a.x-c.x)*(y-c.y))/determinant;
    if (u >= -1e-8 && v >= -1e-8 && u+v <= 1+1e-8) {
      return u*a.red+v*b.red+(1-u-v)*c.red;
    }
  }
  assert.fail(`point ${x},${y} missing from mesh`);
}

test('overlapping artwork shares identical lighting even with different bounds', () => {
  const sample = (x,y) => ({red: Math.exp(-(x*x+y*y)/2000),green:0.5,blue:0.2});
  const a = buildSharedLightMesh({minX:-38,minY:-38,maxX:38,maxY:0},38,19,sample);
  const b = buildSharedLightMesh({minX:-21,minY:-25,maxX:70,maxY:19},38,19,sample);
  for (const [x,y] of [[0,-19],[15,-12],[-20,-24],[30,-1],[5,-22]]) {
    assert.ok(Math.abs(interpolate(a,x,y)-interpolate(b,x,y)) < 1e-10);
  }
});

test('clipped lighting mesh exactly covers artwork without overlapping triangle area', () => {
  const bounds = {minX:-29.5,minY:-33,maxX:42.25,maxY:5};
  const mesh = buildSharedLightMesh(bounds,38,19,()=>({red:1,green:1,blue:1}));
  let area = 0;
  for (let i=0;i<mesh.length;i+=3) {
    const [a,b,c]=mesh.slice(i,i+3);
    area += Math.abs((b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x))/2;
  }
  assert.ok(Math.abs(area-(bounds.maxX-bounds.minX)*(bounds.maxY-bounds.minY))<1e-8);
});

function renderElements(sprites) {
  const elements = [];
  const pipe = new LitSpritePipe({ uid: 917, roundPixels: false,
    renderPipes: { batch: { addToBatch: element => elements.push(element) } },
  });
  for (const sprite of sprites) pipe.addRenderable(sprite, {});
  return {pipe,elements};
}

test('smooth tiles use cached mesh lighting in the same batch as ordinary sprites', () => {
  const tile = new LitTileSprite(Texture.WHITE);
  const entity = new Sprite(Texture.WHITE);
  let samples=0;
  const environment={ getRevision:()=>0, getColor:()=>{ samples++; return {red:0.5,green:0.5,blue:0.5}; } };
  const coordinate={x:0,y:0,z:0};
  tile.setSmoothLighting(environment,coordinate,0);
  const {pipe,elements:[element,ordinary]}=renderElements([tile,entity]);
  assert.equal(element.batcherName,ordinary.batcherName);
  assert.equal(element.packAsQuad,false);
  assert.equal(ordinary.packAsQuad,true);
  assert.ok(!tile.filters?.length);
  const count=samples;
  pipe.addRenderable(tile,{});
  assert.equal(samples,count);
  const buffer = new ArrayBuffer(element.attributeSize*6*4);
  LitTileBatcher.prototype.packAttributes.call({}, element,new Float32Array(buffer),new Uint32Array(buffer),0,3);
  assert.equal(new Uint32Array(buffer)[4],0xff808080);
  tile.setSmoothLighting(null,coordinate,0);
  pipe.addRenderable(tile,{});
  assert.equal(element.packAsQuad,true);
  assert.equal(element.attributeSize,4);
  pipe.destroy(); tile.destroy(); entity.destroy();
});


test('moving lights update colors in place without reallocating geometry or indices', () => {
  const tile = new LitTileSprite(Texture.WHITE);
  const coordinate = {x:0,y:0,z:0};
  let revision = 0, brightness = 0.2, samples = 0;
  const environment = {
    getRevision: () => revision,
    getColor: () => { samples++; return { red:brightness, green:brightness, blue:brightness }; },
  };
  const bounds = {minX:-38,maxX:38,minY:-37,maxY:0};
  tile.setSmoothLighting(environment,coordinate,0);
  const mesh = tile.getLightMesh(bounds);
  const positions = mesh.positions, indices = mesh.indices, uvs = mesh.uvs, colors = mesh.colors;
  assert.ok(positions.length / 2 < indices.length, 'triangles share vertices');
  const dark = colors[0];
  brightness = 0.8; revision++;
  tile.setSmoothLighting(environment,coordinate,0);
  const updated = tile.getLightMesh(bounds);
  assert.equal(updated,mesh);
  assert.equal(updated.positions,positions);
  assert.equal(updated.indices,indices);
  assert.equal(updated.uvs,uvs);
  assert.equal(updated.colors,colors);
  assert.notEqual(colors[0],dark);
  const count = samples;
  tile.tint = 0xeeeeee;
  tile.getLightMesh(bounds);
  assert.equal(samples,count,'ordinary view updates do not resample lights');
  assert.equal(tile.getLightMesh(bounds).positions,positions);
  assert.notEqual(tile.getLightMesh({...bounds,maxX:45}).positions,positions,'artwork bounds rebuild geometry');
  tile.destroy();
});

const { PixiTilemapLayer } = await import(moduleUrl('../src/renderer/tiles/PixiTilemapLayer.ts', {
  "import { chunkTiming, chunkTimingEnabled, reportChunkWork } from '@/core/ChunkTiming';": '',
  "import { TILE_STEP_X, TILE_STEP_Y } from '@/core/RenderGrid';": 'const TILE_STEP_X=38,TILE_STEP_Y=19;',
  "import { getRenderOrder, projectCoordinate } from '../IsoProjection';": 'const projectCoordinate=({x,y,z})=>({x:(x+y)*38,y:-(x-y)*19-z*114});',
  ...Object.fromEntries(['PixiTileStackRenderer','TileCuller','TileOcclusionFader','TileSpatialIndex','LitTileSprite'].map(name=>[
    `import { ${name} } from './${name}';`, '',
  ])),
  "import { FrameWorkQueue, frameWorkBudget } from './FrameWorkQueue';": '',
}));

test('local light changes query overlapping artwork, deduplicate regions, and skip other floors', () => {
  const layer = Object.create(PixiTilemapLayer.prototype);
  const near = {coordinate:{x:0,y:0,z:0}};
  const overhang = {coordinate:{x:20,y:0,z:0}};
  const otherFloor = {coordinate:{x:0,y:0,z:1}};
  const updated = [], queries = [];
  Object.assign(layer, {
    lighting: {}, smoothLighting: true, minLightSurfaceOffset:0, maxLightSurfaceOffset:200,
    renderedStacks: { values:()=>assert.fail('local lights must not scan all loaded tiles') },
    spatialIndex: { query: bounds=>{ queries.push(bounds); return new Set([near,overhang,otherFloor]); } },
    tintStack: stack=>updated.push(stack),
  });
  layer.updateLighting([{x:0,y:0,z:0},{x:16,y:0,z:0}]);
  assert.deepEqual(updated,[near,overhang]);
  assert.equal(queries.length,2);
  assert.ok(queries[0].y < -200,'query includes raised artwork');
});
