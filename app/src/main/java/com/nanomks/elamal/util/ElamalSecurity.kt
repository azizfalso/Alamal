package com.nanomks.elamal.util

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Dedicated security module handling:
 * 1. Salted PBKDF2 PIN hashing (15,000 iterations) with legacy compatibility.
 * 2. Android Keystore hardware-backed encryption for the backup password.
 * 3. AES-256-GCM encryption/decryption for SAF JSON backup files.
 */
object ElamalSecurity {

    private const val KEYSTORE_ALIAS = "elamal_keystore_master_key_v1"

    fun hashPinPbkdf2(pin: String): String {
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val spec = PBEKeySpec(pin.toCharArray(), salt, 15000, 256)
        val hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val enc = Base64.getEncoder()
        return "PBKDF2_V1:${enc.encodeToString(salt)}:${enc.encodeToString(hash)}"
    }

    fun verifyPin(inputPin: String, storedPin: String): Boolean {
        if (storedPin.isBlank()) return true
        return when {
            storedPin.startsWith("PBKDF2_V1:") -> {
                val parts = storedPin.split(":")
                if (parts.size != 3) return false
                val dec = Base64.getDecoder()
                val salt = runCatching { dec.decode(parts[1]) }.getOrNull() ?: return false
                val expected = runCatching { dec.decode(parts[2]) }.getOrNull() ?: return false
                val spec = PBEKeySpec(inputPin.toCharArray(), salt, 15000, 256)
                val actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                MessageDigest.isEqual(actual, expected)
            }
            storedPin.startsWith("SHA256:") -> {
                val digest = MessageDigest.getInstance("SHA-256")
                val bytes = digest.digest(("ELAMAL_SALT_V4:" + inputPin).toByteArray(Charsets.UTF_8))
                val legacyHash = "SHA256:" + Base64.getEncoder().encodeToString(bytes)
                legacyHash == storedPin
            }
            else -> inputPin == storedPin
        }
    }

    private fun getOrCreateHardwareSecretKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun saveBackupPassword(prefs: SharedPreferences, plainPassword: String) {
        if (plainPassword.isBlank()) {
            prefs.edit().remove("backup_password_enc").remove("backup_password").apply()
            return
        }
        runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateHardwareSecretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(plainPassword.toByteArray(Charsets.UTF_8))
            val enc = Base64.getEncoder()
            val payload = "KS_V1:${enc.encodeToString(iv)}:${enc.encodeToString(encrypted)}"
            prefs.edit().putString("backup_password_enc", payload).remove("backup_password").apply()
        }
    }

    fun getBackupPassword(prefs: SharedPreferences): String {
        val encPayload = prefs.getString("backup_password_enc", null)
        if (!encPayload.isNullOrBlank() && encPayload.startsWith("KS_V1:")) {
            return runCatching {
                val parts = encPayload.split(":")
                val dec = Base64.getDecoder()
                val iv = dec.decode(parts[1])
                val cipherBytes = dec.decode(parts[2])
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, getOrCreateHardwareSecretKey(), GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            }.getOrDefault("")
        }
        // Migrate legacy plaintext backup password if present
        val legacy = prefs.getString("backup_password", "") ?: ""
        if (legacy.isNotBlank()) {
            saveBackupPassword(prefs, legacy)
        }
        return legacy
    }

    fun encryptBackup(plainJson: String, password: String): String {
        if (password.isBlank()) return plainJson
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val spec = PBEKeySpec(password.toCharArray(), salt, 15000, 256)
        val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        val keySpec = SecretKeySpec(secret, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        val enc = Base64.getEncoder()
        return "ELAMAL_ENC_V1:" + enc.encodeToString(salt) + ":" + enc.encodeToString(iv) + ":" + enc.encodeToString(encrypted)
    }

    fun decryptBackup(payload: String, password: String): String {
        if (!payload.startsWith("ELAMAL_ENC_V1:")) return payload
        val parts = payload.split(":")
        require(parts.size == 4) { "ملف مشفر غير صالح" }
        val dec = Base64.getDecoder()
        val salt = dec.decode(parts[1])
        val iv = dec.decode(parts[2])
        val cipherBytes = dec.decode(parts[3])

        // Try 15,000 iterations first, fallback to 10,000 for v4.0/v4.1 backups
        for (iterations in listOf(15000, 10000)) {
            val result = runCatching {
                val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
                val secret = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
                val keySpec = SecretKeySpec(secret, "AES")
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, keySpec, GCMParameterSpec(128, iv))
                String(cipher.doFinal(cipherBytes), Charsets.UTF_8)
            }.getOrNull()
            if (result != null) return result
        }
        throw IllegalArgumentException("كلمة المرور غير صحيحة")
    }
}



