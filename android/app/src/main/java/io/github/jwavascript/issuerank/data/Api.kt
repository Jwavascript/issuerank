package io.github.jwavascript.issuerank.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.CoroutineContext

/** 실패 종류 = 서버가 실제로 내는 상태(openapi · routes · Better Auth) + 네트워크 + 모양 틀림 */
enum class ErrorKind {
    /** 연결 실패 · 시간 초과(응답 없음) */
    NETWORK,
    BAD_REQUEST,          // 400 검증(날짜 형식 · 미래 · 폴더 이름 · 요청 형식 · Better Auth 입력)
    UNAUTHORIZED,         // 401 로그인 필요 · 이메일/비밀번호 틀림 · Google 토큰 거절
    FORBIDDEN,            // 403 Origin(MISSING_OR_NULL_ORIGIN · INVALID_ORIGIN)
    NOT_FOUND,            // 404 날짜 · 이슈 · 폴더 · 북마크 없음 · Google 꺼짐(PROVIDER_NOT_FOUND)
    CONFLICT,             // 409 이미 북마크한 이슈
    UNPROCESSABLE,        // 422 이미 가입된 이메일
    TOO_MANY_REQUESTS,    // 429 운영 속도 제한
    SERVER,               // 5xx(500 글 · 502 BFF)
    /** 성공 상태인데 본문이 계약과 다름 · 본문 한도 넘음 */
    BAD_RESPONSE,
    /** 그 밖 상태(3xx — 리디렉션은 따라가지 않음 — 등) */
    OTHER;

    companion object {
        fun of(status: Int): ErrorKind = when (status) {
            400 -> BAD_REQUEST
            401 -> UNAUTHORIZED
            403 -> FORBIDDEN
            404 -> NOT_FOUND
            409 -> CONFLICT
            422 -> UNPROCESSABLE
            429 -> TOO_MANY_REQUESTS
            in 500..599 -> SERVER
            else -> OTHER
        }
    }
}

/** status · code(Better Auth) · message(서버 본문, 빈 글은 null)만 — 쿠키 · 토큰은 담지 않음 */
data class ApiError(val kind: ErrorKind, val status: Int? = null, val code: String? = null, val message: String? = null)

sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Fail(val error: ApiError) : ApiResult<Nothing>
}

/**
 * 서버 API(계획 3절 '그대로 부름' 전부). 요청 규칙: User-Agent `issuerank-android/{versionName}` · Accept JSON ·
 * 본문이 있으면 Content-Type JSON · POST · DELETE에 Origin = 서버 출처 · Cookie는 그 출처의 세션만 · 연결 5초 · 읽기 10초.
 * 위젯은 쿠키 0 · 읽기 4초 · 64KB(WidgetRules). 함수는 IO 문맥에서 돌고 예외 대신 ApiResult(취소는 그대로 던짐).
 */
