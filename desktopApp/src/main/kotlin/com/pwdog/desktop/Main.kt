package com.pwdog.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Tray
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberTrayState
import androidx.compose.ui.window.rememberWindowState
import com.pwdog.core.account.Account
import com.pwdog.core.account.AccountService
import com.pwdog.core.account.VaultSession
import com.pwdog.desktop.autofill.AutofillRuntime
import com.pwdog.desktop.design.QuietButton
import com.pwdog.desktop.design.QuietButtonStyle
import com.pwdog.desktop.design.QuietNotice
import com.pwdog.desktop.design.NoticeTone
import com.pwdog.desktop.security.WindowsQuickUnlock
import com.pwdog.desktop.settings.AppSettings
import com.pwdog.desktop.settings.AppSettingsStore
import com.pwdog.desktop.theme.LocalQuiet
import com.pwdog.desktop.theme.PwdogTheme
import com.pwdog.desktop.theme.QuietType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess
import org.jetbrains.skia.Image

fun main(args: Array<String>) = application {
    val service = remember { AccountService() }
    var session by remember { mutableStateOf<VaultSession?>(null) }
    val sessionRef = remember { AtomicReference<VaultSession?>(null) }
    val autofill = remember { AutofillRuntime { sessionRef.get() } }
    val traySupported = remember {
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    }
    var windowVisible by remember {
        mutableStateOf(!args.contains("--tray") || !traySupported)
    }
    val windowState = rememberWindowState(width = 1180.dp, height = 760.dp)
    val trayIcon = remember { loadAppIcon() }
    val trayState = rememberTrayState()
    val appScope = rememberCoroutineScope()
    val settingsStore = remember { AppSettingsStore() }
    var appSettings by remember { mutableStateOf(settingsStore.load()) }
    val updateSettings: (AppSettings) -> Unit = { updated ->
        appSettings = updated
        runCatching { settingsStore.save(updated) }
    }

    fun quitApplication() {
        runCatching { exitApplication() }
        Thread {
            try {
                Thread.sleep(600)
            } catch (_: InterruptedException) {
                // Exit anyway.
            }
            exitProcess(0)
        }.apply {
            name = "pwdog-exit"
            isDaemon = true
        }.start()
    }

    fun closeResources() {
        PasswordClipboard.clearActive()
        sessionRef.set(null)
        session?.close()
        session = null
        autofill.close()
        service.close()
    }

    val closeApp = {
        runCatching { closeResources() }
            .onFailure { System.err.println("Shutdown cleanup failed: ${it.message}") }
        quitApplication()
    }

    val closeOrHide = {
        if (traySupported) {
            windowVisible = false
        } else {
            runCatching { closeResources() }
                .onFailure { System.err.println("Shutdown cleanup failed: ${it.message}") }
            quitApplication()
        }
    }

    Window(
        onCloseRequest = closeOrHide,
        visible = windowVisible,
        state = windowState,
        icon = trayIcon,
        title = "PWDog",
    ) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            runCatching { window.minimumSize = java.awt.Dimension(980, 700) }
        }
        PwdogTheme(appSettings) {
            PwdogApp(
                service = service,
                session = session,
                autofill = autofill,
                settings = appSettings,
                onSettingsChange = updateSettings,
                onSessionChanged = { newSession ->
                    sessionRef.set(newSession)
                    session = newSession
                },
            )
        }
    }

    if (traySupported) {
        Tray(
            icon = trayIcon,
            state = trayState,
            tooltip = "PWDog Password Manager",
            onAction = { windowVisible = true },
        ) {
            Item(
                if (session != null) "PWDog - Unlocked" else "PWDog - Locked",
                enabled = false,
                onClick = {},
            )
            Separator()
            Item("Show PWDog", onClick = { windowVisible = true })
            Item(
                "Lock PWDog",
                enabled = session != null,
                onClick = {
                    PasswordClipboard.clearActive()
                    sessionRef.set(null)
                    session?.close()
                    session = null
                    windowVisible = true
                },
            )
            Separator()
            Item("Exit PWDog", onClick = closeApp)
        }
    }
}

