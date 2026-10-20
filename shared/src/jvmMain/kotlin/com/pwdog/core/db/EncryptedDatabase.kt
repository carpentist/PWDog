package com.pwdog.core.db

import org.sqlite.mc.SQLiteMCSqlCipherConfig
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection

class EncryptedDatabase(
    private val path: Path,
    rawKey: ByteArray,
) : AutoCloseable {
    private val key = rawKey.copyOf()
    private var connection: Connection? = null

    init {
        require(key.size == 32) { "SQLCipher raw key must be 32 bytes" }
    }

    fun open(): Connection {
        connection?.let { return it }

        path.parent?.let { Files.createDirectories(it) }
        val config = SQLiteMCSqlCipherConfig.getDefault()
            .withRawUnsaltedKey(key.copyOf())
            .build()
        val url = "jdbc:sqlite:" + path.toAbsolutePath().toString().replace('\\', '/')

        return config.createConnection(url).also { conn ->
            connection = conn
            conn.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL")
                statement.execute("PRAGMA secure_delete=ON")
                statement.execute("PRAGMA temp_store=MEMORY")
            }
        }
    }

    override fun close() {
        connection?.close()
        connection = null
        key.fill(0)
    }
}
