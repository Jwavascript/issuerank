package io.github.jwavascript.issuerank.platform

import io.github.jwavascript.issuerank.data.HttpRequest
import io.github.jwavascript.issuerank.data.HttpResponse
import io.github.jwavascript.issuerank.data.HttpTransport
import io.github.jwavascript.issuerank.data.ResponseTooLargeException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * HttpURLConnection 전송(새 라이브러리 0 — 계획 1절). 코루틴 IO · 리디렉션 안 따라감 · 캐시 안 씀 · 4xx/5xx 본문 = errorStream ·
 * 머리는 HttpRequest 그대로(User-Agent가 기기 기본값을 덮음 — 기기 모델 안 보냄). 취소되면 연결을 끊어 막힌 읽기를 풂.
 * 쿠키는 이 클래스가 다루지 않음(CookieHandler 없음 — data/SessionCookies만).
 */
class UrlConnectionTransport : HttpTransport {
    override suspend fun execute(request: HttpRequest): HttpResponse = withContext(Dispatchers.IO) {
        val conn = URL(request.url).openConnection() as HttpURLConnection
        val cancelWatch = launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() } finally { conn.disconnect() }
        }
        try {
            conn.instanceFollowRedirects = false
            conn.useCaches = false
            conn.connectTimeout = request.connectTimeoutMs
            conn.readTimeout = request.readTimeoutMs
            conn.requestMethod = request.method
            for ((name, value) in request.headers) conn.setRequestProperty(name, value)
            val body = request.body
            if (body != null) {
                conn.doOutput = true
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val status = conn.responseCode
            val stream = if (status >= 400) conn.errorStream else conn.inputStream
            val bytes = stream?.use { read(it, request.maxResponseBytes) } ?: ByteArray(0)
            val headers = conn.headerFields.orEmpty().mapNotNull { (k, v) -> k?.let { it to v.orEmpty() } }.toMap()
            HttpResponse(status, headers, bytes)
        } finally {
            cancelWatch.cancel()
            conn.disconnect()
        }
    }

    private fun read(input: InputStream, limit: Int?): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (limit != null && out.size() > limit) throw ResponseTooLargeException(limit)
        }
        return out.toByteArray()
    }
}
