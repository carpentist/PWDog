package com.pwdog.core.account

import com.pwdog.core.crypto.KdfParams
import com.pwdog.core.crypto.RecoveryKey
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountServiceTest {
    private val params = KdfParams(
        memoryKiB = 8 * 1024,
        iterations = 1,
        parallelism = 1,
    )

    @Test
    fun accountSurvivesRestartAndRejectsWrongPassword() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val password = "correct-password".toByteArray()
        var accountId: String? = null
        var service: AccountService? = null
        var session: VaultSession? = null

        try {
            service = AccountService(base, params)
            val created = service.createAccount("Alice", password)
            session = created.session
            accountId = session.account.id

            assertNotNull(RecoveryKey.parse(created.recoveryKey))
            assertTrue(Files.exists(session.account.vaultFile))
            assertEquals(1, service.listAccounts().size)
            assertEquals("Alice", service.listAccounts().single().displayName)
            assertEquals(2, service.listAccounts().single().keyVersion)
            session.connection.createStatement().use { statement ->
                statement.executeQuery("select count(*) from entries").use { rs ->
                    assertTrue(rs.next())
                    assertEquals(0, rs.getInt(1))
                }
            }
            session.close()
            session = null
            service.close()
            service = null

            val header = Files.newInputStream(base.resolve("vaults").resolve("$accountId.db")).use {
                it.readNBytes(16)
            }
            assertFalse(header.contentEquals(PLAINTEXT_SQLITE_MAGIC))

            val reopened = AccountService(base, params)
            service = reopened

            val unlocked = reopened.unlock(requireNotNull(accountId), password)
            try {
                assertEquals("Alice", unlocked.account.displayName)
                assertFalse(unlocked.connection.isClosed)
            } finally {
                unlocked.close()
            }

            assertFailsWith<InvalidPasswordException> {
                reopened.unlock(accountId, "wrong-password".toByteArray())
            }
        } finally {
            session?.close()
            service?.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun recoveryKeyResetsPasswordAndInvalidatesOldPassword() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val oldPassword = "correct-password".toByteArray()
        val newPassword = "new-correct-password".toByteArray()
        val service = AccountService(base, params)
        val created = service.createAccount("Alice", oldPassword)
        val accountId = created.session.account.id
        created.session.close()

        try {
            assertFailsWith<InvalidPasswordException> {
                service.recoverAccount(accountId, RecoveryKey.generate(), newPassword)
            }

            val recovered = service.recoverAccount(accountId, created.recoveryKey, newPassword)
            try {
                assertEquals("Alice", recovered.account.displayName)
                assertFalse(recovered.connection.isClosed)
            } finally {
                recovered.close()
            }

            assertFailsWith<InvalidPasswordException> {
                service.unlock(accountId, oldPassword)
            }

            val unlocked = service.unlock(accountId, newPassword)
            try {
                assertEquals("Alice", unlocked.account.displayName)
            } finally {
                unlocked.close()
            }

            val recoveredAgain = service.recoverAccount(
                accountId,
                created.recoveryKey,
                "third-password".toByteArray(),
            )
            recoveredAgain.close()
        } finally {
            service.close()
            oldPassword.fill(0)
            newPassword.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun quickUnlockCanBeEnabledUsedAndDisabled() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val created = service.createAccount("Alice", password)
        val accountId = created.session.account.id
        val identity: (ByteArray) -> ByteArray = { it.copyOf() }

        try {
            service.enableQuickUnlock(created.session, identity)
            val stored = service.listAccounts().single()
            assertNotNull(stored.quickUnlockBlob)
            assertTrue(stored.quickUnlockBlob.isNotEmpty())

            created.session.close()
            val unlocked = service.unlockWithQuickUnlock(accountId, identity)
            try {
                assertEquals("Alice", unlocked.account.displayName)
                assertFalse(unlocked.connection.isClosed)
            } finally {
                unlocked.close()
            }

            service.disableQuickUnlock(accountId)
            assertEquals(null, service.listAccounts().single().quickUnlockBlob)
            assertFailsWith<IllegalArgumentException> {
                service.unlockWithQuickUnlock(accountId, identity)
            }
        } finally {
            created.session.close()
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun changesMasterPasswordAndKeepsRecoveryKey() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val oldPassword = "correct-password".toByteArray()
        val newPassword = "new-correct-password".toByteArray()
        val service = AccountService(base, params)
        val created = service.createAccount("Alice", oldPassword)
        val accountId = created.session.account.id

        try {
            assertFailsWith<InvalidPasswordException> {
                service.changePassword(
                    created.session,
                    "wrong-password".toByteArray(),
                    newPassword,
                )
            }

            service.changePassword(created.session, oldPassword, newPassword)
            created.session.close()

            assertFailsWith<InvalidPasswordException> {
                service.unlock(accountId, oldPassword)
            }
            service.unlock(accountId, newPassword).close()

            service.recoverAccount(
                accountId,
                created.recoveryKey,
                "third-password".toByteArray(),
            ).close()
        } finally {
            created.session.close()
            service.close()
            oldPassword.fill(0)
            newPassword.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun profileUpdatesPersist() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val created = service.createAccount("Alice", password)
        val accountId = created.session.account.id
        val avatar = byteArrayOf(1, 2, 3, 4)

        try {
            val updated = service.updateProfile(
                accountId = accountId,
                displayName = "Alice Work",
                avatarColor = 0xFF2563EB.toInt(),
                avatarImage = avatar,
            )
            assertEquals("Alice Work", updated.displayName)
            assertEquals(0xFF2563EB.toInt(), updated.avatarColor)
            assertContentEquals(avatar, updated.avatarImage)

            val reopened = AccountService(base, params)
            try {
                val persisted = reopened.listAccounts().single()
                assertEquals("Alice Work", persisted.displayName)
                assertEquals(0xFF2563EB.toInt(), persisted.avatarColor)
                assertContentEquals(avatar, persisted.avatarImage)

                val bob = reopened.createAccount("Bob", password)
                try {
                    assertFailsWith<IllegalArgumentException> {
                        reopened.updateProfile(accountId, "bob", null, null)
                    }
                } finally {
                    bob.session.close()
                }

                val removed = reopened.updateProfile(accountId, "Alice Work", null, null)
                assertEquals(null, removed.avatarImage)
            } finally {
                reopened.close()
            }
        } finally {
            created.session.close()
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun duplicateDisplayNameIsRejectedCaseInsensitively() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val session = service.createAccount("Alice", password).session

        try {
            assertFailsWith<IllegalArgumentException> {
                service.createAccount("alice", password)
            }
        } finally {
            session.close()
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun deletingOneAccountLeavesOtherAccountsIntact() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val password = "correct-password".toByteArray()
        val service = AccountService(base, params)
        val alice = service.createAccount("Alice", password)
        val aliceId = alice.session.account.id
        val aliceVault = alice.session.account.vaultFile
        alice.session.close()
        val bob = service.createAccount("Bob", password)
        val bobId = bob.session.account.id
        bob.session.close()

        try {
            service.deleteAccount(aliceId)

            assertFalse(Files.exists(aliceVault))
            assertEquals(listOf("Bob"), service.listAccounts().map { it.displayName })
            val unlocked = service.unlock(bobId, password)
            try {
                assertEquals("Bob", unlocked.account.displayName)
            } finally {
                unlocked.close()
            }
        } finally {
            service.close()
            password.fill(0)
            deleteRecursively(base)
        }
    }

    @Test
    fun shortPasswordIsRejected() {
        val base = Files.createTempDirectory("pwdog-account-test-")
        val service = AccountService(base, params)

        try {
            assertFailsWith<IllegalArgumentException> {
                service.createAccount("Alice", "short".toByteArray())
            }
            assertTrue(service.listAccounts().isEmpty())
        } finally {
            service.close()
            deleteRecursively(base)
        }
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { paths ->
            paths.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private companion object {
        val PLAINTEXT_SQLITE_MAGIC = byteArrayOf(
            0x53, 0x51, 0x4c, 0x69, 0x74, 0x65, 0x20,
            0x66, 0x6f, 0x72, 0x6d, 0x61, 0x74, 0x20,
            0x33, 0x00,
        )
    }
}
