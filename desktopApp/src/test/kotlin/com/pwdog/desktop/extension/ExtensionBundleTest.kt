package com.pwdog.desktop.extension

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertTrue

class ExtensionBundleTest {
    @Test
    fun extractsBundledExtensionAndRefreshesChangedFiles() {
        val base = Files.createTempDirectory("pwdog-extension-test-")
        try {
            val directory = ExtensionBundle.ensureExtracted(base)
            val manifest = directory.resolve("manifest.json")
            assertTrue(Files.exists(manifest))
            assertTrue(Files.readString(manifest).contains("\"manifest_version\": 3"))
            assertTrue(Files.exists(directory.resolve("popup.js")))
            assertTrue(Files.exists(directory.resolve("options.js")))

            Files.writeString(directory.resolve("popup.js"), "stale")
            ExtensionBundle.ensureExtracted(base)
            assertTrue(Files.readString(directory.resolve("popup.js")).contains("chrome"))
        } finally {
            deleteRecursively(base)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
