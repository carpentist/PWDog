package com.pwdog.core.account

import java.nio.file.Path

object AppPaths {
    fun defaultBaseDir(): Path {
        val override = System.getProperty("pwdog.dataDir") ?: System.getenv("PWDOG_DATA_DIR")
        if (!override.isNullOrBlank()) {
            return Path.of(override)
        }

        val localAppData = System.getenv("LOCALAPPDATA")
        return if (!localAppData.isNullOrBlank()) {
            Path.of(localAppData, "PWDogData")
        } else {
            Path.of(System.getProperty("user.home"), ".pwdog")
        }
    }
}
