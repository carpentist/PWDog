package com.pwdog.desktop

import com.pwdog.core.account.AccountService
import com.pwdog.core.crypto.KdfParams
import com.pwdog.desktop.security.LocalDataWipe
import com.pwdog.desktop.security.WindowsQuickUnlock
import com.pwdog.desktop.settings.AccentPreset
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.settings.AppSettingsStore
import com.pwdog.desktop.settings.ThemeMode
import com.pwdog.desktop.ui.AvatarPicker
import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import javax.imageio.ImageIO

fun main() {
    val input = "pwdog-dpapi-check".toByteArray()
    val blob = WindowsQuickUnlock.protect(input)
    val output = WindowsQuickUnlock.unprotect(blob)
    println("dpapiRoundtrip=${input.contentEquals(output)} blobBytes=${blob.size}")
    input.fill(0)
    output.fill(0)

    println("helloAvailable=${WindowsQuickUnlock.isHelloAvailable()}")

    val testKey = "Software\\PWDogDevCheck"
    val registryRoundtrip = runCatching {
        Advapi32Util.registryCreateKey(WinReg.HKEY_CURRENT_USER, testKey)
        Advapi32Util.registrySetStringValue(
            WinReg.HKEY_CURRENT_USER,
            testKey,
            "check",
            "ok",
        )
        val value = Advapi32Util.registryGetStringValue(
            WinReg.HKEY_CURRENT_USER,
            testKey,
            "check",
        )
        Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, testKey)
        value == "ok"
    }.getOrElse {
        runCatching { Advapi32Util.registryDeleteKey(WinReg.HKEY_CURRENT_USER, testKey) }
        false
    }
    println("registryRoundtrip=$registryRoundtrip")

    val wipeBase = Files.createTempDirectory("pwdog-wipe-check-")
    Files.createDirectories(wipeBase.resolve("vaults"))
    Files.writeString(wipeBase.resolve("accounts.db"), "check")
    Files.writeString(wipeBase.resolve("vaults").resolve("vault.db"), "check")
    LocalDataWipe.wipe(wipeBase)
    println("wipeRoundtrip=${!Files.exists(wipeBase)}")

    val settingsBase = Files.createTempDirectory("pwdog-settings-")
    val settingsStore = AppSettingsStore(settingsBase)
    val defaults = settingsStore.load()
    val savedSettings = AppSettings(
        themeMode = ThemeMode.DARK,
        accent = AccentPreset.BLUE,
    )
    settingsStore.save(savedSettings)
    val reloadedSettings = AppSettingsStore(settingsBase).load()
    println(
        "settingsRoundtrip=" +
            (defaults == AppSettings() && reloadedSettings == savedSettings),
    )
    deleteRecursively(settingsBase)

    val avatarFile = Files.createTempFile("pwdog-avatar-", ".png")
    val sourceImage = BufferedImage(400, 200, BufferedImage.TYPE_INT_ARGB)
    val graphics = sourceImage.createGraphics()
    graphics.color = java.awt.Color.BLUE
    graphics.fillRect(0, 0, 400, 200)
    graphics.dispose()
    ImageIO.write(sourceImage, "png", avatarFile.toFile())
    val avatarBytes = AvatarPicker.normalize(avatarFile.toFile())
    val avatarDecoded = ImageIO.read(avatarBytes.inputStream())
    println(
        "avatarNormalize=" +
            (avatarDecoded.width == 256 &&
                avatarDecoded.height == 256 &&
                avatarBytes.size <= 512 * 1024),
    )
    Files.deleteIfExists(avatarFile)

    val base = Files.createTempDirectory("pwdog-dpapi-quick-")
    val password = "dpapi-password".toByteArray()
    val params = KdfParams(memoryKiB = 8 * 1024, iterations = 1, parallelism = 1)

    try {
        AccountService(base, params).use { service ->
            val created = service.createAccount("DpapiUser", password)
            val accountId = created.session.account.id
            try {
                service.enableQuickUnlock(created.session, WindowsQuickUnlock::protect)
                created.session.close()

                val unlocked = service.unlockWithQuickUnlock(
                    accountId,
                    WindowsQuickUnlock::unprotect,
                )
                try {
                    println("quickUnlockRoundtrip=${unlocked.account.displayName == "DpapiUser"}")
                } finally {
                    unlocked.close()
                }
            } finally {
                created.session.close()
            }
        }
    } finally {
        password.fill(0)
        deleteRecursively(base)
    }
}

private fun deleteRecursively(root: Path) {
    if (!Files.exists(root)) return
    Files.walk(root).use { paths ->
        paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
    }
}
