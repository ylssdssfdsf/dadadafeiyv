package com.dafeiyu.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key 用 Android Keystore 里的 AES-GCM 密钥加密后存本机，
 * 不硬编码、不上传、也不参与备份（见 backup_rules.xml）。
 */
class SecurePrefs(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("fish_secure", Context.MODE_PRIVATE)

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun secretKey(): SecretKey {
        (keyStore.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun saveApiKey(value: String) {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            prefs.edit().remove(KEY_CIPHER).remove(KEY_IV).apply()
            return
        }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
        prefs.edit()
            .putString(KEY_CIPHER, Base64.encodeToString(encrypted, Base64.DEFAULT))
            .putString(KEY_IV, Base64.encodeToString(cipher.iv, Base64.DEFAULT))
            .apply()
    }

    fun apiKey(): String {
        val raw = prefs.getString(KEY_CIPHER, null) ?: return ""
        val iv = prefs.getString(KEY_IV, null) ?: return ""
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.DEFAULT))
            )
            String(cipher.doFinal(Base64.decode(raw, Base64.DEFAULT)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun hasApiKey(): Boolean = apiKey().isNotBlank()

    fun proactiveEnabled(): Boolean = prefs.getBoolean(KEY_PROACTIVE, false)

    fun setProactiveEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PROACTIVE, enabled).apply()
    }

    fun model(): String = prefs.getString(KEY_MODEL, null)
        ?.takeIf { it.isNotBlank() } ?: "deepseek-flash"

    fun setModel(value: String) {
        prefs.edit().putString(KEY_MODEL, value.trim()).apply()
    }

    fun richLayout(): Boolean = prefs.getBoolean(KEY_RICH_LAYOUT, false)

    fun setRichLayout(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RICH_LAYOUT, enabled).apply()
    }

    companion object {
        private const val ALIAS = "dafeiyu_api_key"
        private const val KEY_CIPHER = "cipher"
        private const val KEY_IV = "iv"
        private const val KEY_PROACTIVE = "proactive"
        private const val KEY_MODEL = "model"
        private const val KEY_RICH_LAYOUT = "rich_layout"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
