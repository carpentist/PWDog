package com.pwdog.core.db

import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EncryptedDatabaseTest {
    @Test
    fun writesReopensAndRejectsWrongKey() {
        val dir = Files.createTempDirectory("pwdog-db-test-")
        val dbPath = dir.resolve("vault.db")
        val key = ByteArray(32) { it.toByte() }

        try {
            EncryptedDatabase(dbPath, key).use { db ->
                db.open().use { conn ->
                    conn.createStatement().use { statement ->
                        statement.executeUpdate(
                            "create table entries (" +
                                "id integer primary key, " +
                                "title text not null, " +
                                "password text not null)",
                        )
                        statement.executeUpdate(
                            "insert into entries(title, password) values ('example.com', 'secret')",
                        )
                    }
                }
            }

            val header = Files.newInputStream(dbPath).use { it.readNBytes(16) }
            val plaintextMagic = byteArrayOf(
                0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20,
                0x66, 0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20,
                0x33, 0x00,
            )
            assertFalse(header.contentEquals(plaintextMagic))

            EncryptedDatabase(dbPath, key).use { db ->
                db.open().use { conn ->
                    conn.createStatement().use { statement ->
                        statement.executeQuery("select count(*) from entries").use { rs ->
                            assertTrue(rs.next())
                            assertEquals(1, rs.getInt(1))
                        }
                    }
                }
            }

            val wrongKeyFailed = runCatching {
                EncryptedDatabase(dbPath, ByteArray(32) { 9 }).use { db ->
                    db.open().use { conn ->
                        conn.createStatement().use { statement ->
                            statement.executeQuery("select count(*) from entries").use { rs ->
                                rs.next()
                            }
                        }
                    }
                }
            }.isFailure

            assertTrue(wrongKeyFailed)
        } finally {
            key.fill(0)
            deleteRecursively(dir)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
