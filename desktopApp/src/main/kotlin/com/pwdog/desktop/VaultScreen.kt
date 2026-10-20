@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.pwdog.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pwdog.core.account.AccountService
import com.pwdog.core.account.AppPaths
import com.pwdog.core.account.VaultSession
import com.pwdog.core.entry.BrowserPasswordImporter
import com.pwdog.core.entry.EntryGroup
import com.pwdog.core.entry.EntryGrouping
import com.pwdog.core.entry.EntryRepository
import com.pwdog.core.entry.PasswordGenerator
import com.pwdog.core.entry.PasswordImportPreview
import com.pwdog.core.entry.VaultEntry
import com.pwdog.desktop.autofill.AutofillRuntime
import com.pwdog.desktop.design.AccentSwatches
import com.pwdog.desktop.design.BackgroundStyleSelector
import com.pwdog.desktop.design.BrandMark
import com.pwdog.desktop.design.Eyebrow
import com.pwdog.desktop.design.Hairline
import com.pwdog.desktop.design.MetaText
import com.pwdog.desktop.design.NoticeTone
import com.pwdog.desktop.design.QIcon
import com.pwdog.desktop.design.QuietButton
import com.pwdog.desktop.design.QuietButtonStyle
import com.pwdog.desktop.design.QuietChip
import com.pwdog.desktop.design.QuietDialog
import com.pwdog.desktop.design.QuietField
import com.pwdog.desktop.design.QuietIconButton
import com.pwdog.desktop.design.QuietNotice
import com.pwdog.desktop.design.QuietSheet
import com.pwdog.desktop.design.QuietToast
import com.pwdog.desktop.design.SectionLabel
import com.pwdog.desktop.design.SheetHeader
import com.pwdog.desktop.design.ThemeModeSelector
import com.pwdog.desktop.extension.ExtensionBundle
import com.pwdog.desktop.security.WindowsAutostart
import com.pwdog.desktop.security.WindowsQuickUnlock
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.settings.ThemeMode
import com.pwdog.desktop.theme.LocalQuiet
import com.pwdog.desktop.theme.QuietType
import com.pwdog.desktop.ui.AccountAvatar
import com.pwdog.desktop.ui.AVATAR_COLORS
import com.pwdog.desktop.ui.AvatarView
import com.pwdog.desktop.ui.AvatarPicker
import com.pwdog.desktop.ui.PasswordImportPicker
import com.pwdog.desktop.ui.defaultAvatarColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private sealed interface EditorState {
    data class New(
        val title: String = "",
        val url: String = "",
    ) : EditorState
    data class Existing(val entry: VaultEntry) : EditorState
}

private data class EntryDraft(
    val title: String,
    val url: String,
    val username: String,
    val password: String,
    val notes: String,
)

private enum class VaultPane { Entries, Settings }

