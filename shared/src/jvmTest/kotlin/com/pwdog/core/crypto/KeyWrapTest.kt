package com.pwdog.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import javax.crypto.AEADBadTagException

class KeyWrapTest {
    private val key = ByteArray(32) { it.toByte() }
    private val wrappingKey = ByteArray(32) { (it * 3).toByte() }
    private val aad = "slot|account".toByteArray()

    @Test
    fun wrapsAndUnwraps() {
        val wrapped = KeyWrap.wrap(key, wrappingKey, aad)
        val unwrapped = KeyWrap.unwrap(wrapped, wrappingKey, aad)
        assertContentEquals(key, unwrapped)
        unwrapped.fill(0)
    }

    @Test
    fun rejectsWrongWrappingKey() {
        val wrapped = KeyWrap.wrap(key, wrappingKey, aad)
        assertFailsWith<AEADBadTagException> {
            KeyWrap.unwrap(wrapped, ByteArray(32) { 9 }, aad)
        }
    }

    @Test
    fun rejectsWrongAssociatedData() {
        val wrapped = KeyWrap.wrap(key, wrappingKey, aad)
        assertFailsWith<AEADBadTagException> {
            KeyWrap.unwrap(wrapped, wrappingKey, "other-slot".toByteArray())
        }
    }
}
