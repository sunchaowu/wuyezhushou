package com.guanjiazhushou.assistant.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal object BackupCrypto {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "AES"
    private const val KEY_DERIVATION = "PBKDF2WithHmacSHA256"
    private const val KEY_LENGTH_BITS = 256
    private const val ITERATION_COUNT = 100_000
    private const val SALT_LENGTH = 16
    private const val IV_LENGTH = 12
    private const val GCM_TAG_LENGTH_BITS = 128

    data class EncryptedPayload(
        val salt: ByteArray,
        val iv: ByteArray,
        val ciphertext: ByteArray,
    )

    fun encrypt(plainBytes: ByteArray, password: String): EncryptedPayload {
        val salt = ByteArray(SALT_LENGTH).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val ciphertext = cipher.doFinal(plainBytes)
        return EncryptedPayload(salt = salt, iv = iv, ciphertext = ciphertext)
    }

    fun decrypt(payload: EncryptedPayload, password: String): ByteArray {
        val key = deriveKey(password, payload.salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(
            Cipher.DECRYPT_MODE,
            key,
            GCMParameterSpec(GCM_TAG_LENGTH_BITS, payload.iv),
        )
        return cipher.doFinal(payload.ciphertext)
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance(KEY_DERIVATION)
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS)
        val secretKey = factory.generateSecret(spec)
        return SecretKeySpec(secretKey.encoded, KEY_ALGORITHM)
    }
}
