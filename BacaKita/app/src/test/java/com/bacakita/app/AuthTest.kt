package com.bacakita.app

import com.bacakita.app.data.AuthValidator
import com.bacakita.app.data.PasswordHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthTest {
    @Test fun validUsernameAccepted() = assertNull(AuthValidator.usernameError("  Budi_01 "))
    @Test fun shortOrInvalidUsernameRejected() {
        assertNotNull(AuthValidator.usernameError("ab"))
        assertNotNull(AuthValidator.usernameError("nama dengan spasi"))
    }
    @Test fun usernameIsNormalized() = assertEquals("budi", AuthValidator.normalizeUsername("  BUDI "))
    @Test fun shortPasswordRejected() = assertNotNull(AuthValidator.passwordError("12345"))
    @Test fun passwordAccepted() = assertNull(AuthValidator.passwordError("123456"))

    @Test fun hashVerifiesCorrectPasswordOnly() {
        val salt = PasswordHasher.newSalt()
        val hash = PasswordHasher.hash("rahasia123", salt)
        assertTrue(PasswordHasher.verify("rahasia123", salt, hash))
        assertFalse(PasswordHasher.verify("salah", salt, hash))
    }

    @Test fun saltsAreRandom() {
        assertFalse(PasswordHasher.newSalt().contentEquals(PasswordHasher.newSalt()))
    }

    @Test fun encodeDecodeRoundTrip() {
        val bytes = PasswordHasher.newSalt()
        assertTrue(bytes.contentEquals(PasswordHasher.decode(PasswordHasher.encode(bytes))))
    }
}
