package com.example.chemochat

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import javax.crypto.BadPaddingException

@RunWith(RobolectricTestRunner::class)
class EncryptionUtilsTest {

    private val password = "testPassword123"
    private val testMessage = "Hello, ChemoChat!"

    @Test
    fun `test encryptText and decryptText round trip`() {
        val encrypted = EncryptionUtils.encryptText(testMessage, password)
        val decrypted = EncryptionUtils.decryptText(encrypted, password)
        assertEquals(testMessage, decrypted)
    }

    @Test
    fun `test encrypt and decrypt round trip`() {
        val data = testMessage.toByteArray(Charsets.UTF_8)
        val encrypted = EncryptionUtils.encrypt(data, password)
        val decrypted = EncryptionUtils.decrypt(encrypted, password)
        assertArrayEquals(data, decrypted)
    }

    @Test
    fun `test decryption with wrong password fails`() {
        val encrypted = EncryptionUtils.encryptText(testMessage, password)
        val wrongPassword = "wrongPassword"

        assertThrows(BadPaddingException::class.java) {
            EncryptionUtils.decryptText(encrypted, wrongPassword)
        }
    }

    @Test
    fun `test encryption is non-deterministic`() {
        val encrypted1 = EncryptionUtils.encryptText(testMessage, password)
        val encrypted2 = EncryptionUtils.encryptText(testMessage, password)

        // Different salts/IVs should produce different ciphertexts
        assertNotEquals(encrypted1, encrypted2)

        // But both should decrypt back to the same plaintext
        assertEquals(testMessage, EncryptionUtils.decryptText(encrypted1, password))
        assertEquals(testMessage, EncryptionUtils.decryptText(encrypted2, password))
    }

    @Test
    fun `test empty string encryption`() {
        val emptyMessage = ""
        val encrypted = EncryptionUtils.encryptText(emptyMessage, password)
        val decrypted = EncryptionUtils.decryptText(encrypted, password)
        assertEquals(emptyMessage, decrypted)
    }

    @Test
    fun `test special characters encryption`() {
        val specialMessage = "!@#$%^&*()_+ {}:\"<>?|~`-=[]\\;',./"
        val encrypted = EncryptionUtils.encryptText(specialMessage, password)
        val decrypted = EncryptionUtils.decryptText(encrypted, password)
        assertEquals(specialMessage, decrypted)
    }
}
