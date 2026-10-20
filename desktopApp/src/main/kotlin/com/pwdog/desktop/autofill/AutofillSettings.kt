package com.pwdog.desktop.autofill

import com.pwdog.core.account.AppPaths
import com.pwdog.core.account.VaultSession
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermission
import java.security.SecureRandom
import java.util.HexFormat

data class AutofillSettings(
    val port: Int,
    val token: String,
) {
    val apiUrl: String
        get() = "http://127.0.0.1:$port"
}

object AutofillConfig {
    const val DEFAULT_PORT = 27831

    fun loadOrCreate(baseDir: Path = AppPaths.defaultBaseDir()): AutofillSettings {
        Files.createDirectories(baseDir)
        val tokenFile = baseDir.resolve("extension-token.txt")

        val existing = if (Files.exists(tokenFile)) {
            runCatching { Files.readString(tokenFile).trim() }.getOrNull()
        } else {
            null
        }

        val token = if (existing != null && TOKEN_PATTERN.matches(existing)) {
            existing
        } else {
            HexFormat.of()
                .formatHex(ByteArray(32).also { SecureRandom().nextBytes(it) })
                .also {
                    Files.writeString(
                        tokenFile,
                        it,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE,
                    )
                    runCatching {
                        Files.setPosixFilePermissions(
                            tokenFile,
                            setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE),
                        )
                    }
                }
        }

        val port = System.getenv("PWDOG_AUTOFILL_PORT")
            ?.toIntOrNull()
            ?.takeIf { it in 1..65535 }
            ?: DEFAULT_PORT

        return AutofillSettings(port, token)
    }

    private val TOKEN_PATTERN = Regex("[0-9a-f]{64}")
}

class AutofillRuntime(
    sessionProvider: () -> VaultSession?,
) : AutoCloseable {
    val settings: AutofillSettings?
    val error: String?
    private val server: AutofillServer?

    init {
        val loaded = runCatching { AutofillConfig.loadOrCreate() }
        settings = loaded.getOrNull()

        if (settings == null) {
            server = null
            error = "读取自动填充配置失败：${loaded.exceptionOrNull()?.message ?: "未知错误"}"
        } else {
            val candidate = AutofillServer(settings.port, settings.token, sessionProvider)
            val started = runCatching { candidate.start() }
            if (started.isSuccess) {
                server = candidate
                error = null
            } else {
                server = null
                error = "自动填充服务启动失败（端口 ${settings.port}）：${started.exceptionOrNull()?.message ?: "未知错误"}"
            }
        }
    }

    override fun close() {
        server?.close()
    }
}
