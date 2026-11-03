package io.github.jwavascript.issuerank.data

import java.io.IOException
import java.net.URI
import java.net.URISyntaxException

/**
 * 요청 하나. 머리는 넣은 순서 그대로. 리디렉션은 따라가지 않음(전송 구현 몫).
 * toString에는 메서드 · 주소만 — Cookie 머리 · 본문(비밀번호 · Google 토큰)은 글로 남기지 않는다.
 */
class HttpRequest(
    val method: String,
    val url: String,
    val headers: List<Pair<String, String>>,
    val body: ByteArray?,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Int,
    /** 본문 한도(바이트). 넘으면 전송이 ResponseTooLargeException */
    val maxResponseBytes: Int? = null,
) {
    fun header(name: String): String? = headers.lastOrNull { it.first.equals(name, ignoreCase = true) }?.second

    override fun toString(): String = "HttpRequest($method $url)"
}

/** 응답: 머리 이름은 소문자, 같은 이름의 여러 줄(Set-Cookie 등)은 목록으로 */
class HttpResponse(val status: Int, headers: Map<String, List<String>>, val body: ByteArray) {
    val headers: Map<String, List<String>> = headers.entries
        .groupBy({ it.key.lowercase() }, { it.value })
        .mapValues { (_, v) -> v.flatten() }

    fun headerValues(name: String): List<String> = headers[name.lowercase()] ?: emptyList()

    val text: String get() = String(body, Charsets.UTF_8)

    override fun toString(): String = "HttpResponse($status)"
}

/** 실제 전송(platform/UrlConnectionTransport) · 시험의 가짜 전송. 연결 실패 · 시간 초과 = IOException */
fun interface HttpTransport {
    suspend fun execute(request: HttpRequest): HttpResponse
}

class ResponseTooLargeException(limit: Int) : IOException("response body over $limit bytes")

/** 웹 쪽 작은 규칙(브라우저 · JS 내장과 같은 결과) */
object Web {
    /** URL → 출처(scheme://host[:port], 기본 포트 생략) = 브라우저 Origin 머리 · Better Auth trustedOrigins 꼴. http(s)가 아니면 null */
    fun originOf(url: String): String? {
        val u = try { URI(url) } catch (e: URISyntaxException) { return null }
        val scheme = u.scheme?.lowercase() ?: return null
        val host = u.host?.lowercase() ?: return null
        val default = when (scheme) { "https" -> 443; "http" -> 80; else -> return null }
        return if (u.port == -1 || u.port == default) "$scheme://$host" else "$scheme://$host:${u.port}"
    }

    private const val HEX = "0123456789ABCDEF"

    /** JS encodeURIComponent: A–Z a–z 0–9 - _ . ! ~ * ' ( ) 말고는 UTF-8 %XX */
    fun encodeURIComponent(s: String): String {
        val out = StringBuilder()
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xff
            if (c < 0x80 && (c.toChar().isLetterOrDigit() || c.toChar() in "-_.!~*'()")) out.append(c.toChar())
            else out.append('%').append(HEX[c shr 4]).append(HEX[c and 15])
        }
        return out.toString()
    }

    /** URLSearchParams 값 풀기: + → 공백, %XX → 바이트(틀린 %는 글자 그대로), UTF-8(틀린 바이트 = U+FFFD) */
    fun decodeQueryComponent(s: String): String {
        val bytes = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '+') {
                bytes.write(' '.code); i++
            } else if (c == '%' && i + 2 < s.length && isHex(s[i + 1]) && isHex(s[i + 2])) {
                bytes.write(s.substring(i + 1, i + 3).toInt(16)); i += 3
            } else {
                // 다음 + · % 앞까지 한 덩어리로(서로게이트 쌍을 쪼개지 않게)
                var j = i + 1
                while (j < s.length && s[j] != '+' && s[j] != '%') j++
                val run = s.substring(i, j)
                bytes.write(run.toByteArray(Charsets.UTF_8)); i = j
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun isHex(c: Char) = c in '0'..'9' || c in 'a'..'f' || c in 'A'..'F'

    /** URLSearchParams(query): 이름 → 값 목록(넣은 순서) */
    fun queryParams(query: String): List<Pair<String, String>> = query.removePrefix("?").split('&')
        .filter { it.isNotEmpty() }
        .map { part ->
            val eq = part.indexOf('=')
            if (eq < 0) decodeQueryComponent(part) to ""
            else decodeQueryComponent(part.substring(0, eq)) to decodeQueryComponent(part.substring(eq + 1))
        }

    /** JS String.prototype.trim(서버 Zod .trim()과 같음): WhiteSpace(TAB · VT · FF · SP · NBSP · FEFF · Zs) + LineTerminator */
    fun jsTrim(s: String): String = s.trim(::isJsSpace)

    // 글자는 코드 값으로(소스에 BOM · 줄 구분 같은 보이지 않는 글자를 두지 않음 — lint ByteOrderMark)
    private val JS_SPACES = intArrayOf(0x09, 0x0B, 0x0C, 0xFEFF, 0x0A, 0x0D, 0x2028, 0x2029)

    private fun isJsSpace(c: Char): Boolean = c.code in JS_SPACES || Character.getType(c) == Character.SPACE_SEPARATOR.toInt()
}