@Composable
internal fun VaultScreen(
    session: VaultSession,
    service: AccountService,
    autofill: AutofillRuntime,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onDeleteAccount: (String) -> Unit,
    onLock: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val scope = rememberCoroutineScope()
    val repository = remember(session) { EntryRepository(session.connection) }
    var entries by remember(session) { mutableStateOf<List<VaultEntry>>(emptyList()) }
    var query by remember(session) { mutableStateOf("") }
    var loading by remember(session) { mutableStateOf(true) }
    var error by remember(session) { mutableStateOf<String?>(null) }
    var status by remember(session) { mutableStateOf<String?>(null) }
    var selectedEntryId by remember(session) { mutableStateOf<String?>(null) }
    var revealed by remember(session) { mutableStateOf(false) }
    var pane by remember(session) { mutableStateOf(VaultPane.Entries) }

    var editor by remember(session) { mutableStateOf<EditorState?>(null) }
    var editorError by remember(session) { mutableStateOf<String?>(null) }
    var saving by remember(session) { mutableStateOf(false) }
    var deleteTarget by remember(session) { mutableStateOf<VaultEntry?>(null) }
    var refreshToken by remember(session) { mutableStateOf(0) }

    var importPreview by remember(session) { mutableStateOf<PasswordImportPreview?>(null) }
    var importFileName by remember(session) { mutableStateOf("") }
    var importBusy by remember(session) { mutableStateOf(false) }
    var importError by remember(session) { mutableStateOf<String?>(null) }

    var settingsOpen by remember(session) { mutableStateOf(false) }
    var autofillOpen by remember(session) { mutableStateOf(false) }
    var passwordOpen by remember(session) { mutableStateOf(false) }
    var deleteAccountOpen by remember(session) { mutableStateOf(false) }

    val searchFocus = remember { FocusRequester() }
    val groups = remember(entries) { EntryGrouping.group(entries) }
    val allEntries = remember(entries) { entries }
    val selectedEntry = allEntries.firstOrNull { it.id == selectedEntryId }
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val lockNow = {
        PasswordClipboard.clearActive()
        onLock()
    }

    fun pickImportFile() {
        if (importBusy) return
        val file = PasswordImportPicker.choose(darkTheme) ?: return
        if (file.length() > 20L * 1024 * 1024) {
            importError = "导入文件不能超过 20 MB"
            return
        }

        importBusy = true
        importError = null
        scope.launch {
            try {
                val preview = withContext(Dispatchers.IO) {
                    BrowserPasswordImporter.parse(file.readText(Charsets.UTF_8))
                }
                importFileName = file.name
                importPreview = preview
            } catch (t: Throwable) {
                importError = friendlyError(t)
            } finally {
                importBusy = false
            }
        }
    }

    LaunchedEffect(session, query, refreshToken) {
        loading = true
        try {
            if (query.isNotBlank()) delay(120)
            entries = withContext(Dispatchers.IO) { repository.search(query) }
            error = null
        } catch (t: Throwable) {
            error = friendlyError(t)
        } finally {
            loading = false
        }
    }

    LaunchedEffect(entries) {
        if (entries.none { it.id == selectedEntryId }) {
            selectedEntryId = entries.firstOrNull()?.id
            revealed = false
        }
    }

    LaunchedEffect(status) {
        if (status != null) {
            delay(3_000)
            status = null
        }
    }

    fun openEntry(entry: VaultEntry) {
        selectedEntryId = entry.id
        revealed = false
    }

    fun copySecret(secret: String, what: String) {
        PasswordClipboard.copy(scope, secret)
        status = "${what}已复制，15 秒后自动清除"
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (event.isCtrlPressed && event.key == Key.K) {
                    searchFocus.requestFocus()
                    true
                } else if (event.isCtrlPressed && event.key == Key.L) {
                    lockNow()
                    true
                } else if (event.key == Key.Escape) {
                    when {
                        deleteTarget != null -> deleteTarget = null
                        deleteAccountOpen -> deleteAccountOpen = false
                        importPreview != null -> if (!importBusy) {
                            importPreview = null
                            importFileName = ""
                            importError = null
                        }
                        editor != null -> if (!saving) {
                            editor = null
                            editorError = null
                        }
                        passwordOpen -> passwordOpen = false
                        autofillOpen -> autofillOpen = false
                        settingsOpen -> {
                            settingsOpen = false
                            pane = VaultPane.Entries
                        }
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                } else {
                    false
                }
            },
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 1080.dp
            val sidebarWidth = if (maxWidth < 1180.dp) 212.dp else 236.dp
            Row(Modifier.fillMaxSize()) {
                Sidebar(
                    session = session,
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    entryCount = entries.size,
                    pane = pane,
                    autofillConnected = autofill.settings != null && autofill.error == null,
                    autofillError = autofill.error != null,
                    width = sidebarWidth,
                    onEntries = {
                        pane = VaultPane.Entries
                        settingsOpen = false
                    },
                    onSettings = {
                        pane = VaultPane.Settings
                        settingsOpen = true
                    },
                    onAutofill = { autofillOpen = true },
                    onLock = lockNow,
                )

                Column(Modifier.weight(1f).fillMaxHeight()) {
                    TopBar(
                        query = query,
                        onQueryChange = { query = it },
                        searchFocus = searchFocus,
                        importBusy = importBusy,
                        onImport = { pickImportFile() },
                        onNew = {
                            editor = EditorState.New()
                            editorError = null
                        },
                    )

                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .padding(top = 20.dp, bottom = 14.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom,
                        ) {
                            Column {
                                Eyebrow(if (query.isBlank()) "YOUR VAULT" else "SEARCH")
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    if (query.isBlank()) "全部密码" else "搜索结果",
                                    color = quiet.text,
                                    style = QuietType.pageTitle,
                                )
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    "${groups.size} 个网站 · ${entries.size} 个账户",
                                    color = quiet.muted,
                                    style = QuietType.tiny,
                                )
                            }
                            MetaText(if (loading) "正在检索..." else "本地 · 已解锁")
                        }
                    }

                    error?.let {
                        Box(Modifier.padding(horizontal = 28.dp).padding(bottom = 10.dp)) {
                            QuietNotice(it, NoticeTone.Error)
                        }
                    }

                    if (loading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = quiet.accent,
                            trackColor = Color.Transparent,
                        )
                    }

                    if (!loading && entries.isEmpty()) {
                        EmptyVault(
                            search = query.isNotBlank(),
                            onNew = {
                                editor = EditorState.New()
                                editorError = null
                            },
                            onImport = { pickImportFile() },
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                start = 28.dp,
                                end = 28.dp,
                                bottom = 30.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(groups, key = { it.key }) { group ->
                                SiteCard(
                                    group = group,
                                    selectedId = selectedEntryId,
                                    onSelect = { openEntry(it) },
                                    onCopy = { copySecret(it.password, "密码") },
                                    onEdit = {
                                        editor = EditorState.Existing(it)
                                        editorError = null
                                    },
                                    onDelete = { deleteTarget = it },
                                    onAddAccount = {
                                        editor = EditorState.New(url = group.urlTemplate)
                                        editorError = null
                                    },
                                )
                            }
                        }
                    }
                }

                if (wide) {
                    DetailPanel(
                        entry = selectedEntry,
                        revealed = revealed,
                        onReveal = { revealed = !revealed },
                        onCopy = { entry, what ->
                            copySecret(
                                if (what == "用户名") entry.username else entry.password,
                                what,
                            )
                        },
                        onEdit = {
                            editor = EditorState.Existing(it)
                            editorError = null
                        },
                        onOpenUrl = { entry ->
                            if (entry.url.isBlank()) {
                                status = "这个条目没有填写网址"
                            } else {
                                runCatching { Desktop.getDesktop().browse(URI(entry.url)) }
                                    .onFailure { status = "无法打开网址" }
                            }
                        },
                        width = 372.dp,
                    )
                }
            }
        }

        EditorSheet(
            visible = editor != null,
            state = editor,
            saving = saving,
            error = editorError,
            onClose = {
                if (!saving) {
                    editor = null
                    editorError = null
                }
            },
            onCopy = { secret -> copySecret(secret, "密码") },
            onDelete = { entry -> deleteTarget = entry },
            onSave = { draft ->
                val current = editor ?: return@EditorSheet
                saving = true
                editorError = null
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            when (current) {
                                is EditorState.New -> repository.create(
                                    title = draft.title,
                                    url = draft.url,
                                    username = draft.username,
                                    password = draft.password,
                                    notes = draft.notes,
                                )
                                is EditorState.Existing -> repository.update(
                                    id = current.entry.id,
                                    title = draft.title,
                                    url = draft.url,
                                    username = draft.username,
                                    password = draft.password,
                                    notes = draft.notes,
                                )
                            }
                        }
                        editor = null
                        editorError = null
                        refreshToken++
                        status = "密码条目已保存"
                    } catch (t: Throwable) {
                        editorError = friendlyError(t)
                    } finally {
                        saving = false
                    }
                }
            },
        )

        ImportSheet(
            visible = importPreview != null,
            preview = importPreview,
            fileName = importFileName,
            importing = importBusy,
            error = importError,
            onClose = {
                if (!importBusy) {
                    importPreview = null
                    importFileName = ""
                    importError = null
                }
            },
            onConfirm = {
                val preview = importPreview ?: return@ImportSheet
                importBusy = true
                importError = null
                scope.launch {
                    try {
                        val summary = withContext(Dispatchers.IO) {
                            repository.importAll(preview.credentials)
                        }
                        importPreview = null
                        importFileName = ""
                        refreshToken++
                        status = if (summary.duplicates > 0) {
                            "已导入 ${summary.imported} 条，跳过 ${summary.duplicates} 条重复"
                        } else {
                            "已导入 ${summary.imported} 条"
                        }
                    } catch (t: Throwable) {
                        importError = friendlyError(t)
                    } finally {
                        importBusy = false
                    }
                }
            },
        )

        SettingsSheet(
            visible = settingsOpen,
            session = session,
            service = service,
            settings = settings,
            onSettingsChange = onSettingsChange,
            onClose = {
                settingsOpen = false
                pane = VaultPane.Entries
            },
            onOpenPassword = { passwordOpen = true },
            onOpenAutofill = { autofillOpen = true },
            onOpenDeleteAccount = {
                settingsOpen = false
                pane = VaultPane.Entries
                deleteAccountOpen = true
            },
            onToast = { status = it },
        )

        PasswordChangeSheet(
            visible = passwordOpen,
            session = session,
            service = service,
            onClose = { passwordOpen = false },
            onToast = { status = it },
        )

        AutofillSheet(
            visible = autofillOpen,
            runtime = autofill,
            accountName = session.account.displayName,
            onClose = { autofillOpen = false },
            onCopy = { scope, text -> copySecret(text, "内容") },
        )

        deleteTarget?.let { target ->
            QuietDialog(
                visible = true,
                title = "删除密码条目",
                message = "确定删除“${target.title}”吗？此操作不可撤销。",
                confirmLabel = "删除",
                danger = true,
                onDismiss = { deleteTarget = null },
                onConfirm = {
                    deleteTarget = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { repository.delete(target.id) }
                            refreshToken++
                            status = "条目已删除"
                        } catch (t: Throwable) {
                            error = friendlyError(t)
                        }
                    }
                },
            )
        }

        DeleteAccountDialog(
            visible = deleteAccountOpen,
            accountName = session.account.displayName,
            onCancel = { deleteAccountOpen = false },
            onConfirm = {
                deleteAccountOpen = false
                onDeleteAccount(session.account.id)
            },
        )

        QuietToast(
            text = status,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp),
        )
    }
}

