package com.pwdog.core.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class WrappedKey(
    val nonce: ByteArray,
    val ciphertext: ByteArray,
)

object KeyWrap {
    private const val NONCE_LENGTH = 12
    private const val TAG_BITS = 128
    private val random = SecureRandom()

    fun wrap(key: ByteArray, wrappingKey: ByteArray, associatedData: ByteArray): WrappedKey {
        require(key.size == 32) { "key must be 32 bytes" }
        require(wrappingKey.size == 32) { "wrappingKey must be 32 bytes" }

        val nonce = ByteArray(NONCE_LENGTH).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(wrappingKey, "AES"),
            GCMParameterSpec(TAG_BITS, nonce),
        )
        cipher.updateAAD(associatedData)
        return WrappedKey(nonce = nonce, ciphertext = cipher.doFinal(key))
    }

    fun unwrap(wrapped: WrappedKey, wrappingKey: ByteArray, associatedData: ByteArray): ByteArray {
        require(wrappingKey.size == 32) { "wrappingKey must be 32 bytes" }
        require(wrapped.nonce.size == NONCE_LENGTH) { "invalid nonce length" }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(wrappingKey, "AES"),
            GCMParameterSpec(TAG_BITS, wrapped.nonce),
        )
        cipher.updateAAD(associatedData)
        return cipher.doFinal(wrapped.ciphertext)
    }
}
