package com.pwdog.desktop.settings

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class AccentPreset {
    PURPLE,
    BLUE,
    GREEN,
    ORANGE,
    RED,
}

enum class BackgroundStyle {
    SOLID,
    GRADIENT,
    GLOW,
}

data class AppSettings(
    val version: Int = 1,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPreset = AccentPreset.PURPLE,
    val backgroundStyle: BackgroundStyle = BackgroundStyle.GRADIENT,
)
