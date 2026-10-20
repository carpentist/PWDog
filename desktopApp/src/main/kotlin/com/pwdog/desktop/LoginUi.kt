package com.pwdog.desktop

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pwdog.core.account.Account
import com.pwdog.desktop.design.AccentSwatches
import com.pwdog.desktop.design.BrandMark
import com.pwdog.desktop.design.Eyebrow
import com.pwdog.desktop.design.NoticeTone
import com.pwdog.desktop.design.QIcon
import com.pwdog.desktop.design.QuietButton
import com.pwdog.desktop.design.QuietButtonStyle
import com.pwdog.desktop.design.QuietChip
import com.pwdog.desktop.design.QuietField
import com.pwdog.desktop.design.QuietIconButton
import com.pwdog.desktop.design.QuietNotice
import com.pwdog.desktop.design.QuietSheet
import com.pwdog.desktop.design.SheetHeader
import com.pwdog.desktop.design.ThemeToggle
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.theme.LocalQuiet
import com.pwdog.desktop.theme.QuietType
import com.pwdog.desktop.ui.AccountAvatar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun LoginScreen(
    accounts: List<Account>,
    selectedId: String?,
    busy: Boolean,
    error: String?,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onSelect: (String) -> Unit,
    onCreateNew: () -> Unit,
    onUseRecoveryKey: () -> Unit,
    onQuickUnlock: (String) -> Unit,
    onUnlock: (String, String) -> Unit,
) {
    val quiet = LocalQuiet.current
    val selected = accounts.firstOrNull { it.id == selectedId }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val passwordFocus = remember { FocusRequester() }

    fun submit() {
        val id = selectedId ?: return
        if (busy || password.isEmpty()) return
        onUnlock(id, password)
        password = ""
    }

    LaunchedEffect(selectedId, busy) {
        if (!busy && selected != null) {
            kotlinx.coroutines.delay(60)
            runCatching { passwordFocus.requestFocus() }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 960.dp
        Row(Modifier.fillMaxSize()) {
            if (!compact) {
                BrandPanel(
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    modifier = Modifier.weight(1.08f).fillMaxHeight(),
                )
            }
            Box(
                Modifier
                    .weight(if (compact) 1f else 0.92f)
                    .fillMaxHeight()
                    .background(quiet.surface.copy(alpha = if (quiet.dark) 0.46f else 0.52f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 410.dp)
                        .fillMaxWidth()
                        .padding(horizontal = if (compact) 42.dp else 38.dp, vertical = 34.dp),
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column {
                            Eyebrow("LOCAL VAULT")
                            Spacer(Modifier.height(11.dp))
                            Text("选择账户", color = quiet.text, style = QuietType.display)
                        }
                        ThemeToggle(settings, onSettingsChange)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "每个账户都有独立的主密码和独立加密库。",
                        color = quiet.muted,
                        style = QuietType.small,
                    )
                    Spacer(Modifier.height(22.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        accounts.forEach { account ->
                            AccountCard(
                                account = account,
                                selected = account.id == selectedId,
                                onClick = {
                                    if (!busy) {
                                        onSelect(account.id)
                                        password = ""
                                        showPassword = false
                                    }
                                },
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text(
                        "${selected?.displayName ?: "账户"} 的主密码",
                        color = quiet.muted,
                        style = QuietType.label,
                        modifier = Modifier.padding(bottom = 7.dp),
                    )
                    QuietField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "输入主密码",
                        password = true,
                        masked = !showPassword,
                        leading = QIcon.Lock,
                        trailing = {
                            QuietIconButton(
                                if (showPassword) QIcon.EyeOff else QIcon.Eye,
                                onClick = { showPassword = !showPassword },
                                size = 28.dp,
                                iconSize = 15.dp,
                                enabled = !busy,
                            )
                        },
                        focusRequester = passwordFocus,
                        onEnter = { submit() },
                        enabled = !busy && selected != null,
                        minHeight = 48.dp,
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("按 Enter 直接解锁 · 登录后保持解锁", color = quiet.faint, style = QuietType.tiny)
                    Spacer(Modifier.height(14.dp))

                    QuietButton(
                        text = if (busy) "正在解锁..." else "解锁密码库",
                        onClick = { submit() },
                        modifier = Modifier.fillMaxWidth(),
                        style = QuietButtonStyle.Primary,
                        icon = if (busy) null else QIcon.Key,
                        enabled = !busy && selectedId != null && password.isNotEmpty(),
                        height = 48.dp,
                    )

                    if (selected != null && selected.quickUnlockBlob != null) {
                        Spacer(Modifier.height(9.dp))
                        QuietButton(
                            text = "使用 Windows Hello 解锁",
                            onClick = {
                                password = ""
                                onQuickUnlock(selected.id)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            icon = QIcon.Fingerprint,
                            enabled = !busy,
                        )
                    }

                    Spacer(Modifier.height(15.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            if (selected != null && selected.keyVersion >= 2) {
                                TextLink("使用恢复密钥", enabled = !busy, onClick = onUseRecoveryKey)
                            }
                            TextLink("创建本地账户", enabled = !busy, onClick = onCreateNew)
                        }
                    }

                    if (busy) {
                        Spacer(Modifier.height(18.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = quiet.accent,
                            trackColor = quiet.border,
                        )
                    }

                    if (error != null) {
                        Spacer(Modifier.height(16.dp))
                        QuietNotice(error, NoticeTone.Error)
                    }

                    Spacer(Modifier.height(22.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        QIcon(QIcon.Shield, Modifier.size(14.dp), quiet.success)
                        Text(
                            "主密码只用于本机解锁，数据不会离开这台电脑。",
                            color = quiet.faint,
                            style = QuietType.tiny,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandPanel(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    val quiet = LocalQuiet.current
    BoxWithConstraints(
        modifier
            .drawBehind {
                drawCircle(
                    Brush.radialGradient(
                        colors = listOf(quiet.accent.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(size.width * 0.18f, size.height * 0.12f),
                        radius = size.maxDimension * 0.62f,
                    ),
                    radius = size.maxDimension * 0.62f,
                    center = Offset(size.width * 0.18f, size.height * 0.12f),
                )
            }
    ) {
        val tight = maxHeight < 760.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 58.dp, vertical = 42.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                BrandMark(40.dp, 20.dp)
                Spacer(Modifier.width(12.dp))
                Text("PWDog", color = quiet.text, style = QuietType.heading.copy(fontSize = 26.sp, lineHeight = 28.sp))
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .border(1.dp, quiet.border, RoundedCornerShape(999.dp))
                        .background(quiet.surface.copy(alpha = 0.55f))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                ) {
                    Text("LOCAL VAULT", color = quiet.faint, style = QuietType.monoSmall.copy(letterSpacing = 0.08.em))
                }
            }

            Spacer(Modifier.height(if (tight) 26.dp else 44.dp))
            Eyebrow("LOCAL / ENCRYPTED")
            Spacer(Modifier.height(12.dp))
            Text(
                "一个安静、克制的本地密码库。多个独立账户、加密存储、浏览器自动填充，数据不离开这台机器。",
                color = quiet.muted,
                style = QuietType.body.copy(fontSize = 14.sp, lineHeight = 23.sp),
                modifier = Modifier.widthIn(max = 430.dp),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietChip("AES-256 加密", dot = true)
                QuietChip("多账户隔离")
                QuietChip("Windows Hello")
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                OrbitVisual(
                    Modifier
                        .size(if (tight) 280.dp else 360.dp)
                        .aspectRatio(1f),
                )
            }

            Spacer(Modifier.height(18.dp))
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(top = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(quiet.success),
                        )
                        Text("离线运行 · 数据不离开本机", color = quiet.muted, style = QuietType.tiny)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("主题色", color = quiet.faint, style = QuietType.monoSmall.copy(letterSpacing = 0.12.em))
                        Spacer(Modifier.height(6.dp))
                        AccentSwatches(settings, onSettingsChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrbitVisual(modifier: Modifier = Modifier) {
    val quiet = LocalQuiet.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension
            drawCircle(quiet.accent.copy(alpha = 0.30f), r * 0.49f, style = Stroke(1f))
            drawCircle(quiet.borderStrong, r * 0.38f, style = Stroke(1f))
            drawCircle(quiet.accent.copy(alpha = 0.20f), r * 0.27f, style = Stroke(1f))
            drawCircle(quiet.accent, r * 0.013f, Offset(r * 0.01f, r * 0.5f))
            drawCircle(quiet.accent.copy(alpha = 0.55f), r * 0.012f, Offset(r * 0.69f, r * 0.19f))
            drawCircle(quiet.accent.copy(alpha = 0.45f), r * 0.010f, Offset(r * 0.34f, r * 0.94f))
            drawCircle(
                Brush.radialGradient(
                    colors = listOf(quiet.accent.copy(alpha = 0.16f), Color.Transparent),
                    center = center,
                    radius = r * 0.42f,
                ),
                radius = r * 0.42f,
                center = center,
            )
        }
        Box(
            Modifier
                .fillMaxSize(0.36f)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(quiet.accent, quiet.accentStrong))),
            contentAlignment = Alignment.Center,
        ) {
            QIcon(
                QIcon.Lock,
                Modifier.fillMaxSize(0.34f),
                quiet.accentContrast,
                strokeWidth = 1.5f,
            )
        }
        FloatTag("AES-256 · GCM", Modifier.align(Alignment.TopEnd).offset(x = (-8).dp, y = 54.dp))
        FloatTag("ARGON2ID · 本机", Modifier.align(Alignment.BottomStart).offset(x = 4.dp, y = (-56).dp))
        FloatTag("SQLCIPHER · WAL", Modifier.align(Alignment.BottomEnd).offset(x = (-26).dp, y = (-12).dp))
    }
}

@Composable
private fun FloatTag(text: String, modifier: Modifier) {
    val quiet = LocalQuiet.current
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(quiet.surface.copy(alpha = 0.78f))
            .border(1.dp, quiet.border, RoundedCornerShape(9.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = quiet.muted,
        style = QuietType.monoSmall,
    )
}

@Composable
private fun AccountCard(
    account: Account,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) quiet.accentSoft else quiet.surface.copy(alpha = if (hovered) 0.95f else 0.74f))
            .border(1.dp, if (selected) quiet.accent else if (hovered) quiet.borderStrong else quiet.border, shape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        AccountAvatar(account, 38.dp)
        Column(Modifier.weight(1f)) {
            Text(
                account.displayName,
                color = quiet.text,
                style = QuietType.entryTitle.copy(fontSize = 13.sp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                accountCaption(account),
                color = quiet.muted,
                style = QuietType.tiny,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            QIcon(QIcon.Check, Modifier.size(15.dp), quiet.accent)
        }
    }
}

@Composable
private fun TextLink(text: String, enabled: Boolean, onClick: () -> Unit) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Text(
        text,
        modifier = Modifier
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        color = when {
            !enabled -> quiet.faint
            hovered -> quiet.accentStrong
            else -> quiet.muted
        },
        style = QuietType.link,
    )
}

// ---------------------------------------------------------------------------
// Account create / recover sheets
// ---------------------------------------------------------------------------

@Composable
internal fun CreateAccountSheet(
    visible: Boolean,
    canGoBack: Boolean,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit,
) {
    val quiet = LocalQuiet.current
    var displayName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            displayName = ""
            password = ""
            confirmation = ""
        }
    }

    QuietSheet(
        visible = visible,
        onDismiss = if (busy || !canGoBack) null else onDismiss,
    ) {
        SheetHeader(
            eyebrow = "NEW LOCAL ACCOUNT",
            title = "创建本地账户",
            subtitle = "账户只保存在这台电脑上。主密码无法找回，请妥善保存。",
            onClose = if (busy || !canGoBack) null else onDismiss,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietField(displayName, { displayName = it }, label = "账户名称", placeholder = "例如 工作账户", enabled = !busy)
            QuietField(
                password,
                { password = it },
                label = "主密码（至少 8 位）",
                placeholder = "输入主密码",
                password = true,
                enabled = !busy,
            )
            QuietField(
                confirmation,
                { confirmation = it },
                label = "确认主密码",
                placeholder = "再次输入主密码",
                password = true,
                enabled = !busy,
                onEnter = { if (!busy) onCreate(displayName, password, confirmation) },
            )
            QuietNotice(
                "创建后会显示一次恢复密钥。它可以在忘记主密码时重置密码。",
                NoticeTone.Info,
            )
            error?.let { QuietNotice(it, NoticeTone.Error) }
            if (busy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
                    color = quiet.accent,
                    trackColor = quiet.border,
                )
                Text("正在创建加密库...", color = quiet.muted, style = QuietType.small)
            }
            Spacer(Modifier.height(4.dp))
            QuietButton(
                "创建并解锁",
                { onCreate(displayName, password, confirmation) },
                Modifier.fillMaxWidth(),
                style = QuietButtonStyle.Primary,
                enabled = !busy,
                height = 46.dp,
            )
            if (canGoBack) {
                QuietButton(
                    "取消",
                    onDismiss,
                    Modifier.fillMaxWidth(),
                    style = QuietButtonStyle.Quiet,
                    enabled = !busy,
                )
            }
        }
    }
}

@Composable
internal fun RecoverAccountSheet(
    visible: Boolean,
    account: Account?,
    busy: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRecover: (String, String, String) -> Unit,
) {
    val quiet = LocalQuiet.current
    var recoveryKey by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }

    LaunchedEffect(visible, account?.id) {
        if (visible) {
            recoveryKey = ""
            newPassword = ""
            confirmation = ""
        }
    }

    QuietSheet(visible = visible, onDismiss = if (busy) null else onDismiss) {
        SheetHeader(
            eyebrow = "RECOVERY KEY",
            title = "使用恢复密钥",
            subtitle = account?.let { "账户：${it.displayName}" } ?: "重置主密码并解锁密码库。",
            onClose = if (busy) null else onDismiss,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietField(
                recoveryKey,
                { recoveryKey = it },
                label = "恢复密钥",
                placeholder = "XXXX-XXXX-XXXX-...",
                enabled = !busy,
                onEnter = {},
            )
            QuietField(
                newPassword,
                { newPassword = it },
                label = "新主密码（至少 8 位）",
                placeholder = "输入新主密码",
                password = true,
                enabled = !busy,
            )
            QuietField(
                confirmation,
                { confirmation = it },
                label = "确认新主密码",
                placeholder = "再次输入新主密码",
                password = true,
                enabled = !busy,
                onEnter = { if (!busy) onRecover(recoveryKey, newPassword, confirmation) },
            )
            QuietNotice("重置后旧主密码立即失效，恢复密钥仍然有效。", NoticeTone.Info)
            error?.let { QuietNotice(it, NoticeTone.Error) }
            Spacer(Modifier.height(4.dp))
            QuietButton(
                "重置密码并解锁",
                { onRecover(recoveryKey, newPassword, confirmation) },
                Modifier.fillMaxWidth(),
                style = QuietButtonStyle.Primary,
                enabled = !busy && recoveryKey.isNotBlank() && newPassword.isNotEmpty(),
                height = 46.dp,
            )
            QuietButton("取消", onDismiss, Modifier.fillMaxWidth(), style = QuietButtonStyle.Quiet, enabled = !busy)
        }
    }
}

@Composable
internal fun RecoveryKeySheet(
    visible: Boolean,
    accountName: String,
    recoveryKey: String,
    onContinue: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var confirmed by remember { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (visible) {
            copied = false
            confirmed = false
        }
    }
    LaunchedEffect(copied) {
        if (copied) {
            kotlinx.coroutines.delay(3_000)
            copied = false
        }
    }

    QuietSheet(visible = visible, onDismiss = null) {
        SheetHeader(
            eyebrow = "RECOVERY KEY",
            title = "保存恢复密钥",
            subtitle = "账户 $accountName 已创建。这串密钥只显示一次。",
            onClose = null,
        )
        QuietNotice(
            "请把恢复密钥保存到离线、安全的位置。丢失主密码又丢失恢复密钥后，账户数据无法恢复。",
            NoticeTone.Info,
        )
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(quiet.surface2)
                .border(1.dp, quiet.borderStrong, RoundedCornerShape(11.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                recoveryKey,
                color = quiet.text,
                style = QuietType.body.copy(fontFamily = QuietType.monoSmall.fontFamily, fontSize = 11.5.sp),
                modifier = Modifier.weight(1f),
            )
            QuietIconButton(
                QIcon.Copy,
                onClick = {
                    PasswordClipboard.copy(scope, recoveryKey)
                    copied = true
                },
                tint = quiet.accent,
            )
        }
        if (copied) {
            Spacer(Modifier.height(6.dp))
            Text("已复制，15 秒后自动清除", color = quiet.success, style = QuietType.tiny)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { confirmed = !confirmed }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(if (confirmed) quiet.accent else Color.Transparent)
                    .border(1.dp, if (confirmed) quiet.accent else quiet.borderStrong, RoundedCornerShape(5.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (confirmed) {
                    QIcon(QIcon.Check, Modifier.size(12.dp), quiet.accentContrast, strokeWidth = 2.2f)
                }
            }
            Text("我已将恢复密钥保存到安全位置", color = quiet.muted, style = QuietType.small)
        }
        Spacer(Modifier.height(12.dp))
        QuietButton(
            "进入密码库",
            onContinue,
            Modifier.fillMaxWidth(),
            style = QuietButtonStyle.Primary,
            enabled = confirmed,
            height = 46.dp,
        )
    }
}

internal fun accountCaption(account: Account): String {
    val last = account.lastUsedAt ?: return "本地账户 · 尚未使用"
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(last).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    val relative = when (date) {
        today -> "今天"
        today.minusDays(1) -> "昨天"
        else -> date.format(DateTimeFormatter.ofPattern("M 月 d 日"))
    }
    return "本地账户 · 最近使用 $relative"
}
