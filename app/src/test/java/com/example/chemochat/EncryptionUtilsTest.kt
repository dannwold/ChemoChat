package com.example.chemochat

import io.mockk.every
import io.mockk.mockkStatic
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Base64 as JavaBase64

class EncryptionUtilsTest {

    @Before
    fun setup() {
        mockkStatic(android.util.Base64::class)
        every { android.util.Base64.encodeToString(any(), any()) } answers {
            JavaBase64.getEncoder().encodeToString(it.invocation.args[0] as ByteArray)
        }
        every { android.util.Base64.decode(any<String>(), any()) } answers {
            JavaBase64.getDecoder().decode(it.invocation.args[0] as String)
        }
    }

    @Test
    fun testEncryptionDecryption() {
        val originalText = "Hello, this is a secret message!"
        val password = "StrongPassword123"

        val encrypted = EncryptionUtils.encryptText(originalText, password)
        assertNotEquals(originalText, encrypted)

        val decrypted = EncryptionUtils.decryptText(encrypted, password)
        assertEquals(originalText, decrypted)
    }

    @Test
    fun testEncryptionDifferentEachTime() {
        val originalText = "Same message"
        val password = "password"

        val encrypted1 = EncryptionUtils.encryptText(originalText, password)
        val encrypted2 = EncryptionUtils.encryptText(originalText, password)

        assertNotEquals(encrypted1, encrypted2)
    }

    @Test(expected = Exception::class)
    fun testDecryptionWithWrongPassword() {
        val originalText = "Secret"
        val password = "correct_password"
        val wrongPassword = "wrong_password"

        val encrypted = EncryptionUtils.encryptText(originalText, password)
        EncryptionUtils.decryptText(encrypted, wrongPassword)
    }
}
