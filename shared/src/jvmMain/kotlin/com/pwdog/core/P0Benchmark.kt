package com.pwdog.core

import com.pwdog.core.db.EncryptedDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.system.measureNanoTime

data class BenchmarkReport(
    val entryCount: Int,
    val seedMillis: Double,
    val exactDomainMicros: Double,
    val substringSearchMillis: Double,
    val reopenMillis: Double,
    val fileSizeBytes: Long,
    val workDir: Path,
)

object P0Benchmark {
    fun run(entryCount: Int = 10_000): BenchmarkReport {
        require(entryCount >= 1)
        val workDir = Files.createTempDirectory("pwdog-bench-")
        val dbPath = workDir.resolve("vault.db")
        val key = ByteArray(32) { (it * 31 + 17).toByte() }

        try {
            var seedNanos = 0L
            var exactNanos = 0.0
            var substringNanos = 0.0

            EncryptedDatabase(dbPath, key).use { db ->
                val conn = db.open()
                conn.createStatement().use { statement ->
                    statement.executeUpdate(
                        """
                        create table entries (
                          id integer primary key,
                          title text not null,
                          domain text not null,
                          username text not null,
                          password text not null
                        )
                        """.trimIndent(),
                    )
                    statement.executeUpdate("create index idx_entries_domain on entries(domain)")
                    statement.executeUpdate("create index idx_entries_title on entries(title)")
                }

                seedNanos = measureNanoTime {
                    conn.autoCommit = false
                    conn.prepareStatement(
                        "insert into entries(title, domain, username, password) values(?, ?, ?, ?)",
                    ).use { ps ->
                        for (i in 0 until entryCount) {
                            ps.setString(1, "Site ${i.toString().padStart(5, '0')}")
                            ps.setString(2, "site-$i.example.com")
                            ps.setString(3, "user$i")
                            ps.setString(4, "secret-$i")
                            ps.addBatch()
                        }
                        ps.executeBatch()
                    }
                    conn.commit()
                    conn.autoCommit = true
                    conn.createStatement().use { it.execute("ANALYZE") }
                }

                exactNanos = averageNanos(100) {
                    conn.prepareStatement(
                        "select id, title, username from entries where domain = ? limit 5",
                    ).use { ps ->
                        ps.setString(1, "site-5000.example.com")
                        ps.executeQuery().use { rs -> rs.next() }
                    }
                }

                substringNanos = averageNanos(20) {
                    conn.prepareStatement(
                        "select id, title from entries where title like ? limit 20",
                    ).use { ps ->
                        ps.setString(1, "%Site 09999%")
                        ps.executeQuery().use { rs -> rs.next() }
                    }
                }

                val actualCount = conn.createStatement().use { statement ->
                    statement.executeQuery("select count(*) from entries").use { rs ->
                        if (rs.next()) rs.getInt(1) else 0
                    }
                }
                check(actualCount == entryCount) {
                    "expected $entryCount rows, found $actualCount"
                }
            }

            val reopenNanos = measureNanoTime {
                EncryptedDatabase(dbPath, key).use { db ->
                    db.open().createStatement().use { statement ->
                        statement.executeQuery("select count(*) from entries").use { rs ->
                            rs.next()
                        }
                    }
                }
            }

            return BenchmarkReport(
                entryCount = entryCount,
                seedMillis = seedNanos / 1_000_000.0,
                exactDomainMicros = exactNanos / 1_000.0,
                substringSearchMillis = substringNanos / 1_000_000.0,
                reopenMillis = reopenNanos / 1_000_000.0,
                fileSizeBytes = Files.size(dbPath),
                workDir = workDir,
            )
        } finally {
            key.fill(0)
        }
    }

    fun cleanup(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private inline fun averageNanos(iterations: Int, block: () -> Unit): Double {
        block()
        var total = 0L
        repeat(iterations) {
            total += measureNanoTime { block() }
        }
        return total.toDouble() / iterations
    }
}
