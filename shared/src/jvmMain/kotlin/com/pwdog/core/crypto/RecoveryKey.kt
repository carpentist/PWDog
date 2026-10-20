package com.pwdog.core.crypto

import java.security.SecureRandom
import java.util.HexFormat

object RecoveryKey {
    private val random = SecureRandom()
    private val HEX_PATTERN = Regex("[0-9a-f]{64}")

    fun generate(): String =
        format(ByteArray(32).also { random.nextBytes(it) })

    fun parse(input: String): ByteArray? {
        val normalized = input
            .filterNot { it == '-' || it.isWhitespace() }
            .lowercase()
        if (!HEX_PATTERN.matches(normalized)) return null
        return HexFormat.of().parseHex(normalized)
    }

    fun format(bytes: ByteArray): String {
        require(bytes.size == 32) { "recovery key must be 32 bytes" }
        return HexFormat.of().formatHex(bytes).chunked(8).joinToString("-")
    }
}
