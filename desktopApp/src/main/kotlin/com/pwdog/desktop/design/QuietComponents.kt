package com.pwdog.desktop.design

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pwdog.desktop.settings.AccentPreset
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.settings.BackgroundStyle
import com.pwdog.desktop.settings.ThemeMode
import com.pwdog.desktop.theme.QuietColors
import com.pwdog.desktop.theme.QuietType
import com.pwdog.desktop.theme.accentColors
import com.pwdog.desktop.theme.LocalQuiet

// ---------------------------------------------------------------------------
// Icons
// ---------------------------------------------------------------------------

enum class QIcon {
    Lock, Search, Plus, Upload, Settings, Shield, Copy, Edit, Trash, Eye, EyeOff,
    Key, Globe, ArrowUpRight, Close, Check, Database, Fingerprint, ChevronDown,
    Folder, Plug, Sun, Moon, Monitor, Sparkle, Info, External,
}

@Composable
fun QIcon(
    icon: QIcon,
    modifier: Modifier = Modifier.size(18.dp),
    tint: Color = LocalQuiet.current.muted,
    strokeWidth: Float = 1.7f,
) {
    Canvas(modifier) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, Offset.Zero) }) {
            drawIcon(icon, tint, strokeWidth)
        }
    }
}

private fun DrawScope.drawIcon(icon: QIcon, tint: Color, sw: Float) {
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun path(block: Path.() -> Unit) = drawPath(Path().apply(block), tint, style = stroke)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float) =
        drawLine(tint, Offset(x1, y1), Offset(x2, y2), strokeWidth = sw, cap = StrokeCap.Round)
    fun circle(x: Float, y: Float, r: Float) = drawCircle(tint, r, Offset(x, y), style = stroke)
    fun roundRect(x: Float, y: Float, w: Float, h: Float, r: Float) = drawRoundRect(
        color = tint,
        topLeft = Offset(x, y),
        size = Size(w, h),
        cornerRadius = CornerRadius(r, r),
        style = stroke,
    )

    when (icon) {
        QIcon.Lock -> {
            roundRect(4f, 10f, 16f, 10f, 2f)
            path {
                moveTo(8f, 10f); lineTo(8f, 7f)
                cubicTo(8f, 2.6f, 16f, 2.6f, 16f, 7f)
                lineTo(16f, 10f)
            }
            circle(12f, 15f, 1f)
            line(12f, 16f, 12f, 18f)
        }
        QIcon.Search -> {
            circle(11f, 11f, 6.5f)
            line(16f, 16f, 20f, 20f)
        }
        QIcon.Plus -> {
            line(12f, 5f, 12f, 19f)
            line(5f, 12f, 19f, 12f)
        }
        QIcon.Upload -> {
            line(12f, 16f, 12f, 4f)
            path { moveTo(7f, 9f); lineTo(12f, 4f); lineTo(17f, 9f) }
            path {
                moveTo(5f, 16f); lineTo(5f, 19f)
                cubicTo(5f, 20.1f, 5.9f, 21f, 7f, 21f)
                lineTo(17f, 21f)
                cubicTo(18.1f, 21f, 19f, 20.1f, 19f, 19f)
                lineTo(19f, 16f)
            }
        }
        QIcon.Settings -> {
            line(4f, 6f, 6.5f, 6f); circle(9f, 6f, 2f); line(11.5f, 6f, 20f, 6f)
            line(4f, 12f, 12.5f, 12f); circle(15f, 12f, 2f); line(17.5f, 12f, 20f, 12f)
            line(4f, 18f, 4.5f, 18f); circle(7f, 18f, 2f); line(9.5f, 18f, 20f, 18f)
        }
        QIcon.Shield -> {
            path {
                moveTo(12f, 3f); lineTo(20f, 6f); lineTo(20f, 11f)
                cubicTo(20f, 16f, 16.6f, 19.4f, 12f, 21f)
                cubicTo(7.4f, 19.4f, 4f, 16f, 4f, 11f)
                lineTo(4f, 6f); close()
            }
            path { moveTo(8.5f, 12f); lineTo(10.8f, 14.3f); lineTo(15.6f, 9.5f) }
        }
        QIcon.Copy -> {
            roundRect(8f, 8f, 11f, 11f, 2f)
            path {
                moveTo(16f, 8f); lineTo(16f, 5f)
                cubicTo(16f, 4.4f, 15.6f, 4f, 15f, 4f)
                lineTo(5f, 4f)
                cubicTo(4.4f, 4f, 4f, 4.4f, 4f, 5f)
                lineTo(4f, 15f)
                cubicTo(4f, 15.6f, 4.4f, 16f, 5f, 16f)
                lineTo(8f, 16f)
            }
        }
        QIcon.Edit -> {
            path {
                moveTo(4f, 20f); lineTo(8f, 20f); lineTo(18f, 10f); lineTo(14f, 6f); lineTo(4f, 16f); close()
            }
            line(13.5f, 6.5f, 17.5f, 10.5f)
        }
        QIcon.Trash -> {
            line(4f, 7f, 20f, 7f)
            path { moveTo(9f, 7f); lineTo(9f, 4f); lineTo(15f, 4f); lineTo(15f, 7f) }
            path {
                moveTo(6.5f, 7f); lineTo(7.5f, 20f); lineTo(16.5f, 20f); lineTo(17.5f, 7f)
            }
            line(10f, 11f, 10f, 16f)
            line(14f, 11f, 14f, 16f)
        }
        QIcon.Eye -> {
            path {
                moveTo(2.5f, 12f)
                cubicTo(6f, 6f, 18f, 6f, 21.5f, 12f)
                cubicTo(18f, 18f, 6f, 18f, 2.5f, 12f); close()
            }
            circle(12f, 12f, 2.5f)
        }
        QIcon.EyeOff -> {
            line(3f, 3f, 21f, 21f)
            path {
                moveTo(10.6f, 6.2f)
                cubicTo(11f, 6.1f, 11.5f, 6f, 12f, 6f)
                cubicTo(18f, 6f, 21.5f, 12f, 21.5f, 12f)
                cubicTo(20.9f, 13f, 20.1f, 13.9f, 19.2f, 14.7f)
            }
            path {
                moveTo(6.2f, 6.4f)
                cubicTo(3.8f, 8.2f, 2.5f, 12f, 2.5f, 12f)
                cubicTo(6f, 18f, 18f, 18f, 21.5f, 12f)
            }
            path {
                moveTo(9.9f, 9.9f)
                cubicTo(8.9f, 10.9f, 8.9f, 12.5f, 10.1f, 13.6f)
                cubicTo(11.2f, 14.7f, 12.9f, 14.7f, 13.9f, 13.7f)
            }
        }
        QIcon.Key -> {
            circle(8f, 8f, 4f)
            path { moveTo(11f, 11f); lineTo(20f, 20f) }
            line(17f, 17f, 19f, 15f)
            line(15f, 15f, 17f, 13f)
        }
        QIcon.Globe -> {
            circle(12f, 12f, 9f)
            line(3f, 12f, 21f, 12f)
            path {
                moveTo(12f, 3f)
                cubicTo(8f, 6f, 8f, 18f, 12f, 21f)
                cubicTo(16f, 18f, 16f, 6f, 12f, 3f); close()
            }
        }
        QIcon.ArrowUpRight -> {
            line(7f, 17f, 17f, 7f)
            path { moveTo(9f, 7f); lineTo(17f, 7f); lineTo(17f, 15f) }
        }
        QIcon.Close -> {
            line(6f, 6f, 18f, 18f)
            line(18f, 6f, 6f, 18f)
        }
        QIcon.Check -> path { moveTo(5f, 12.5f); lineTo(10f, 17.5f); lineTo(19f, 7f) }
        QIcon.Database -> {
            drawOval(tint, Offset(5f, 3f), Size(14f, 5f), style = stroke)
            path {
                moveTo(5f, 5.5f); lineTo(5f, 11.5f)
                cubicTo(5f, 13f, 8.1f, 14f, 12f, 14f)
                cubicTo(15.9f, 14f, 19f, 13f, 19f, 11.5f)
                lineTo(19f, 5.5f)
            }
            path {
                moveTo(5f, 11.5f); lineTo(5f, 17.5f)
                cubicTo(5f, 19f, 8.1f, 20f, 12f, 20f)
                cubicTo(15.9f, 20f, 19f, 19f, 19f, 17.5f)
                lineTo(19f, 11.5f)
            }
        }
        QIcon.Fingerprint -> {
            path { moveTo(4f, 12f); cubicTo(4f, 7.6f, 7.6f, 4f, 12f, 4f); cubicTo(16.4f, 4f, 20f, 7.6f, 20f, 12f); lineTo(20f, 16f) }
            path { moveTo(7f, 15f); lineTo(7f, 12f); cubicTo(7f, 9.2f, 9.2f, 7f, 12f, 7f); cubicTo(14.8f, 7f, 17f, 9.2f, 17f, 12f); lineTo(17f, 17f) }
            path { moveTo(10f, 20f); lineTo(10f, 13f); cubicTo(10f, 11.9f, 10.9f, 11f, 12f, 11f); cubicTo(13.1f, 11f, 14f, 11.9f, 14f, 13f); lineTo(14f, 18f) }
        }
        QIcon.ChevronDown -> path { moveTo(6f, 9f); lineTo(12f, 15f); lineTo(18f, 9f) }
        QIcon.Folder -> path {
            moveTo(3f, 7f)
            cubicTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            lineTo(9f, 5f); lineTo(11f, 7f); lineTo(19f, 7f)
            cubicTo(20.1f, 7f, 21f, 7.9f, 21f, 9f)
            lineTo(21f, 17f)
            cubicTo(21f, 18.1f, 20.1f, 19f, 19f, 19f)
            lineTo(5f, 19f)
            cubicTo(3.9f, 19f, 3f, 18.1f, 3f, 17f); close()
        }
        QIcon.Plug -> {
            line(9f, 3f, 9f, 8f)
            line(15f, 3f, 15f, 8f)
            path {
                moveTo(6f, 8f); lineTo(18f, 8f); lineTo(18f, 11f)
                cubicTo(18f, 14.3f, 15.3f, 17f, 12f, 17f)
                cubicTo(8.7f, 17f, 6f, 14.3f, 6f, 11f); close()
            }
            line(12f, 17f, 12f, 21f)
        }
        QIcon.Sun -> {
            circle(12f, 12f, 4f)
            for (i in 0 until 8) {
                val a = Math.PI * i / 4.0
                val c = kotlin.math.cos(a).toFloat()
                val s = kotlin.math.sin(a).toFloat()
                line(12f + c * 6.6f, 12f + s * 6.6f, 12f + c * 8.8f, 12f + s * 8.8f)
            }
        }
        QIcon.Moon -> path {
            moveTo(20f, 14f)
            cubicTo(18.7f, 18.4f, 14.1f, 20.9f, 9.7f, 19.6f)
            cubicTo(5.3f, 18.3f, 2.8f, 13.7f, 4.1f, 9.3f)
            cubicTo(4.8f, 7f, 6.4f, 5.2f, 8.4f, 4.2f)
            cubicTo(6.9f, 7.5f, 7.4f, 11.5f, 9.8f, 14.2f)
            cubicTo(12f, 16.5f, 15.4f, 17.3f, 20f, 14f)
            close()
        }
        QIcon.Monitor -> {
            roundRect(3f, 4f, 18f, 12f, 2f)
            line(8f, 20f, 16f, 20f)
            line(12f, 16f, 12f, 20f)
        }
        QIcon.Sparkle -> path {
            moveTo(12f, 3f); lineTo(13.7f, 9.3f); lineTo(20f, 11f); lineTo(13.7f, 12.7f)
            lineTo(12f, 19f); lineTo(10.3f, 12.7f); lineTo(4f, 11f); lineTo(10.3f, 9.3f); close()
        }
        QIcon.Info -> {
            circle(12f, 12f, 9f)
            line(12f, 11f, 12f, 16f)
            drawCircle(tint, 1f, Offset(12f, 8f))
        }
        QIcon.External -> {
            path { moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f) }
            line(20f, 4f, 11f, 13f)
            path {
                moveTo(18f, 14f); lineTo(18f, 19f)
                cubicTo(18f, 19.6f, 17.6f, 20f, 17f, 20f)
                lineTo(5f, 20f)
                cubicTo(4.4f, 20f, 4f, 19.6f, 4f, 19f)
                lineTo(4f, 7f)
                cubicTo(4f, 6.4f, 4.4f, 6f, 5f, 6f)
                lineTo(10f, 6f)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Buttons
// ---------------------------------------------------------------------------

enum class QuietButtonStyle { Primary, Secondary, Quiet, Danger }

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: QuietButtonStyle = QuietButtonStyle.Secondary,
    icon: QIcon? = null,
    enabled: Boolean = true,
    height: Dp = 42.dp,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(10.dp)
    val (container, content, borderColor) = when (style) {
        QuietButtonStyle.Primary -> Triple(quiet.accent, quiet.accentContrast, Color.Transparent)
        QuietButtonStyle.Secondary -> Triple(
            if (hovered) quiet.surface2 else quiet.surface,
            quiet.text,
            quiet.borderStrong,
        )
        QuietButtonStyle.Quiet -> Triple(
            if (hovered) quiet.surface2 else Color.Transparent,
            quiet.muted,
            Color.Transparent,
        )
        QuietButtonStyle.Danger -> Triple(
            if (hovered) quiet.danger.copy(alpha = 0.14f) else Color.Transparent,
            quiet.danger,
            quiet.danger.copy(alpha = 0.5f),
        )
    }
    val alpha = if (enabled) 1f else 0.4f
    Row(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(container.copy(alpha = container.alpha * alpha))
            .border(1.dp, borderColor.copy(alpha = borderColor.alpha * alpha), shape)
            .hoverable(interaction, enabled = enabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            QIcon(icon, Modifier.size(16.dp), content.copy(alpha = alpha))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            color = content.copy(alpha = alpha),
            style = QuietType.button,
            maxLines = 1,
        )
    }
}

@Composable
fun QuietIconButton(
    icon: QIcon,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 32.dp,
    iconSize: Dp = 16.dp,
    enabled: Boolean = true,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered) quiet.surface2 else Color.Transparent)
            .hoverable(interaction, enabled = enabled)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        QIcon(icon, Modifier.size(iconSize), (tint ?: quiet.muted).copy(alpha = if (enabled) 1f else 0.4f))
    }
}

