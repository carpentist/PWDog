package com.pwdog.core.entry

import java.sql.Connection
import java.sql.ResultSet
import java.time.Instant
import java.util.UUID

data class ImportSummary(
    val imported: Int,
    val duplicates: Int,
)

class EntryRepository(private val connection: Connection) {
    fun search(query: String): List<VaultEntry> = synchronized(connection) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            connection.prepareStatement(
                "select * from entries order by updated_at desc",
            ).use { statement ->
                statement.executeQuery().use { rs -> rs.readAll() }
            }
        } else {
            val pattern = "%${escapeLike(trimmed)}%"
            connection.prepareStatement(
                """
                select * from entries
                where title like ? escape '!'
                   or url like ? escape '!'
                   or username like ? escape '!'
                order by updated_at desc
                """.trimIndent(),
            ).use { statement ->
                statement.setString(1, pattern)
                statement.setString(2, pattern)
                statement.setString(3, pattern)
                statement.executeQuery().use { rs -> rs.readAll() }
            }
        }
    }

    fun findByHost(host: String): List<VaultEntry> = synchronized(connection) {
        search("").filter { HostMatcher.matches(it.url, host) }
    }

    fun get(id: String): VaultEntry? = synchronized(connection) {
        connection.prepareStatement("select * from entries where id = ?").use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { rs ->
                if (rs.next()) rs.toEntry() else null
            }
        }
    }

    fun create(
        title: String,
        url: String,
        username: String,
        password: String,
        notes: String,
    ): VaultEntry = synchronized(connection) {
        require(title.isNotBlank()) { "标题不能为空" }
        require(password.isNotEmpty()) { "密码不能为空" }

        val id = UUID.randomUUID().toString()
        val now = Instant.now().toEpochMilli()
        connection.prepareStatement(
            """
            insert into entries(id, title, url, username, password, notes, created_at, updated_at)
            values (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, id)
            statement.setString(2, title.trim())
            statement.setString(3, url.trim())
            statement.setString(4, username)
            statement.setString(5, password)
            statement.setString(6, notes)
            statement.setLong(7, now)
            statement.setLong(8, now)
            statement.executeUpdate()
        }

        requireNotNull(get(id)) { "创建条目失败" }
    }

    fun update(
        id: String,
        title: String,
        url: String,
        username: String,
        password: String,
        notes: String,
    ): VaultEntry = synchronized(connection) {
        require(title.isNotBlank()) { "标题不能为空" }
        require(password.isNotEmpty()) { "密码不能为空" }

        val now = Instant.now().toEpochMilli()
        val updated = connection.prepareStatement(
            """
            update entries
            set title = ?, url = ?, username = ?, password = ?, notes = ?, updated_at = ?
            where id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, title.trim())
            statement.setString(2, url.trim())
            statement.setString(3, username)
            statement.setString(4, password)
            statement.setString(5, notes)
            statement.setLong(6, now)
            statement.setString(7, id)
            statement.executeUpdate()
        }
        require(updated > 0) { "条目不存在" }

        requireNotNull(get(id)) { "更新条目失败" }
    }

    fun importAll(credentials: List<ImportedCredential>): ImportSummary = synchronized(connection) {
        if (credentials.isEmpty()) return ImportSummary(imported = 0, duplicates = 0)

        val seen = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("select url, username, password from entries").use { rs ->
                while (rs.next()) {
                    seen += importKey(rs.getString(1), rs.getString(2), rs.getString(3))
                }
            }
        }

        var imported = 0
        var duplicates = 0
        val now = Instant.now().toEpochMilli()

        connection.autoCommit = false
        try {
            connection.prepareStatement(
                """
                insert into entries(id, title, url, username, password, notes, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
            ).use { statement ->
                for (credential in credentials) {
                    if (credential.title.isBlank() || credential.password.isEmpty()) continue
                    val key = importKey(
                        credential.url,
                        credential.username,
                        credential.password,
                    )
                    if (!seen.add(key)) {
                        duplicates++
                        continue
                    }
                    statement.setString(1, UUID.randomUUID().toString())
                    statement.setString(2, credential.title)
                    statement.setString(3, credential.url)
                    statement.setString(4, credential.username)
                    statement.setString(5, credential.password)
                    statement.setString(6, credential.notes)
                    statement.setLong(7, now)
                    statement.setLong(8, now)
                    statement.addBatch()
                    imported++
                }
                statement.executeBatch()
            }
            connection.commit()
        } catch (t: Throwable) {
            connection.rollback()
            throw t
        } finally {
            connection.autoCommit = true
        }

        ImportSummary(imported = imported, duplicates = duplicates)
    }

    fun delete(id: String): Boolean = synchronized(connection) {
        connection.prepareStatement("delete from entries where id = ?").use { statement ->
            statement.setString(1, id)
            statement.executeUpdate() > 0
        }
    }

    fun count(): Int = synchronized(connection) {
        connection.createStatement().use { statement ->
            statement.executeQuery("select count(*) from entries").use { rs ->
                if (rs.next()) rs.getInt(1) else 0
            }
        }
    }

    private fun ResultSet.readAll(): List<VaultEntry> {
        val entries = mutableListOf<VaultEntry>()
        while (next()) entries += toEntry()
        return entries
    }

    private fun ResultSet.toEntry(): VaultEntry = VaultEntry(
        id = getString("id"),
        title = getString("title"),
        url = getString("url"),
        username = getString("username"),
        password = getString("password"),
        notes = getString("notes"),
        createdAt = getLong("created_at"),
        updatedAt = getLong("updated_at"),
    )

    private fun escapeLike(value: String): String =
        value.replace("!", "!!").replace("%", "!%").replace("_", "!_")

    private fun importKey(url: String, username: String, password: String): String =
        "${url.trim().lowercase()}$username$password"
}
