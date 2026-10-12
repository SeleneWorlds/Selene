package com.seleneworlds.server.benchmark

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.server.ServerApi
import com.seleneworlds.server.ServerLuaApi
import com.seleneworlds.server.config.ServerConfig
import party.iroiro.luajava.lua54.Lua54
import party.iroiro.luajava.Lua
import java.io.File
import java.lang.management.ManagementFactory
import java.time.Instant
import java.util.Properties
import kotlin.math.ceil

/** Single-threaded, like the server's Lua owner thread. Never shares a native state across threads. */
object LuaIntegrationBenchmark {
    private val scenarios = linkedMapOf(
        "lua-loop" to "sum = sum + 1",
        "callback" to "sum = sum + increment(0)",
        "server-api" to "sum = sum + (Server.hasRuntimeData('benchmark') and 1 or 0)",
        "userdata-getter" to "sum = sum + coordinate.x",
        "userdata-method" to "sum = sum + coordinate:getHorizontalDistanceTo(other)",
        "userdata-method-cached" to "sum = sum + distance(coordinate, other)",
        "direct-method" to "sum = sum + directDistance(coordinate, other)",
        "table-conversion" to "sum = sum + convert(payload)",
        "table-field" to "sum = sum + readValue(payload)",
        "userdata-allocation" to "sum = sum + makeCoordinate().x",
        "userdata-reuse" to "sum = sum + reuseCoordinate().x",
        "platform-pcall" to "",
        "platform-xpcall" to "",
    )

    @JvmStatic
    fun main(args: Array<String>) {
        val options = args.associate { arg ->
            require(arg.startsWith("--") && '=' in arg) { "Use --name=value; see server/BENCHMARKS.md" }
            arg.removePrefix("--").split('=', limit = 2).let { it[0] to it[1] }
        }
        require(options.keys.all { it in setOf("sizes", "samples", "warmup", "scenario", "output", "stall-ms", "implementation") })
        val implementations = when (options["implementation"] ?: "custom") {
            "custom" -> listOf("custom")
            "standard" -> listOf("standard")
            "compare" -> listOf("custom", "standard")
            else -> error("implementation must be custom, standard, or compare")
        }
        val sizes = (options["sizes"] ?: "100,1000,10000").split(',').map(String::toInt)
        val samples = (options["samples"] ?: "200").toInt()
        val warmup = (options["warmup"] ?: "50").toInt()
        val stallMs = (options["stall-ms"] ?: "50").toDouble()
        val selected = (options["scenario"] ?: scenarios.keys.joinToString(",")).split(',')
        require(sizes.isNotEmpty() && sizes.all { it > 0 } && samples > 0 && warmup >= 0)
        require(stallMs.isFinite() && stallMs > 0 && selected.all { it in scenarios })
        val output = File(options["output"] ?: "server/build/benchmarks/lua-${Instant.now().toEpochMilli()}")
        require(!output.exists()) { "Output already exists: $output (choose a new directory)" }
        check(output.mkdirs())
        Properties().apply {
            setProperty("started", Instant.now().toString())
            setProperty("java", System.getProperty("java.runtime.version"))
            setProperty("vm", System.getProperty("java.vm.name"))
            setProperty("os", "${System.getProperty("os.name")} ${System.getProperty("os.arch")}")
            setProperty("processors", Runtime.getRuntime().availableProcessors().toString())
            setProperty("maxHeapBytes", Runtime.getRuntime().maxMemory().toString())
            setProperty("jvmArgs", ManagementFactory.getRuntimeMXBean().inputArguments.joinToString(" "))
            setProperty("options", "sizes=$sizes samples=$samples warmup=$warmup scenarios=$selected stallMs=$stallMs implementations=$implementations")
            File(output, "environment.properties").writer().use { store(it, "Lua integration benchmark; record Git revision alongside results") }
        }
        File(output, "samples.csv").bufferedWriter().use { raw ->
            File(output, "summary.csv").bufferedWriter().use { summary ->
                raw.appendLine("implementation,scenario,operations,sample,start_ns,elapsed_ns,gc_count,gc_ms,heap_used_bytes")
                val header = "implementation,scenario,operations,samples,ops_per_second,p50_ms,p95_ms,p99_ms,max_ms,stalls,gc_count,gc_ms"
                summary.appendLine(header)
                println(header)
                for (scenario in selected) for (size in sizes) for (implementation in implementations) {
                    // Runtime-data storage is a Selene API, with no stock LuaJava equivalent.
                    if (implementation == "standard" && scenario == "server-api") continue
                    val lua = if (implementation == "custom") LuaManager(LuaPackageModule()).lua else Lua54()

                    try {
                        if (implementation == "custom") install(lua) else installStandard(lua)
                        val body = if (implementation == "standard") standardBody(scenario) else scenarios.getValue(scenario)
                        val methodName = if (implementation == "custom") "getHorizontalDistanceTo" else "horizontalDistanceTo"
                        lua.load(LuaManager.loadBuffer("""
                            local distance = coordinate.$methodName
                            local payload = {value = 1, nested = {name = 'benchmark', values = {1, 2, 3}}}
                            function batch(n)
                                local sum = 0
                                for i = 1, n do $body end
                                return sum
                            end
                            function noop() end
                            function stockXpcall() assert(xpcall(noop, debug.traceback)) end
                        """.trimIndent()), "benchmark-$scenario")
                        lua.pCall(0, 0)
                        fun run() {
                            if (scenario.startsWith("platform-")) {
                                repeat(size) {
                                    lua.getGlobal(if (scenario == "platform-xpcall" && implementation == "standard") "stockXpcall" else "noop")
                                    if (scenario == "platform-xpcall" && implementation == "custom") lua.xpCall(0, 0) else lua.pCall(0, 0)
                                }
                            } else {
                                lua.getGlobal("batch")
                                lua.push(size)
                                lua.pCall(1, 1)
                                check(lua.toInteger(-1) == size.toLong()) { "Incorrect result for $scenario" }
                                lua.pop(1)
                            }
                            check(lua.top == 0) { "Stack leak in $scenario: ${lua.top}" }
                        }
                        repeat(warmup) { run() }
                        val times = LongArray(samples)
                        var gcCount = 0L
                        var gcMs = 0L
                        repeat(samples) { sample ->
                            val before = gc()
                            val start = System.nanoTime()
                            run()
                            val elapsed = System.nanoTime() - start
                            val after = gc()
                            times[sample] = elapsed
                            gcCount += after.first - before.first
                            gcMs += after.second - before.second
                            val heap = ManagementFactory.getMemoryMXBean().heapMemoryUsage.used
                            raw.appendLine("$implementation,$scenario,$size,$sample,$start,$elapsed,${after.first - before.first},${after.second - before.second},$heap")
                        }
                        val sorted = times.sorted()
                        fun percentile(p: Double) = sorted[(ceil(p * samples).toInt() - 1).coerceAtLeast(0)] / 1e6
                        val row = "$implementation,$scenario,$size,$samples,${size.toDouble() * samples * 1e9 / times.sum()},${percentile(.50)},${percentile(.95)},${percentile(.99)},${sorted.last() / 1e6},${times.count { it >= stallMs * 1e6 }},$gcCount,$gcMs"
                        summary.appendLine(row)
                        println(row)
                    } finally {
                        lua.close()
                    }
                }
            }
        }
        println("Results: ${output.absolutePath}")
    }