// ---------------------------------------------------------------------------
// Fields
// ---------------------------------------------------------------------------

@Composable
fun QuietField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    password: Boolean = false,
    masked: Boolean = password,
    singleLine: Boolean = true,
    minHeight: Dp = 46.dp,
    leading: QIcon? = null,
    trailing: (@Composable () -> Unit)? = null,
    focusRequester: FocusRequester? = null,
    onEnter: (() -> Unit)? = null,
    textStyle: TextStyle = QuietType.body,
) {
    val quiet = LocalQuiet.current
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(11.dp)

    Column(modifier) {
        if (label != null) {
            Text(
                label,
                color = quiet.muted,
                style = QuietType.label,
                modifier = Modifier.padding(bottom = 7.dp),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .clip(shape)
                .background(quiet.surface.copy(alpha = if (enabled) 1f else 0.5f))
                .border(
                    1.dp,
                    when {
                        !enabled -> quiet.border
                        focused -> quiet.accent
                        else -> quiet.border
                    },
                    shape,
                )
                .padding(horizontal = 12.dp, vertical = if (singleLine) 0.dp else 10.dp),
            verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
        ) {
            if (leading != null) {
                QIcon(leading, Modifier.size(16.dp), quiet.muted)
                Spacer(Modifier.width(9.dp))
            }
            Box(Modifier.weight(1f)) {
                if (value.isEmpty() && placeholder != null) {
                    Text(
                        placeholder,
                        color = quiet.faint,
                        style = textStyle,
                        maxLines = if (singleLine) 1 else 2,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (singleLine) Modifier else Modifier.heightIn(min = 72.dp))
                        .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                        .onFocusChanged { focused = it.isFocused }
                        .onPreviewKeyEvent { event ->
                            if (
                                onEnter != null &&
                                event.type == KeyEventType.KeyDown &&
                                (event.key == Key.Enter || event.key == Key.NumPadEnter)
                            ) {
                                onEnter()
                                true
                            } else {
                                false
                            }
                        },
                    enabled = enabled,
                    singleLine = singleLine,
                    textStyle = textStyle.copy(color = quiet.text),
                    cursorBrush = SolidColor(quiet.accent),
                    visualTransformation = if (masked) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (password) KeyboardType.Password else KeyboardType.Text,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onEnter?.invoke() }),
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                trailing()
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Sheet / dialog / notices
// ---------------------------------------------------------------------------

@Composable
fun QuietSheet(
    visible: Boolean,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    width: Dp = 430.dp,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val quiet = LocalQuiet.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(140)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.46f))
                    .then(
                        if (onDismiss != null) {
                            Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onDismiss,
                            )
                        } else {
                            Modifier
                        },
                    ),
            )
        }
        AnimatedVisibility(
            visible = visible,
            enter = slideInHorizontally(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(160)),
        ) {
            val targetWidth = if (maxWidth < 760.dp) maxWidth else width.coerceAtMost(maxWidth * 0.9f)
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(targetWidth)
                        .shadow(28.dp)
                        .background(quiet.surface)
                        .border(1.dp, quiet.border)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 26.dp, vertical = 26.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SheetHeader(
    eyebrow: String,
    title: String,
    onClose: (() -> Unit)?,
    subtitle: String? = null,
) {
    val quiet = LocalQuiet.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 22.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), color = quiet.faint, style = QuietType.eyebrow)
            Spacer(Modifier.height(10.dp))
            Text(title, color = quiet.text, style = QuietType.sheetTitle)
            if (subtitle != null) {
                Spacer(Modifier.height(6.dp))
                Text(subtitle, color = quiet.muted, style = QuietType.small)
            }
        }
        if (onClose != null) {
            QuietIconButton(QIcon.Close, onClose, Modifier.padding(top = 2.dp))
        }
    }
}

