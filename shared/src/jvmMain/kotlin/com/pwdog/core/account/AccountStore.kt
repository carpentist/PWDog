package com.pwdog.core.account

import com.pwdog.core.crypto.KdfParams
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Types

class AccountStore(baseDir: Path) : AutoCloseable {
    private val vaultsDir = baseDir.resolve("vaults").toAbsolutePath().normalize()
    private val connection: Connection

    init {
        Files.createDirectories(vaultsDir)
        Class.forName("org.sqlite.JDBC")
        val registryPath = baseDir.resolve("accounts.db").toAbsolutePath().toString().replace('\\', '/')
        connection = DriverManager.getConnection("jdbc:sqlite:$registryPath")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode=WAL")
            statement.executeUpdate(
                """
                create table if not exists accounts (
                  id text primary key,
                  display_name text not null collate nocase unique,
                  kdf_salt blob not null,
                  kdf_memory_kib integer not null,
                  kdf_iterations integer not null,
                  kdf_parallelism integer not null,
                  verifier blob not null,
                  vault_file text not null,
                  created_at integer not null,
                  last_used_at integer,
                  key_version integer not null default 1,
                  wrapped_key blob,
                  key_nonce blob,
                  recovery_wrapped_key blob,
                  recovery_nonce blob,
                  quick_unlock_blob blob,
                  avatar_color integer,
                  avatar_png blob
                )
                """.trimIndent(),
            )
        }
        migrateColumns()
    }

    fun list(): List<Account> {
        connection.prepareStatement(
            """
            select * from accounts
            order by (last_used_at is null) asc, last_used_at desc, display_name collate nocase asc
            """.trimIndent(),
        ).use { statement ->
            statement.executeQuery().use { rs ->
                val accounts = mutableListOf<Account>()
                while (rs.next()) accounts += rs.toAccount()
                return accounts
            }
        }
    }

    fun findById(id: String): Account? {
        connection.prepareStatement("select * from accounts where id = ?").use { statement ->
            statement.setString(1, id)
            statement.executeQuery().use { rs ->
                return if (rs.next()) rs.toAccount() else null
            }
        }
    }

    fun existsByDisplayName(displayName: String): Boolean {
        connection.prepareStatement("select 1 from accounts where display_name = ?").use { statement ->
            statement.setString(1, displayName)
            statement.executeQuery().use { rs -> return rs.next() }
        }
    }

    fun insert(account: Account) {
        connection.prepareStatement(
            """
            insert into accounts(
              id, display_name, kdf_salt, kdf_memory_kib, kdf_iterations,
              kdf_parallelism, verifier, vault_file, created_at, last_used_at,
              key_version, wrapped_key, key_nonce, recovery_wrapped_key, recovery_nonce,
              quick_unlock_blob
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, account.id)
            statement.setString(2, account.displayName)
            statement.setBytes(3, account.kdfSalt)
            statement.setInt(4, account.kdfParams.memoryKiB)
            statement.setInt(5, account.kdfParams.iterations)
            statement.setInt(6, account.kdfParams.parallelism)
            statement.setBytes(7, account.verifier)
            statement.setString(8, account.vaultFile.fileName.toString())
            statement.setLong(9, account.createdAt)
            account.lastUsedAt?.let { statement.setLong(10, it) }
                ?: statement.setNull(10, Types.BIGINT)
            statement.setInt(11, account.keyVersion)
            statement.setBytesOrNull(12, account.wrappedKey)
            statement.setBytesOrNull(13, account.keyNonce)
            statement.setBytesOrNull(14, account.recoveryWrappedKey)
            statement.setBytesOrNull(15, account.recoveryNonce)
            statement.setBytesOrNull(16, account.quickUnlockBlob)
            statement.executeUpdate()
        }
    }

    fun updatePasswordSlot(
        id: String,
        salt: ByteArray,
        params: KdfParams,
        wrappedKey: ByteArray,
        keyNonce: ByteArray,
        verifier: ByteArray,
        lastUsedAt: Long,
    ) {
        connection.prepareStatement(
            """
            update accounts
            set kdf_salt = ?, kdf_memory_kib = ?, kdf_iterations = ?, kdf_parallelism = ?,
                wrapped_key = ?, key_nonce = ?, verifier = ?, key_version = 2, last_used_at = ?
            where id = ?
            """.trimIndent(),
        ).use { statement ->
            statement.setBytes(1, salt)
            statement.setInt(2, params.memoryKiB)
            statement.setInt(3, params.iterations)
            statement.setInt(4, params.parallelism)
            statement.setBytes(5, wrappedKey)
            statement.setBytes(6, keyNonce)
            statement.setBytes(7, verifier)
            statement.setLong(8, lastUsedAt)
            statement.setString(9, id)
            statement.executeUpdate()
        }
    }

    fun setQuickUnlockBlob(id: String, blob: ByteArray?) {
        connection.prepareStatement("update accounts set quick_unlock_blob = ? where id = ?").use { statement ->
            statement.setBytesOrNull(1, blob)
            statement.setString(2, id)
            statement.executeUpdate()
        }
    }

    fun updateProfile(
        id: String,
        displayName: String,
        avatarColor: Int?,
        avatarImage: ByteArray?,
    ) {
        connection.prepareStatement(
            "update accounts set display_name = ?, avatar_color = ?, avatar_png = ? where id = ?",
        ).use { statement ->
            statement.setString(1, displayName)
            if (avatarColor == null) {
                statement.setNull(2, Types.INTEGER)
            } else {
                statement.setInt(2, avatarColor)
            }
            statement.setBytesOrNull(3, avatarImage)
            statement.setString(4, id)
            statement.executeUpdate()
        }
    }

    fun delete(id: String) {
        connection.prepareStatement("delete from accounts where id = ?").use { statement ->
            statement.setString(1, id)
            statement.executeUpdate()
        }
    }

    fun touchLastUsed(id: String, timestamp: Long) {
        connection.prepareStatement("update accounts set last_used_at = ? where id = ?").use { statement ->
            statement.setLong(1, timestamp)
            statement.setString(2, id)
            statement.executeUpdate()
        }
    }

    override fun close() {
        connection.close()
    }

    private fun migrateColumns() {
        val columns = mutableSetOf<String>()
        connection.createStatement().use { statement ->
            statement.executeQuery("PRAGMA table_info(accounts)").use { rs ->
                while (rs.next()) columns += rs.getString("name")
            }
            addColumnIfMissing(columns, "key_version", "key_version integer not null default 1")
            addColumnIfMissing(columns, "wrapped_key", "wrapped_key blob")
            addColumnIfMissing(columns, "key_nonce", "key_nonce blob")
            addColumnIfMissing(columns, "recovery_wrapped_key", "recovery_wrapped_key blob")
            addColumnIfMissing(columns, "recovery_nonce", "recovery_nonce blob")
            addColumnIfMissing(columns, "quick_unlock_blob", "quick_unlock_blob blob")
            addColumnIfMissing(columns, "avatar_color", "avatar_color integer")
            addColumnIfMissing(columns, "avatar_png", "avatar_png blob")
        }
    }

    private fun addColumnIfMissing(columns: Set<String>, name: String, definition: String) {
        if (name in columns) return
        connection.createStatement().use { statement ->
            statement.executeUpdate("alter table accounts add column $definition")
        }
    }

    private fun ResultSet.toAccount(): Account {
        val salt = getBytes("kdf_salt")
        val vaultFile = vaultsDir.resolve(getString("vault_file")).normalize()
        check(vaultFile.parent == vaultsDir) { "invalid vault file path" }
        val lastUsed = getLong("last_used_at").let { value -> if (wasNull()) null else value }

        return Account(
            id = getString("id"),
            displayName = getString("display_name"),
            kdfSalt = salt,
            kdfParams = KdfParams(
                memoryKiB = getInt("kdf_memory_kib"),
                iterations = getInt("kdf_iterations"),
                parallelism = getInt("kdf_parallelism"),
                saltLength = salt.size,
                keyLength = 32,
            ),
            verifier = getBytes("verifier"),
            vaultFile = vaultFile,
            createdAt = getLong("created_at"),
            lastUsedAt = lastUsed,
            keyVersion = getInt("key_version"),
            wrappedKey = getBytes("wrapped_key"),
            keyNonce = getBytes("key_nonce"),
            recoveryWrappedKey = getBytes("recovery_wrapped_key"),
            recoveryNonce = getBytes("recovery_nonce"),
            quickUnlockBlob = getBytes("quick_unlock_blob"),
            avatarColor = getInt("avatar_color").let { value -> if (wasNull()) null else value },
            avatarImage = getBytes("avatar_png"),
        )
    }

    private fun java.sql.PreparedStatement.setBytesOrNull(index: Int, value: ByteArray?) {
        if (value == null) setNull(index, Types.BLOB) else setBytes(index, value)
    }
}
