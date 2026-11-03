package io.github.jwavascript.issuerank

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

// 뼈대 확인: debug BuildConfig 값 · 시험 classpath의 org.json이 실제 구현인지(android.jar 껍데기면 "Stub!" 예외)
class SkeletonTest {
    @Test
    fun debugBuildConfig() {
        assertEquals("io.github.jwavascript.issuerank.debug", BuildConfig.APPLICATION_ID)
        assertEquals("1.0.0-debug", BuildConfig.VERSION_NAME)
        assertEquals("http://127.0.0.1:3100", BuildConfig.SERVER_URL)
    }

    @Test
    fun orgJsonIsRealImplementation() {
        assertEquals(3100, JSONObject("""{"port":3100}""").getInt("port"))
    }
}
