package com.pwdog.desktop.security

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.nio.file.Path

object WindowsAutostart {
    private const val RUN_KEY = "Software\\Microsoft\\Windows\\CurrentVersion\\Run"
    private const val VALUE_NAME = "PWDog"

    fun isSupported(): Boolean = currentLauncher() != null

    fun isEnabled(): Boolean {
        if (!isWindows()) return false
        return runCatching {
            Advapi32Util.registryGetStringValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY,
                VALUE_NAME,
            )
        }.isSuccess
    }

    fun enable(): Boolean {
        val launcher = currentLauncher() ?: return false
        val command = "\"${launcher.toAbsolutePath()}\" --tray"
        return runCatching {
            Advapi32Util.registrySetStringValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY,
                VALUE_NAME,
                command,
            )
            true
        }.getOrDefault(false)
    }

    fun disable(): Boolean {
        if (!isWindows()) return false
        return runCatching {
            Advapi32Util.registryDeleteValue(
                WinReg.HKEY_CURRENT_USER,
                RUN_KEY,
                VALUE_NAME,
            )
            true
        }.getOrElse { !isEnabled() }
    }

    private fun currentLauncher(): Path? {
        if (!isWindows()) return null

        val jpackagePath = System.getProperty("jpackage.app-path")
            ?.takeIf { it.isNotBlank() }
        if (jpackagePath != null) {
            return runCatching { Path.of(jpackagePath) }.getOrNull()
        }

        val command = ProcessHandle.current().info().command().orElse(null) ?: return null
        val path = runCatching { Path.of(command) }.getOrNull() ?: return null
        val name = path.fileName?.toString()?.lowercase() ?: return null
        if (name == "java.exe" || name == "javaw.exe") return null
        return path
    }

    private fun isWindows(): Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
}
