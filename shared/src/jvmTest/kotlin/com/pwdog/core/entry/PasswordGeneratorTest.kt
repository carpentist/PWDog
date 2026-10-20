package com.pwdog.core.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PasswordGeneratorTest {
    @Test
    fun defaultPasswordContainsAllCharacterClasses() {
        repeat(50) {
            val password = PasswordGenerator.generate()
            assertEquals(20, password.length)
            assertTrue(password.any { it in 'a'..'z' })
            assertTrue(password.any { it in 'A'..'Z' })
            assertTrue(password.any { it in '0'..'9' })
            assertTrue(password.any { !it.isLetterOrDigit() })
        }
    }

    @Test
    fun honorsSelectedCharacterTypes() {
        val options = PasswordGeneratorOptions(
            length = 12,
            lowercase = true,
            uppercase = false,
            digits = true,
            symbols = false,
        )
        repeat(20) {
            val password = PasswordGenerator.generate(options)
            assertEquals(12, password.length)
            assertTrue(password.all { it in 'a'..'z' || it in '0'..'9' })
        }
    }

    @Test
    fun generatesDifferentPasswords() {
        assertNotEquals(PasswordGenerator.generate(), PasswordGenerator.generate())
    }

    @Test
    fun rejectsInvalidOptions() {
        assertFailsWith<IllegalArgumentException> {
            PasswordGeneratorOptions(length = 7)
        }
        assertFailsWith<IllegalArgumentException> {
            PasswordGeneratorOptions(
                lowercase = false,
                uppercase = false,
                digits = false,
                symbols = false,
            )
        }
    }
}