@Composable
private fun Sidebar(
    session: VaultSession,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    entryCount: Int,
    pane: VaultPane,
    autofillConnected: Boolean,
    autofillError: Boolean,
    width: androidx.compose.ui.unit.Dp,
    onEntries: () -> Unit,
    onSettings: () -> Unit,
    onAutofill: () -> Unit,
    onLock: () -> Unit,
) {
    val quiet = LocalQuiet.current
    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(quiet.surface.copy(alpha = 0.58f))
            .border(0.dp, Color.Transparent)
            .padding(start = 16.dp, end = 16.dp, top = 22.dp, bottom = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            BrandMark(30.dp, 15.dp)
            Text(
                "PWDog",
                color = quiet.text,
                style = QuietType.heading.copy(fontSize = 21.sp, lineHeight = 24.sp),
            )
        }
        Spacer(Modifier.height(20.dp))

        val interaction = remember { MutableInteractionSource() }
        val hovered by interaction.collectIsHoveredAsState()
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(quiet.surface.copy(alpha = if (hovered) 0.9f else 0.7f))
                .border(1.dp, if (hovered) quiet.borderStrong else quiet.border, RoundedCornerShape(11.dp))
                .hoverable(interaction)
                .clickable(interactionSource = interaction, indication = null, onClick = onSettings)
                .padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            AccountAvatar(session.account, 30.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    session.account.displayName,
                    color = quiet.text,
                    style = QuietType.entryTitle.copy(fontSize = 12.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    sidebarCaption(session.account),
                    color = quiet.muted,
                    style = QuietType.monoSmall.copy(fontSize = 9.sp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            QIcon(QIcon.ChevronDown, Modifier.size(13.dp), quiet.faint)
        }

        Spacer(Modifier.height(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            NavItem(QIcon.Key, "全部密码", entryCount, pane == VaultPane.Entries, onEntries)
            NavItem(QIcon.Settings, "设置", null, pane == VaultPane.Settings, onSettings)
        }

        Spacer(Modifier.height(16.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(quiet.surface.copy(alpha = 0.54f))
                .border(1.dp, quiet.border, RoundedCornerShape(11.dp))
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(quiet.success),
                )
                Text("本机 · 已解锁", color = quiet.text, style = QuietType.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold))
            }
            Spacer(Modifier.height(7.dp))
            Text(
                "关闭窗口会最小化到托盘，密码库保持解锁。需要立即锁定时使用下方按钮。",
                color = quiet.muted,
                style = QuietType.tiny,
            )
            Spacer(Modifier.height(9.dp))
            Hairline()
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onAutofill,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                QIcon(
                    QIcon.Plug,
                    Modifier.size(13.dp),
                    when {
                        autofillConnected -> quiet.accent
                        autofillError -> quiet.danger
                        else -> quiet.faint
                    },
                )
                Text(
                    when {
                        autofillConnected -> "自动填充服务已启动"
                        autofillError -> "自动填充服务启动失败"
                        else -> "浏览器扩展"
                    },
                    color = quiet.muted,
                    style = QuietType.tiny,
                )
            }
        }

        Spacer(Modifier.weight(1f))
        Text("主题色", color = quiet.faint, style = QuietType.monoSmall.copy(letterSpacing = 0.12.em))
        Spacer(Modifier.height(7.dp))
        AccentSwatches(settings, onSettingsChange)
        Spacer(Modifier.height(14.dp))
        QuietButton(
            "锁定密码库",
            onLock,
            Modifier.fillMaxWidth(),
            icon = QIcon.Lock,
            height = 36.dp,
        )
    }
}

@Composable
private fun NavItem(
    icon: QIcon,
    label: String,
    count: Int?,
    active: Boolean,
    onClick: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    active -> quiet.accentSoft
                    hovered -> quiet.surface2
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        QIcon(icon, Modifier.size(16.dp), if (active) quiet.accent else quiet.muted)
        Text(
            label,
            color = if (active) quiet.accent else quiet.muted,
            style = QuietType.nav.copy(
                fontWeight = if (active) androidx.compose.ui.text.font.FontWeight.SemiBold
                else androidx.compose.ui.text.font.FontWeight.Medium,
            ),
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            Text(
                count.toString(),
                color = if (active) quiet.accent else quiet.faint,
                style = QuietType.monoSmall.copy(fontSize = 10.sp),
            )
        }
    }
}

@Composable
private fun TopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchFocus: FocusRequester,
    importBusy: Boolean,
    onImport: () -> Unit,
    onNew: () -> Unit,
) {
    val quiet = LocalQuiet.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(top = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuietField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f).widthIn(max = 620.dp),
            placeholder = "搜索标题、网址或用户名",
            leading = QIcon.Search,
            singleLine = true,
            minHeight = 40.dp,
            focusRequester = searchFocus,
            trailing = {
                Text(
                    "Ctrl K",
                    color = quiet.faint,
                    style = QuietType.monoSmall.copy(fontSize = 8.sp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, quiet.border, RoundedCornerShape(4.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                )
            },
        )
        Spacer(Modifier.weight(1f))
        QuietButton(
            if (importBusy) "读取中..." else "导入",
            onImport,
            icon = QIcon.Upload,
            enabled = !importBusy,
            height = 40.dp,
        )
        QuietButton(
            "新增密码",
            onNew,
            style = QuietButtonStyle.Primary,
            icon = QIcon.Plus,
            height = 40.dp,
        )
    }
}

