package com.pwdog.core.account

import com.pwdog.core.crypto.KdfParams
import java.nio.file.Path

data class Account(
    val id: String,
    val displayName: String,
    val kdfSalt: ByteArray,
    val kdfParams: KdfParams,
    val verifier: ByteArray,
    val vaultFile: Path,
    val createdAt: Long,
    val lastUsedAt: Long?,
    val keyVersion: Int = 1,
    val wrappedKey: ByteArray? = null,
    val keyNonce: ByteArray? = null,
    val recoveryWrappedKey: ByteArray? = null,
    val recoveryNonce: ByteArray? = null,
    val quickUnlockBlob: ByteArray? = null,
    val avatarColor: Int? = null,
    val avatarImage: ByteArray? = null,
)

class InvalidPasswordException(
    message: String = "主密码错误",
    cause: Throwable? = null,
) : Exception(message, cause)

data class CreatedAccount(
    val session: VaultSession,
    val recoveryKey: String,
)
