package com.example.chemochat

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Utility class for AES-256 encryption and decryption.
 * Uses PBKDF2 for key derivation from a password.
 */
object EncryptionUtils {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_LENGTH = 256
    private const val ITERATIONS = 10000
    private const val SALT_SIZE = 16
    private const val IV_SIZE = 16

    /**
     * Derives a SecretKey from a password and salt.
     */
    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Encrypts data using AES-256.
     * Returns a Base64 encoded string containing [salt + iv + encryptedData].
     */
    fun encrypt(data: ByteArray, password: String): String {
        val salt = ByteArray(SALT_SIZE).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(IV_SIZE).apply { SecureRandom().nextBytes(this) }
        val key = deriveKey(password, salt)

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))
        val encrypted = cipher.doFinal(data)

        val combined = salt + iv + encrypted
        return Base64.encodeToString(combined, Base64.DEFAULT)
    }

    /**
     * Decrypts a Base64 encoded string using AES-256.
     */
    fun decrypt(encryptedBase64: String, password: String): ByteArray {
        val combined = Base64.decode(encryptedBase64, Base64.DEFAULT)
        
        val salt = combined.sliceArray(0 until SALT_SIZE)
        val iv = combined.sliceArray(SALT_SIZE until SALT_SIZE + IV_SIZE)
        val encrypted = combined.sliceArray(SALT_SIZE + IV_SIZE until combined.size)

        val key = deriveKey(password, salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))
        
        return cipher.doFinal(encrypted)
    }

    fun encryptText(text: String, password: String): String {
        return encrypt(text.toByteArray(Charsets.UTF_8), password)
    }

    fun decryptText(encryptedBase64: String, password: String): String {
        return String(decrypt(encryptedBase64, password), Charsets.UTF_8)
    }
}
