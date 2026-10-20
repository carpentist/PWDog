package com.pwdog.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class Argon2idTest {
    private val params = KdfParams(
        memoryKiB = 8 * 1024,
        iterations = 1,
        parallelism = 1,
    )

    @Test
    fun derivesDeterministicKey() {
        val password = "master-password".toByteArray()
        val salt = ByteArray(16) { it.toByte() }

        val first = Argon2id.deriveKey(password, salt, params)
        val second = Argon2id.deriveKey(password, salt, params)

        assertContentEquals(first, second)
        assertEquals(32, first.size)
        first.fill(0)
        second.fill(0)
        password.fill(0)
    }

    @Test
    fun differentSaltProducesDifferentKey() {
        val password = "master-password".toByteArray()
        val saltA = ByteArray(16) { it.toByte() }
        val saltB = ByteArray(16) { (it + 1).toByte() }

        val keyA = Argon2id.deriveKey(password, saltA, params)
        val keyB = Argon2id.deriveKey(password, saltB, params)

        assertFalse(keyA.contentEquals(keyB))
        keyA.fill(0)
        keyB.fill(0)
        password.fill(0)
    }

    @Test
    fun rejectsWrongSaltLength() {
        assertFailsWith<IllegalArgumentException> {
            Argon2id.deriveKey("master-password".toByteArray(), ByteArray(8), params)
        }
    }
}
