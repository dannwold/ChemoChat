package com.example.chemochat

import android.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EncryptionUtilsTest {

    @Test
    fun testEncryptDecrypt() {
        val password = "strongpassword"
        val originalText = "Hello, World!"

        val encrypted = EncryptionUtils.encryptText(originalText, password)
        val decrypted = EncryptionUtils.decryptText(encrypted, password)

        assertEquals(originalText, decrypted)
    }

    @Test
    fun testDecryptWithEmptyStringThrowsException() {
        val password = "password"
        val exception = assertThrows(IllegalArgumentException::class.java) {
            EncryptionUtils.decryptText("", password)
        }
        assertEquals("Encrypted data string is empty", exception.message)
    }

    @Test
    fun testDecryptWithShortInputThrowsException() {
        val password = "password"
        // Base64 for something shorter than 32 bytes (16 salt + 16 IV)
        val shortData = Base64.encodeToString(ByteArray(10), Base64.DEFAULT)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            EncryptionUtils.decryptText(shortData, password)
        }
        assertEquals("Encrypted data is too short", exception.message)
    }

    @Test
    fun testDecryptWithWrongPasswordThrowsException() {
        val password = "correct_password"
        val wrongPassword = "wrong_password"
        val originalText = "Secret Message"

        val encrypted = EncryptionUtils.encryptText(originalText, password)

        assertThrows(Exception::class.java) {
            EncryptionUtils.decryptText(encrypted, wrongPassword)
        }
    }
}
