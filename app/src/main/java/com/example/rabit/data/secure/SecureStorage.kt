package com.example.rabit.data.secure

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecureStorage(private val context: Context) {
    
    private val prefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secure_prefs",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_API, key).apply()
    }

    fun getApiKey(): String? = prefs.getString(KEY_GEMINI_API, null)

    fun saveUnlockPassword(password: String) {
        prefs.edit().putString(KEY_UNLOCK_PASSWORD, password).apply()
    }

    fun getUnlockPassword(): String? = prefs.getString(KEY_UNLOCK_PASSWORD, null)

    fun saveMacPassword(password: String) {
        prefs.edit().putString(KEY_MAC_PASSWORD, password).apply()
    }

    fun getMacPassword(): String? = prefs.getString(KEY_MAC_PASSWORD, null)

    fun clearApiKey() {
        prefs.edit().remove(KEY_GEMINI_API).apply()
    }

    fun savePasswordVaultJson(json: String) {
        prefs.edit().putString(KEY_PASSWORD_VAULT_JSON, json).apply()
    }

    fun getPasswordVaultJson(): String = prefs.getString(KEY_PASSWORD_VAULT_JSON, "[]") ?: "[]"

    companion object {
        private const val KEY_GEMINI_API = "gemini_api_key"
        private const val KEY_UNLOCK_PASSWORD = "unlock_password"
        private const val KEY_MAC_PASSWORD = "mac_password"
        private const val KEY_PASSWORD_VAULT_JSON = "password_vault_json"
    }
}
