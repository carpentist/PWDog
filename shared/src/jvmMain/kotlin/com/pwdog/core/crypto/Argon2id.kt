package com.pwdog.core.crypto

import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import java.security.SecureRandom

object Argon2id {
    fun deriveKey(
        password: ByteArray,
        salt: ByteArray,
        params: KdfParams = KdfParams(),
    ): ByteArray {
        require(password.isNotEmpty()) { "password must not be empty" }
        require(salt.size == params.saltLength) {
            "salt must be ${params.saltLength} bytes, got ${salt.size}"
        }

        val generator = Argon2BytesGenerator()
        val argonParams = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(params.iterations)
            .withMemoryAsKB(params.memoryKiB)
            .withParallelism(params.parallelism)
            .withSalt(salt)
            .build()

        generator.init(argonParams)
        return ByteArray(params.keyLength).also { generator.generateBytes(password, it) }
    }

    fun randomSalt(length: Int = 16): ByteArray =
        ByteArray(length).also { SecureRandom().nextBytes(it) }
}
