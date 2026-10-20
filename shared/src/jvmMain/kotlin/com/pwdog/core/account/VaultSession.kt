package com.pwdog.core.account

import com.pwdog.core.db.EncryptedDatabase
import java.sql.Connection

class VaultSession internal constructor(
    account: Account,
    private val database: EncryptedDatabase,
    private val key: ByteArray,
) : AutoCloseable {
    var account: Account = account
        private set

    private var closed = false

    val connection: Connection
        get() = database.open()

    internal fun keyCopy(): ByteArray = key.copyOf()

    fun updateAccountMetadata(updated: Account) {
        require(updated.id == account.id) { "account id mismatch" }
        account = updated
    }

    override fun close() {
        if (closed) return
        closed = true
        database.close()
        key.fill(0)
    }
}
