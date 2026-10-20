package com.pwdog.core

import com.pwdog.core.crypto.Argon2id
import com.pwdog.core.db.EncryptedDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Statement
import kotlin.system.measureNanoTime

data class SelfTestReport(
    val argon2Millis: Double,
    val dbCreateMillis: Double,
    val dbReadMillis: Double,
    val writeCount: Int,
    val readCount: Int,
    val wrongKeyRejected: Boolean,
    val fileLooksEncrypted: Boolean,
    val fileSizeBytes: Long,
    val sqliteVersion: String,
    val workDir: Path,
)

object P0SelfTest {
    fun run(entryCount: Int = 500): SelfTestReport {
        val workDir = Files.createTempDirectory("pwdog-p0-")
        val dbPath = workDir.resolve("vault.db")
        val salt = Argon2id.randomSalt()
        val password = "correct horse battery staple".toByteArray()
        var key: ByteArray? = null

        try {
            val argonNanos = measureNanoTime {
                key = Argon2id.deriveKey(password, salt)
            }
            val keyBytes = requireNotNull(key)

            var createNanos = 0L
            var readNanos = 0L
            var writeCount = 0
            var readCount = 0
            var sqliteVersion = "unknown"

            EncryptedDatabase(dbPath, keyBytes).use { db ->
                val conn = db.open()
                conn.createStatement().use { statement ->
                    statement.executeQuery("select sqlite_version()").use { rs ->
                        if (rs.next()) sqliteVersion = rs.getString(1)
                    }
                    statement.executeUpdate(
                        "create table entries (" +
                            "id integer primary key, " +
                            "title text not null, " +
                            "username text not null, " +
                            "password text not null)",
                    )
                }

                createNanos = measureNanoTime {
                    conn.autoCommit = false
                    conn.prepareStatement(
                        "insert into entries(title, username, password) values(?, ?, ?)",
                    ).use { ps ->
                        for (i in 0 until entryCount) {
                            ps.setString(1, "site-$i.example.com")
                            ps.setString(2, "user$i")
                            ps.setString(3, "secret-$i")
                            ps.addBatch()
                        }
                        writeCount = ps.executeBatch().sumOf {
                            if (it == Statement.SUCCESS_NO_INFO) 1 else it
                        }
                    }
                    conn.commit()
                    conn.autoCommit = true
                }

                readNanos = measureNanoTime {
                    conn.createStatement().use { statement ->
                        statement.executeQuery("select count(*) from entries").use { rs ->
                            if (rs.next()) readCount = rs.getInt(1)
                        }
                    }
                }
            }

            val fileLooksEncrypted = Files.exists(dbPath) && run {
                val header = Files.newInputStream(dbPath).use { it.readNBytes(16) }
                val plaintextMagic = byteArrayOf(
                    0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20,
                    0x66, 0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20,
                    0x33, 0x00,
                )
                !header.contentEquals(plaintextMagic)
            }

            val wrongKeyRejected = try {
                EncryptedDatabase(dbPath, ByteArray(32) { 7 }).use { db ->
                    db.open().createStatement().use { statement ->
                        statement.executeQuery("select count(*) from entries").use { rs ->
                            rs.next()
                        }
                    }
                }
                false
            } catch (_: Exception) {
                true
            }

            val fileSize = if (Files.exists(dbPath)) Files.size(dbPath) else 0L
            return SelfTestReport(
                argon2Millis = argonNanos / 1_000_000.0,
                dbCreateMillis = createNanos / 1_000_000.0,
                dbReadMillis = readNanos / 1_000_000.0,
                writeCount = writeCount,
                readCount = readCount,
                wrongKeyRejected = wrongKeyRejected,
                fileLooksEncrypted = fileLooksEncrypted,
                fileSizeBytes = fileSize,
                sqliteVersion = sqliteVersion,
                workDir = workDir,
            )
        } finally {
            key?.fill(0)
            password.fill(0)
            salt.fill(0)
        }
    }
}
