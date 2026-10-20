package com.pwdog.core.crypto

data class KdfParams(
    val memoryKiB: Int = 64 * 1024,
    val iterations: Int = 3,
    val parallelism: Int = 4,
    val saltLength: Int = 16,
    val keyLength: Int = 32,
) {
    init {
        require(memoryKiB >= 8 * 1024) { "memoryKiB must be at least 8192" }
        require(iterations >= 1) { "iterations must be at least 1" }
        require(parallelism >= 1) { "parallelism must be at least 1" }
        require(saltLength >= 16) { "saltLength must be at least 16" }
        require(keyLength == 32) { "SQLCipher requires a 256-bit key" }
    }
}
