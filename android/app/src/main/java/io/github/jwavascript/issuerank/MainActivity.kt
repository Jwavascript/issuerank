package io.github.jwavascript.issuerank

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.jwavascript.issuerank.ui.IssueRankApp

/**
 * 화면 하나(Compose · edge-to-edge — inset은 화면이 처리). 상태는 MainViewModel, 객체는 AppGraph.
 * 앱 경로(위젯 → 앱): Intent Extra [EXTRA_PATH]("/?issue=<id>" · "/#list" · "/") → onCreate(새로 열 때) · onNewIntent(떠 있을 때 — singleTop).
 */
class MainActivity : ComponentActivity() {
    companion object {
        /** 앱 경로 Extra(위젯 PendingIntent · 시험). 값 = data/AppRoutes 꼴 */
        const val EXTRA_PATH = "io.github.jwavascript.issuerank.extra.PATH"

        /**
         * 위젯 줄 · 줄 밖 · 실패 안내 → 앱(A5가 PendingIntent.getActivity(…, FLAG_IMMUTABLE)로 감쌈).
         * NEW_TASK(앱 밖에서 시작) + CLEAR_TOP(위에 원문 탭 등이 있으면 걷어 냄) + 매니페스트 singleTop → 떠 있는 화면이면 onNewIntent.
         * data = 경로마다 다른 값: PendingIntent는 Extra를 비교하지 않아, 줄마다 같은 requestCode여도 다른 PendingIntent가 되게(화면은 읽지 않음).
         */
        fun routeIntent(context: Context, path: String): Intent = Intent(context, MainActivity::class.java)
            .setData(Uri.fromParts("issuerank", path, null))
            .putExtra(EXTRA_PATH, path)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    internal val viewModel: MainViewModel by viewModels { MainViewModel.factory(AppGraph.get(applicationContext)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // 새로 열 때만 경로를 따름. 다시 만들기(돌리기 · 프로세스 복원) = 저장된 날짜 · 탭(웹이 읽은 쿼리를 지워 새로 고침 = 평소 화면인 것과 같음)
        viewModel.start(if (savedInstanceState == null) routePath(intent) else null)
        setContent { IssueRankApp(viewModel) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        routePath(intent)?.let(viewModel::openPath)
    }

    /** 최근 앱 목록에서 다시 연 Intent(같은 Extra가 다시 옴)는 경로 없음 = 첫 화면 */
    private fun routePath(intent: Intent?): String? {
        if (intent == null || intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return intent.getStringExtra(EXTRA_PATH)
    }
}
