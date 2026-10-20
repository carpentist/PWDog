@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.pwdog.desktop.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pwdog.desktop.settings.AccentPreset
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.settings.BackgroundStyle
import com.pwdog.desktop.settings.ThemeMode
import com.pwdog.desktop.ui.WindowsTitleBar
import kotlinx.coroutines.delay

data class AccentColors(
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val accentContrast: Color,
)

data class QuietColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val border: Color,
    val borderStrong: Color,
    val accent: Color,
    val accentStrong: Color,
    val accentSoft: Color,
    val accentContrast: Color,
    val success: Color,
    val danger: Color,
    val warning: Color,
)

fun accentColors(preset: AccentPreset, dark: Boolean): AccentColors = when (preset) {
    AccentPreset.PURPLE -> if (dark) {
        AccentColors(Color(0xFFCBB4E8), Color(0xFFE5D7F5), Color(0xFF2E243B), Color(0xFF1A1124))
    } else {
        AccentColors(Color(0xFF6A4B8C), Color(0xFF543A73), Color(0xFFEFE7F7), Color.White)
    }
    AccentPreset.BLUE -> if (dark) {
        AccentColors(Color(0xFFA9C7FF), Color(0xFFD2E2FF), Color(0xFF1B2C49), Color(0xFF0C1930))
    } else {
        AccentColors(Color(0xFF2563EB), Color(0xFF1D4ED8), Color(0xFFE2ECFF), Color.White)
    }
    AccentPreset.GREEN -> if (dark) {
        AccentColors(Color(0xFFA8D8B6), Color(0xFFD0EFDA), Color(0xFF1D3526), Color(0xFF0B1C11))
    } else {
        AccentColors(Color(0xFF2F7D4F), Color(0xFF24663F), Color(0xFFE0F2E5), Color.White)
    }
    AccentPreset.ORANGE -> if (dark) {
        AccentColors(Color(0xFFF0B17B), Color(0xFFFFD3AD), Color(0xFF3A2718), Color(0xFF2A1808))
    } else {
        AccentColors(Color(0xFFB2622A), Color(0xFF95501F), Color(0xFFF9E8DC), Color.White)
    }
    AccentPreset.RED -> if (dark) {
        AccentColors(Color(0xFFF0A9A3), Color(0xFFFFD0CB), Color(0xFF3A2220), Color(0xFF2B1210))
    } else {
        AccentColors(Color(0xFFB3443B), Color(0xFF94362F), Color(0xFFF8E4E1), Color.White)
    }
}

fun accentSwatch(preset: AccentPreset): Color = accentColors(preset, dark = false).accent

fun quietColors(dark: Boolean, accent: AccentColors): QuietColors =
    if (dark) {
        QuietColors(
            dark = true,
            bg = Color(0xFF111318),
            surface = Color(0xFF161A20),
            surface2 = Color(0xFF1C222A),
            surface3 = Color(0xFF242B34),
            text = Color(0xFFEDF1F5),
            muted = Color(0xFF96A0AC),
            faint = Color(0xFF6C7784),
            border = Color(0x14FFFFFF),
            borderStrong = Color(0x26FFFFFF),
            accent = accent.accent,
            accentStrong = accent.accentStrong,
            accentSoft = accent.accentSoft,
            accentContrast = accent.accentContrast,
            success = Color(0xFF71C891),
            danger = Color(0xFFFF9B92),
            warning = Color(0xFFE8B86D),
        )
    } else {
        QuietColors(
            dark = false,
            bg = Color(0xFFF4F1EA),
            surface = Color(0xFFFBFAF7),
            surface2 = Color(0xFFF0ECE3),
            surface3 = Color(0xFFE8E2D8),
            text = Color(0xFF1A1B1F),
            muted = Color(0xFF6F7278),
            faint = Color(0xFF999B9E),
            border = Color(0x1A1A1B1F),
            borderStrong = Color(0x2E1A1B1F),
            accent = accent.accent,
            accentStrong = accent.accentStrong,
            accentSoft = accent.accentSoft,
            accentContrast = accent.accentContrast,
            success = Color(0xFF2F7D55),
            danger = Color(0xFFB3443B),
            warning = Color(0xFFA8681F),
        )
    }

val LocalQuiet = staticCompositionLocalOf {
    quietColors(dark = true, accent = accentColors(AccentPreset.PURPLE, dark = true))
}

// Georgia and Segoe UI ship with Windows; Chinese glyphs fall back through Skia.
private val QuietDisplay = FontFamily("Georgia")
private val QuietSans = FontFamily("Segoe UI")
private val QuietMono = FontFamily("Consolas")

