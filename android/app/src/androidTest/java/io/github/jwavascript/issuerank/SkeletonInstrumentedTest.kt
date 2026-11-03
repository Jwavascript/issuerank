package io.github.jwavascript.issuerank

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// 뼈대 확인: debug 앱이 열리고 첫 화면이 그려짐. 지금은 컴파일만(기기 실행은 나중 — 사용자 결정 11-02)
@RunWith(AndroidJUnit4::class)
class SkeletonInstrumentedTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun opensMainActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.github.jwavascript.issuerank.debug", context.packageName)
        compose.onRoot().assertIsDisplayed()
    }
}