@Composable
fun QuietDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    danger: Boolean = false,
) {
    if (!visible) return
    val quiet = LocalQuiet.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(0.42f)
                .shadow(30.dp, RoundedCornerShape(18.dp))
                .background(quiet.surface)
                .border(1.dp, quiet.border, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(24.dp),
        ) {
            Text(title, color = quiet.text, style = QuietType.dialogTitle)
            Spacer(Modifier.height(10.dp))
            Text(message, color = quiet.muted, style = QuietType.body)
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                QuietButton("取消", onDismiss, style = QuietButtonStyle.Quiet)
                QuietButton(
                    confirmLabel,
                    onConfirm,
                    style = if (danger) QuietButtonStyle.Danger else QuietButtonStyle.Primary,
                )
            }
        }
    }
}

@Composable
fun QuietNotice(
    text: String?,
    tone: NoticeTone = NoticeTone.Error,
    modifier: Modifier = Modifier,
) {
    if (text.isNullOrBlank()) return
    val quiet = LocalQuiet.current
    val color = when (tone) {
        NoticeTone.Error -> quiet.danger
        NoticeTone.Success -> quiet.success
        NoticeTone.Info -> quiet.accent
    }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        QIcon(
            if (tone == NoticeTone.Error) QIcon.Info else QIcon.Check,
            Modifier.size(15.dp),
            color,
        )
        Text(text, color = color, style = QuietType.small)
    }
}

