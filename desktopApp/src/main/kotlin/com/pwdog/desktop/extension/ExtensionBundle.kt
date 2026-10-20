package com.pwdog.desktop.extension

import com.pwdog.core.account.AppPaths
import java.nio.file.Files
import java.nio.file.Path

data class ExtensionFolders(
    val chromium: Path,
    val firefox: Path,
)

object ExtensionBundle {
    private val SHARED_FILES = listOf(
        "popup.html",
        "popup.js",
        "options.html",
        "options.js",
        "browser-api.js",
        "style.css",
        "README.md",
        "icons/icon48.png",
        "icons/icon96.png",
        "icons/icon128.png",
    )

    fun ensureExtracted(baseDir: Path = AppPaths.defaultBaseDir()): ExtensionFolders {
        val root = baseDir.resolve("extension").toAbsolutePath().normalize()
        val chromium = root.resolve("chromium")
        val firefox = root.resolve("firefox")
        Files.createDirectories(chromium)
        Files.createDirectories(firefox)

        for (name in SHARED_FILES) {
            copyResource("extension/$name", chromium.resolve(name))
            copyResource("extension/$name", firefox.resolve(name))
        }
        copyResource("extension/manifest.json", chromium.resolve("manifest.json"))
        copyResource("extension/manifest.firefox.json", firefox.resolve("manifest.json"))

        require(Files.exists(chromium.resolve("manifest.json"))) { "Chromium 扩展资源缺失，请重新安装 PWDog" }
        require(Files.exists(firefox.resolve("manifest.json"))) { "Firefox 扩展资源缺失，请重新安装 PWDog" }
        return ExtensionFolders(chromium = chromium, firefox = firefox)
    }

    private fun copyResource(resource: String, target: Path) {
        val bytes = ExtensionBundle::class.java.classLoader
            ?.getResourceAsStream(resource)
            ?.use { it.readBytes() }
            ?: error("扩展资源缺失: $resource")
        target.parent?.let { Files.createDirectories(it) }
        if (!Files.exists(target) || !Files.readAllBytes(target).contentEquals(bytes)) {
            Files.write(target, bytes)
        }
    }
}
