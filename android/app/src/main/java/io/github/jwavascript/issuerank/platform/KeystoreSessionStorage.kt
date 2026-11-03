package io.github.jwavascript.issuerank.platform

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import io.github.jwavascript.issuerank.data.SessionStorage
import io.github.jwavascript.issuerank.data.StoredSession
import org.json.JSONObject
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * 세션 쿠키 보관(계획 3절 R4): AndroidKeyStore AES-256-GCM 키(기기 밖으로 못 나감)로 암호화한 글만 SharedPreferences에.
 * 풀 수 없으면(키 없음 · 깨짐 · 다른 기기에서 옮겨 옴) 지우고 null = 로그아웃 상태. 값은 로그 · 예외 글에 남기지 않음.
 * security-crypto(2025 폐기) 대신 공식 대안 = 플랫폼 API + Keystore. 백업 0(allowBackup=false).
 */
class KeystoreSessionStorage(context: Context) : SessionStorage {
    private companion object {
        const val PREFS = "issuerank_session"
        const val KEY_BLOB = "session"
        const val ALIAS = "issuerank_session_v1"
        const val VERSION = "v1"
        const val TAG_BITS = 128
        val AAD = "issuerank-session-v1".toByteArray(Charsets.UTF_8)
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun load(): StoredSession? {
        val blob = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val (version, iv, data) = blob.split(':').also { require(it.size == 3) }
            require(version == VERSION)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, existingKey() ?: error("no key"), GCMParameterSpec(TAG_BITS, decode(iv)))
            cipher.updateAAD(AAD)
            val o = JSONObject(String(cipher.doFinal(decode(data)), Charsets.UTF_8))
            StoredSession(
                origin = o.getString("origin"),
                name = o.getString("name"),
                value = o.getString("value"),
                expiresAtMs = if (o.has("expiresAtMs")) o.getLong("expiresAtMs") else null,
            )
        } catch (e: Exception) {
            wipe()
            null
        }
    }

    override fun save(session: StoredSession) {
        try {
            val plain = JSONObject()
                .put("origin", session.origin).put("name", session.name).put("value", session.value)
                .apply { session.expiresAtMs?.let { put("expiresAtMs", it) } }
                .toString().toByteArray(Charsets.UTF_8)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, existingKey() ?: newKey()) // IV는 Keystore가 무작위로
            cipher.updateAAD(AAD)
            val data = cipher.doFinal(plain)
            prefs.edit().putString(KEY_BLOB, "$VERSION:${encode(cipher.iv)}:${encode(data)}").apply()
        } catch (e: Exception) {
            // 암호화를 못 하면 평문으로 두지 않고 지움(이번 실행 메모리에만 남음)
            wipe()
        }
    }

    override fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private fun wipe() {
        clear()
        try { keyStore().deleteEntry(ALIAS) } catch (e: Exception) { /* 다음 저장 때 새 키 */ }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    private fun existingKey(): SecretKey? = keyStore().getKey(ALIAS, null) as? SecretKey

    private fun newKey(): SecretKey = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
        init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        generateKey()
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
}