enum class NoticeTone { Error, Success, Info }

@Composable
fun QuietToast(text: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = !text.isNullOrBlank(),
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(140)),
        modifier = modifier,
    ) {
        val quiet = LocalQuiet.current
        Row(
            Modifier
                .shadow(18.dp, RoundedCornerShape(999.dp))
                .background(quiet.surface)
                .border(1.dp, quiet.borderStrong, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(quiet.accent))
            Text(text.orEmpty(), color = quiet.text, style = QuietType.small)
        }
    }
}

// ---------------------------------------------------------------------------
// Misc
// ---------------------------------------------------------------------------

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalQuiet.current.border),
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = LocalQuiet.current.text,
        style = QuietType.section,
    )
}

@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text.uppercase(),
        modifier = modifier,
        color = color ?: LocalQuiet.current.faint,
        style = QuietType.eyebrow,
    )
}

@Composable
fun BrandMark(size: Dp = 40.dp, iconSize: Dp = 20.dp) {
    val quiet = LocalQuiet.current
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(Brush.linearGradient(listOf(quiet.accent, quiet.accentStrong))),
        contentAlignment = Alignment.Center,
    ) {
        QIcon(QIcon.Lock, Modifier.size(iconSize), quiet.accentContrast, strokeWidth = 1.8f)
    }
}

