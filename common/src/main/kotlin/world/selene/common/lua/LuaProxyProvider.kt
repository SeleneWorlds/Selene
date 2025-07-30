package world.selene.common.lua

interface LuaProxyProvider<T> {
    val luaProxy: T
}