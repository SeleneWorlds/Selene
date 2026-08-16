package com.seleneworlds.server.http

import com.seleneworlds.common.bundles.Bundle
import com.seleneworlds.common.bundles.BundleDatabase
import kotlinx.serialization.Serializable
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

class ClientAssetIndexProvider(
    private val bundleDatabase: BundleDatabase
) {
    private val lock = Any()
    private val current = AtomicReference<ClientAssetIndex?>()

    internal fun current(): ClientAssetIndex {
        return current.get() ?: throw IllegalStateException("Client asset index has not been built")
    }

    internal fun rebuild(): ClientAssetIndex {
        return synchronized(lock) {
            ClientAssetIndex.build(bundleDatabase).also { current.set(it) }
        }
    }
}

internal class ClientAssetIndex private constructor(
    val manifest: ClientAssetManifest,
    val manifestEtag: String,
    private val clientAssetsByHashedPath: Map<String, HashedClientAsset>,
    private val bundleAssetsByBundleAndHashedPath: Map<BundleAssetKey, HashedClientAsset>,
    private val bundleManifestsByBundleName: Map<String, VersionedAssetManifest>
) {
    fun resolveClient(hashedPath: String): HashedClientAsset? {
        return clientAssetsByHashedPath[normalizePublicPath(hashedPath)]
    }

    fun resolveBundle(bundleName: String, hashedPath: String): HashedClientAsset? {
        return bundleAssetsByBundleAndHashedPath[BundleAssetKey(bundleName, normalizePublicPath(hashedPath))]
    }

    fun bundleManifest(bundleName: String): VersionedAssetManifest? {
        return bundleManifestsByBundleName[bundleName]
    }

    fun isContentCurrent(asset: HashedClientAsset): Boolean {
        if (!asset.file.exists() || !asset.file.isFile) {
            return false
        }
        val currentHash = sha256(asset.file)
        return currentHash == asset.fullHash && currentHash.startsWith(asset.hashPrefix)
    }

    companion object {
        const val HASH_PREFIX_LENGTH = 12
        const val IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable"
        const val MANIFEST_CACHE_CONTROL = "no-cache"

        fun build(bundleDatabase: BundleDatabase): ClientAssetIndex {
            return build(bundleDatabase.enabledBundles)
        }

        internal fun build(
            enabledBundles: List<Bundle>,
            hashPrefixLength: Int = HASH_PREFIX_LENGTH
        ): ClientAssetIndex {
            require(hashPrefixLength in 1..64) { "Hash prefix length must be between 1 and 64" }

            val seenHashPrefixes = linkedMapOf<String, HashedClientAsset>()
            val bundleAssets = linkedMapOf<BundleAssetKey, HashedClientAsset>()
            val bundleManifests = linkedMapOf<String, VersionedAssetManifest>()
            val assetsByBundle = enabledBundles.map { bundle ->
                bundle to scanBundleAssets(bundle, hashPrefixLength)
            }

            for ((bundle, assets) in assetsByBundle) {
                val manifestAssets = linkedMapOf<String, String>()
                for (source in assets) {
                    val existingHash = seenHashPrefixes[source.hashPrefix]
                    if (existingHash != null && existingHash.fullHash != source.fullHash) {
                        throw IllegalStateException(
                            "SHA-256 hash prefix collision for '${source.logicalPath}' and " +
                                "'${existingHash.logicalPath}' at prefix '${source.hashPrefix}'"
                        )
                    }
                    seenHashPrefixes[source.hashPrefix] = source

                    val key = BundleAssetKey(bundle.manifest.name, source.hashedPath)
                    val previous = bundleAssets.put(key, source)
                    if (previous != null && previous.fullHash != source.fullHash) {
                        throw IllegalStateException(
                            "Hashed asset URL collision for bundle '${bundle.manifest.name}' at '${source.hashedPath}'"
                        )
                    }
                    manifestAssets[source.logicalPath] =
                        "/bundles/${bundle.manifest.name}/content/${source.hashedPath}"
                }
                bundleManifests[bundle.manifest.name] = versionManifest(manifestAssets)
            }

            val clientAssets = linkedMapOf<String, HashedClientAsset>()
            val manifestAssets = linkedMapOf<String, String>()
            for ((_, assets) in assetsByBundle.asReversed()) {
                for (asset in assets) {
                    if (asset.logicalPath !in manifestAssets) {
                        val previous = clientAssets.put(asset.hashedPath, asset)
                        if (previous != null && previous.fullHash != asset.fullHash) {
                            throw IllegalStateException("Hashed client asset URL collision at '${asset.hashedPath}'")
                        }
                        manifestAssets[asset.logicalPath] = "/client/content/${asset.hashedPath}"
                    }
                }
            }

            val versionedManifest = versionManifest(manifestAssets)
            return ClientAssetIndex(
                manifest = versionedManifest.manifest,
                manifestEtag = versionedManifest.etag,
                clientAssetsByHashedPath = clientAssets,
                bundleAssetsByBundleAndHashedPath = bundleAssets,
                bundleManifestsByBundleName = bundleManifests
            )
        }

        private fun versionManifest(assets: Map<String, String>): VersionedAssetManifest {
            val manifest = ClientAssetManifest(assets.toSortedMap())
            val etag = "\"" + sha256(manifest.assets.entries.joinToString("\n") { "${it.key}=${it.value}" }) + "\""
            return VersionedAssetManifest(manifest, etag)
        }

        private fun scanBundleAssets(bundle: Bundle, hashPrefixLength: Int): List<HashedClientAsset> {
            val assets = mutableListOf<HashedClientAsset>()
            val bundlePath = bundle.dir.toPath()
            for (rootName in listOf("common", "client")) {
                val root = bundlePath.resolve(rootName)
                if (!Files.exists(root) || !Files.isDirectory(root)) {
                    continue
                }
                Files.walkFileTree(root, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (dir != root && dir.fileName.toString().startsWith(".")) {
                            return FileVisitResult.SKIP_SUBTREE
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        if (!attrs.isRegularFile || file.fileName.toString().startsWith(".")) {
                            return FileVisitResult.CONTINUE
                        }

                        val logicalPath = normalizePublicPath(bundlePath.relativize(file).toString())
                        val fullHash = sha256(file.toFile())
                        val hashPrefix = fullHash.take(hashPrefixLength)
                        assets += HashedClientAsset(
                            logicalPath = logicalPath,
                            hashedPath = addHashToPath(logicalPath, hashPrefix),
                            file = file.toFile(),
                            fullHash = fullHash,
                            hashPrefix = hashPrefix
                        )
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                        if (exc is NoSuchFileException || exc is AccessDeniedException) {
                            return FileVisitResult.CONTINUE
                        }
                        throw exc
                    }
                })
            }
            return assets.sortedBy { it.logicalPath }
        }

        private fun addHashToPath(path: String, hashPrefix: String): String {
            val slash = path.lastIndexOf('/')
            val directory = if (slash >= 0) path.substring(0, slash + 1) else ""
            val fileName = if (slash >= 0) path.substring(slash + 1) else path
            val dot = fileName.lastIndexOf('.')
            val hashedFileName = if (dot > 0) {
                fileName.substring(0, dot) + "." + hashPrefix + fileName.substring(dot)
            } else {
                "$fileName.$hashPrefix"
            }
            return directory + hashedFileName
        }

        private fun normalizePublicPath(path: String): String {
            return path.replace(File.separatorChar, '/').replace('\\', '/')
        }

        private fun sha256(text: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        private fun sha256(file: File): String {
            val hasher = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    hasher.update(buffer, 0, read)
                }
            }
            return hasher.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

@Serializable
internal data class ClientAssetManifest(
    val assets: Map<String, String>
)

internal data class HashedClientAsset(
    val logicalPath: String,
    val hashedPath: String,
    val file: File,
    val fullHash: String,
    val hashPrefix: String
)

internal data class VersionedAssetManifest(
    val manifest: ClientAssetManifest,
    val etag: String
)

private data class BundleAssetKey(
    val bundleName: String,
    val hashedPath: String
)