@Composable
fun QuietChip(text: String, dot: Boolean = false) {
    val quiet = LocalQuiet.current
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(quiet.surface.copy(alpha = 0.6f))
            .border(1.dp, quiet.border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (dot) {
            Box(Modifier.size(5.dp).clip(CircleShape).background(quiet.success))
        }
        Text(text, color = quiet.muted, style = QuietType.monoSmall)
    }
}

@Composable
fun ThemeModeSelector(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit) {
    val quiet = LocalQuiet.current
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        ThemeMode.entries.forEach { mode ->
            val selected = settings.themeMode == mode
            Column(
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSettingsChange(settings.copy(themeMode = mode)) }
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    when (mode) {
                        ThemeMode.SYSTEM -> "跟随系统"
                        ThemeMode.LIGHT -> "浅色"
                        ThemeMode.DARK -> "深色"
                    },
                    color = if (selected) quiet.accent else quiet.muted,
                    style = QuietType.body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(if (selected) quiet.accent else Color.Transparent),
                )
            }
        }
    }
}

@Composable
fun BackgroundStyleSelector(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit) {
    val quiet = LocalQuiet.current
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        BackgroundStyle.entries.forEach { style ->
            val selected = settings.backgroundStyle == style
            Column(
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSettingsChange(settings.copy(backgroundStyle = style)) }
                    .padding(vertical = 2.dp),
            ) {
                Text(
                    when (style) {
                        BackgroundStyle.SOLID -> "纯色"
                        BackgroundStyle.GRADIENT -> "渐变"
                        BackgroundStyle.GLOW -> "光晕"
                    },
                    color = if (selected) quiet.accent else quiet.muted,
                    style = QuietType.body,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .width(22.dp)
                        .height(2.dp)
                        .background(if (selected) quiet.accent else Color.Transparent),
                )
            }
        }
    }
}

