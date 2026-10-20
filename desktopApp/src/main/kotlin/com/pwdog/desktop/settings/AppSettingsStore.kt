package com.pwdog.desktop.settings

import com.google.gson.GsonBuilder
import com.pwdog.core.account.AppPaths
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

class AppSettingsStore(private val baseDir: Path = AppPaths.defaultBaseDir()) {
    private val settingsFile = baseDir.resolve("settings.json")
    private val gson = GsonBuilder().setPrettyPrinting().create()

    fun load(): AppSettings {
        return runCatching {
            if (!Files.exists(settingsFile)) {
                return AppSettings()
            }
            val dto = gson.fromJson(Files.readString(settingsFile), SettingsDto::class.java)
            AppSettings(
                version = dto?.version ?: 1,
                themeMode = parseEnum(dto?.themeMode, ThemeMode.SYSTEM),
                accent = parseEnum(dto?.accent, AccentPreset.PURPLE),
                backgroundStyle = parseEnum(
                    dto?.backgroundStyle,
                    BackgroundStyle.GRADIENT,
                ),
            )
        }.getOrDefault(AppSettings())
    }

    fun save(settings: AppSettings) {
        Files.createDirectories(baseDir)
        val temp = baseDir.resolve("settings.json.tmp")
        val dto = SettingsDto(
            version = settings.version,
            themeMode = settings.themeMode.name,
            accent = settings.accent.name,
            backgroundStyle = settings.backgroundStyle.name,
        )
        Files.writeString(
            temp,
            gson.toJson(dto),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE,
        )
        try {
            Files.move(
                temp,
                settingsFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (_: Exception) {
            Files.move(temp, settingsFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private inline fun <reified T : Enum<T>> parseEnum(value: String?, fallback: T): T {
        val candidate = value?.trim()?.uppercase() ?: return fallback
        return runCatching { enumValueOf<T>(candidate) }.getOrDefault(fallback)
    }

    private data class SettingsDto(
        val version: Int? = null,
        val themeMode: String? = null,
        val accent: String? = null,
        val backgroundStyle: String? = null,
    )
}
