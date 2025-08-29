local bit32 = {}

local function trim(x) return (x & 0xffffffff) end
local function norm(x) return trim(math.tointeger(x) or 0) end

local rshift -- forward decl (used by lshift)
local function lshift(x, d)
  x = norm(x); d = math.tointeger(d) or 0
  if d <= -32 or d >= 32 then return 0 end
  if d < 0 then return rshift(x, -d) end
  return trim(x << d)
end

rshift = function(x, d)
  x = norm(x); d = math.tointeger(d) or 0
  if d <= -32 or d >= 32 then return 0 end
  if d < 0 then return lshift(x, -d) end
  -- logical right shift (zero-fill) via masking to keep sign bit clear in 64-bit space
  return (x >> d) & 0xffffffff
end

local function arshift(x, d)
  x = norm(x); d = math.tointeger(d) or 0
  if d <= -32 or d >= 32 then
    -- arithmetic -> all ones if sign bit set, else zero
    return (x & 0x80000000 ~= 0) and 0xffffffff or 0
  end
  if d < 0 then return lshift(x, -d) end
  local s = x >> d
  if (x & 0x80000000) ~= 0 then
    local fill = (~((1 << (32 - d)) - 1)) & 0xffffffff
    return (s | fill) & 0xffffffff
  else
    return s & 0xffffffff
  end
end

local function lrotate(x, d)
  x = norm(x); d = math.tointeger(d) or 0
  d = d % 32
  if d == 0 then return x end
  return trim((x << d) | ((x >> (32 - d)) & ((1 << d) - 1)))
end

local function rrotate(x, d)
  x = norm(x); d = math.tointeger(d) or 0
  d = d % 32
  if d == 0 then return x end
  return trim(((x >> d) | ((x << (32 - d)) & 0xffffffff)))
end

local function band(a, ...)
  a = norm(a)
  if select("#", ...) == 0 then return a end
  for i = 1, select("#", ...) do a = a & norm(select(i, ...)) end
  return trim(a)
end

local function bor(a, ...)
  a = norm(a)
  if select("#", ...) == 0 then return a end
  for i = 1, select("#", ...) do a = a | norm(select(i, ...)) end
  return trim(a)
end

local function bxor(a, ...)
  a = norm(a)
  if select("#", ...) == 0 then return a end
  for i = 1, select("#", ...) do a = a ~ norm(select(i, ...)) end
  return trim(a)
end

local function bnot(x) return trim(~norm(x)) end

local function btest(...)
  return band(...) ~= 0
end

local function extract(n, field, width)
  n = norm(n); field = math.tointeger(field) or 0
  width = math.tointeger(width or 1) or 1
  -- (Lua 5.2) field in [0,31], width in [1, 32-field]
  local mask = ((1 << width) - 1) & 0xffffffff
  return band(rshift(n, field), mask)
end

local function replace(n, v, field, width)
  n = norm(n); v = norm(v); field = math.tointeger(field) or 0
  width = math.tointeger(width or 1) or 1
  local mask = ((1 << width) - 1) & 0xffffffff
  local shifted = (mask << field) & 0xffffffff
  n = band(n, bnot(shifted))
  v = band(v, mask)
  return bor(n, lshift(v, field))
end

bit32.band, bit32.bor, bit32.bxor, bit32.bnot = band, bor, bxor, bnot
bit32.lshift, bit32.rshift, bit32.arshift     = lshift, rshift, arshift
bit32.lrotate, bit32.rrotate                  = lrotate, rrotate
bit32.extract, bit32.replace, bit32.btest     = extract, replace, btest

return bit32
