package io.github.jwavascript.issuerank

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import io.github.jwavascript.issuerank.data.GoogleCredentials
import io.github.jwavascript.issuerank.data.HttpTransport
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.SessionCookies
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.platform.CustomTabs
import io.github.jwavascript.issuerank.platform.GoogleCredentialManager
import io.github.jwavascript.issuerank.platform.KeystoreSessionStorage
import io.github.jwavascript.issuerank.platform.ShareSheet
import io.github.jwavascript.issuerank.platform.UrlConnectionTransport
import java.time.Clock

/**
 * 앱 객체 묶음(프로세스에 하나 — AppGraph.get). 화면 · 위젯 · debug 도구가 모두 이것을 씀:
 * SessionCookies는 메모리 사본이 있어 둘 만들면 어긋남(A2 함정 7).
 * 서버 주소는 serverUrl() 한 곳에서만 읽음(API가 요청마다 부름). 걸이(var)는 계측 시험 · 다음 에이전트가 바꿈 — 기본 = 실제 동작.
 */
class AppGraph private constructor(context: Context) {
    private val app: Context = context.applicationContext

    val clock: Clock = Clock.systemUTC()
    val transport: HttpTransport = UrlConnectionTransport()
    val cookies = SessionCookies(KeystoreSessionStorage(app))
    val api = IssueRankApi(baseUrl = ::serverUrl, versionName = BuildConfig.VERSION_NAME, transport = transport, cookies = cookies)

    /** release = BuildConfig.SERVER_URL 고정(DEBUG가 상수 false라 R8이 debug 갈래를 지움). debug = debug 도구 값(DebugServer) 또는 기본 */
    fun serverUrl(): String =
        if (BuildConfig.DEBUG) DebugServer.read(app) ?: BuildConfig.SERVER_URL else BuildConfig.SERVER_URL

    /** 원문 · 정책 링크 열기(기본 = Custom Tab, Chrome 먼저). 시험은 바꿔서 연 주소만 적음 */
    @Volatile var openUrl: (Context, String) -> Unit = DEFAULT_OPEN_URL

    /** 공유 창(기본 = ACTION_SEND 고르기 창). 시험은 바꿔서 보낸 글만 적음 */
    @Volatile var share: (Context, Share.Data) -> Unit = DEFAULT_SHARE

    /** Google 자격(기본 = Credential Manager — Activity 문맥, 자리표시자 ID면 Unavailable). 시험 = 가짜 서버 토큰을 주는 가짜 */
    @Volatile var googleCredentials: (Activity) -> GoogleCredentials = DEFAULT_GOOGLE

    /** 계측 시험 앞뒤: 걸이를 기본으로 */
    fun resetHooks() {
        openUrl = DEFAULT_OPEN_URL
        share = DEFAULT_SHARE
        googleCredentials = DEFAULT_GOOGLE
    }

    companion object {
        private val DEFAULT_OPEN_URL: (Context, String) -> Unit = CustomTabs::open
        private val DEFAULT_SHARE: (Context, Share.Data) -> Unit = ShareSheet::open
        private val DEFAULT_GOOGLE: (Activity) -> GoogleCredentials = ::GoogleCredentialManager

        // 담는 Context = applicationContext뿐(프로세스와 수명이 같아 새지 않음 — lint는 구분하지 못함)
        @SuppressLint("StaticFieldLeak")
        @Volatile private var instance: AppGraph? = null

        fun get(context: Context): AppGraph =
            instance ?: synchronized(this) { instance ?: AppGraph(context).also { instance = it } }
    }
}

/**
 * debug 서버 주소 걸이(A5 debug 도구가 씀 · 앱은 읽기만): SharedPreferences [PREFS]의 [KEY_SERVER_URL](http(s) 주소, 비면 기본).
 * release는 부르지 않음(AppGraph.serverUrl의 BuildConfig.DEBUG 갈래). 주소를 바꾸면 출처가 달라 세션 쿠키가 안 감(A2 SessionCookies).
 */
object DebugServer {
    const val PREFS = "issuerank_debug"
    const val KEY_SERVER_URL = "server_url"

    fun read(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SERVER_URL, null)?.takeIf { it.isNotBlank() }
}
