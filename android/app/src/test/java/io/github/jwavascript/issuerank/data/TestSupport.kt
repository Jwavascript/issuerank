package io.github.jwavascript.issuerank.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.w3c.dom.Element
import java.io.File
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory

/** 시험 공용: 파일 · 고정 시계 · 가짜 전송 · 리소스 문구(네트워크 0) */
object TestFiles {
    /** 저장소 루트(Gradle 단위 시험 작업 폴더 = android/app) */
    val repo: File = generateSequence(File(System.getProperty("user.dir") ?: ".").absoluteFile) { it.parentFile }
        .first { File(it, "ex12/public/index.html").isFile && File(it, "android/app").isDirectory }

    fun repoFile(path: String): File = File(repo, path)

    fun resource(path: String): String =
        requireNotNull(TestFiles::class.java.getResource(path)) { "시험 자원 없음: $path" }.readText(Charsets.UTF_8)

    fun fixture(name: String): String = resource("/fixtures/$name")

    /** 웹 기대값 파일(resources/web, gen-web-expected.mjs)의 cases */
    fun webCases(name: String, kind: String? = null): List<JSONObject> {
        val cases = JSONObject(resource("/web/$name")).getJSONArray("cases")
        return List(cases.length()) { cases.getJSONObject(it) }.filter { kind == null || it.optString("kind") == kind }
    }

    fun clock(iso: String): Clock = Clock.fixed(Instant.parse(iso), ZoneOffset.UTC)

    fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()
}

/** strings_web.xml을 읽어 Android getString(id, args)처럼(값 그대로 · 인자 = String.format) — 화면과 같은 글 */
object XmlStrings : Strings {
    val values: Map<String, String> by lazy { read(TestFiles.repoFile("android/app/src/main/res/values/strings_web.xml")) }

    override fun get(key: Str, vararg args: Any): String {
        val v = requireNotNull(values[key.resName]) { "strings_web.xml에 ${key.resName} 없음" }
        return if (args.isEmpty()) v else String.format(Locale.KOREA, v, *args)
    }

    fun read(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val list = doc.getElementsByTagName("string")
        return (0 until list.length).associate { i ->
            val e = list.item(i) as Element
            e.getAttribute("name") to androidResourceText(e.textContent)
        }
    }

    /** aapt2 문자열 규칙(쓰는 만큼): 따옴표 밖 공백 묶기 · 앞뒤 공백 빼기 · 큰따옴표 지움 · \n \t \' \" \\ \@ \? \uXXXX */
    fun androidResourceText(raw: String): String {
        val out = StringBuilder()
        var quoted = false
        var space = false // 따옴표 밖 날 공백 = 다음 글자 앞에 한 칸(앞 · 끝 날 공백은 버림)
        fun put(c: Char) {
            if (space && out.isNotEmpty()) out.append(' ')
            space = false
            out.append(c)
        }
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when {
                c == '\\' && i + 1 < raw.length -> {
                    val n = raw[i + 1]
                    if (n == 'u' && i + 5 < raw.length) { put(raw.substring(i + 2, i + 6).toInt(16).toChar()); i += 6; continue }
                    put(when (n) { 'n' -> '\n'; 't' -> '\t'; else -> n })
                    i += 2; continue
                }
                c == '"' -> quoted = !quoted
                !quoted && c.isWhitespace() -> space = true
                else -> put(c)
            }
            i++
        }
        return out.toString()
    }
}

/** 요청을 적고 정해 둔 응답(또는 예외)을 돌려주는 전송. 실제 네트워크 0 */
class FakeTransport(private val respond: (HttpRequest) -> HttpResponse) : HttpTransport {
    val requests = mutableListOf<HttpRequest>()

    override suspend fun execute(request: HttpRequest): HttpResponse {
        requests += request
        return respond(request)
    }

    val last: HttpRequest get() = requests.last()

    companion object {
        fun json(status: Int, body: String, vararg headers: Pair<String, String>): HttpResponse =
            HttpResponse(status, headers.groupBy({ it.first }, { it.second }) + ("Content-Type" to listOf("application/json")), body.toByteArray())

        fun empty(status: Int, vararg headers: Pair<String, String>): HttpResponse =
            HttpResponse(status, headers.groupBy({ it.first }, { it.second }), ByteArray(0))

        fun failing(e: IOException = IOException("test-only connection refused")) = FakeTransport { throw e }
    }
}

fun HttpRequest.bodyJson(): JSONObject = JSONObject(String(requireNotNull(body), Charsets.UTF_8))

/** 웹 기대값과 소수 셋째 자리까지(NaN · Infinity는 글자로 적혀 있음) */
fun assertWebNumber(message: String, expected: Any?, actual: Double) {
    val e = when (expected) {
        is Number -> expected.toDouble()
        "NaN" -> Double.NaN
        "Infinity" -> Double.POSITIVE_INFINITY
        "-Infinity" -> Double.NEGATIVE_INFINITY
        else -> throw AssertionError("$message: 숫자 아님 $expected")
    }
    if (e.isNaN() || e.isInfinite()) assertTrue("$message: 기대 $e, 실제 $actual", e.isNaN() && actual.isNaN() || e == actual)
    else assertEquals(message, e, actual, 0.0005)
}

fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

/** JSON 칸 → Kotlin 값(null · 문자열) — Maven org.json null 함정을 피해 isNull로 */
fun JSONObject.text(key: String): String? = if (isNull(key)) null else getString(key)

/** 칸 이름들(시험 컴파일은 android.jar의 org.json 꼴이라 Maven keySet()이 없음 — keys()만) */
fun JSONObject.keyNames(): Set<String> = keys().asSequence().toSet()
