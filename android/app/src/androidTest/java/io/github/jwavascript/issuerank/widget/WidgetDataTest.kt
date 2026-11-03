package io.github.jwavascript.issuerank.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.ui.FakeServer
import io.github.jwavascript.issuerank.ui.FakeServerRule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * 위젯 데이터(검증 8 위젯 줄 · 11): 실제 AppGraph.api.widget() + 앱 전용 저장(파일 · SharedPreferences)으로 가짜 서버 `/api/widget` live 1번 →
 * 때가 아니면 요청 0 · 캐시 → `mode down` → 캐시 → 캐시 없음 + down → "불러오지 못했습니다". 요청 기록 = 쿠키 · Origin 0.
 * + 시스템에 등록된 위젯 정보(이름 · 크기 · 30분 · 설명). 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class WidgetDataTest {
    @get:Rule
    val server = FakeServerRule()

    private val context get() = server.context
    private lateinit var store: WidgetStore

    @Before
    fun clearStore() {
        store = WidgetStore(context)
        store.clear()
    }

    @After
    fun clearAgain() {
        store.clear()
    }

    private fun widgetRequests() = FakeServer.requests().filter { it.getString("path") == "/api/widget" }

    @Test
    fun liveOnce_thenNotDue_thenDownKeepsTheCache_thenNoCacheIsTheFailNotice() = runBlocking {
        val refresh = IssueRankWidget.refresh(context)
        // 1) 처음 = 받음(30줄 · 받은 원문 그대로 캐시 · 다음 시각 = 3시간 뒤(오늘 데이터))
        val before = System.currentTimeMillis()
        val first = refresh.update()
        assertFalse(first.fromCache)
        val widget = first.widget!!
        assertEquals(30, widget.items.size)
        assertEquals(server.today, widget.date)
        assertEquals(widget, ApiJson.widget(store.cache.read()!!))
        val next = store.schedule.nextAt()!!
        assertTrue(next in before + 3 * 3_600_000L..System.currentTimeMillis() + 3 * 3_600_000L)
        val log = widgetRequests()
        assertEquals(1, log.size)
        assertFalse(log[0].getBoolean("cookie"))
        assertTrue(log[0].isNull("origin"))
        assertEquals(200, log[0].getInt("status"))
        // 2) 때가 아님 = 요청 0 · 캐시
        val again = refresh.update()
        assertTrue(again.fromCache)
        assertEquals(1, widgetRequests().size)
        assertEquals(next, store.schedule.nextAt())
        // 3) down + 때가 됨 = 캐시 그대로(원문 안 바뀜)
        val cachedText = store.cache.read()
        FakeServer.down()
        store.schedule.setNextAt(0)
        val down = refresh.update()
        assertTrue(down.fromCache)
        assertEquals(first.widget, down.widget)
        assertEquals(cachedText, store.cache.read())
        assertEquals(2, widgetRequests().size)
        assertTrue(widgetRequests()[1].isNull("status")) // 응답 없이 끊김
        // 4) 캐시 없음 + down = 실패 안내
        store.cache.clear()
        store.schedule.setNextAt(0)
        val failed = refresh.update()
        assertTrue(failed.failed)
        assertNull(store.cache.read())
        val r = WidgetRender.render(failed, 117)
        assertEquals("불러오지 못했습니다", r.fail.text.toString())
        assertEquals(0, r.rows.size)
    }

    @Test
    fun storeIsAppPrivate_nextTimeSurvivesANewInstance_clearRemovesBoth() {
        assertNull(store.schedule.nextAt())
        store.schedule.setNextAt(1_234L)
        assertEquals(1_234L, WidgetStore(context).schedule.nextAt()) // SharedPreferences "issuerank_widget"
        store.cache.write("""{"test":"only"}""")
        val file = File(context.filesDir, WidgetStore.CACHE_FILE)
        assertEquals("""{"test":"only"}""", file.readText())
        assertFalse(File(context.filesDir, WidgetStore.CACHE_FILE + ".tmp").exists())
        store.clear()
        assertNull(WidgetStore(context).schedule.nextAt())
        assertFalse(file.exists())
    }

    @Test
    fun registeredProvider_isThePlanWidget() {
        val manager = AppWidgetManager.getInstance(context)
        val info = manager.getInstalledProvidersForPackage(context.packageName, null)
            .single { it.provider == ComponentName(context, IssueRankWidget::class.java) }
        assertEquals("이슈랭크", info.loadLabel(context.packageManager)) // debug 앱 이름("이슈랭크 시험")과 따로
        assertEquals(1_800_000, info.updatePeriodMillis)
        val density = context.resources.displayMetrics.density
        assertEquals(276 * density, info.minWidth.toFloat(), 1f)
        assertEquals(117 * density, info.minHeight.toFloat(), 1f)
        assertEquals(130 * density, info.minResizeWidth.toFloat(), 1f)
        assertEquals(66 * density, info.minResizeHeight.toFloat(), 1f)
        assertEquals(AppWidgetProviderInfo.RESIZE_BOTH, info.resizeMode)
        assertEquals(AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN, info.widgetCategory)
        assertEquals(R.layout.widget_preview, info.initialLayout)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            assertEquals(4, info.targetCellWidth)
            assertEquals(2, info.targetCellHeight)
            assertEquals(R.layout.widget_preview, info.previewLayout)
            assertEquals("어제 가장 많이 보도된 이슈 헤드라인", info.loadDescription(context)?.toString())
        }
    }
}