@Composable
private fun PwdogApp(
    service: AccountService,
    session: VaultSession?,
    autofill: AutofillRuntime,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onSessionChanged: (VaultSession?) -> Unit,
) {
    var accounts by remember { mutableStateOf<List<Account>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }
    var showRecovery by remember { mutableStateOf(false) }
    var pendingRecoveryKey by remember { mutableStateOf<String?>(null) }
    var pendingRecoveryName by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val selectedAccount = accounts.firstOrNull { it.id == selectedId }
    val activeSession = session

    suspend fun refreshAccounts() {
        val loaded = withContext(Dispatchers.IO) { service.listAccounts() }
        accounts = loaded
        if (selectedId == null || loaded.none { it.id == selectedId }) {
            selectedId = loaded.firstOrNull()?.id
        }
        if (loaded.isEmpty()) {
            showCreate = true
        }
    }

    LaunchedEffect(Unit) {
        try {
            refreshAccounts()
        } catch (t: Throwable) {
            loadError = friendlyError(t)
        } finally {
            loading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown || event.key != Key.Escape) {
                    return@onPreviewKeyEvent false
                }
                when {
                    pendingRecoveryKey != null -> false
                    showRecovery -> {
                        showRecovery = false
                        error = null
                        true
                    }
                    showCreate && accounts.isNotEmpty() -> {
                        showCreate = false
                        error = null
                        true
                    }
                    else -> false
                }
            },
    ) {
        when {
            loading -> QuietLoading()
            loadError != null -> QuietLoadError(
                message = requireNotNull(loadError),
                onRetry = {
                    loading = true
                    loadError = null
                    scope.launch {
                        try {
                            refreshAccounts()
                        } catch (t: Throwable) {
                            loadError = friendlyError(t)
                        } finally {
                            loading = false
                        }
                    }
                },
            )
            activeSession != null -> {
                VaultScreen(
                    session = activeSession,
                    service = service,
                    autofill = autofill,
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onDeleteAccount = { accountId ->
                        scope.launch {
                            activeSession.close()
                            onSessionChanged(null)
                            runCatching {
                                withContext(Dispatchers.IO) { service.deleteAccount(accountId) }
                            }.onFailure { error = friendlyError(it) }
                            runCatching { refreshAccounts() }
                                .onFailure { error = friendlyError(it) }
                        }
                    },
                    onLock = {
                        activeSession.close()
                        onSessionChanged(null)
                        error = null
                        scope.launch {
                            runCatching { refreshAccounts() }
                                .onFailure { error = friendlyError(it) }
                        }
                    },
                )
                RecoveryKeySheet(
                    visible = pendingRecoveryKey != null,
                    accountName = pendingRecoveryName,
                    recoveryKey = pendingRecoveryKey.orEmpty(),
                    onContinue = { pendingRecoveryKey = null },
                )
            }
            else -> {
                LoginScreen(
                    accounts = accounts,
                    selectedId = selectedId,
                    busy = busy,
                    error = error,
                    settings = settings,
                    onSettingsChange = onSettingsChange,
                    onSelect = {
                        selectedId = it
                        error = null
                    },
                    onCreateNew = {
                        showCreate = true
                        error = null
                    },
                    onUseRecoveryKey = {
                        showRecovery = true
                        error = null
                    },
                    onQuickUnlock = { accountId ->
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val verified = withContext(Dispatchers.IO) {
                                    WindowsQuickUnlock.verify("解锁 PWDog")
                                }
                                if (!verified) {
                                    error = "Windows Hello 不可用或验证未通过"
                                    return@launch
                                }
                                val unlocked = withContext(Dispatchers.IO) {
                                    service.unlockWithQuickUnlock(
                                        accountId,
                                        WindowsQuickUnlock::unprotect,
                                    )
                                }
                                refreshAccounts()
                                onSessionChanged(unlocked)
                            } catch (t: Throwable) {
                                error = friendlyError(t)
                            } finally {
                                busy = false
                            }
                        }
                    },
                    onUnlock = { accountId, password ->
                        busy = true
                        error = null
                        scope.launch {
                            val passwordBytes = password.toByteArray(Charsets.UTF_8)
                            try {
                                val unlocked = withContext(Dispatchers.IO) {
                                    service.unlock(accountId, passwordBytes)
                                }
                                refreshAccounts()
                                onSessionChanged(unlocked)
                            } catch (t: Throwable) {
                                error = friendlyError(t)
                            } finally {
                                passwordBytes.fill(0)
                                busy = false
                            }
                        }
                    },
                )

                CreateAccountSheet(
                    visible = showCreate,
                    canGoBack = accounts.isNotEmpty(),
                    busy = busy,
                    error = error,
                    onDismiss = {
                        showCreate = false
                        error = null
                    },
                    onCreate = { displayName, password, confirmation ->
                        when {
                            displayName.isBlank() -> error = "请输入账户名称"
                            password.length < 8 -> error = "主密码至少需要 8 个字符"
                            password != confirmation -> error = "两次输入的主密码不一致"
                            else -> {
                                busy = true
                                error = null
                                scope.launch {
                                    val passwordBytes = password.toByteArray(Charsets.UTF_8)
                                    try {
                                        val created = withContext(Dispatchers.IO) {
                                            service.createAccount(displayName, passwordBytes)
                                        }
                                        refreshAccounts()
                                        selectedId = created.session.account.id
                                        showCreate = false
                                        pendingRecoveryName = created.session.account.displayName
                                        pendingRecoveryKey = created.recoveryKey
                                        onSessionChanged(created.session)
                                    } catch (t: Throwable) {
                                        error = friendlyError(t)
                                    } finally {
                                        passwordBytes.fill(0)
                                        busy = false
                                    }
                                }
                            }
                        }
                    },
                )

                RecoverAccountSheet(
                    visible = showRecovery && selectedAccount != null,
                    account = selectedAccount,
                    busy = busy,
                    error = error,
                    onDismiss = {
                        showRecovery = false
                        error = null
                    },
                    onRecover = { recoveryKey, newPassword, confirmation ->
                        when {
                            recoveryKey.isBlank() -> error = "请输入恢复密钥"
                            newPassword.length < 8 -> error = "新主密码至少需要 8 个字符"
                            newPassword != confirmation -> error = "两次输入的新密码不一致"
                            else -> {
                                busy = true
                                error = null
                                val account = selectedAccount ?: return@RecoverAccountSheet
                                scope.launch {
                                    val newPasswordBytes = newPassword.toByteArray(Charsets.UTF_8)
                                    try {
                                        val recovered = withContext(Dispatchers.IO) {
                                            service.recoverAccount(
                                                account.id,
                                                recoveryKey,
                                                newPasswordBytes,
                                            )
                                        }
                                        refreshAccounts()
                                        showRecovery = false
                                        onSessionChanged(recovered)
                                    } catch (t: Throwable) {
                                        error = friendlyError(t)
                                    } finally {
                                        newPasswordBytes.fill(0)
                                        busy = false
                                    }
                                }
                            }
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun QuietLoading() {
    val quiet = LocalQuiet.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = quiet.accent,
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.height(16.dp))
        Text("正在读取本地账户...", color = quiet.muted, style = QuietType.small)
    }
}

@Composable
private fun QuietLoadError(message: String, onRetry: () -> Unit) {
    val quiet = LocalQuiet.current
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("无法打开本地密码库", color = quiet.text, style = QuietType.pageTitle)
        Spacer(Modifier.height(12.dp))
        QuietNotice(message, NoticeTone.Error)
        Spacer(Modifier.height(18.dp))
        QuietButton("重试", onRetry, style = QuietButtonStyle.Primary)
    }
}

private fun loadAppIcon(): Painter {
    val bytes = Thread.currentThread().contextClassLoader
        ?.getResourceAsStream("tray-icon.png")
        ?.use { it.readBytes() }
        ?: error("Missing tray-icon.png")
    return BitmapPainter(Image.makeFromEncoded(bytes).toComposeImageBitmap())
}

internal fun friendlyError(t: Throwable): String = when (t) {
    is com.pwdog.core.account.InvalidPasswordException -> t.message ?: "密码错误"
    is IllegalArgumentException -> t.message ?: "输入不合法"
    else -> t.message?.takeIf { it.isNotBlank() } ?: "操作失败"
}
