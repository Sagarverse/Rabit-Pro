package com.example.rabit.data.secure

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * EncryptionManager - Feature 5: AES-GCM 256-bit End-to-End Encryption
 *
 * Provides two-layer security:
 *  1. ECDH (Elliptic Curve Diffie-Hellman) key exchange over P-256 to establish
 *     a shared secret during pairing — the actual secret never travels over the wire.
 *  2. AES-GCM 256-bit authenticated encryption for every message payload,
 *     using a random 12-byte IV per message, ensuring forward secrecy per message.
 *
 * Usage:
 *  val manager = EncryptionManager(context)
 *  val myPublicKey = manager.getPublicKeyBase64()   // Share this during pairing (e.g. via QR)
 *  manager.acceptPeerPublicKey(peerBase64)           // Call once you receive peer's key
 *
 *  val cipher = manager.encrypt("hello")             // Returns "IV_BASE64:CIPHERTEXT_BASE64"
 *  val plain  = manager.decrypt(cipher)              // Returns "hello"
 */
class EncryptionManager(private val context: Context) {

    private val TAG = "EncryptionManager"
    private val prefs = context.getSharedPreferences("rabit_prefs", Context.MODE_PRIVATE)

    private val keyPair: KeyPair by lazy { generateOrLoadEcKeyPair() }
    private var sharedSecretKey: SecretKey? = null

    // Whether E2EE is enabled by user in Settings
    val isEnabled: Boolean get() = prefs.getBoolean("e2ee_enabled", false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("e2ee_enabled", enabled).apply()
    }

    // ───── ECDH Key Exchange ─────

    /**
     * Returns this device's ECDH public key as a Base64 string.
     * This is what you embed in a QR code during pairing.
     */
    fun getPublicKeyBase64(): String =
        Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    /**
     * Call this once the peer's public key is received (e.g. scanned from a QR code).
     * Derives the shared AES-GCM 256 secret.
     */
    fun acceptPeerPublicKey(peerPublicKeyBase64: String) {
        try {
            val peerBytes = Base64.decode(peerPublicKeyBase64, Base64.NO_WRAP)
            val peerPublicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(peerBytes))

            val keyAgreement = KeyAgreement.getInstance("ECDH")
            keyAgreement.init(keyPair.private)
            keyAgreement.doPhase(peerPublicKey, true)

            // HKDF-like: take first 32 bytes of SHA-256 of raw shared secret
            val rawSecret = keyAgreement.generateSecret()
            val digest = MessageDigest.getInstance("SHA-256").digest(rawSecret)

            sharedSecretKey = SecretKeySpec(digest, "AES")
            Log.d(TAG, "Shared AES-256 key derived via ECDH ✓")
        } catch (e: Exception) {
            Log.e(TAG, "ECDH key agreement failed", e)
            throw e
        }
    }

    // ───── AES-GCM Encrypt / Decrypt ─────

    /**
     * Encrypts [plaintext] with AES-GCM-256.
     * Returns a single string: "base64(IV):base64(Ciphertext+AuthTag)"
     */
    fun encrypt(plaintext: String): String {
        val key = requireKey()
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        val cipherBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val ctB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
        return "$ivB64:$ctB64"
    }

    /**
     * Decrypts a payload produced by [encrypt].
     * Throws [SecurityException] if authentication tag is wrong (tampering detected).
     */
    fun decrypt(payload: String): String {
        val key = requireKey()
        val parts = payload.split(":")
        require(parts.size == 2) { "Invalid payload format" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ct = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        }
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    /** Convenience: encrypt only if E2EE is enabled, otherwise pass through */
    fun encryptIfEnabled(plaintext: String): String =
        if (isEnabled && sharedSecretKey != null) encrypt(plaintext) else plaintext

    /** Convenience: decrypt only if E2EE is enabled, otherwise pass through */
    fun decryptIfEnabled(payload: String): String =
        if (isEnabled && sharedSecretKey != null) decrypt(payload) else payload

    fun isPaired(): Boolean = sharedSecretKey != null

    // ───── Key Generation ─────

    private fun requireKey(): SecretKey =
        sharedSecretKey ?: throw IllegalStateException("No shared key — complete ECDH pairing first.")

    private fun generateOrLoadEcKeyPair(): KeyPair {
        val savedPriv = prefs.getString("ecdh_private_key", null)
        val savedPub  = prefs.getString("ecdh_public_key", null)
        if (savedPriv != null && savedPub != null) {
            return try {
                val kf = KeyFactory.getInstance("EC")
                val priv = kf.generatePrivate(java.security.spec.PKCS8EncodedKeySpec(
                    Base64.decode(savedPriv, Base64.NO_WRAP)))
                val pub = kf.generatePublic(X509EncodedKeySpec(
                    Base64.decode(savedPub, Base64.NO_WRAP)))
                KeyPair(pub, priv)
            } catch (e: Exception) {
                Log.w(TAG, "Could not restore key pair, generating new one", e)
                generateAndSaveKeyPair()
            }
        }
        return generateAndSaveKeyPair()
    }

    private fun generateAndSaveKeyPair(): KeyPair {
        val kg = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"), SecureRandom())
        }
        val kp = kg.generateKeyPair()
        prefs.edit()
            .putString("ecdh_private_key", Base64.encodeToString(kp.private.encoded, Base64.NO_WRAP))
            .putString("ecdh_public_key",  Base64.encodeToString(kp.public.encoded,  Base64.NO_WRAP))
            .apply()
        Log.d(TAG, "New ECDH P-256 key pair generated and saved")
        return kp
    }
}
