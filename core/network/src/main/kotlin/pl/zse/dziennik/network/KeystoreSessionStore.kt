package pl.zse.dziennik.network

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Dedicated encrypted store excluded from backups by the app manifest. Never stores a password. */
class KeystoreSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("encrypted_session", Context.MODE_PRIVATE)
    private val alias = "zse.session.aes.v1"
    private val lock = Any()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    suspend fun write(serializedSession: ByteArray) = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
            val ciphertext = cipher.doFinal(serializedSession)
            val encoded = Base64.encodeToString(cipher.iv + ciphertext, Base64.NO_WRAP)
            check(preferences.edit().putString("session", encoded).commit())
        }
    }

    /** Keystore invalidation/authentication errors are propagated so the caller can require re-login. */
    suspend fun read(): ByteArray? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            preferences.getString("session", null)?.let { encoded ->
                val bytes = Base64.decode(encoded, Base64.NO_WRAP)
                require(bytes.size >= 28)
                Cipher.getInstance("AES/GCM/NoPadding").run {
                    init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
                    doFinal(bytes.copyOfRange(12, bytes.size))
                }
            }
        }
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        synchronized(lock) { check(preferences.edit().clear().commit()) }
    }
}
