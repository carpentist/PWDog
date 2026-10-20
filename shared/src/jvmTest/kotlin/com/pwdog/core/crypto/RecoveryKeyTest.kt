package com.pwdog.core.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RecoveryKeyTest {
    @Test
    fun generatesFormattedKeyThatParsesBack() {
        val key = RecoveryKey.generate()
        assertEquals(71, key.length)
        val bytes = RecoveryKey.parse(key)
        assertNotNull(bytes)
        assertEquals(32, bytes.size)
        assertEquals(key, RecoveryKey.format(bytes))
        bytes.fill(0)
    }

    @Test
    fun acceptsUnformattedInput() {
        val key = RecoveryKey.generate()
        val parsed = RecoveryKey.parse(" " + key.replace("-", "").uppercase() + " ")
        assertNotNull(parsed)
        val expected = RecoveryKey.parse(key)
        assertNotNull(expected)
        assertContentEquals(expected, parsed)
        parsed.fill(0)
        expected.fill(0)
    }

    @Test
    fun rejectsInvalidKeys() {
        assertNull(RecoveryKey.parse(""))
        assertNull(RecoveryKey.parse("not-a-key"))
        assertNull(RecoveryKey.parse("0".repeat(63)))
    }
}
