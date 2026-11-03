package io.github.jwavascript.issuerank

import io.github.jwavascript.issuerank.IgnoredExtra.Reason.NOT_ALLOWED
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.OUT_OF_RANGE
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.UNKNOWN_NAME
import io.github.jwavascript.issuerank.IgnoredExtra.Reason.WRONG_TYPE
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * debug 도구 adb Extra(검증 11 · 12): 허용 목록 · 형 · 범위만 받고 그 밖은 무시 + 이름과 까닭을 남김(화면 "무시함").
 * server는 fake · prod · default 셋뿐(자유 주소는 화면 입력 — customServer). debug 소스셋 시험(testDebug — release 단위 시험에는 없음).
 */
class DebugExtrasTest {
    private fun ignored(vararg pairs: Pair<String, Any?>) = DebugExtras.parse(mapOf(*pairs)).ignored

    @Test
    fun allowedValues_areTaken() {
        val r = DebugExtras.parse(mapOf("state" to "ok30", "height" to 117, "dark" to true, "scale" to 1.3f, "click" to 2, "server" to "fake"))
        assertEquals(DebugRequest(DebugWidgetStates.State.OK30, 117, true, 1.3f, 2, DebugServerChoice.FAKE), r)
        assertEquals(emptyList<IgnoredExtra>(), r.ignored)
        for (s in listOf("ok30", "ok3", "cache", "fail", "live")) assertEquals(s, DebugExtras.parse(mapOf("state" to s)).state?.key)
        assertEquals(DebugRequest(), DebugExtras.parse(emptyMap())) // 없는 칸 = 화면 기본값
    }

    @Test
    fun ranges_areInclusive_andOutsideIsIgnored() {
        for (h in listOf(40, 600)) assertEquals(h, DebugExtras.parse(mapOf("height" to h)).heightDp)
        for (h in listOf(39, 601, 0, -117)) assertEquals("$h", listOf(IgnoredExtra("height", OUT_OF_RANGE)), ignored("height" to h))
        for (s in listOf(0.85f, 1f, 2f)) assertEquals(s, DebugExtras.parse(mapOf("scale" to s)).scale)
        for (s in listOf(0.84f, 2.01f, 0f, Float.NaN, Float.POSITIVE_INFINITY)) assertEquals("$s", listOf(IgnoredExtra("scale", OUT_OF_RANGE)), ignored("scale" to s))
        for (c in listOf(1, 10)) assertEquals(c, DebugExtras.parse(mapOf("click" to c)).click)
        for (c in listOf(0, 11, -1)) assertEquals("$c", listOf(IgnoredExtra("click", OUT_OF_RANGE)), ignored("click" to c))
    }

    @Test
    fun wrongTypes_unknownNames_andValuesOffTheList_areIgnoredWithTheirReason() {
        // Bundle 형 꺼내기가 기본값을 줘서 null(= 형이 다름): --es height 117 · --ei dark 1 · --ed scale 1.3 등
        assertEquals(
            listOf("click", "dark", "height", "scale", "server", "state").map { IgnoredExtra(it, WRONG_TYPE) },
            ignored("height" to null, "dark" to null, "scale" to null, "click" to null, "state" to null, "server" to null),
        )
        assertEquals(listOf(IgnoredExtra("height", WRONG_TYPE)), ignored("height" to "117"))
        assertEquals(listOf(IgnoredExtra("scale", WRONG_TYPE)), ignored("scale" to 1.3)) // Double(--ed)는 받지 않음
        assertEquals(listOf(IgnoredExtra("state", NOT_ALLOWED)), ignored("state" to "OK30"))
        assertEquals(listOf(IgnoredExtra("state", NOT_ALLOWED)), ignored("state" to "weird"))
        // 자유 주소는 Extra로 받지 않음
        for (s in listOf("http://10.0.2.2:3100", "https://example.invalid", "PROD", "")) {
            val r = DebugExtras.parse(mapOf("server" to s))
            assertNull(s, r.server)
            assertEquals(s, listOf(IgnoredExtra("server", NOT_ALLOWED)), r.ignored)
        }
        // 모르는 이름(값은 적지 않음) · 섞여도 맞는 것은 받음
        val mixed = DebugExtras.parse(mapOf("foo" to "bar", "state" to "fail", "height" to 700, "zzz" to 1))
        assertEquals(DebugWidgetStates.State.FAIL, mixed.state)
        assertNull(mixed.heightDp)
        assertEquals(listOf(IgnoredExtra("foo", UNKNOWN_NAME), IgnoredExtra("height", OUT_OF_RANGE), IgnoredExtra("zzz", UNKNOWN_NAME)), mixed.ignored)
    }

    @Test
    fun serverChoices_areTheThreeAddresses() {
        assertEquals("http://127.0.0.1:3100", DebugServerChoice.FAKE.url)
        assertEquals(BuildConfig.SERVER_URL, DebugServerChoice.FAKE.url) // debug 기본 = 가짜 서버
        assertEquals("https://issuerank-kappa.vercel.app", DebugServerChoice.PROD.url)
        assertNull(DebugServerChoice.DEFAULT.url) // 덮어쓰기 지움
        assertEquals(listOf("fake", "prod", "default"), DebugServerChoice.entries.map { it.key })
        assertEquals(setOf("state", "height", "dark", "scale", "click", "server"), DebugExtras.NAMES)
    }

    @Test
    fun customServer_onlyHttpOrHttpsWithAHost() {
        assertEquals("http://127.0.0.1:3100", DebugExtras.customServer("  http://127.0.0.1:3100 "))
        assertEquals("https://issuerank-kappa.vercel.app", DebugExtras.customServer("https://issuerank-kappa.vercel.app"))
        for (bad in listOf("", "127.0.0.1:3100", "ftp://x.invalid", "http://", "javascript:alert(1)", "https:// spaced.invalid")) {
            assertNull(bad, DebugExtras.customServer(bad))
        }
    }
}
