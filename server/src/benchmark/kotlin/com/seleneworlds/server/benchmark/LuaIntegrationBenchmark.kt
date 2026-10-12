package com.seleneworlds.server.benchmark

import com.seleneworlds.common.grid.Coordinate
import com.seleneworlds.common.lua.LuaManager
import com.seleneworlds.common.lua.libraries.LuaPackageModule
import com.seleneworlds.common.lua.util.*
import com.seleneworlds.server.ServerApi
import com.seleneworlds.server.ServerLuaApi
import com.seleneworlds.server.config.ServerConfig
import com.seleneworlds.server.data.ServerCustomData
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
        require(options.keys.all { it in setOf("sizes", "samples", "warmup", "scenario", "output", "stall-ms") })
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
            setProperty("options", "sizes=$sizes samples=$samples warmup=$warmup scenarios=$selected stallMs=$stallMs")
            File(output, "environment.properties").writer().use { store(it, "Lua integration benchmark; record Git revision alongside results") }
        }
        File(output, "samples.csv").bufferedWriter().use { raw ->
            File(output, "summary.csv").bufferedWriter().use { summary ->
                raw.appendLine("scenario,operations,sample,start_ns,elapsed_ns,gc_count,gc_ms,heap_used_bytes")
                val header = "scenario,operations,samples,ops_per_second,p50_ms,p95_ms,p99_ms,max_ms,stalls,gc_count,gc_ms"
                summary.appendLine(header)
                println(header)
                for (scenario in selected) for (size in sizes) {
                    val manager = LuaManager(LuaPackageModule())
                    val lua = manager.lua
                    try {
                        install(manager)
                        val body = scenarios.getValue(scenario)
                        lua.load(LuaManager.loadBuffer("""
                            local distance = coordinate.getHorizontalDistanceTo
                            local payload = {value = 1, nested = {name = 'benchmark', values = {1, 2, 3}}}
                            function batch(n)
                                local sum = 0
                                for i = 1, n do $body end
                                return sum
                            end
                            function noop() end
                        """.trimIndent()), "benchmark-$scenario")
                        lua.pCall(0, 0)
                        fun run() {
                            if (scenario.startsWith("platform-")) {
                                repeat(size) {
                                    lua.getGlobal("noop")
                                    if (scenario == "platform-xpcall") lua.xpCall(0, 0) else lua.pCall(0, 0)
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
                            raw.appendLine("$scenario,$size,$sample,$start,$elapsed,${after.first - before.first},${after.second - before.second},$heap")
                        }
                        val sorted = times.sorted()
                        fun percentile(p: Double) = sorted[(ceil(p * samples).toInt() - 1).coerceAtLeast(0)] / 1e6
                        val row = "$scenario,$size,$samples,${size.toDouble() * samples * 1e9 / times.sum()},${percentile(.50)},${percentile(.95)},${percentile(.99)},${sorted.last() / 1e6},${times.count { it >= stallMs * 1e6 }},$gcCount,$gcMs"
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

    private fun install(manager: LuaManager) {
        val lua = manager.lua
        val server = ServerLuaApi(ServerApi(ServerConfig(), ServerCustomData()))
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
            state.push((map.getValue("value") as Number).toInt())
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
