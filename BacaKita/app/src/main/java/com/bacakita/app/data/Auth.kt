package com.bacakita.app.data

import android.database.sqlite.SQLiteConstraintException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class User(val id: Long, val username: String)

object AuthValidator {
    private val usernameRegex = Regex("^[a-z0-9._-]{3,30}$")

    fun normalizeUsername(raw: String) = raw.trim().lowercase()

    fun usernameError(raw: String): String? =
        if (usernameRegex.matches(normalizeUsername(raw))) null
        else "Nama pengguna 3–30 karakter: huruf, angka, titik, garis bawah, atau strip."

    fun passwordError(password: String): String? = when {
        password.length < 6 -> "Kata sandi minimal 6 karakter."
        password.length > 128 -> "Kata sandi terlalu panjang."
        else -> null
    }
}

/** PBKDF2-HMAC-SHA256 dengan salt acak per pengguna. Kata sandi tidak pernah disimpan. */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(16).also { SecureRandom().nextBytes(it) }

    fun hash(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun verify(password: String, salt: ByteArray, expected: ByteArray): Boolean =
        MessageDigest.isEqual(hash(password, salt), expected)

    fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
}

sealed interface AuthResult {
    data class Success(val user: User) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/** Akun lokal (di perangkat). Belum ada sinkronisasi antarperangkat. */
class AuthRepository(private val dao: UserDao) {

    suspend fun register(rawUsername: String, password: String): AuthResult = withContext(Dispatchers.Default) {
        AuthValidator.usernameError(rawUsername)?.let { return@withContext AuthResult.Failure(it) }
        AuthValidator.passwordError(password)?.let { return@withContext AuthResult.Failure(it) }
        val username = AuthValidator.normalizeUsername(rawUsername)
        if (dao.findByUsername(username) != null) return@withContext AuthResult.Failure("Nama pengguna sudah dipakai.")

        val salt = PasswordHasher.newSalt()
        val entity = UserEntity(
            username = username,
            passwordHash = PasswordHasher.encode(PasswordHasher.hash(password, salt)),
            salt = PasswordHasher.encode(salt),
            createdAt = System.currentTimeMillis(),
        )
        try {
            val id = dao.insert(entity)
            AuthResult.Success(User(id, username))
        } catch (e: SQLiteConstraintException) {
            AuthResult.Failure("Nama pengguna sudah dipakai.")
        }
    }

    suspend fun login(rawUsername: String, password: String): AuthResult = withContext(Dispatchers.Default) {
        val invalid = AuthResult.Failure("Nama pengguna atau kata sandi salah.")
        val user = dao.findByUsername(AuthValidator.normalizeUsername(rawUsername)) ?: return@withContext invalid
        val ok = try {
            PasswordHasher.verify(password, PasswordHasher.decode(user.salt), PasswordHasher.decode(user.passwordHash))
        } catch (e: IllegalArgumentException) {
            false
        }
        if (ok) AuthResult.Success(User(user.id, user.username)) else invalid
    }

    suspend fun getUser(id: Long): User? = dao.findById(id)?.let { User(it.id, it.username) }
}
