package com.pwdog.desktop.extension

import com.pwdog.core.account.AppPaths
import java.nio.file.Files
import java.nio.file.Path

object ExtensionBundle {
    private val FILES = listOf(
        "manifest.json",
        "popup.html",
        "popup.js",
        "options.html",
        "options.js",
        "style.css",
        "README.md",
    )

    fun ensureExtracted(baseDir: Path = AppPaths.defaultBaseDir()): Path {
        val target = baseDir.resolve("extension").toAbsolutePath().normalize()
        Files.createDirectories(target)
        val loader = ExtensionBundle::class.java.classLoader
            ?: error("无法读取扩展资源")
        for (name in FILES) {
            val bytes = loader.getResourceAsStream("extension/$name")
                ?.use { it.readBytes() }
                ?: continue
            val file = target.resolve(name)
            if (!Files.exists(file) || !Files.readAllBytes(file).contentEquals(bytes)) {
                Files.write(file, bytes)
            }
        }
        require(Files.exists(target.resolve("manifest.json"))) { "扩展资源缺失，请重新安装 PWDog" }
        return target
    }
}
