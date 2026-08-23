package com.seleneworlds.client.ui.cef

import com.seleneworlds.client.config.ClientConfig
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class BundleUiStorage(config: ClientConfig) {
    private val root = Path.of(config.browserUiStorageDir).toAbsolutePath().normalize()

    @Synchronized
    fun load(bundle: String, entrypoint: String, key: String): String? {
        val file = resolve(bundle, entrypoint, key)
        return if (Files.isRegularFile(file)) Files.readString(file) else null
    }

    @Synchronized
    fun save(bundle: String, entrypoint: String, key: String, value: String) {
        require(value.toByteArray().size <= MAX_VALUE_BYTES) { "Storage value exceeds $MAX_VALUE_BYTES bytes" }
        val file = resolve(bundle, entrypoint, key)
        Files.createDirectories(file.parent)
        val staging = Files.createTempFile(file.parent, ".${file.fileName}-", ".tmp")
        try {
            Files.writeString(staging, value, StandardOpenOption.TRUNCATE_EXISTING)
            try {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(staging, file, StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            Files.deleteIfExists(staging)
        }
    }

    private fun resolve(bundle: String, entrypoint: String, key: String): Path {
        requireStorageName("bundle", bundle)
        requireStorageName("entrypoint", entrypoint)
        requireStorageName("key", key)
        return root.resolve(bundle).resolve(entrypoint).resolve(key).normalize().also {
            require(it.startsWith(root)) { "Storage path escapes its root" }
        }
    }

    private fun requireStorageName(label: String, value: String) {
        require(value.isNotEmpty() && value.length <= MAX_NAME_LENGTH && STORAGE_NAME.matches(value)) {
            "Storage $label must contain 1-$MAX_NAME_LENGTH letters, numbers, dots, underscores, or hyphens"
        }
    }

    private companion object {
        val STORAGE_NAME = Regex("^[a-zA-Z0-9._-]+$")
        const val MAX_NAME_LENGTH = 128
        const val MAX_VALUE_BYTES = 16 * 1024 * 1024
    }
}
