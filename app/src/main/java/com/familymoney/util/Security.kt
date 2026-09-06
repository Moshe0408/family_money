package com.familymoney.util

import java.security.MessageDigest

object Security {

    /**
     * PIN hashing. Salted SHA-256 over many rounds — the PIN never leaves the
     * device and the digest lives in EncryptedSharedPreferences, so this only
     * has to stop casual inspection of the prefs file.
     */
    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        var bytes = (SALT + pin).toByteArray(Charsets.UTF_8)
        repeat(ROUNDS) { bytes = digest.digest(bytes) }
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /** Masks all but the last four digits for display. */
    fun maskCard(last4: String): String = "•••• $last4"

    private const val SALT = "family_money::v1::"
    private const val ROUNDS = 8192
}
