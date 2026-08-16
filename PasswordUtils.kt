package com.familyguard.screentime.util

import java.security.MessageDigest

/**
 * The plaintext password is never stored. Only a SHA-256 hash is persisted,
 * so the value in SharedPreferences cannot be read back as a real password.
 */
object PasswordUtils {

    fun hash(plainText: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(plainText.toByteArray(Charsets.UTF_8))
        return bytes.joinToString(separator = "") { "%02x".format(it) }
    }

    fun matches(plainText: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrEmpty()) return false
        return hash(plainText) == storedHash
    }

    /**
     * Normalizes free-text answers (trims and lower-cases) before hashing,
     * so recovery-question answers aren't rejected over capitalization or
     * stray whitespace.
     */
    fun normalizedHash(rawAnswer: String): String =
        hash(rawAnswer.trim().lowercase())
}
