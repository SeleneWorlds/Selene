package world.selene.common.i18n

import org.slf4j.Logger
import world.selene.common.bundles.BundleDatabase
import world.selene.common.bundles.BundleLoader
import world.selene.common.bundles.LocatedBundle
import java.io.File
import java.util.*

class Messages(
    private val bundleDatabase: BundleDatabase,
    private val bundleLoader: BundleLoader,
    private val logger: Logger
) {
    private val messagesByLocale = mutableMapOf<Locale, Properties>()
    var defaultLocale: Locale = Locale.getDefault()

    fun get(key: String, locale: Locale? = null): String? {
        val messages = getOrLoadMessages(locale ?: defaultLocale)
        return messages.getProperty(key)
    }

    fun format(key: String, args: Map<String, Any>, locale: Locale? = null): String? {
        val template = get(key, locale) ?: return null
        return template.replace(Regex("\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*}")) { match ->
            args[match.groupValues[1]]?.toString() ?: match.value
        }
    }

    fun has(key: String, locale: Locale?): Boolean {
        val messages = getOrLoadMessages(locale ?: defaultLocale)
        return messages.containsKey(key)
    }

    private fun getOrLoadMessages(locale: Locale): Properties {
        return messagesByLocale.getOrPut(locale) {
            loadMessagesForLocale(locale)
        }
    }

    private fun loadMessagesForLocale(locale: Locale): Properties {
        val mergedMessages = Properties()
        for (bundle in bundleDatabase.loadedBundles) {
            val bundleMessages = loadBundleMessages(bundle, locale)
            mergedMessages.putAll(bundleMessages)
        }
        return mergedMessages
    }

    private fun loadBundleMessages(bundle: LocatedBundle, locale: Locale): Properties {
        val messages = Properties()

        val i18nDirs = listOf(
            File(bundle.dir, "client/i18n"),
            File(bundle.dir, "common/i18n"),
            File(bundle.dir, "server/i18n")
        ).filter { it.exists() && it.isDirectory }
        if (i18nDirs.isEmpty()) {
            return messages
        }

        for (i18nDir in i18nDirs) {
            val localeFiles = findLocaleFiles(i18nDir, locale)
            localeFiles.forEach { file ->
                try {
                    val content = file.readText()
                    val props = Properties()
                    props.load(content.byteInputStream())
                    messages.putAll(props)
                } catch (e: Exception) {
                    logger.error("Failed to load locale messages from ${bundle.manifest.name}/${i18nDir.relativeTo(bundle.dir)}/${file.name}: ${e.message}")
                }
            }
        }

        return messages
    }

    private fun findLocaleFiles(i18nDir: File, locale: Locale): List<File> {
        val files = mutableListOf<File>()
        
        val localeTags = listOf(
            "${locale.language}_${locale.country}_${locale.variant}",
            "${locale.language}_${locale.country}",
            locale.language
        ).filter { it.isNotBlank() && !it.endsWith("_") }

        for (localeTag in localeTags) {
            val matchingFiles = i18nDir.listFiles { file ->
                file.isFile && file.name.matches(Regex(".*_${Regex.escape(localeTag)}\\.properties"))
            }
            matchingFiles?.let { files.addAll(it) }
        }

        return files
    }
}
