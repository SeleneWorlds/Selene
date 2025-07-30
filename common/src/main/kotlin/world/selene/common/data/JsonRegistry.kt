package world.selene.common.data

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import world.selene.common.bundles.BundleDatabase
import java.io.File
import kotlin.reflect.KClass

abstract class JsonRegistry<TData : Any>(
    private val objectMapper: ObjectMapper,
    val platform: String,
    override val name: String,
    private val dataClass: KClass<TData>
): Registry<TData> {
    private val logger = LoggerFactory.getLogger("selene")
    protected val entries: MutableMap<String, TData> = mutableMapOf()

    override fun get(name: String): TData? = entries[name]
    override fun getAll(): Map<String, TData> = entries

    fun load(bundleDatabase: BundleDatabase) {
        entries.clear()
        for (bundle in bundleDatabase.loadedBundles) {
            val dataDir = File(bundle.dir, "$platform/data")
            val files = dataDir.listFiles { _, file -> file == "$name.json" || file.endsWith(".$name.json") }
            if (files != null) {
                for (file in files) {
                    try {
                        val type = objectMapper.typeFactory.constructParametricType(RegistryFile::class.java, dataClass.java)
                        val parsed = objectMapper.readValue<RegistryFile<TData>>(file, type)
                        for ((name, data) in parsed.entries) {
                            entries[name] = data
                        }
                    } catch (e: Exception) {
                        logger.error("Failed to load $file from bundle ${bundle.manifest.name}", e)
                    }
                }
            }
        }
    }

}