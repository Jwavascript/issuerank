package io.github.jwavascript.issuerank.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.data.WidgetView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 홈 화면 위젯(계획 4절 · AC-15-4 · AC-11-1~4). 방송 하나 = goAsync + 코루틴(받기는 9초에서 끊음 — 방송 10초 한도 안).
 * - onUpdate(30분마다 · 처음 놓을 때 · 기기 시작): 받을 때가 됐으면 GET /api/widget(widget.js 규칙) → 이 위젯 모두를 다시 그림.
 * - onAppWidgetOptionsChanged(크기 바꿈): 받지 않고 캐시로 그 위젯만. 캐시가 없으면 그리지 않음(처음 놓을 때 받는 중에 실패 안내가 먼저 보이지 않게 —
 *   실패 안내는 크기와 무관해 onUpdate가 그린 그대로).
 * - onDisabled(마지막 위젯을 치움): 캐시 · 다음 시각 지움 → 다시 놓으면 바로 받음.
 * 데이터 = AppGraph.api.widget()(쿠키 0 — 화면과 같은 그래프 · 새 Api · SessionCookies 0) · 시계 = AppGraph.clock.
 */
class IssueRankWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        work(context) { app ->
            val view = refresh(app).update()
            draw(app, appWidgetManager, appWidgetManager.getAppWidgetIds(ComponentName(app, IssueRankWidget::class.java)), view)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: Bundle) {
        work(context) { app ->
            val view = refresh(app).cached()
            if (!view.failed) draw(app, appWidgetManager, intArrayOf(appWidgetId), view)
        }
    }

    override fun onDisabled(context: Context) {
        work(context) { app -> WidgetStore(app).clear() }
    }

    private fun work(context: Context, block: suspend (Context) -> Unit) {
        val app = context.applicationContext
        val pending = goAsync()
        scope.launch {
            try {
                block(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** 방송 일(프로세스에 하나 · 파일 · 네트워크 = IO) */
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /** 앱 그래프의 API · 시계 + 위젯 상태(파일 · prefs) */
        fun refresh(context: Context): WidgetRefresh {
            val graph = AppGraph.get(context)
            val store = WidgetStore(context)
            return WidgetRefresh(graph.api::widget, store.cache, store.schedule) { graph.clock.millis() }
        }

        /** 위젯마다 그림: 글자 배율 = 지금 기기 · Android 11 이하 높이 = 옵션 MIN_HEIGHT(없으면 117) */
        fun draw(context: Context, manager: AppWidgetManager, ids: IntArray, view: WidgetView) {
            val fontScale = context.resources.configuration.fontScale.toDouble()
            for (id in ids) {
                val minHeight = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                val height = if (minHeight > 0) minHeight else WidgetSizes.DEFAULT_HEIGHT_DP
                manager.updateAppWidget(id, WidgetViews.forWidget(context, id, view, fontScale, height))
            }
        }
    }
}
