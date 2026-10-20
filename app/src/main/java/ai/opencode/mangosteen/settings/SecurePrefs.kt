package ai.opencode.mangosteen.settings

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
 * Preferences where the API key is sealed with an AES/GCM key held in the
 * Android Keystore; only the ciphertext touches SharedPreferences.
 */
class SecurePrefs(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("mangosteen", Context.MODE_PRIVATE)

    fun apiKey(): String? = decrypt(prefs.getString(KEY_API, null))

    fun setApiKey(value: String?) {
        val editor = prefs.edit()
        if (value.isNullOrBlank()) editor.remove(KEY_API) else editor.putString(KEY_API, encrypt(value))
        editor.apply()
    }

    fun defaultModel(): String? = prefs.getString(KEY_MODEL, null)

    fun setDefaultModel(value: String) {
        prefs.edit().putString(KEY_MODEL, value).apply()
    }

    fun defaultSystemPrompt(): String = prefs.getString(KEY_SYS, "").orEmpty()

    fun setDefaultSystemPrompt(value: String) {
        prefs.edit().putString(KEY_SYS, value).apply()
    }

    /** One of: off, on, low, high. "on" leaves the provider default. */
    fun reasoningLevel(): String = prefs.getString(KEY_REASONING, "on").orEmpty().ifBlank { "on" }

    fun setReasoningLevel(value: String) {
        prefs.edit().putString(KEY_REASONING, value).apply()
    }

    fun searchEnabled(): Boolean = prefs.getBoolean(KEY_SEARCH, false)

    fun setSearchEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_SEARCH, value).apply()
    }

    /** Whether Mangosteen may offer and run proactive check-ins. */
    fun proactiveEnabled(): Boolean = prefs.getBoolean(KEY_PROACTIVE, true)

    fun setProactiveEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_PROACTIVE, value).apply()
    }

    /** Whether check-ins raise a system notification. */
    fun notificationsEnabled(): Boolean = prefs.getBoolean(KEY_NOTIFICATIONS, true)

    fun setNotificationsEnabled(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()
    }

    /** Conversation ids starred by the user, shown behind the "Favorites" chip. */
    fun favorites(): Set<Long> = prefs.getString(KEY_FAVORITES, "")
        .orEmpty()
        .split(',')
        .mapNotNull { it.trim().toLongOrNull() }
        .toSet()

    fun themeId(): String = prefs.getString(KEY_THEME, null).orEmpty()

    fun setThemeId(value: String) {
        prefs.edit().putString(KEY_THEME, value).apply()
    }

    fun toggleFavorite(id: Long) {
        val next = favorites().toMutableSet()
        if (!next.add(id)) next.remove(id)
        prefs.edit().putString(KEY_FAVORITES, next.joinToString(",")).apply()
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
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

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val cipherText = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        val combined = ByteArray(1 + iv.size + cipherText.size)
        combined[0] = iv.size.toByte()
        System.arraycopy(iv, 0, combined, 1, iv.size)
        System.arraycopy(cipherText, 0, combined, 1 + iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded == null) return null
        return runCatching {
            val combined = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = combined[0].toInt()
            val iv = combined.copyOfRange(1, 1 + ivLength)
            val cipherText = combined.copyOfRange(1 + ivLength, combined.size)
            val cipher = Cipher.getInstance(TRANSFORM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        }.getOrNull()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val ALIAS = "mangosteen_api_key"
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val KEY_API = "api_key"
        private const val KEY_MODEL = "default_model"
        private const val KEY_SYS = "default_system_prompt"
        private const val KEY_REASONING = "reasoning_level"
        private const val KEY_SEARCH = "search_enabled"
        private const val KEY_PROACTIVE = "proactive_enabled"
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_FAVORITES = "favorite_chats"
        private const val KEY_THEME = "theme_id"
    }
}
