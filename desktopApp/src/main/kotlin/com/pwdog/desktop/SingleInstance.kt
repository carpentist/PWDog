package com.pwdog.desktop

import com.pwdog.core.account.AppPaths
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant

class SingleInstance private constructor(
    private val channel: FileChannel,
    private val lock: FileLock,
    private val showFile: Path,
) : AutoCloseable {
    fun consumeShowRequest(): Boolean {
        if (!Files.exists(showFile)) return false
        return runCatching { Files.deleteIfExists(showFile) }.getOrDefault(false)
    }

    override fun close() {
        runCatching { lock.release() }
        runCatching { channel.close() }
    }

    companion object {
        fun acquire(baseDir: Path = AppPaths.defaultBaseDir()): SingleInstance? {
            Files.createDirectories(baseDir)
            val channel = FileChannel.open(
                baseDir.resolve("instance.lock"),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
            )
            val lock = runCatching { channel.tryLock() }.getOrNull()
            if (lock == null) {
                runCatching { channel.close() }
                return null
            }
            return SingleInstance(channel, lock, baseDir.resolve("show.request"))
        }

        fun requestShow(baseDir: Path = AppPaths.defaultBaseDir()) {
            runCatching {
                Files.createDirectories(baseDir)
                Files.writeString(
                    baseDir.resolve("show.request"),
                    Instant.now().toString(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE,
                )
            }
        }
    }
}
