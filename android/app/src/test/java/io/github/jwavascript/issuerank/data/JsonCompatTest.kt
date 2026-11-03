package io.github.jwavascript.issuerank.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * org.json 함정 고정: 앱(Android 프레임워크 org.json)과 이 시험(Maven org.json:json)은 JSON null · 형 바꾸기가 다르다
 * (Android optString(null 칸) = "null" 글자, Maven = ""). data 층은 두 구현에서 같게 도는 도우미(Json.kt)만 쓴다.
 */
class JsonCompatTest {
    private val o = JSONObject("""{"s":"x","empty":"","n":null,"i":5,"d":5.0,"f":5.5,"big":3000000000,"b":true,"a":[1],"o":{},"ns":"12"}""")

    @Test
    fun mavenOptStringOnNullIsEmptyText_soDataLayerNeverUsesIt() {
        // Maven: "" · Android: "null" — 같은 코드가 두 곳에서 다르게 돈다(그래서 optString 금지)
        assertEquals("", o.optString("n"))
        assertTrue(o.isNull("n"))
        assertTrue(o.isNull("missing"))
        assertEquals(false, o.isNull("s"))
    }

    @Test
    fun strOrNullTreatsNullAbsentAndOtherTypesAsNull() {
        assertEquals("x", o.strOrNull("s"))
        assertEquals("", o.strOrNull("empty"))
        assertNull(o.strOrNull("n"))
        assertNull(o.strOrNull("missing"))
        assertNull(o.strOrNull("i")) // 숫자를 글자로 바꾸지 않음(Android getString은 바꿈)
        assertNull(o.strOrNull("b"))
        assertThrows(BadResponseException::class.java) { o.str("n") }
        assertThrows(BadResponseException::class.java) { o.str("i") }
    }

    @Test
    fun intAcceptsOnlyIntegralNumbers() {
        assertEquals(5, o.int("i"))
        assertEquals(5, o.int("d")) // Android Double · Maven BigDecimal — 같은 값
        for (k in listOf("f", "big", "n", "missing", "ns", "s", "b")) {
            assertThrows("칸 $k", BadResponseException::class.java) { o.int(k) }
        }
    }

    @Test
    fun objectAndArrayHelpers() {
        assertNotNull(o.objOrNull("o"))
        assertNull(o.objOrNull("n"))
        assertNull(o.objOrNull("a"))
        assertNotNull(o.arrOrNull("a"))
        assertNull(o.arrOrNull("o"))
        assertThrows(BadResponseException::class.java) { JSONArray("[null]").objAt(0) }
        assertThrows(BadResponseException::class.java) { JSONArray("[3]").strAt(0) }
        assertEquals(JSONObject.NULL, jsonValue("null"))
        assertThrows(BadResponseException::class.java) { jsonObject("[]") }
        assertThrows(BadResponseException::class.java) { jsonArray("{}") }
    }

    /** data 층 소스에 형을 바꾸는 org.json 읽기가 없음(Android · Maven 차이가 생기는 곳) */
    @Test
    fun dataLayerSourcesUseOnlySafeHelpers() {
        val dir = TestFiles.repoFile("android/app/src/main/java/io/github/jwavascript/issuerank/data")
        val banned = Regex("""\.(optString|getString|optInt|getInt|optLong|getLong|optDouble|getDouble|optBoolean|getBoolean|optJSONObject|getJSONObject|optJSONArray|getJSONArray)\(""")
        val hits = dir.listFiles { f -> f.extension == "kt" }!!.flatMap { f ->
            f.readLines().withIndex().filter { banned.containsMatchIn(it.value) && !it.value.trimStart().startsWith("//") }
                .map { "${f.name}:${it.index + 1}: ${it.value.trim()}" }
        }
        assertEquals(emptyList<String>(), hits)
    }
}