@Composable
private fun SiteCard(
    group: EntryGroup,
    selectedId: String?,
    onSelect: (VaultEntry) -> Unit,
    onCopy: (VaultEntry) -> Unit,
    onEdit: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onAddAccount: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(quiet.surface.copy(alpha = if (hovered) 0.92f else 0.82f))
            .border(1.dp, if (hovered) quiet.borderStrong else quiet.border, RoundedCornerShape(14.dp))
            .hoverable(interaction),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Box(
                Modifier
                    .size(29.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(quiet.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    group.label.take(1).uppercase(),
                    color = quiet.accent,
                    style = QuietType.heading.copy(fontSize = 16.sp),
                )
            }
            Text(
                group.label,
                color = quiet.text,
                style = QuietType.monoSmall.copy(fontSize = 10.5.sp, letterSpacing = (-0.01f).em, fontFamily = FontFamily("Consolas")),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Text("${group.entries.size} 个账户", color = quiet.faint, style = QuietType.tiny)
            Spacer(Modifier.weight(1f))
            val addInteraction = remember { MutableInteractionSource() }
            val addHovered by addInteraction.collectIsHoveredAsState()
            Text(
                "新增账户",
                color = if (addHovered) quiet.accent else quiet.muted,
                style = QuietType.tiny,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (addHovered) quiet.accentSoft else Color.Transparent)
                    .hoverable(addInteraction)
                    .clickable(interactionSource = addInteraction, indication = null, onClick = onAddAccount)
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        group.entries.forEachIndexed { index, entry ->
            if (index > 0) Hairline()
            EntryRow(
                entry = entry,
                selected = entry.id == selectedId,
                onSelect = { onSelect(entry) },
                onCopy = { onCopy(entry) },
                onEdit = { onEdit(entry) },
                onDelete = { onDelete(entry) },
            )
        }
    }
}

@Composable
private fun EntryRow(
    entry: VaultEntry,
    selected: Boolean,
    onSelect: () -> Unit,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val showActions = hovered || selected
    Row(
        Modifier
            .fillMaxWidth()
            .background(
                when {
                    selected -> quiet.accentSoft
                    hovered -> quiet.accent.copy(alpha = 0.05f)
                    else -> Color.Transparent
                },
            )
            .hoverable(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onSelect)
            .padding(horizontal = 13.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                entry.title,
                color = quiet.text,
                style = QuietType.entryTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                entry.username.ifBlank { "无用户名" },
                color = quiet.muted,
                style = QuietType.entryUser,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
            Box(
                Modifier
                    .size(27.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onCopy,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                QIcon(QIcon.Copy, Modifier.size(14.dp), quiet.muted.copy(alpha = if (showActions) 1f else 0f))
            }
            Box(
                Modifier
                    .size(27.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEdit,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                QIcon(QIcon.Edit, Modifier.size(14.dp), quiet.muted.copy(alpha = if (showActions) 1f else 0f))
            }
            Box(
                Modifier
                    .size(27.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDelete,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                QIcon(QIcon.Trash, Modifier.size(14.dp), quiet.danger.copy(alpha = if (showActions) 0.9f else 0f))
            }
        }
    }
}

@Composable
private fun EmptyVault(
    search: Boolean,
    onNew: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val quiet = LocalQuiet.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.padding(top = 54.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(quiet.surface)
                    .border(1.dp, quiet.border, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                QIcon(if (search) QIcon.Search else QIcon.Lock, Modifier.size(26.dp), quiet.faint)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (search) "没有匹配的密码条目" else "密码库还是空的",
                color = quiet.text,
                style = QuietType.heading,
            )
            Spacer(Modifier.height(7.dp))
            Text(
                if (search) {
                    "换个关键词试试，搜索会匹配标题、网址和用户名。"
                } else {
                    "从第一条登录信息开始，或者导入浏览器的密码 CSV。"
                },
                color = quiet.muted,
                style = QuietType.small,
            )
            if (!search) {
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietButton("新增密码", onNew, style = QuietButtonStyle.Primary, icon = QIcon.Plus)
                    QuietButton("导入 CSV", onImport, icon = QIcon.Upload)
                }
            }
        }
    }
}

@Composable
private fun DetailPanel(
    entry: VaultEntry?,
    revealed: Boolean,
    onReveal: () -> Unit,
    onCopy: (VaultEntry, String) -> Unit,
    onEdit: (VaultEntry) -> Unit,
    onOpenUrl: (VaultEntry) -> Unit,
    width: androidx.compose.ui.unit.Dp,
) {
    val quiet = LocalQuiet.current
    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(quiet.surface.copy(alpha = 0.62f))
            .drawBehind {
                drawLine(quiet.border, Offset(0.5f, 0f), Offset(0.5f, size.height), 1.dp.toPx())
            }
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        if (entry == null) {
            Column(
                Modifier.fillMaxWidth().padding(top = 62.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                QIcon(QIcon.Key, Modifier.size(32.dp), quiet.faint)
                Spacer(Modifier.height(14.dp))
                Text("选择一个条目", color = quiet.text, style = QuietType.heading)
                Spacer(Modifier.height(6.dp))
                Text("密码详情和安全操作会显示在这里。", color = quiet.muted, style = QuietType.small)
            }
            return@Column
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(quiet.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    entryHost(entry).take(1).uppercase(),
                    color = quiet.accent,
                    style = QuietType.heading.copy(fontSize = 25.sp).copy(fontFamily = FontFamily("Georgia")),
                )
            }
            Column(Modifier.weight(1f)) {
                Text(entryHost(entry).uppercase(), color = quiet.accent, style = QuietType.eyebrow)
                Spacer(Modifier.height(2.dp))
                Text(entry.title, color = quiet.text, style = QuietType.entryTitle.copy(fontSize = 15.sp))
                Spacer(Modifier.height(3.dp))
                Text(formatEntryTime(entry.updatedAt), color = quiet.muted, style = QuietType.tiny)
            }
        }

        Spacer(Modifier.height(23.dp))
        Text("用户名", color = quiet.faint, style = QuietType.eyebrow)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(quiet.surface.copy(alpha = 0.72f))
                .border(1.dp, quiet.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                entry.username.ifBlank { "无用户名" },
                color = quiet.text,
                style = QuietType.detailValue,
                modifier = Modifier.weight(1f),
            )
            QuietIconButton(QIcon.Copy, { onCopy(entry, "用户名") }, size = 26.dp, iconSize = 14.dp)
        }

        Spacer(Modifier.height(20.dp))
        Text("密码", color = quiet.faint, style = QuietType.eyebrow)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(quiet.surface)
                .border(1.dp, quiet.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 11.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                if (revealed) entry.password else "••••••••••••",
                color = if (revealed) quiet.text else quiet.muted,
                style = QuietType.detailValue.copy(
                    fontFamily = FontFamily("Consolas"),
                    fontSize = 12.sp,
                    letterSpacing = if (revealed) 0.08f.em else 0.18f.em,
                ),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            QuietIconButton(
                if (revealed) QIcon.EyeOff else QIcon.Eye,
                onReveal,
                size = 26.dp,
                iconSize = 14.dp,
            )
            QuietIconButton(QIcon.Copy, { onCopy(entry, "密码") }, size = 26.dp, iconSize = 14.dp)
        }

        Spacer(Modifier.height(20.dp))
        Text("网址", color = quiet.faint, style = QuietType.eyebrow)
        Spacer(Modifier.height(7.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(quiet.surface.copy(alpha = 0.72f))
                .border(1.dp, quiet.border, RoundedCornerShape(10.dp))
                .padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QIcon(QIcon.Globe, Modifier.size(14.dp), quiet.muted)
            Text(
                entry.url.ifBlank { "未填写" },
                color = if (entry.url.isBlank()) quiet.faint else quiet.text,
                style = QuietType.detailValue,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            QuietIconButton(QIcon.ArrowUpRight, { onOpenUrl(entry) }, size = 26.dp, iconSize = 14.dp)
        }

        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            QuietButton(
                "编辑条目",
                { onEdit(entry) },
                Modifier.weight(1f),
                style = QuietButtonStyle.Primary,
                height = 38.dp,
            )
            QuietButton(
                "复制密码",
                { onCopy(entry, "密码") },
                Modifier.weight(1f),
                height = 38.dp,
            )
        }

        Spacer(Modifier.height(21.dp))
        Text(
            entry.notes.ifBlank { "暂无备注。" },
            color = quiet.muted,
            style = QuietType.small,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(11.dp))
                .background(quiet.surface.copy(alpha = 0.56f))
                .border(1.dp, quiet.border, RoundedCornerShape(11.dp))
                .padding(13.dp),
        )

        Spacer(Modifier.height(20.dp))
        MetaRow("最近更新", formatEntryTime(entry.updatedAt))
        Spacer(Modifier.height(9.dp))
        MetaRow("自动填充", entryHost(entry))
        Spacer(Modifier.height(9.dp))
        MetaRow("存储位置", "本机加密库")
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    val quiet = LocalQuiet.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = quiet.faint, style = QuietType.tiny)
        Text(value, color = quiet.muted, style = QuietType.tiny, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun sidebarCaption(account: com.pwdog.core.account.Account): String =
    accountCaption(account).removePrefix("本地账户 · ")

private fun entryHost(entry: VaultEntry): String {
    val host = com.pwdog.core.entry.HostMatcher.hostOf(entry.url)
    return host ?: entry.title
}

private val entryTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun formatEntryTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(entryTimeFormatter)

// ---------------------------------------------------------------------------
// Entry editor
// ---------------------------------------------------------------------------

@Composable
private fun EditorSheet(
    visible: Boolean,
    state: EditorState?,
    saving: Boolean,
    error: String?,
    onClose: () -> Unit,
    onCopy: (String) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    onSave: (EntryDraft) -> Unit,
) {
    if (state == null) return
    val quiet = LocalQuiet.current
    val existing = (state as? EditorState.Existing)?.entry
    val template = state as? EditorState.New
    var title by remember(state) { mutableStateOf(existing?.title ?: template?.title ?: "") }
    var url by remember(state) { mutableStateOf(existing?.url ?: template?.url ?: "") }
    var username by remember(state) { mutableStateOf(existing?.username ?: "") }
    var password by remember(state) { mutableStateOf(existing?.password ?: "") }
    var notes by remember(state) { mutableStateOf(existing?.notes ?: "") }
    var showPassword by remember(state) { mutableStateOf(false) }
    var localError by remember(state) { mutableStateOf<String?>(null) }
    var generated by remember(state) { mutableStateOf(false) }

    fun submit() {
        localError = when {
            title.isBlank() -> "标题不能为空"
            password.isEmpty() -> "密码不能为空"
            else -> null
        }
        if (localError == null) {
            onSave(EntryDraft(title, url, username, password, notes))
        }
    }

    QuietSheet(visible = visible, onDismiss = if (saving) null else onClose) {
        SheetHeader(
            eyebrow = if (existing == null) "NEW ENTRY" else "EDIT ENTRY",
            title = if (existing == null) "新增密码" else "编辑密码",
            subtitle = "同一网站有多个账户时，为每个账户分别保存一条记录。",
            onClose = if (saving) null else onClose,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietField(title, { title = it }, label = "标题", placeholder = "例如 GitHub · 工作账号", enabled = !saving)
            QuietField(url, { url = it }, label = "网址", placeholder = "https://example.com", enabled = !saving)
            QuietField(username, { username = it }, label = "用户名 / 邮箱", placeholder = "name@example.com", enabled = !saving)
            QuietField(
                password,
                { password = it },
                label = "密码",
                placeholder = "输入或生成密码",
                password = true,
                masked = !showPassword,
                enabled = !saving,
                leading = QIcon.Key,
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                        QuietIconButton(
                            if (showPassword) QIcon.EyeOff else QIcon.Eye,
                            { showPassword = !showPassword },
                            size = 26.dp,
                            iconSize = 14.dp,
                            enabled = !saving,
                        )
                        QuietIconButton(
                            QIcon.Copy,
                            { onCopy(password) },
                            size = 26.dp,
                            iconSize = 14.dp,
                            enabled = !saving && password.isNotEmpty(),
                        )
                    }
                },
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                QuietButton(
                    "生成强密码",
                    {
                        password = PasswordGenerator.generate()
                        showPassword = true
                        generated = true
                    },
                    icon = QIcon.Sparkle,
                    enabled = !saving,
                    height = 36.dp,
                )
                Text(
                    if (generated) "已生成 20 位强密码" else "默认 20 位，包含大小写、数字和符号",
                    color = quiet.faint,
                    style = QuietType.tiny,
                )
            }
            QuietField(
                notes,
                { notes = it },
                label = "备注",
                placeholder = "可选备注",
                singleLine = false,
                minHeight = 92.dp,
                enabled = !saving,
            )

            (localError ?: error)?.let { QuietNotice(it, NoticeTone.Error) }
            if (saving) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
                    color = quiet.accent,
                    trackColor = quiet.border,
                )
                Text("正在保存到加密库...", color = quiet.muted, style = QuietType.tiny)
            }

            Spacer(Modifier.height(2.dp))
            QuietButton(
                if (saving) "保存中..." else "保存条目",
                { submit() },
                Modifier.fillMaxWidth(),
                style = QuietButtonStyle.Primary,
                enabled = !saving,
                height = 46.dp,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietButton(
                    "取消",
                    onClose,
                    Modifier.weight(1f),
                    style = QuietButtonStyle.Quiet,
                    enabled = !saving,
                )
                if (existing != null) {
                    QuietButton(
                        "删除条目",
                        { onDelete(existing) },
                        Modifier.weight(1f),
                        style = QuietButtonStyle.Danger,
                        enabled = !saving,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Import
// ---------------------------------------------------------------------------

@Composable
private fun ImportSheet(
    visible: Boolean,
    preview: PasswordImportPreview?,
    fileName: String,
    importing: Boolean,
    error: String?,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (preview == null) return
    val quiet = LocalQuiet.current
    QuietSheet(visible = visible, onDismiss = if (importing) null else onClose) {
        SheetHeader(
            eyebrow = "已识别 · ${preview.format}",
            title = "导入密码",
            subtitle = if (fileName.isNotBlank()) "文件：$fileName" else null,
            onClose = if (importing) null else onClose,
        )
        QuietNotice(
            "浏览器导出的 CSV 是明文文件。导入完成后请删除原文件，避免密码以明文留在磁盘上。",
            NoticeTone.Info,
        )
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(quiet.surface2)
                .border(1.dp, quiet.border, RoundedCornerShape(12.dp))
                .padding(vertical = 12.dp),
        ) {
            ImportStat(preview.credentials.size, "可导入", Modifier.weight(1f))
            ImportStat(preview.skippedInvalid, "无效 / 非登录", Modifier.weight(1f))
            ImportStat(preview.skippedDuplicates, "文件内重复", Modifier.weight(1f))
        }
        Spacer(Modifier.height(14.dp))
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, quiet.border, RoundedCornerShape(12.dp)),
        ) {
            val shown = preview.credentials.take(8)
            shown.forEachIndexed { index, credential ->
                if (index > 0) Hairline()
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    Box(
                        Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(quiet.accentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            credential.title.take(1).uppercase(),
                            color = quiet.accent,
                            style = QuietType.small.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            credential.title,
                            color = quiet.text,
                            style = QuietType.entryTitle.copy(fontSize = 12.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${credential.username.ifBlank { "无用户名" }} · ${credential.url.ifBlank { "无网址" }}",
                            color = quiet.muted,
                            style = QuietType.tiny,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            if (preview.credentials.isEmpty()) {
                Text(
                    "没有可导入的有效登录条目。",
                    color = quiet.danger,
                    style = QuietType.small,
                    modifier = Modifier.padding(14.dp),
                )
            }
            if (preview.credentials.size > shown.size) {
                Hairline()
                Text(
                    "还有 ${preview.credentials.size - shown.size} 条未显示",
                    color = quiet.faint,
                    style = QuietType.tiny,
                    modifier = Modifier.padding(11.dp),
                )
            }
        }

        if (importing) {
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                Modifier.fillMaxWidth().height(2.dp),
                color = quiet.accent,
                trackColor = quiet.border,
            )
        }
        error?.let {
            Spacer(Modifier.height(12.dp))
            QuietNotice(it, NoticeTone.Error)
        }
        Spacer(Modifier.height(16.dp))
        QuietButton(
            if (importing) "正在导入..." else "导入 ${preview.credentials.size} 条",
            onConfirm,
            Modifier.fillMaxWidth(),
            style = QuietButtonStyle.Primary,
            enabled = !importing && preview.credentials.isNotEmpty(),
            height = 46.dp,
        )
        QuietButton(
            "取消",
            onClose,
            Modifier.fillMaxWidth(),
            style = QuietButtonStyle.Quiet,
            enabled = !importing,
        )
    }
}

@Composable
private fun ImportStat(value: Int, label: String, modifier: Modifier = Modifier) {
    val quiet = LocalQuiet.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = quiet.text, style = QuietType.heading.copy(fontSize = 20.sp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = quiet.faint, style = QuietType.tiny)
    }
}

// ---------------------------------------------------------------------------
// Settings
// ---------------------------------------------------------------------------

@Composable
private fun SettingsSheet(
    visible: Boolean,
    session: VaultSession,
    service: AccountService,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onClose: () -> Unit,
    onOpenPassword: () -> Unit,
    onOpenAutofill: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onToast: (String) -> Unit,
) {
    val quiet = LocalQuiet.current
    val scope = rememberCoroutineScope()
    var helloAvailable by remember { mutableStateOf<Boolean?>(null) }
    var quickUnlockEnabled by remember { mutableStateOf(session.account.quickUnlockBlob != null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var autostartSupported by remember { mutableStateOf<Boolean?>(null) }
    var autostartEnabled by remember { mutableStateOf(false) }
    var editingProfile by remember { mutableStateOf(false) }
    var profileName by remember(session.account.id) { mutableStateOf(session.account.displayName) }
    var profileColor by remember(session.account.id) { mutableStateOf(session.account.avatarColor) }
    var profileImage by remember(session.account.id) { mutableStateOf(session.account.avatarImage) }
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dataDir = remember { AppPaths.defaultBaseDir() }

    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        error = null
        message = null
        val current = withContext(Dispatchers.IO) {
            service.listAccounts().firstOrNull { it.id == session.account.id }
        }
        quickUnlockEnabled = current?.quickUnlockBlob != null
        if (current != null) {
            profileName = current.displayName
            profileColor = current.avatarColor
            profileImage = current.avatarImage
        }
        helloAvailable = withContext(Dispatchers.IO) { WindowsQuickUnlock.isHelloAvailable() }
        autostartSupported = withContext(Dispatchers.IO) { WindowsAutostart.isSupported() }
        autostartEnabled = withContext(Dispatchers.IO) { WindowsAutostart.isEnabled() }
    }

    fun toggleQuickUnlock() {
        busy = true
        error = null
        message = null
        scope.launch {
            try {
                if (quickUnlockEnabled) {
                    withContext(Dispatchers.IO) { service.disableQuickUnlock(session.account.id) }
                    quickUnlockEnabled = false
                    message = "已关闭 Windows Hello 快速解锁"
                } else {
                    val available = withContext(Dispatchers.IO) { WindowsQuickUnlock.isHelloAvailable() }
                    helloAvailable = available
                    if (!available) {
                        error = "Windows Hello 不可用"
                        return@launch
                    }
                    val verified = withContext(Dispatchers.IO) {
                        WindowsQuickUnlock.verify("启用 PWDog 快速解锁")
                    }
                    if (!verified) {
                        error = "Windows Hello 验证未通过"
                        return@launch
                    }
                    withContext(Dispatchers.IO) {
                        service.enableQuickUnlock(session, WindowsQuickUnlock::protect)
                    }
                    quickUnlockEnabled = true
                    message = "已启用 Windows Hello 快速解锁"
                }
            } catch (t: Throwable) {
                error = friendlyError(t)
            } finally {
                busy = false
            }
        }
    }

    fun toggleAutostart() {
        busy = true
        error = null
        message = null
        scope.launch {
            try {
                val changed = withContext(Dispatchers.IO) {
                    if (autostartEnabled) WindowsAutostart.disable() else WindowsAutostart.enable()
                }
                if (changed) {
                    autostartEnabled = !autostartEnabled
                    message = if (autostartEnabled) "已启用开机自启" else "已关闭开机自启"
                } else {
                    error = "修改开机自启失败"
                }
            } catch (t: Throwable) {
                error = friendlyError(t)
            } finally {
                busy = false
            }
        }
    }

    fun chooseAvatar() {
        try {
            val bytes = AvatarPicker.choose(darkTheme) ?: return
            profileImage = bytes
            if (profileColor == null) profileColor = defaultAvatarColor(session.account.id)
            editingProfile = true
            message = "已选择新头像，保存资料后生效"
        } catch (t: Throwable) {
            error = friendlyError(t)
        }
    }

    fun saveProfile() {
        busy = true
        error = null
        message = null
        scope.launch {
            try {
                val updated = withContext(Dispatchers.IO) {
                    service.updateProfile(
                        accountId = session.account.id,
                        displayName = profileName,
                        avatarColor = profileColor ?: defaultAvatarColor(session.account.id),
                        avatarImage = profileImage,
                    )
                }
                session.updateAccountMetadata(updated)
                profileName = updated.displayName
                profileColor = updated.avatarColor
                profileImage = updated.avatarImage
                editingProfile = false
                message = "账户资料已保存"
                onToast("账户资料已保存")
            } catch (t: Throwable) {
                error = friendlyError(t)
            } finally {
                busy = false
            }
        }
    }

    QuietSheet(
        visible = visible,
        onDismiss = if (busy) null else onClose,
        width = 500.dp,
    ) {
        SheetHeader(
            eyebrow = "PREFERENCES",
            title = "设置",
            subtitle = "账户资料、外观与本机安全。",
            onClose = if (busy) null else onClose,
        )

        Column(Modifier.fillMaxWidth()) {
            SectionHeader("账户资料", action = {
                Text(
                    if (editingProfile) "收起" else "修改资料",
                    color = quiet.muted,
                    style = QuietType.tiny,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { editingProfile = !editingProfile }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                )
            })
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(quiet.surface2.copy(alpha = 0.6f))
                    .border(1.dp, quiet.border, RoundedCornerShape(13.dp))
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                AvatarView(
                    imageBytes = profileImage,
                    color = profileColor ?: defaultAvatarColor(session.account.id),
                    name = profileName,
                    size = 48.dp,
                )
                Column(Modifier.weight(1f)) {
                    Text(profileName, color = quiet.text, style = QuietType.entryTitle.copy(fontSize = 13.sp))
                    Spacer(Modifier.height(2.dp))
                    Text(accountCaption(session.account), color = quiet.muted, style = QuietType.tiny)
                }
                QuietChip("本机")
            }

            if (editingProfile) {
                Spacer(Modifier.height(12.dp))
                QuietField(profileName, { profileName = it }, label = "显示名称", enabled = !busy)
                Spacer(Modifier.height(11.dp))
                Text("头像颜色", color = quiet.muted, style = QuietType.label)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AVATAR_COLORS.forEach { color ->
                        Box(
                            Modifier
                                .size(25.dp)
                                .clip(CircleShape)
                                .background(Color(color))
                                .border(
                                    width = if (profileColor == color) 2.dp else 1.dp,
                                    color = if (profileColor == color) quiet.text else quiet.borderStrong,
                                    shape = CircleShape,
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { profileColor = color },
                        )
                    }
                }
                Spacer(Modifier.height(11.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuietButton("上传图片", { chooseAvatar() }, icon = QIcon.Upload, enabled = !busy, height = 36.dp)
                    if (profileImage != null) {
                        QuietButton(
                            "移除图片",
                            { profileImage = null },
                            style = QuietButtonStyle.Quiet,
                            enabled = !busy,
                            height = 36.dp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    QuietButton(
                        if (busy) "保存中..." else "保存资料",
                        { saveProfile() },
                        style = QuietButtonStyle.Primary,
                        enabled = !busy,
                        height = 36.dp,
                    )
                }
                Spacer(Modifier.height(7.dp))
                Text(
                    "头像显示在登录页，未加密保存；图片会裁剪为 256×256。",
                    color = quiet.faint,
                    style = QuietType.tiny,
                )
            }
        }

        Spacer(Modifier.height(19.dp))
        Hairline()
        Spacer(Modifier.height(17.dp))
        Column {
            SectionHeader("外观")
            ThemeModeSelector(settings, onSettingsChange)
            Spacer(Modifier.height(16.dp))
            Text("强调色", color = quiet.muted, style = QuietType.label)
            Spacer(Modifier.height(7.dp))
            AccentSwatches(settings, onSettingsChange)
            Spacer(Modifier.height(16.dp))
            BackgroundStyleSelector(settings, onSettingsChange)
        }

        Spacer(Modifier.height(19.dp))
        Hairline()
        Spacer(Modifier.height(17.dp))
        Column {
            SectionHeader("安全")
            SettingsRow(
                icon = QIcon.Key,
                title = "主密码",
                subtitle = "修改后旧主密码立即失效",
                value = "修改",
                onClick = onOpenPassword,
            )
            SettingsRow(
                icon = QIcon.Fingerprint,
                title = "Windows Hello",
                subtitle = when (helloAvailable) {
                    null -> "正在检查..."
                    true -> if (quickUnlockEnabled) "已启用快速解锁" else "可用，尚未启用"
                    false -> "此设备不可用"
                },
                action = {
                    QuietButton(
                        when {
                            busy -> "请稍候"
                            quickUnlockEnabled -> "关闭"
                            helloAvailable == true -> "启用"
                            else -> "不可用"
                        },
                        { toggleQuickUnlock() },
                        style = if (quickUnlockEnabled) QuietButtonStyle.Quiet else QuietButtonStyle.Secondary,
                        enabled = !busy && (quickUnlockEnabled || helloAvailable == true),
                        height = 32.dp,
                    )
                },
            )
            SettingsRow(
                icon = QIcon.Monitor,
                title = "开机自启",
                subtitle = when (autostartSupported) {
                    null -> "正在检查..."
                    false -> "仅在安装版中可用"
                    true -> if (autostartEnabled) "登录 Windows 后在托盘启动" else "未开启"
                },
                action = {
                    QuietButton(
                        when {
                            autostartSupported == false -> "不可用"
                            autostartEnabled -> "关闭"
                            else -> "启用"
                        },
                        { toggleAutostart() },
                        style = QuietButtonStyle.Quiet,
                        enabled = !busy && autostartSupported == true,
                        height = 32.dp,
                    )
                },
            )
        }

        Spacer(Modifier.height(19.dp))
        Hairline()
        Spacer(Modifier.height(17.dp))
        Column {
            SectionHeader("本机数据")
            SettingsRow(
                icon = QIcon.Folder,
                title = "数据目录",
                subtitle = dataDir.toString(),
                value = "打开",
                onClick = {
                    runCatching {
                        Files.createDirectories(dataDir)
                        Desktop.getDesktop().open(dataDir.toFile())
                    }.onFailure { error = friendlyError(it) }
                },
            )
            SettingsRow(
                icon = QIcon.Plug,
                title = "浏览器自动填充",
                subtitle = "Chrome / Edge 扩展配对",
                value = "查看",
                onClick = onOpenAutofill,
            )
        }

        Spacer(Modifier.height(19.dp))
        Hairline()
        Spacer(Modifier.height(17.dp))
        Column {
            SectionHeader("危险操作")
            SettingsRow(
                icon = QIcon.Trash,
                title = "删除当前账户",
                subtitle = "仅删除此账户和它的加密密码库",
                tone = NoticeTone.Error,
                onClick = onOpenDeleteAccount,
            )
        }

        if (message != null) {
            Spacer(Modifier.height(14.dp))
            QuietNotice(message, NoticeTone.Success)
        }
        error?.let {
            Spacer(Modifier.height(14.dp))
            QuietNotice(it, NoticeTone.Error)
        }
    }
}

@Composable
private fun SectionHeader(title: String, action: (@Composable () -> Unit)? = null) {
    val quiet = LocalQuiet.current
    Row(
        Modifier.fillMaxWidth().padding(bottom = 11.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SectionLabel(title)
        action?.invoke()
    }
}

@Composable
private fun SettingsRow(
    icon: QIcon,
    title: String,
    subtitle: String,
    value: String? = null,
    tone: NoticeTone = NoticeTone.Info,
    onClick: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null,
) {
    val quiet = LocalQuiet.current
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (hovered && onClick != null) quiet.surface2 else Color.Transparent)
            .hoverable(interaction)
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 7.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        QIcon(
            icon,
            Modifier.size(15.dp),
            if (tone == NoticeTone.Error) quiet.danger else quiet.muted,
        )
        Column(Modifier.weight(1f)) {
            Text(
                title,
                color = if (tone == NoticeTone.Error) quiet.danger else quiet.text,
                style = QuietType.entryTitle.copy(fontSize = 11.5.sp),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = quiet.muted,
                style = QuietType.tiny,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (action != null) {
            action()
        } else if (value != null) {
            Text(value, color = quiet.muted, style = QuietType.tiny)
        }
    }
    Hairline()
}

@Composable
private fun PasswordChangeSheet(
    visible: Boolean,
    session: VaultSession,
    service: AccountService,
    onClose: () -> Unit,
    onToast: (String) -> Unit,
) {
    val quiet = LocalQuiet.current
    val scope = rememberCoroutineScope()
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            currentPassword = ""
            newPassword = ""
            confirmNewPassword = ""
            error = null
            message = null
        }
    }

    fun submit() {
        error = when {
            currentPassword.isEmpty() -> "请输入当前主密码"
            newPassword.length < 8 -> "新主密码至少需要 8 个字符"
            newPassword != confirmNewPassword -> "两次输入的新密码不一致"
            else -> null
        }
        if (error != null) return
        busy = true
        message = null
        scope.launch {
            val currentBytes = currentPassword.toByteArray(Charsets.UTF_8)
            val newBytes = newPassword.toByteArray(Charsets.UTF_8)
            try {
                withContext(Dispatchers.IO) {
                    service.changePassword(session, currentBytes, newBytes)
                }
                currentPassword = ""
                newPassword = ""
                confirmNewPassword = ""
                message = "主密码已修改。恢复密钥和快速解锁保持有效。"
                onToast("主密码已修改")
            } catch (t: Throwable) {
                error = friendlyError(t)
            } finally {
                currentBytes.fill(0)
                newBytes.fill(0)
                busy = false
            }
        }
    }

    QuietSheet(visible = visible, onDismiss = if (busy) null else onClose) {
        SheetHeader(
            eyebrow = "MASTER PASSWORD",
            title = "修改主密码",
            subtitle = "修改后旧主密码立即失效；恢复密钥和 Windows Hello 快速解锁保持有效。",
            onClose = if (busy) null else onClose,
        )
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            QuietField(
                currentPassword,
                { currentPassword = it },
                label = "当前主密码",
                placeholder = "输入当前主密码",
                password = true,
                enabled = !busy,
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
                confirmNewPassword,
                { confirmNewPassword = it },
                label = "确认新主密码",
                placeholder = "再次输入新主密码",
                password = true,
                enabled = !busy,
                onEnter = { if (!busy) submit() },
            )
            QuietNotice(
                "恢复密钥和 Windows Hello 快速解锁仍然有效。",
                NoticeTone.Info,
            )
            if (busy) {
                LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp),
                    color = quiet.accent,
                    trackColor = quiet.border,
                )
                Text("正在更新主密码...", color = quiet.muted, style = QuietType.tiny)
            }
            error?.let { QuietNotice(it, NoticeTone.Error) }
            message?.let { QuietNotice(it, NoticeTone.Success) }
            Spacer(Modifier.height(2.dp))
            QuietButton(
                if (busy) "更新中..." else "保存新主密码",
                { submit() },
                Modifier.fillMaxWidth(),
                style = QuietButtonStyle.Primary,
                enabled = !busy,
                height = 46.dp,
            )
            QuietButton("取消", onClose, Modifier.fillMaxWidth(), style = QuietButtonStyle.Quiet, enabled = !busy)
        }
    }
}

@Composable
private fun AutofillSheet(
    visible: Boolean,
    runtime: AutofillRuntime,
    accountName: String,
    onClose: () -> Unit,
    onCopy: (CoroutineScope, String) -> Unit,
) {
    val quiet = LocalQuiet.current
    val scope = rememberCoroutineScope()
    val settings = runtime.settings
    var copiedLabel by remember { mutableStateOf<String?>(null) }
    var extensionDir by remember { mutableStateOf<Path?>(null) }
    var bundleError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(visible) {
        if (visible) {
            runCatching { ExtensionBundle.ensureExtracted() }
                .onSuccess { extensionDir = it }
                .onFailure { bundleError = friendlyError(it) }
        }
    }

    LaunchedEffect(copiedLabel) {
        if (copiedLabel != null) {
            delay(3_000)
            copiedLabel = null
        }
    }

    QuietSheet(visible = visible, onDismiss = onClose, width = 470.dp) {
        SheetHeader(
            eyebrow = "BROWSER EXTENSION",
            title = "浏览器扩展配对",
            subtitle = "当前账户：$accountName",
            onClose = onClose,
        )
        runtime.error?.let {
            QuietNotice(it, NoticeTone.Error)
            Spacer(Modifier.height(12.dp))
        }

        QuietNotice(
            "配对令牌只保存在本机。扩展只能通过它访问 127.0.0.1 上的 PWDog 接口；桌面程序锁定时不会返回密码。",
            NoticeTone.Info,
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            StepRow("1", "在 Chrome / Edge 打开扩展管理页，启用开发者模式。")
            StepRow("2", "选择“加载已解压的扩展程序”，然后选择下面打开的扩展文件夹。")
            StepRow("3", "打开扩展设置，填写下面的接口地址和配对令牌。")
        }
        Spacer(Modifier.height(16.dp))
        Text("扩展文件夹", color = quiet.muted, style = QuietType.label)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(quiet.surface2)
                .border(1.dp, quiet.border, RoundedCornerShape(10.dp))
                .padding(start = 11.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            QIcon(QIcon.Folder, Modifier.size(15.dp), quiet.muted)
            Text(
                extensionDir?.toString() ?: "正在准备扩展文件...",
                color = if (extensionDir == null) quiet.faint else quiet.text,
                style = QuietType.body.copy(fontFamily = FontFamily("Consolas"), fontSize = 10.5.sp),
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (extensionDir != null) {
                QuietIconButton(
                    QIcon.Copy,
                    {
                        onCopy(scope, extensionDir?.toString().orEmpty())
                        copiedLabel = "扩展文件夹路径已复制"
                    },
                    size = 28.dp,
                    iconSize = 14.dp,
                )
                QuietIconButton(
                    QIcon.External,
                    {
                        runCatching { Desktop.getDesktop().open(extensionDir?.toFile()) }
                            .onFailure { bundleError = "无法打开扩展文件夹" }
                    },
                    size = 28.dp,
                    iconSize = 14.dp,
                    tint = quiet.accent,
                )
            }
        }
        if (bundleError != null) {
            Spacer(Modifier.height(6.dp))
            QuietNotice(bundleError, NoticeTone.Error)
        }
        if (settings == null) return@QuietSheet

        Spacer(Modifier.height(16.dp))
        Text("本地接口", color = quiet.muted, style = QuietType.label)
        Spacer(Modifier.height(6.dp))
        PairingValue(settings.apiUrl, onCopy = {
            onCopy(scope, settings.apiUrl)
            copiedLabel = "接口地址已复制"
        })
        Spacer(Modifier.height(11.dp))
        Text("配对令牌", color = quiet.muted, style = QuietType.label)
        Spacer(Modifier.height(6.dp))
        PairingValue(settings.token, onCopy = {
            onCopy(scope, settings.token)
            copiedLabel = "配对令牌已复制"
        })
        if (copiedLabel != null) {
            Spacer(Modifier.height(9.dp))
            QuietNotice("$copiedLabel · 15 秒后从剪贴板清除", NoticeTone.Success)
        }
        Spacer(Modifier.height(16.dp))
        QuietNotice(
            "扩展不会自动提交表单。填充完成后仍由你确认登录。",
            NoticeTone.Info,
        )
    }
}

@Composable
private fun StepRow(number: String, text: String) {
    val quiet = LocalQuiet.current
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(19.dp)
                .clip(CircleShape)
                .background(quiet.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text(number, color = quiet.accent, style = QuietType.monoSmall.copy(fontSize = 9.sp))
        }
        Text(text, color = quiet.muted, style = QuietType.small, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PairingValue(value: String, onCopy: () -> Unit) {
    val quiet = LocalQuiet.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(quiet.surface2)
            .border(1.dp, quiet.border, RoundedCornerShape(10.dp))
            .padding(start = 11.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            value,
            color = quiet.text,
            style = QuietType.body.copy(fontFamily = FontFamily("Consolas"), fontSize = 11.sp),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        QuietIconButton(QIcon.Copy, onCopy, size = 28.dp, iconSize = 14.dp, tint = quiet.accent)
    }
}

@Composable
private fun DeleteAccountDialog(
    visible: Boolean,
    accountName: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    if (!visible) return
    val quiet = LocalQuiet.current
    var confirmation by remember { mutableStateOf("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onCancel,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 470.dp)
                .fillMaxWidth(0.46f)
                .clip(RoundedCornerShape(18.dp))
                .background(quiet.surface)
                .border(1.dp, quiet.border, RoundedCornerShape(18.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("删除当前账户", color = quiet.danger, style = QuietType.dialogTitle)
            Text(
                "只会删除账户“$accountName”和它的加密密码库。其他本地账户、浏览器配对令牌和外观设置都不受影响。此操作不可恢复。",
                color = quiet.muted,
                style = QuietType.small,
            )
            QuietField(
                confirmation,
                { confirmation = it },
                label = "输入账户名称“$accountName”以确认",
                placeholder = accountName,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                QuietButton("取消", onCancel, style = QuietButtonStyle.Quiet)
                QuietButton(
                    "永久删除此账户",
                    onConfirm,
                    style = QuietButtonStyle.Danger,
                    enabled = confirmation.trim().equals(accountName, ignoreCase = true),
                )
            }
        }
    }
}

internal object PasswordClipboard {
    private const val CLEAR_AFTER_MS = 15_000L
    private var activeSecret: String? = null

    fun copy(scope: CoroutineScope, secret: String) {
        if (secret.isEmpty()) return
        activeSecret = secret
        runCatching {
            Toolkit.getDefaultToolkit().systemClipboard
                .setContents(StringSelection(secret), null)
        }
        scope.launch {
            delay(CLEAR_AFTER_MS)
            clearIfMatches(secret)
        }
    }

    fun clearActive() {
        val secret = activeSecret ?: return
        activeSecret = null
        clearIfMatches(secret)
    }

    private fun clearIfMatches(secret: String) {
        runCatching {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val current = clipboard.getData(DataFlavor.stringFlavor) as? String
            if (current == secret) {
                clipboard.setContents(StringSelection(""), null)
            }
        }
        if (activeSecret == secret) {
            activeSecret = null
        }
    }
}
