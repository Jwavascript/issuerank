package io.github.jwavascript.issuerank.platform

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.StoredSession
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

/** 세션 보관(계획 3절 R4): AndroidKeyStore AES-GCM — 기기에서만(JVM android.jar에는 Keystore가 없음). 지금은 컴파일만(C2) */
@RunWith(AndroidJUnit4::class)
class KeystoreSessionStorageTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val prefs = context.getSharedPreferences("issuerank_session", Context.MODE_PRIVATE) // KeystoreSessionStorage.PREFS
    private val storage = KeystoreSessionStorage(context)
    private val session = StoredSession("http://127.0.0.1:3100", "better-auth.session_token", "test-only-device-value.sig", 1_900_000_000_000)

    @Before
    @After
    fun clean() = storage.clear()

    @Test
    fun savesOnlyCiphertextAndLoadsBack() {
        storage.save(session)
        assertEquals(session, KeystoreSessionStorage(context).load())
        val raw = prefs.all.values.joinToString(" ")
        assertTrue(raw, raw.startsWith("v1:"))
        for (plain in listOf("test-only-device-value", "127.0.0.1", "better-auth")) assertFalse(plain, raw.contains(plain))
        assertTrue(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias("issuerank_session_v1"))
    }

    @Test
    fun unreadableBlobMeansLoggedOutAndIsWiped() {
        storage.save(session)
        for (broken in listOf("v1:AAAAAAAAAAAAAAAA:BBBBBBBBBBBBBBBB", "v0:x:y", "garbage")) {
            prefs.edit().putString("session", broken).commit()
            assertNull(broken, storage.load())
            assertNull(broken, prefs.getString("session", null))
        }
        // 다음 저장은 새 키로 다시 됨
        storage.save(session)
        assertEquals(session, storage.load())
    }

    @Test
    fun clearAndJarUseTheStorage() {
        val jar = SessionCookies(storage)
        jar.onResponse(session.origin, listOf("better-auth.session_token=test-only-jar; Max-Age=604800; Path=/; HttpOnly; SameSite=Lax"))
        assertEquals("better-auth.session_token=test-only-jar", SessionCookies(KeystoreSessionStorage(context)).cookieHeader(session.origin))
        jar.clear()
        assertNull(KeystoreSessionStorage(context).load())
    }
}