class IssueRankApi(
    /** 서버 주소(release 고정 · debug는 debug 도구에서 바꿈) — 요청마다 읽음 */
    private val baseUrl: () -> String,
    versionName: String,
    private val transport: HttpTransport,
    private val cookies: SessionCookies,
    private val io: CoroutineContext = Dispatchers.IO,
) {
    companion object {
        const val CONNECT_TIMEOUT_MS = 5_000
        const val READ_TIMEOUT_MS = 10_000
    }

    private val userAgent = "issuerank-android/$versionName"

    /** GET /api/rankings[?date=] — 날짜 생략 = 최근 수집일 */
    suspend fun rankings(date: String? = null): ApiResult<Ranking> =
        call("GET", "/api/rankings" + (date?.let { "?date=" + Web.encodeURIComponent(it) } ?: "")) { ApiJson.ranking(it.text) }

    /** GET /api/widget — 쿠키 0 · 연결 5초 · 읽기 4초 · 64KB · 200만 성공(widget.js load) */
    suspend fun widget(): ApiResult<WidgetPayload> = call(
        "GET", "/api/widget", session = false,
        readTimeoutMs = WidgetRules.READ_TIMEOUT_MS, maxBytes = WidgetRules.MAX_BODY_BYTES,
    ) {
        if (it.status != 200) bad("status")
        val text = it.text
        WidgetPayload(ApiJson.widget(text), text)
    }

    /** 웹 authForm submit: 본문 { email, password, name } */
    suspend fun signUpEmail(name: String, email: String, password: String): ApiResult<AuthUser> = call(
        "POST", "/api/auth/sign-up/email", JSONObject().put("email", email).put("password", password).put("name", name),
    ) { ApiJson.authUser(it.text) }

    suspend fun signInEmail(email: String, password: String): ApiResult<AuthUser> = call(
        "POST", "/api/auth/sign-in/email", JSONObject().put("email", email).put("password", password),
    ) { ApiJson.authUser(it.text) }

    /** Google ID 토큰(D35): { provider: 'google', idToken: { token, nonce } } → 세션 쿠키. 토큰은 보내기만 하고 두지 않음 */
    suspend fun signInGoogle(idToken: String, nonce: String): ApiResult<AuthUser> = call(
        "POST", "/api/auth/sign-in/social",
        JSONObject().put("provider", "google").put("idToken", JSONObject().put("token", idToken).put("nonce", nonce)),
    ) { ApiJson.authUser(it.text) }

    /** GET get-session: 로그인 사용자 또는 null(쿠키 없음 · 만료) */
    suspend fun session(): ApiResult<AuthUser?> = call("GET", "/api/auth/get-session") { ApiJson.session(it.text) }

    /** 로그아웃 = POST sign-out(본문 {} — 웹과 같음) + 기기 세션 삭제(서버가 실패 · 취소돼도 지움, 계획 3절) */
    suspend fun signOut(): ApiResult<Unit> = try {
        call("POST", "/api/auth/sign-out", JSONObject()) { }
    } finally {
        withContext(NonCancellable + io) { cookies.clear() }
    }

    suspend fun bookmarks(): ApiResult<List<Bookmark>> = call("GET", "/api/bookmarks") { ApiJson.bookmarks(it.text) }

    /** POST /api/bookmarks 201 — 웹처럼 본문은 쓰지 않고 다시 받음(loadBookmarks) */
    suspend fun saveBookmark(issueId: String, folderId: String): ApiResult<Unit> = call(
        "POST", "/api/bookmarks", JSONObject().put("issueId", issueId).put("folderId", folderId),
    ) { }

    suspend fun deleteBookmark(issueId: String): ApiResult<Unit> =
        call("DELETE", "/api/bookmarks/" + Web.encodeURIComponent(issueId)) { }

    suspend fun folders(): ApiResult<List<Folder>> = call("GET", "/api/folders") { ApiJson.folders(it.text) }

    /** 이름은 부르는 쪽이 앞뒤 공백을 뺀 값(웹 createFolderAndSave) — FolderNames.normalize */
    suspend fun createFolder(name: String): ApiResult<Folder> =
        call("POST", "/api/folders", JSONObject().put("name", name)) { ApiJson.folder(it.text) }

    suspend fun deleteFolder(folderId: String): ApiResult<Unit> =
        call("DELETE", "/api/folders/" + Web.encodeURIComponent(folderId)) { }

    private suspend fun <T> call(
        method: String,
        path: String,
        body: JSONObject? = null,
        session: Boolean = true,
        readTimeoutMs: Int = READ_TIMEOUT_MS,
        maxBytes: Int? = null,
        success: (HttpResponse) -> T,
    ): ApiResult<T> = withContext(io) {
        val base = baseUrl().trimEnd('/')
        val origin = Web.originOf(base) ?: return@withContext ApiResult.Fail(ApiError(ErrorKind.NETWORK))
        val headers = buildList {
            add("User-Agent" to userAgent)
            add("Accept" to "application/json")
            if (body != null) add("Content-Type" to "application/json")
            if (method == "POST" || method == "DELETE") add("Origin" to origin)
            if (session) cookies.cookieHeader(origin)?.let { add("Cookie" to it) }
        }
        val request = HttpRequest(
            method, base + path, headers, body?.toString()?.toByteArray(Charsets.UTF_8),
            CONNECT_TIMEOUT_MS, readTimeoutMs, maxBytes,
        )
        val response = try {
            transport.execute(request)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ResponseTooLargeException) {
            return@withContext ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE))
        } catch (e: Exception) {
            return@withContext ApiResult.Fail(ApiError(ErrorKind.NETWORK))
        }
        if (session) cookies.onResponse(origin, response.headerValues("set-cookie"))
        if (maxBytes != null && response.body.size > maxBytes) {
            return@withContext ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, response.status))
        }
        if (response.status !in 200..299) {
            val (code, message) = ApiJson.errorBody(response.text)
            return@withContext ApiResult.Fail(ApiError(ErrorKind.of(response.status), response.status, code, message))
        }
        // 모양 틀림(BadResponseException · JSONException · 그 밖 해석 예외)은 앱을 멈추지 않고 BAD_RESPONSE로
        try {
            ApiResult.Ok(success(response))
        } catch (e: Exception) {
            ApiResult.Fail(ApiError(ErrorKind.BAD_RESPONSE, response.status))
        }
    }
}

/** 웹 loadRanking(637~648) 결과: 200 · 404 = 서버가 답함(answered), 그 밖 = 오류 토스트(Messages.toast) */
sealed interface RankingLoad {
    data class Loaded(val ranking: Ranking) : RankingLoad
    /** 404: 그 날짜 랭킹 없음 → "해당 날짜의 랭킹 데이터가 없습니다" 빈 화면(토스트 없음) */
    data class NoData(val error: ApiError) : RankingLoad
    data class Failed(val error: ApiError) : RankingLoad

    val answered: Boolean get() = this !is Failed

    /** 이 결과 뒤 화면 날짜(state.date): 받으면 랭킹 날짜, 아니면 요청한 날짜(생략했으면 그대로) */
    fun dateAfter(requested: String?, before: String): String = when (this) {
        is Loaded -> ranking.date
        else -> requested ?: before
    }

    companion object {
        fun of(result: ApiResult<Ranking>): RankingLoad = when (result) {
            is ApiResult.Ok -> Loaded(result.value)
            is ApiResult.Fail -> if (result.error.kind == ErrorKind.NOT_FOUND) NoData(result.error) else Failed(result.error)
        }
    }
}

suspend fun IssueRankApi.loadRanking(date: String?): RankingLoad = RankingLoad.of(rankings(date))