@Composable
fun AccentSwatches(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    swatchSize: Dp = 17.dp,
) {
    val quiet = LocalQuiet.current
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
        AccentPreset.entries.forEach { preset ->
            val selected = settings.accent == preset
            val color = accentColors(preset, dark = quiet.dark).accent
            Box(
                Modifier
                    .size(swatchSize)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) quiet.text else Color(0x2B000000),
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSettingsChange(settings.copy(accent = preset)) },
            )
        }
    }
}

@Composable
fun ThemeToggle(settings: AppSettings, onSettingsChange: (AppSettings) -> Unit) {
    val next = when (settings.themeMode) {
        ThemeMode.DARK -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.SYSTEM
        ThemeMode.SYSTEM -> ThemeMode.DARK
    }
    val icon = when (settings.themeMode) {
        ThemeMode.DARK -> QIcon.Moon
        ThemeMode.LIGHT -> QIcon.Sun
        ThemeMode.SYSTEM -> QIcon.Monitor
    }
    QuietIconButton(
        icon,
        onClick = { onSettingsChange(settings.copy(themeMode = next)) },
        tint = LocalQuiet.current.muted,
    )
}

@Composable
fun QuietDividerLabel(text: String) {
    Text(
        text,
        color = LocalQuiet.current.faint,
        style = QuietType.monoSmall,
        modifier = Modifier.padding(vertical = 6.dp),
    )
}

@Composable
fun MetaText(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        color = LocalQuiet.current.faint,
        style = QuietType.monoSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