object QuietType {
    val display = TextStyle(
        fontFamily = QuietDisplay,
        fontSize = 38.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.035f).em,
    )
    val pageTitle = TextStyle(
        fontFamily = QuietDisplay,
        fontSize = 31.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.03f).em,
    )
    val sheetTitle = TextStyle(
        fontFamily = QuietDisplay,
        fontSize = 27.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.03).em,
    )
    val dialogTitle = TextStyle(
        fontFamily = QuietDisplay,
        fontSize = 23.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02f).em,
    )
    val heading = TextStyle(
        fontFamily = QuietDisplay,
        fontSize = 21.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em,
    )
    val body = TextStyle(
        fontFamily = QuietSans,
        fontSize = 13.sp,
        lineHeight = 20.sp,
    )
    val bodyMedium = TextStyle(
        fontFamily = QuietSans,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
    )
    val small = TextStyle(
        fontFamily = QuietSans,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
    )
    val tiny = TextStyle(
        fontFamily = QuietSans,
        fontSize = 10.5.sp,
        lineHeight = 15.sp,
    )
    val label = TextStyle(
        fontFamily = QuietSans,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val section = TextStyle(
        fontFamily = QuietSans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01f).em,
    )
    val button = TextStyle(
        fontFamily = QuietSans,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )
    val entryTitle = TextStyle(
        fontFamily = QuietSans,
        fontSize = 13.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em,
    )
    val entryUser = TextStyle(
        fontFamily = QuietSans,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
    val detailValue = TextStyle(
        fontFamily = QuietSans,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    )
    val nav = TextStyle(
        fontFamily = QuietSans,
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )
    val eyebrow = TextStyle(
        fontFamily = QuietMono,
        fontSize = 9.5.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.18.em,
        fontWeight = FontWeight.Medium,
    )
    val monoSmall = TextStyle(
        fontFamily = QuietMono,
        fontSize = 9.5.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.04.em,
    )
    val link = TextStyle(
        fontFamily = QuietSans,
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        textDecoration = TextDecoration.None,
    )
}

@Composable
fun PwdogTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = accentColors(settings.accent, dark)
    val quiet = quietColors(dark, accent)

    LaunchedEffect(dark) {
        var attempts = 0
        while (attempts < 10) {
            if (WindowsTitleBar.setDarkMode(dark)) break
            delay(100)
            attempts++
        }
    }

    val scheme = if (dark) {
        androidx.compose.material3.darkColorScheme(
            primary = quiet.accent,
            onPrimary = quiet.accentContrast,
            primaryContainer = quiet.accentSoft,
            onPrimaryContainer = quiet.accent,
            background = quiet.bg,
            onBackground = quiet.text,
            surface = quiet.surface,
            onSurface = quiet.text,
            surfaceVariant = quiet.surface2,
            onSurfaceVariant = quiet.muted,
            outline = quiet.borderStrong,
            outlineVariant = quiet.border,
            error = quiet.danger,
            onError = quiet.accentContrast,
            scrim = Color.Black,
        )
    } else {
        androidx.compose.material3.lightColorScheme(
            primary = quiet.accent,
            onPrimary = quiet.accentContrast,
            primaryContainer = quiet.accentSoft,
            onPrimaryContainer = quiet.accent,
            background = quiet.bg,
            onBackground = quiet.text,
            surface = quiet.surface,
            onSurface = quiet.text,
            surfaceVariant = quiet.surface2,
            onSurfaceVariant = quiet.muted,
            outline = quiet.borderStrong,
            outlineVariant = quiet.border,
            error = quiet.danger,
            onError = quiet.accentContrast,
            scrim = Color.Black,
        )
    }

    val typography = Typography(
        displayLarge = QuietType.display,
        displayMedium = QuietType.pageTitle,
        headlineLarge = QuietType.pageTitle,
        headlineMedium = QuietType.heading,
        headlineSmall = QuietType.sheetTitle,
        titleLarge = QuietType.heading,
        titleMedium = QuietType.section,
        titleSmall = QuietType.label,
        bodyLarge = QuietType.body,
        bodyMedium = QuietType.body,
        bodySmall = QuietType.small,
        labelLarge = QuietType.button,
        labelMedium = QuietType.label,
        labelSmall = QuietType.monoSmall,
    )

    CompositionLocalProvider(LocalQuiet provides quiet) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            AppBackground(settings.backgroundStyle, quiet, accent) {
                content()
            }
        }
    }
}

@Composable
private fun AppBackground(
    style: BackgroundStyle,
    quiet: QuietColors,
    accent: AccentColors,
    content: @Composable () -> Unit,
) {
    val base = quiet.bg
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                when (style) {
                    BackgroundStyle.SOLID -> drawRect(base)

                    BackgroundStyle.GRADIENT -> drawRect(
                        Brush.linearGradient(
                            colors = listOf(
                                base,
                                lerp(base, accent.accent, if (quiet.dark) 0.10f else 0.08f),
                                lerp(base, accent.accentStrong, if (quiet.dark) 0.07f else 0.05f),
                                base,
                            ),
                            start = Offset.Zero,
                            end = Offset(size.width, size.height),
                        ),
                    )

                    BackgroundStyle.GLOW -> {
                        drawRect(base)
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.accent.copy(alpha = if (quiet.dark) 0.17f else 0.15f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.86f, size.height * 0.04f),
                                radius = size.maxDimension * 0.62f,
                            ),
                        )
                        drawRect(
                            Brush.radialGradient(
                                colors = listOf(
                                    accent.accentStrong.copy(alpha = if (quiet.dark) 0.12f else 0.09f),
                                    Color.Transparent,
                                ),
                                center = Offset(size.width * 0.04f, size.height * 0.98f),
                                radius = size.maxDimension * 0.55f,
                            ),
                        )
                    }
                }
            },
    ) {
        content()
    }
}
