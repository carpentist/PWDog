package com.pwdog.desktop.security

import com.sun.jna.platform.win32.Crypt32Util
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit

object WindowsQuickUnlock {
    private const val CRYPTPROTECT_UI_FORBIDDEN = 0x1
    private const val HELLO_TIMEOUT_SECONDS = 120L
    private val isWindows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    private val entropy = "PWDog quick unlock v1".toByteArray(StandardCharsets.UTF_8)

    private val scriptFile: Path by lazy {
        val resource = WindowsQuickUnlock::class.java.getResourceAsStream("/hello.ps1")
            ?: error("hello.ps1 is missing")
        val path = Files.createTempFile("pwdog-hello-", ".ps1")
        resource.use { input ->
            Files.newOutputStream(path).use { output -> input.copyTo(output) }
        }
        path.toFile().deleteOnExit()
        path
    }

    fun isHelloAvailable(): Boolean {
        if (!isWindows) return false
        return runCatching {
            val result = runScript("availability")
            result.exitCode == 0 &&
                result.output.lineSequence()
                    .map { it.trim() }
                    .lastOrNull { it.isNotEmpty() } == "Available"
        }.getOrDefault(false)
    }

    fun verify(message: String): Boolean {
        if (!isWindows) return false
        return runCatching {
            runScript("verify", message).exitCode == 0
        }.getOrDefault(false)
    }

    fun protect(payload: ByteArray): ByteArray {
        check(isWindows) { "Windows DPAPI is only available on Windows" }
        return Crypt32Util.cryptProtectData(
            payload,
            entropy,
            CRYPTPROTECT_UI_FORBIDDEN,
            "PWDog quick unlock",
            null,
        )
    }

    fun unprotect(blob: ByteArray): ByteArray {
        check(isWindows) { "Windows DPAPI is only available on Windows" }
        return Crypt32Util.cryptUnprotectData(
            blob,
            entropy,
            CRYPTPROTECT_UI_FORBIDDEN,
            null,
        )
    }

    private fun runScript(action: String, message: String? = null): ScriptResult {
        val command = mutableListOf(
            "powershell.exe",
            "-NoProfile",
            "-NonInteractive",
            "-STA",
            "-ExecutionPolicy",
            "Bypass",
            "-WindowStyle",
            "Hidden",
            "-File",
            scriptFile.toString(),
            "-Action",
            action,
        )
        if (message != null) {
            command += "-Message"
            command += message
        }

        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()

        val finished = process.waitFor(HELLO_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return ScriptResult(exitCode = -1, output = "")
        }

        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        return ScriptResult(exitCode = process.exitValue(), output = output)
    }

    private data class ScriptResult(
        val exitCode: Int,
        val output: String,
    )
}
