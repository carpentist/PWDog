package com.pwdog.desktop.security

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

object LocalDataWipe {
    fun wipe(baseDir: Path) {
        val target = baseDir.toAbsolutePath().normalize()
        val root = target.root
            ?: throw IllegalArgumentException("Invalid data directory")
        require(target != root) { "Refusing to delete the filesystem root" }

        val home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize()
        require(target != home) { "Refusing to delete the home directory" }

        if (!Files.exists(target)) return
        Files.walk(target).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
