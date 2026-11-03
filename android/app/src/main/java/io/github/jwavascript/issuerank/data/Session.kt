package io.github.jwavascript.issuerank.data

import java.time.Clock
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/** 저장한 세션 쿠키 하나(받은 출처에만 보냄). 값은 toString · 로그 · 예외 글에 남기지 않는다 */
class StoredSession(val origin: String, val name: String, val value: String, val expiresAtMs: Long?) {
    override fun toString(): String = "StoredSession(origin=$origin, name=$name, value=<redacted>, expiresAtMs=$expiresAtMs)"

    override fun equals(other: Any?): Boolean = other is StoredSession && other.origin == origin &&
        other.name == name && other.value == value && other.expiresAtMs == expiresAtMs

    override fun hashCode(): Int = listOf(origin, name, value, expiresAtMs).hashCode()
}

/** 세션 보관처: 앱 = platform/KeystoreSessionStorage(AndroidKeyStore AES-GCM + SharedPreferences), 시험 = 메모리 */
interface SessionStorage {
    /** 없거나 풀 수 없으면 null(풀 수 없으면 구현이 지움 → 로그아웃 상태) */
    fun load(): StoredSession?
    fun save(session: StoredSession)
    fun clear()
}

class MemorySessionStorage(private var session: StoredSession? = null) : SessionStorage {
    override fun load(): StoredSession? = session
    override fun save(session: StoredSession) { this.session = session }
    override fun clear() { session = null }
}

/** Set-Cookie 한 줄(RFC 6265 5.2를 앱에 필요한 만큼). Max-Age가 Expires보다 앞섬 */
class SetCookie(val name: String, val value: String, val maxAgeSec: Long?, val expiresAtMs: Long?) {
    override fun toString(): String = "SetCookie(name=$name, value=<redacted>)"

    companion object {
        private val DIGITS = Regex("-?\\d+")

        fun parse(line: String): SetCookie? {
            val parts = line.split(';')
            val pair = parts[0]
            val eq = pair.indexOf('=')
            if (eq < 0) return null
            val name = pair.substring(0, eq).trim()
            if (name.isEmpty()) return null
            var maxAge: Long? = null
            var expires: Long? = null
            for (attr in parts.drop(1)) {
                val i = attr.indexOf('=')
                val key = (if (i < 0) attr else attr.substring(0, i)).trim().lowercase()
                val v = if (i < 0) "" else attr.substring(i + 1).trim()
                when (key) {
                    "max-age" -> if (DIGITS.matches(v)) maxAge = v.toLongOrNull() ?: if (v.startsWith("-")) Long.MIN_VALUE else Long.MAX_VALUE
                    "expires" -> expires = try {
                        ZonedDateTime.parse(v, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
                    } catch (e: DateTimeParseException) { expires }
                }
            }
            return SetCookie(name, pair.substring(eq + 1).trim(), maxAge, expires)
        }
    }
}

/**
 * 세션 쿠키 보관(계획 3절 세션 R4): 응답 Set-Cookie 중 Better Auth 세션 토큰 2이름만 저장 · 갱신 · 만료 · Max-Age=0이면 지움.
 * 받은 출처(scheme · host · port)가 같은 요청에만 Cookie로 보냄 — debug에서 서버 주소를 바꿔도 다른 호스트로 안 감.
 * 한 프로세스에 하나만 만들 것(메모리 사본 + 보관처).
 */
class SessionCookies(private val storage: SessionStorage, private val clock: Clock = Clock.systemUTC()) {
    companion object {
        /** 운영 https = __Secure- 접두, 로컬 http = 접두 없음(ex12 native-client.test ①) */
        val NAMES = setOf("__Secure-better-auth.session_token", "better-auth.session_token")
    }

    private var loaded = false
    private var current: StoredSession? = null

    private fun session(): StoredSession? {
        if (!loaded) { current = storage.load(); loaded = true }
        val s = current ?: return null
        if (s.expiresAtMs != null && clock.millis() >= s.expiresAtMs) { drop(); return null }
        return s
    }

    private fun drop() { current = null; storage.clear() }

    /** 그 출처로 보낼 Cookie 머리 값("이름=값") 또는 null */
    @Synchronized
    fun cookieHeader(origin: String): String? = session()?.takeIf { it.origin == origin }?.let { "${it.name}=${it.value}" }

    @Synchronized
    fun hasSession(origin: String): Boolean = session()?.origin == origin

    /** 우리 출처 응답의 Set-Cookie 줄들(머리 순서대로) */
    @Synchronized
    fun onResponse(origin: String, setCookieLines: List<String>) {
        for (line in setCookieLines) {
            val c = SetCookie.parse(line) ?: continue
            if (c.name !in NAMES) continue
            val now = clock.millis()
            val expiresAt = when {
                c.maxAgeSec != null -> if (c.maxAgeSec <= 0) Long.MIN_VALUE else now + minOf(c.maxAgeSec, Long.MAX_VALUE / 2000) * 1000
                else -> c.expiresAtMs
            }
            val s = session()
            if (c.value.isEmpty() || (expiresAt != null && expiresAt <= now)) {
                if (s != null && s.origin == origin && s.name == c.name) drop()
            } else {
                current = StoredSession(origin, c.name, c.value, expiresAt).also(storage::save)
            }
        }
    }

    /** 로그아웃 · 풀 수 없음: 기기 세션 지움 */
    @Synchronized
    fun clear() { loaded = true; drop() }
}
