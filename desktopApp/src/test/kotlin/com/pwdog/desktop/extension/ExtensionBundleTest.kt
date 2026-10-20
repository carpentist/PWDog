package com.pwdog.desktop.extension

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertTrue

class ExtensionBundleTest {
    @Test
    fun extractsChromiumAndFirefoxBundlesAndRefreshesChangedFiles() {
        val base = Files.createTempDirectory("pwdog-extension-test-")
        try {
            val folders = ExtensionBundle.ensureExtracted(base)
            val chromiumManifest = folders.chromium.resolve("manifest.json")
            val firefoxManifest = folders.firefox.resolve("manifest.json")
            assertTrue(Files.exists(chromiumManifest))
            assertTrue(Files.readString(chromiumManifest).contains("\"action\""))
            assertTrue(Files.readString(firefoxManifest).contains("browser_specific_settings"))
            assertTrue(Files.exists(folders.chromium.resolve("browser-api.js")))
            assertTrue(Files.exists(folders.firefox.resolve("icons/icon128.png")))

            Files.writeString(folders.chromium.resolve("popup.js"), "stale")
            ExtensionBundle.ensureExtracted(base)
            assertTrue(Files.readString(folders.chromium.resolve("popup.js")).contains("pwdogBrowser"))
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