    private fun gc(): Pair<Long, Long> = ManagementFactory.getGarbageCollectorMXBeans().let { beans ->
        beans.sumOf { it.collectionCount.coerceAtLeast(0) } to beans.sumOf { it.collectionTime.coerceAtLeast(0) }
    }

    private fun standardBody(scenario: String): String = when (scenario) {
        "userdata-getter" -> "sum = sum + coordinate:getX()"
        "userdata-method" -> "sum = sum + coordinate:horizontalDistanceTo(other)"
        "userdata-allocation" -> "sum = sum + makeCoordinate():getX()"
        "userdata-reuse" -> "sum = sum + reuseCoordinate():getX()"
        else -> scenarios.getValue(scenario)
    }

    /** Stock state, metatables, callbacks and conversion; no Selene Lua helpers. */
    private fun installStandard(lua: Lua) {
        lua.openLibraries()
        lua.push(Coordinate(1, 0, 0), Lua.Conversion.NONE)
        lua.setGlobal("coordinate")
        lua.push(Coordinate.Zero, Lua.Conversion.NONE)
        lua.setGlobal("other")
        fun callback(name: String, body: (Lua) -> Int) {
            lua.push { state -> body(state) }
            lua.setGlobal(name)
        }
        callback("increment") { state -> state.push(state.toInteger(1) + 1); 1 }
        callback("convert") { state ->
            val map = state.toObject(1, Map::class.java) as Map<*, *>
            check(map["nested"] is Map<*, *>)
            state.push((map["value"] as Number).toInt())
            1
        }
        callback("makeCoordinate") { state -> state.push(Coordinate(1, 0, 0), Lua.Conversion.NONE); 1 }
        val reused = Coordinate(1, 0, 0)
        callback("reuseCoordinate") { state -> state.push(reused, Lua.Conversion.NONE); 1 }
        callback("directDistance") { state ->
            val self = state.toJavaObject(1) as Coordinate
            val other = state.toJavaObject(2) as Coordinate
            state.push(self.horizontalDistanceTo(other))
            1
        }
        callback("readValue") { state ->
            state.getField(1, "value")
            val value = state.toInteger(-1)
            state.pop(1)
            state.push(value)
            1
        }
    }

    private fun install(lua: Lua54) {
        val server = ServerLuaApi(ServerApi(ServerConfig()))
        lua.push(lua.newTable { server.register(this) })
        lua.setGlobal("Server")
        lua.push(Coordinate(1, 0, 0), Lua.Conversion.NONE)
        lua.setGlobal("coordinate")
        lua.push(Coordinate.Zero, Lua.Conversion.NONE)
        lua.setGlobal("other")
        lua.register("increment") { state -> state.push(state.checkInt(1) + 1); 1 }
        lua.register("convert") { state ->
            val map = state.checkSerializedMap(1)
            check(map["nested"] is Map<*, *>)
            state.push((map["value"] as Number).toInt())
            1
        }
        lua.register("makeCoordinate") { state -> state.push(Coordinate(1, 0, 0), Lua.Conversion.NONE); 1 }
        val reused = Coordinate(1, 0, 0)
        lua.register("reuseCoordinate") { state -> state.push(reused, Lua.Conversion.NONE); 1 }
        lua.register("directDistance") { state ->
            val self = state.checkUserdata<Coordinate>(1)
            val (other, _) = state.checkCoordinate(2)
            state.push(self.horizontalDistanceTo(other))
            1
        }
        lua.register("readValue") { state -> state.push(state.getFieldInt(1, "value")!!); 1 }
        lua.load(LuaManager.loadBuffer("Server.overwriteRuntimeData('benchmark', {})"), "benchmark-setup")
        lua.pCall(0, 0)
    }
}
