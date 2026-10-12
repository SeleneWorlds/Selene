package com.seleneworlds.common.bundles

import java.io.File

val bundleContentDirectories = listOf("common", "server", "client")
private val bundleRootFiles = setOf("bundle.json", "init.lua")

/** Enumerates only the files defined by the bundle structure. */
fun bundleContentFiles(dir: File, directories: List<String> = bundleContentDirectories): Sequence<File> =
    bundleRootFiles.asSequence().map(dir::resolve).filter { it.isFile } +
        directories.asSequence().map(dir::resolve).filter { it.isDirectory }
            .flatMap { it.walkTopDown().filter { file -> file.isFile } }

/** Resolves a content path without allowing traversal outside the content roots. */
fun resolveBundleContentFile(dir: File, path: String): File? {
    val root = dir.canonicalFile.toPath()
    val file = dir.resolve(path).canonicalFile
    val target = file.toPath()
    if (!target.startsWith(root)) return null
    val relative = root.relativize(target)
    val valid = (relative.nameCount == 1 && relative.toString() in bundleRootFiles) ||
        (relative.nameCount > 1 && relative.getName(0).toString() in bundleContentDirectories)
    return file.takeIf { valid }
}
