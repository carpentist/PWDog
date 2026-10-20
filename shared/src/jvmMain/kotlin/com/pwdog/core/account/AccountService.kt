package com.pwdog.core.account

import com.pwdog.core.crypto.Argon2id
import com.pwdog.core.crypto.KdfParams
import com.pwdog.core.crypto.KeyWrap
import com.pwdog.core.crypto.RecoveryKey
import com.pwdog.core.crypto.WrappedKey
import com.pwdog.core.db.EncryptedDatabase
import java.nio.file.Files
import java.nio.file.Path
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class AccountService(
    private val baseDir: Path = AppPaths.defaultBaseDir(),
    private val creationKdfParams: KdfParams = KdfParams(),
) : AutoCloseable {
    private val store = AccountStore(baseDir)
    private val random = SecureRandom()

    fun listAccounts(): List<Account> = store.list()

    fun createAccount(displayName: String, password: ByteArray): CreatedAccount {
        val name = displayName.trim()
        require(name.isNotEmpty()) { "请输入账户名称" }
        require(name.length <= 64) { "账户名称不能超过 64 个字符" }
        require(password.size >= 8) { "主密码至少需要 8 个字符" }
        require(!store.existsByDisplayName(name)) { "账户名称已存在" }

        val id = UUID.randomUUID().toString()
        val salt = Argon2id.randomSalt(creationKdfParams.saltLength)
        val kek = Argon2id.deriveKey(password, salt, creationKdfParams)
        val dek = ByteArray(32).also { random.nextBytes(it) }
        val recoveryBytes = ByteArray(32).also { random.nextBytes(it) }
        val recoveryKey = RecoveryKey.format(recoveryBytes)
        val vaultDir = baseDir.resolve("vaults")
        Files.createDirectories(vaultDir)
        val vaultFile = vaultDir.resolve("$id.db")

        var inserted = false
        var sessionOwnsDek = false

        try {
            val passwordSlot = KeyWrap.wrap(dek, kek, slotAad(id, "password"))
            val recoverySlot = KeyWrap.wrap(dek, recoveryBytes, slotAad(id, "recovery"))
            val verifier = computeVerifier(dek, id)

            EncryptedDatabase(vaultFile, dek).use { database ->
                VaultSchema.create(database.open())
            }

            val now = Instant.now().toEpochMilli()
            val account = Account(
                id = id,
                displayName = name,
                kdfSalt = salt,
                kdfParams = creationKdfParams,
                verifier = verifier,
                vaultFile = vaultFile,
                createdAt = now,
                lastUsedAt = now,
                keyVersion = 2,
                wrappedKey = passwordSlot.ciphertext,
                keyNonce = passwordSlot.nonce,
                recoveryWrappedKey = recoverySlot.ciphertext,
                recoveryNonce = recoverySlot.nonce,
            )

            store.insert(account)
            inserted = true

            val database = EncryptedDatabase(vaultFile, dek)
            try {
                database.open()
            } catch (t: Throwable) {
                database.close()
                throw t
            }

            sessionOwnsDek = true
            return CreatedAccount(
                session = VaultSession(account, database, dek),
                recoveryKey = recoveryKey,
            )
        } catch (t: Throwable) {
            if (inserted) runCatching { store.delete(id) }
            Files.deleteIfExists(vaultFile)
            throw t
        } finally {
            kek.fill(0)
            recoveryBytes.fill(0)
            if (!sessionOwnsDek) dek.fill(0)
        }
    }

    fun unlock(accountId: String, password: ByteArray): VaultSession {
        val account = store.findById(accountId) ?: throw IllegalArgumentException("账户不存在")
        val key = if (
            account.keyVersion >= 2 &&
            account.wrappedKey != null &&
            account.keyNonce != null
        ) {
            unwrapPasswordKey(account, password)
        } else {
            deriveLegacyKey(account, password)
        }

        return openSession(account, key)
    }

    fun recoverAccount(
        accountId: String,
        recoveryKeyText: String,
        newPassword: ByteArray,
    ): VaultSession {
        require(newPassword.size >= 8) { "新主密码至少需要 8 个字符" }
        val account = store.findById(accountId) ?: throw IllegalArgumentException("账户不存在")
        require(
            account.keyVersion >= 2 &&
                account.recoveryWrappedKey != null &&
                account.recoveryNonce != null,
        ) {
            "该账户没有恢复密钥，请使用主密码登录"
        }

        val recoveryBytes = RecoveryKey.parse(recoveryKeyText)
            ?: throw IllegalArgumentException("恢复密钥格式不正确")

        val dek = try {
            KeyWrap.unwrap(
                WrappedKey(account.recoveryNonce, account.recoveryWrappedKey),
                recoveryBytes,
                slotAad(account.id, "recovery"),
            )
        } catch (_: GeneralSecurityException) {
            throw InvalidPasswordException("恢复密钥错误")
        } finally {
            recoveryBytes.fill(0)
        }

        if (!MessageDigest.isEqual(account.verifier, computeVerifier(dek, account.id))) {
            dek.fill(0)
            throw IllegalArgumentException("账户密钥数据损坏")
        }

        val newSalt = Argon2id.randomSalt(creationKdfParams.saltLength)
        val newKek = Argon2id.deriveKey(newPassword, newSalt, creationKdfParams)
        val passwordSlot = try {
            KeyWrap.wrap(dek, newKek, slotAad(account.id, "password"))
        } catch (t: Throwable) {
            dek.fill(0)
            throw t
        } finally {
            newKek.fill(0)
        }

        val now = Instant.now().toEpochMilli()
        val verifier = computeVerifier(dek, account.id)
        try {
            store.updatePasswordSlot(
                id = account.id,
                salt = newSalt,
                params = creationKdfParams,
                wrappedKey = passwordSlot.ciphertext,
                keyNonce = passwordSlot.nonce,
                verifier = verifier,
                lastUsedAt = now,
            )
        } catch (t: Throwable) {
            dek.fill(0)
            throw t
        }

        val updated = account.copy(
            kdfSalt = newSalt,
            kdfParams = creationKdfParams,
            verifier = verifier,
            wrappedKey = passwordSlot.ciphertext,
            keyNonce = passwordSlot.nonce,
            keyVersion = 2,
            lastUsedAt = now,
        )

        return openSession(updated, dek)
    }

    fun enableQuickUnlock(
        session: VaultSession,
        protect: (ByteArray) -> ByteArray,
    ) {
        require(session.account.keyVersion >= 2) { "该账户不支持快速解锁" }

        val key = session.keyCopy()
        val payload = try {
            buildQuickUnlockPayload(session.account.id, key)
        } finally {
            key.fill(0)
        }

        val blob = try {
            protect(payload)
        } finally {
            payload.fill(0)
        }
        require(blob.isNotEmpty()) { "快速解锁数据无效" }

        store.setQuickUnlockBlob(session.account.id, blob)
    }

    fun disableQuickUnlock(accountId: String) {
        store.setQuickUnlockBlob(accountId, null)
    }

    fun unlockWithQuickUnlock(
        accountId: String,
        unprotect: (ByteArray) -> ByteArray,
    ): VaultSession {
        val account = store.findById(accountId) ?: throw IllegalArgumentException("账户不存在")
        val blob = account.quickUnlockBlob
            ?: throw IllegalArgumentException("该账户未启用 Windows Hello 快速解锁")

        val payload = try {
            unprotect(blob)
        } catch (t: Throwable) {
            throw InvalidPasswordException("快速解锁数据无效，请使用主密码", t)
        }

        val key = try {
            parseQuickUnlockPayload(payload, account.id)
        } catch (t: Throwable) {
            throw InvalidPasswordException("快速解锁数据无效，请使用主密码", t)
        } finally {
            payload.fill(0)
        }

        if (!MessageDigest.isEqual(account.verifier, computeVerifier(key, account.id))) {
            key.fill(0)
            throw InvalidPasswordException("快速解锁数据无效，请使用主密码")
        }

        return openSession(account, key)
    }

    fun changePassword(
        session: VaultSession,
        currentPassword: ByteArray,
        newPassword: ByteArray,
    ): Account {
        require(newPassword.size >= 8) { "新主密码至少需要 8 个字符" }
        val account = session.account
        require(account.keyVersion >= 2) { "旧版账户不支持直接修改主密码" }

        val currentKey = unwrapPasswordKey(account, currentPassword)
        try {
            val sessionKey = session.keyCopy()
            try {
                if (!MessageDigest.isEqual(currentKey, sessionKey)) {
                    throw IllegalStateException("账户会话密钥不一致，请重新登录")
                }
            } finally {
                sessionKey.fill(0)
            }

            val newSalt = Argon2id.randomSalt(creationKdfParams.saltLength)
            val newKek = Argon2id.deriveKey(newPassword, newSalt, creationKdfParams)
            val passwordSlot = try {
                KeyWrap.wrap(currentKey, newKek, slotAad(account.id, "password"))
            } finally {
                newKek.fill(0)
            }

            val verifier = computeVerifier(currentKey, account.id)
            val now = Instant.now().toEpochMilli()
            store.updatePasswordSlot(
                id = account.id,
                salt = newSalt,
                params = creationKdfParams,
                wrappedKey = passwordSlot.ciphertext,
                keyNonce = passwordSlot.nonce,
                verifier = verifier,
                lastUsedAt = now,
            )

            val updated = account.copy(
                kdfSalt = newSalt,
                kdfParams = creationKdfParams,
                verifier = verifier,
                wrappedKey = passwordSlot.ciphertext,
                keyNonce = passwordSlot.nonce,
                keyVersion = 2,
                lastUsedAt = now,
            )
            session.updateAccountMetadata(updated)
            return updated
        } finally {
            currentKey.fill(0)
        }
    }

    fun deleteAccount(accountId: String) {
        val account = store.findById(accountId) ?: return
        val vault = account.vaultFile
        // Remove the encrypted vault first so a failure cannot leave the data behind
        // under a deleted registry entry.
        listOf(
            vault,
            Path.of("$vault-wal"),
            Path.of("$vault-shm"),
            Path.of("$vault-journal"),
        ).forEach { Files.deleteIfExists(it) }
        store.delete(accountId)
    }

    fun updateProfile(
        accountId: String,
        displayName: String,
        avatarColor: Int?,
        avatarImage: ByteArray?,
    ): Account {
        val name = displayName.trim()
        require(name.isNotEmpty()) { "请输入账户名称" }
        require(name.length <= 64) { "账户名称不能超过 64 个字符" }
        require(avatarImage == null || avatarImage.size <= 512 * 1024) { "头像图片不能超过 512 KB" }

        val existing = store.findById(accountId) ?: throw IllegalArgumentException("账户不存在")
        if (!existing.displayName.equals(name, ignoreCase = true)) {
            require(!store.existsByDisplayName(name)) { "账户名称已存在" }
        }

        store.updateProfile(accountId, name, avatarColor, avatarImage)
        return store.findById(accountId) ?: throw IllegalStateException("账户不存在")
    }

    override fun close() {
        store.close()
    }

    private fun openSession(account: Account, key: ByteArray): VaultSession {
        val database = EncryptedDatabase(account.vaultFile, key)
        try {
            database.open()
        } catch (t: Throwable) {
            database.close()
            key.fill(0)
            throw t
        }

        store.touchLastUsed(account.id, Instant.now().toEpochMilli())
        return VaultSession(account, database, key)
    }

    private fun unwrapPasswordKey(account: Account, password: ByteArray): ByteArray {
        val kek = Argon2id.deriveKey(password, account.kdfSalt, account.kdfParams)
        val wrapped = WrappedKey(
            nonce = requireNotNull(account.keyNonce),
            ciphertext = requireNotNull(account.wrappedKey),
        )

        val dek = try {
            KeyWrap.unwrap(wrapped, kek, slotAad(account.id, "password"))
        } catch (_: GeneralSecurityException) {
            throw InvalidPasswordException("主密码错误")
        } finally {
            kek.fill(0)
        }

        if (!MessageDigest.isEqual(account.verifier, computeVerifier(dek, account.id))) {
            dek.fill(0)
            throw InvalidPasswordException("主密码错误")
        }
        return dek
    }

    private fun deriveLegacyKey(account: Account, password: ByteArray): ByteArray {
        val key = Argon2id.deriveKey(password, account.kdfSalt, account.kdfParams)
        if (!MessageDigest.isEqual(account.verifier, computeVerifier(key, account.id))) {
            key.fill(0)
            throw InvalidPasswordException("主密码错误")
        }
        return key
    }

    private fun computeVerifier(key: ByteArray, accountId: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        mac.update("PWDog account verifier v1|".toByteArray(Charsets.UTF_8))
        mac.update(accountId.toByteArray(Charsets.UTF_8))
        return mac.doFinal()
    }

    private fun slotAad(accountId: String, slot: String): ByteArray =
        "PWDog key slot v1|$slot|$accountId".toByteArray(Charsets.UTF_8)

    private fun buildQuickUnlockPayload(accountId: String, key: ByteArray): ByteArray {
        require(key.size == 32) { "key must be 32 bytes" }
        val accountBytes = accountId.toByteArray(Charsets.UTF_8)
        val payload = ByteArray(1 + accountBytes.size + 1 + key.size)
        payload[0] = 1
        accountBytes.copyInto(payload, destinationOffset = 1)
        payload[1 + accountBytes.size] = 0
        key.copyInto(payload, destinationOffset = 2 + accountBytes.size)
        return payload
    }

    private fun parseQuickUnlockPayload(payload: ByteArray, expectedAccountId: String): ByteArray {
        require(payload.size >= 34) { "invalid quick unlock payload" }
        require(payload[0] == 1.toByte()) { "unsupported quick unlock payload" }
        val separator = (1 until payload.size).firstOrNull { payload[it] == 0.toByte() }
            ?: throw IllegalArgumentException("invalid quick unlock payload")
        val accountId = String(payload, 1, separator - 1, Charsets.UTF_8)
        require(accountId == expectedAccountId) { "quick unlock data belongs to another account" }
        val key = payload.copyOfRange(separator + 1, payload.size)
        require(key.size == 32) { "invalid quick unlock key" }
        return key
    }
}
