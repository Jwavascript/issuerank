package io.github.jwavascript.issuerank

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.AuthUser
import io.github.jwavascript.issuerank.data.Bookmark
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.DateLine
import io.github.jwavascript.issuerank.data.ErrorKind
import io.github.jwavascript.issuerank.data.Folder
import io.github.jwavascript.issuerank.data.GoogleCredentials
import io.github.jwavascript.issuerank.data.GoogleSignInResult
import io.github.jwavascript.issuerank.data.Inputs
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.IssueLink
import io.github.jwavascript.issuerank.data.IssueRankApi
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.Messages
import io.github.jwavascript.issuerank.data.Msg
import io.github.jwavascript.issuerank.data.PersonalFailure
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.RankingLoad
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.ShortcutOutcome
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.Toast
import io.github.jwavascript.issuerank.data.View
import io.github.jwavascript.issuerank.data.loadRanking
import io.github.jwavascript.issuerank.data.signInWithGoogle
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock

/** 본문 상태: 받기 전 · 랭킹 있음 · 없는 날(404)과 그 밖 오류(웹은 둘 다 ranking = null → 빈 화면) · 네트워크 실패(오프라인 안내) */
enum class Content { LOADING, READY, NO_DATA, OFFLINE }

/**
 * 로그인 창(웹 #authDialog 546~558 · setAuthMode 1398~1410). 웹 폼처럼 닫아도 이름 · 이메일은 남고, 비밀번호는 열기 · 닫기 · 전환 · 제출 뒤 비움.
 * busy = 제출 · Google 진행 중(단추 · 닫기 · Esc 막힘). issueId = 북마크하려던 이슈(웹 authIssue — Google 로그인 뒤 그 상세).
 * revision = 창이 입력 칸 값을 스스로 바꾼 횟수: 화면 입력 칸은 이 값이 바뀔 때만 다시 읽음(입력 중에는 화면이 바로 그림 — 늦게 오는 상태 흐름이 글자를 되돌리지 않게).
 */
data class AuthForm(
    val open: Boolean = false,
    val signUp: Boolean = false,
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val error: Str? = null,
    val busy: Boolean = false,
    val issueId: String? = null,
    val revision: Int = 0,
) {
    // 입력 값(비밀번호 · 이메일)은 글로 남기지 않음
    override fun toString(): String = "AuthForm(open=$open, signUp=$signUp, busy=$busy, error=$error, revision=$revision)"
}

/** 폴더 고르기 창(웹 #pop · popFor 953~964): 북마크할 이슈 + "새 폴더 이름" 입력(16자 — 웹 maxlength) */
data class FolderPicker(val issueId: String, val name: String = "")

/** 폴더 삭제 확인 창(웹 #folderDialog 1035~1088): 대상 폴더(개수 = 그 칩 숫자) · 보내는 중 */
data class FolderDelete(val folder: Folder, val busy: Boolean = false)

/**
 * 화면 상태 하나(웹 state + ranking — index.html 619~633). 돌리기에는 ViewModel이 남고, 날짜 · 탭은 SavedStateHandle로 다시 열기에도 남음.
 * user · bookmarks · folders = 계정 상태(웹 loggedIn · bookmarks · folders), 창 3개(로그인 · 폴더 고르기 · 폴더 삭제) = A4.
 */
data class UiState(
    val view: View = View.MAP,
    /** 화면 날짜(웹 state.date). 첫 응답 전 = KST 오늘 */
    val date: String,
    /** 마지막 요청 때 KST 오늘(날짜 줄 · 달력 최대) */
    val today: String,
    val ranking: Ranking? = null,
    val content: Content = Content.LOADING,
    /** 목록 섹션 탭(null = 전체) */
    val section: SectionKey? = null,
    /** 이슈맵 확대 섹션(null = 전체) */
    val zoom: SectionKey? = null,
    /** 요약 펼치기(기본 켬) */
    val expanded: Boolean = true,
    /** 상세(ModalBottomSheet)에 연 이슈 id — 지금 랭킹에 있을 때만 보임 */
    val selectedId: String? = null,
    /** 로그인 사용자(get-session · 가입 · 로그인 · Google) */
    val user: AuthUser? = null,
    /** 내 북마크: 카드 · 타일 · 상세의 북마크 표시, 머리 숫자, 북마크 화면 */
    val bookmarks: List<Bookmark> = emptyList(),
    /** 내 폴더(서버 순서 · 개수) — 폴더 고르기 · 칩 */
    val folders: List<Folder> = emptyList(),
    /** 북마크 화면 폴더 칩(BookmarkView.ALL 또는 폴더 id) */
    val folderFilter: String = BookmarkView.ALL,
    val auth: AuthForm = AuthForm(),
    val picker: FolderPicker? = null,
    val folderDelete: FolderDelete? = null,
) {
    val dateLine: DateLine get() = DateLine(date, today, view, ranking)
    val selected: Issue? get() = selectedId?.let { ranking?.issueById(it) }
    val bookmarkBadge: Int? get() = BookmarkView.badge(user != null, bookmarks)
    fun isBookmarked(issueId: String): Boolean = BookmarkView.find(bookmarks, issueId) != null

    /** 고른 칩의 폴더가 없어졌으면 전체(웹 renderBookmarks 862) */
    val effectiveFilter: String get() = BookmarkView.effectiveFilter(folderFilter, folders)
}

/**
 * 토스트 하나(웹 toast(msg, action, fn)) — 한 번만 소비(Snackbar 하나). 단추 글 = toast.action, 누르면 onAction.
 * 단추 종류는 [BUTTONS](계획 1절 "되돌리기" · "북마크 보기" · "로그인")만.
 */
class Notice(val toast: Toast, val onAction: (() -> Unit)? = null) {
    init {
        require(toast.action == null || toast.action in BUTTONS) { "toast button ${toast.action}" }
    }

    companion object {
        val BUTTONS = setOf(Str.TOAST_UNDO, Str.TOAST_VIEW_BOOKMARKS, Str.LOGIN)
    }
}

/**
 * 화면 상태 + 동작(웹 start · loadRanking · setDate · go · openDrawer · toast · 로그인 창 · 북마크 · 폴더). 데이터는 A2 data 층 그대로, 문구는 키(Str/Msg)로만.
 * 오래된 응답 버리기: 랭킹 = 요청 순서(loadSeq — 마지막 요청만 그림), 개인 API = authVersion(웹 api() · save · unsave · deleteFolder · loadBookmarks),
 * get-session = authVersion(restoreSession). 로그인 응답은 버리지 않음 — 그 Set-Cookie가 이미 기기 세션이라 버리면 화면 계정 ≠ 세션(대신 바쁜 동안 창이 닫히지 않음).
 */
class MainViewModel(
    private val saved: SavedStateHandle,
    private val api: IssueRankApi,
    private val clock: Clock,
) : ViewModel() {
    companion object {
        /** SavedStateHandle 칸: 탭(View.hash) · 화면 날짜 */
        const val KEY_VIEW = "view"
        const val KEY_DATE = "date"

        fun factory(graph: AppGraph): ViewModelProvider.Factory = viewModelFactory {
            initializer { MainViewModel(createSavedStateHandle(), graph.api, graph.clock) }
        }
    }

    private val _state = MutableStateFlow(
        KstDate.today(clock).let { today ->
            UiState(view = View.of(saved[KEY_VIEW]) ?: View.MAP, date = saved[KEY_DATE] ?: today, today = today)
        },
    )
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _notices = Channel<Notice>(Channel.BUFFERED)

    /** 한 번만 소비되는 토스트(화면이 모아 Snackbar로) */
    val notices: Flow<Notice> = _notices.receiveAsFlow()

    private var started = false
    private var loadJob: Job? = null
    private var loadSeq = 0
    private var sessionJob: Job? = null
    private var sessionKnown = false

    /** 계정이 바뀔 때마다 +1(웹 authVersion). 개인 API 응답 앞뒤로 currentAccount(version) 확인 */
    private var authVersion = 0

    /** 웹 authBusy: 제출 · Google · 로그아웃이 같이 씀(그동안 로그인 창 열기 · 다시 제출 · 로그아웃 무시) */
    private var authBusy = false

    /** get-session이 네트워크로 실패함 — 그 랭킹이 오프라인 안내면 안내가 대신, 아니면 웹처럼 토스트(load가 정함) */
    private var sessionUnreachable = false

    /** 네트워크로 실패한 요청 — '다시 시도' = 같은 요청(웹 offline.html = 그 주소 새로 고침) */
    private var failed: Request? = null

    private data class Request(val date: String?, val shortcut: IssueLink?)

    // ── 시작 · 앱 경로 ──

    /**
     * MainActivity.onCreate에서 부름(두 번째부터는 무시 — 돌리기). path = 새로 열 때의 앱 경로(위젯), 없으면
     * 저장된 날짜 · 탭(다시 만들기) 또는 첫 화면(이슈맵 · 최근 수집일). 웹 start(): 랭킹 · 로그인 확인을 같이, 바로가기 상세는 둘 다 끝난 뒤.
     */
    fun start(path: String?) {
        if (started) return
        started = true
        sessionJob = viewModelScope.launch { restoreSession() }
        if (path != null) open(path) else load(saved[KEY_DATE], null)
    }

    /** onNewIntent의 앱 경로(떠 있는 화면 — 위젯을 다시 누름): 새로 열 때와 같은 처리(그 날짜 랭킹 → 상세 / 목록 / 첫 화면) */
    fun openPath(path: String) {
        if (!started) return start(path)
        open(path)
    }

    private fun open(path: String) {
        val route = AppRoutes.parse(path, KstDate.today(clock))
        setView(route.view)
        load(route.issue?.date, route.issue)
    }

    // ── 랭킹 · 날짜 ──

    /**
     * 웹 loadRanking + setDate(오늘 이후 = 오늘). 응답이 올 때까지 이전 화면 그대로, 오면 날짜 · 랭킹을 한 번에 바꾸고 상세를 닫음.
     * 늦게 온 앞 요청은 버림. 404 = 빈 화면(토스트 0) · 400/5xx = 빈 화면 + 서버 글 토스트 · 네트워크 = 오프라인 안내.
     * 로그인 확인이 진행 중이면 그것까지 끝난 뒤 그림(웹 start의 Promise.all — 늦게 온 로그인 결과가 막 연 상세를 닫지 않게).
     */
    private fun load(date: String?, shortcut: IssueLink?) {
        val seq = ++loadSeq
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val today = KstDate.today(clock)
            val requested = date?.let { KstDate.clampToToday(it, today) }
            val load = api.loadRanking(requested)
            sessionJob?.join()
            if (seq != loadSeq) return@launch
            val offline = load is RankingLoad.Failed && load.error.kind == ErrorKind.NETWORK
            failed = if (offline) Request(requested, shortcut) else null
            _state.update { s ->
                s.copy(
                    date = load.dateAfter(requested, s.date),
                    today = today,
                    ranking = (load as? RankingLoad.Loaded)?.ranking,
                    content = when {
                        load is RankingLoad.Loaded -> Content.READY
                        offline -> Content.OFFLINE
                        else -> Content.NO_DATA
                    },
                    selectedId = null,
                )
            }
            saved[KEY_DATE] = _state.value.date
            // 웹 restoreSession 실패 토스트(1446): 네트워크 실패여도 화면이 랭킹을 그렸으면 토스트, 오프라인 안내면 안내가 대신(offline.html에 없음)
            if (sessionUnreachable) {
                sessionUnreachable = false
                if (!offline) notify(Messages.sessionFailed)
            }
            if (load is RankingLoad.Failed && !offline) notify(Messages.toast(load.error))
            if (shortcut == null) return@launch
            when (val o = AppRoutes.outcome(shortcut, load)) {
                is ShortcutOutcome.Open -> _state.update { it.copy(selectedId = o.issue.id) }
                ShortcutOutcome.NotFound -> notify(AppRoutes.notFoundToast)
                ShortcutOutcome.None -> Unit // 5xx 토스트 · 오프라인 안내 그대로(D25)
            }
        }
    }

    fun prevDay() = load(KstDate.addDays(_state.value.date, -1), null)

    fun nextDay() {
        val s = _state.value
        if (s.dateLine.nextEnabled) load(KstDate.addDays(s.date, 1), null)
    }

    /** "오늘" 단추 · "오늘 랭킹 보기": 웹 setDate(TODAY) — 오늘 수집 전이면 404 빈 화면(웹과 같음) */
    fun goToday() = load(KstDate.today(clock), null)

    /** 달력(오늘 이후는 달력이 막고, 여기서도 오늘로) */
    fun pickDate(date: String) {
        if (date != _state.value.date) load(date, null)
    }

    /** 오프라인 안내 '다시 시도': 실패한 요청 그대로 + (아직 모르면) 로그인 확인 */
    fun retry() {
        if (!sessionKnown && sessionJob?.isActive != true) sessionJob = viewModelScope.launch { restoreSession() }
        val r = failed ?: Request(_state.value.date, null)
        load(r.date, r.shortcut)
    }

    // ── 화면 ──

    /** 탭(웹 go): 상세 · 폴더 고르기를 닫음 */
    fun selectView(view: View) = setView(view)

    private fun setView(view: View) {
        saved[KEY_VIEW] = view.hash
        _state.update { it.copy(view = view, selectedId = null, picker = null) }
    }

    /** 이슈맵 확대(null = 전체) — 웹 data-zoom: 상세를 닫음 */
    fun zoom(section: SectionKey?) = _state.update { it.copy(zoom = section, selectedId = null) }

    /** 목록 섹션 탭(null = 전체) */
    fun selectSection(section: SectionKey?) = _state.update { it.copy(section = section) }

    fun setExpanded(expanded: Boolean) = _state.update { it.copy(expanded = expanded) }

    /** 상세 열기(웹 openDrawer — 지금 랭킹에 없으면 무시) */
    fun openIssue(issueId: String) {
        if (_state.value.ranking?.issueById(issueId) != null) _state.update { it.copy(selectedId = issueId) }
    }

    fun closeIssue() = _state.update { it.copy(selectedId = null) }

    // ── 계정(웹 restoreSession · applyUser · 로그인 창 · Google · signOut) ──

    /**
     * 웹 restoreSession(1443~1447): get-session → applyUser. 실패 = "로그인 상태를 확인하지 못했습니다".
     * 네트워크 실패는 load가 정함(그 랭킹이 오프라인 안내면 안내가 대신 — offline.html에 토스트 없음, 랭킹을 그렸으면 웹처럼 토스트).
     */
    private suspend fun restoreSession() {
        val version = authVersion
        when (val r = api.session()) {
            is ApiResult.Ok -> {
                sessionKnown = true
                if (authVersion == version) applyUser(r.value)
            }
            is ApiResult.Fail -> if (r.error.kind == ErrorKind.NETWORK) sessionUnreachable = true else notify(Messages.sessionFailed)
        }
    }

    /**
     * 웹 applyUser(1434~1442): 계정이 바뀜 → authVersion +1 · 북마크 · 폴더 비움 · 폴더 칩 전체 · 상세 · 폴더 고르기 · 폴더 삭제 창 닫기.
     * 로그인이면 로그인 창도 닫고(보내는 중이면 그 응답이 닫음) 북마크 · 폴더를 받은 뒤 돌아옴(웹 await loadBookmarks — 첫 그림 · 로그인 토스트가 그 뒤).
     */
    private suspend fun applyUser(user: AuthUser?) {
        authVersion++
        _state.update { s ->
            s.copy(
                user = user, bookmarks = emptyList(), folders = emptyList(), folderFilter = BookmarkView.ALL,
                selectedId = null, picker = null, folderDelete = null,
                auth = if (user != null && s.auth.open && !s.auth.busy) s.auth.closed() else s.auth,
            )
        }
        if (user != null) loadBookmarks()
    }

    /** 웹 currentAccount(version): 응답 때 그 계정이 그대로인지 */
    private fun currentAccount(version: Int): Boolean = _state.value.user != null && authVersion == version

    /** 머리 계정 단추(웹 #userBtn): 비로그인 = 로그인 창, 로그인 = 로그아웃 */
    fun onAccount() {
        if (_state.value.user == null) requestLogin(null) else signOut()
    }

    /**
     * 로그아웃(웹 signOut 1448~1455 · 640 이하 북마크 화면 "{이름} · 로그아웃"): sign-out → 기기 세션 지움(서버가 실패해도 — 계획 3절 · A2 7절 2) →
     * 로그아웃 상태 + "로그아웃했습니다". 보내는 동안 다시 누름 · 로그인 창 열기는 무시(웹 authBusy).
     */
    fun signOut() {
        if (authBusy || _state.value.user == null) return
        authBusy = true
        viewModelScope.launch {
            try {
                api.signOut()
            } finally {
                authBusy = false
            }
            applyUser(null)
            notify(Messages.signedOut)
        }
    }

    /**
     * 로그인 창 열기(웹 openAuth 1413~1419 → setAuthMode('sign-in')): 로그인 모드 · 비밀번호 · 오류 비움(이름 · 이메일은 남음 — 웹 폼).
     * issueId = 북마크하려던 이슈(웹 authIssue — Google 로그인 뒤 그 상세). 보내는 중(제출 · Google · 로그아웃)이면 무시.
     */
    fun requestLogin(issueId: String? = null) {
        if (authBusy) return
        _state.update { s ->
            s.copy(auth = s.auth.copy(open = true, signUp = false, password = "", error = null, busy = false, issueId = issueId, revision = s.auth.revision + 1))
        }
    }

    /** "닫기" · 뒤로(웹 authClose · cancel — 보내는 중이면 막힘): 비밀번호 비움 */
    fun closeAuth() {
        if (authBusy) return
        _state.update { s -> if (s.auth.open) s.copy(auth = s.auth.closed()) else s }
    }

    /** "회원가입" ↔ "로그인으로 전환"(웹 setAuthMode): 이름 칸 · 비밀번호 · 오류 비움(보내는 중이면 단추가 막힘) */
    fun switchAuthMode() {
        if (authBusy) return
        _state.update { s ->
            if (!s.auth.open) s else s.copy(auth = s.auth.copy(signUp = !s.auth.signUp, password = "", error = null, revision = s.auth.revision + 1))
        }
    }

    /** 입력 칸(웹 maxlength: 이름 100 · 비밀번호 128 · 이메일 없음) */
    fun setAuthName(value: String) = editAuth { it.copy(name = Inputs.limit(value, Inputs.NAME_MAX)) }

    fun setAuthEmail(value: String) = editAuth { it.copy(email = value) }

    fun setAuthPassword(value: String) = editAuth { it.copy(password = Inputs.limit(value, Inputs.PASSWORD_MAX)) }

    private fun editAuth(change: (AuthForm) -> AuthForm) = _state.update { s -> if (s.auth.open) s.copy(auth = change(s.auth)) else s }

    /**
     * 이메일 가입 · 로그인 제출(웹 authForm submit 1456~1475): 가입인데 이름이 비면 "이름을 입력해 주세요"(요청 0) → 보내는 동안 단추 막음 →
     * 성공 = 창 닫기 → applyUser(북마크 · 폴더까지) → "로그인했습니다", 실패 = 창 안 오류 줄(authMessage). 끝나면 늘 비밀번호 비움.
     */
    fun submitAuth() {
        val a = _state.value.auth
        if (!a.open || authBusy) return
        Inputs.authError(a.signUp, a.name)?.let { e ->
            _state.update { it.copy(auth = it.auth.copy(error = e)) }
            return
        }
        authBusy = true
        _state.update { it.copy(auth = it.auth.copy(busy = true, error = null)) }
        val email = Inputs.email(a.email)
        viewModelScope.launch {
            var user: AuthUser? = null
            try {
                val r = if (a.signUp) api.signUpEmail(Inputs.name(a.name), email, a.password) else api.signInEmail(email, a.password)
                when (r) {
                    is ApiResult.Ok -> user = r.value
                    is ApiResult.Fail -> _state.update { it.copy(auth = it.auth.copy(error = Messages.auth(r.error, signingIn = !a.signUp))) }
                }
            } finally {
                authBusy = false
                finishAuth(signedIn = user != null, clearPassword = true)
            }
            user?.let { signedIn(it, issueId = null) }
        }
    }

    /**
     * "Google로 계속하기"(웹 googleSignIn 1500~1518 → 앱 = Credential Manager ID 토큰 한 번, D35 · A2 signInWithGoogle): 단추 막음 → 계정 창 → sign-in/social.
     * 성공 = 이메일 로그인과 같은 뒤처리 + 북마크하려던 이슈 상세(웹 Google 돌아옴 1614~1618 — 지금 랭킹에 있으면), 실패 = 그 문구를 창 안 오류 줄에.
     * credentials = 화면 Activity로 만든 걸이(AppGraph.googleCredentials) — 계정 창이 떠 있는 동안 돌려도 이어지게 ViewModel에서 돎.
     */
    fun googleSignIn(credentials: GoogleCredentials) {
        val a = _state.value.auth
        if (!a.open || authBusy) return
        authBusy = true
        _state.update { it.copy(auth = it.auth.copy(busy = true, error = null)) }
        viewModelScope.launch {
            var user: AuthUser? = null
            try {
                when (val r = signInWithGoogle(credentials, api)) {
                    is GoogleSignInResult.SignedIn -> user = r.user
                    is GoogleSignInResult.Error -> _state.update { it.copy(auth = it.auth.copy(error = r.message)) }
                }
            } finally {
                authBusy = false
                finishAuth(signedIn = user != null, clearPassword = false)
            }
            user?.let { signedIn(it, a.issueId) }
        }
    }

    /** 보내기 끝(웹 finally · 성공이면 authDialog.close()): 단추 풂, 성공이면 창 닫기 · 이메일 제출이면 비밀번호 비움 */
    private fun finishAuth(signedIn: Boolean, clearPassword: Boolean) = _state.update { s ->
        s.copy(
            auth = when {
                signedIn -> s.auth.closed()
                clearPassword -> s.auth.copy(busy = false, password = "", revision = s.auth.revision + 1)
                else -> s.auth.copy(busy = false)
            },
        )
    }

    /** 로그인 뒤(웹: 창 닫기 → await applyUser → (Google 돌아옴이면 그 이슈 상세) → "로그인했습니다") */
    private suspend fun signedIn(user: AuthUser, issueId: String?) {
        applyUser(user)
        if (issueId != null) openIssue(issueId)
        notify(Messages.signedIn)
    }

    private fun AuthForm.closed() = copy(open = false, password = "", error = null, busy = false, issueId = null, revision = revision + 1)

    // ── 북마크 · 폴더(웹 api() 개인 경로 · loadBookmarks · save · unsave · createFolderAndSave · deleteFolder) ──

    /**
     * 웹 api()의 개인 경로 규칙(586~599): 응답 때 계정이 바뀌었으면 버림(null), 401이면 로그아웃 상태 + "로그인이 필요합니다"(단추 "로그인") 뒤 null.
     * 그 밖은 결과 그대로(실패 토스트는 부른 쪽 — 그 계정일 때만).
     */
    private suspend fun <T> personal(call: suspend () -> ApiResult<T>): ApiResult<T>? {
        val version = authVersion
        val r = call()
        if (version != authVersion) return null
        if (r is ApiResult.Fail && Messages.personal(r.error) == PersonalFailure.LoginRequired) {
            applyUser(null)
            notify(Messages.loginRequired) { requestLogin(null) }
            return null
        }
        return r
    }

    /** 개인 API 쓰기 결과: 성공 + 그 계정 그대로면 true, 실패면(그 계정일 때) 서버 글 토스트(웹 catch: toast(e.message)) */
    private fun done(r: ApiResult<Unit>?, version: Int): Boolean = when (r) {
        null -> false
        is ApiResult.Fail -> {
            if (currentAccount(version)) notify(Messages.toast(r.error))
            false
        }
        is ApiResult.Ok -> currentAccount(version)
    }

    /** 웹 loadBookmarks(649~657): 북마크 · 폴더를 같이 받아 둘 다 오면(그 계정 그대로일 때) 바꿈, 하나라도 실패면 토스트 */
    private suspend fun loadBookmarks() {
        if (_state.value.user == null) return
        val version = authVersion
        val (bookmarks, folders) = coroutineScope {
            val b = async { personal { api.bookmarks() } }
            val f = async { personal { api.folders() } }
            b.await() to f.await()
        }
        if (bookmarks is ApiResult.Ok && folders is ApiResult.Ok) {
            if (currentAccount(version)) _state.update { it.copy(bookmarks = bookmarks.value, folders = folders.value) }
            return
        }
        val error = ((bookmarks as? ApiResult.Fail) ?: (folders as? ApiResult.Fail))?.error ?: return
        if (version == authVersion) notify(Messages.toast(error))
    }

    /**
     * 카드 · 상세의 북마크 단추(웹 1535~1540): 비로그인 = "북마크하려면 로그인이 필요합니다" + "로그인"(그 이슈로 로그인 창),
     * 이미 북마크면 해제(+ "되돌리기"), 아니면 폴더 고르기(같은 이슈로 열려 있으면 닫기).
     */
    fun onBookmark(issue: Issue) {
        val s = _state.value
        if (s.user == null) {
            notify(Messages.bookmarkNeedsLogin) { requestLogin(issue.id) }
            return
        }
        if (s.isBookmarked(issue.id)) {
            closePicker()
            unsave(issue.id)
            return
        }
        _state.update { it.copy(picker = if (it.picker?.issueId == issue.id) null else FolderPicker(issue.id)) }
    }

    /** 폴더 고르기 닫기(바깥 · 뒤로 — 웹 closePop) */
    fun closePicker() = _state.update { it.copy(picker = null) }

    /** "새 폴더 이름"(웹 maxlength 16 — 17번째 글자부터는 안 들어감) */
    fun setFolderName(value: String) = _state.update { s ->
        s.picker?.let { s.copy(picker = it.copy(name = Inputs.limit(value, Inputs.FOLDER_NAME_MAX))) } ?: s
    }

    /** 폴더 고르기의 폴더 하나(웹 data-save) */
    fun saveToFolder(folderId: String) {
        val p = _state.value.picker ?: return
        save(p.issueId, folderId)
    }

    /** "만들기"(웹 newFolder submit 1556~1562): 앞뒤 공백을 뺀 1~16자 → 폴더를 만들고 거기에 저장. 비면 아무것도 안 함 · 17자 이상은 거절(요청 0) */
    fun createFolder() {
        val p = _state.value.picker ?: return
        if (_state.value.user == null) return requestLogin(null)
        val name = Inputs.folderName(p.name) ?: return
        if (name.length > Inputs.FOLDER_NAME_MAX) return
        createFolderAndSave(name, p.issueId)
    }

    /** 웹 createFolderAndSave(1006~1013): 만들기 실패면 고르기 창은 그대로 + 토스트 */
    private fun createFolderAndSave(name: String, issueId: String) {
        val version = authVersion
        if (!currentAccount(version)) return
        viewModelScope.launch {
            when (val r = personal { api.createFolder(name) }) {
                null -> Unit
                is ApiResult.Fail -> if (currentAccount(version)) notify(Messages.toast(r.error))
                is ApiResult.Ok -> if (currentAccount(version)) save(issueId, r.value.id)
            }
        }
    }

    /** 웹 save(969~984): 고르기 닫기 → 저장 → 다시 받기 → "‘폴더’에 저장했습니다" + "북마크 보기"(그 폴더 칩으로 북마크 화면) */
    private fun save(issueId: String, folderId: String) {
        if (_state.value.user == null) return requestLogin(null)
        val version = authVersion
        closePicker()
        viewModelScope.launch {
            if (!done(personal { api.saveBookmark(issueId, folderId) }, version)) return@launch
            loadBookmarks()
            if (!currentAccount(version)) return@launch
            val folder = BookmarkView.folderName(_state.value.folders, folderId)
            notify(Toast(Msg.Res(Str.TOAST_SAVED, listOf(folder)), Str.TOAST_VIEW_BOOKMARKS)) {
                if (currentAccount(version)) {
                    _state.update { it.copy(folderFilter = folderId) }
                    setView(View.BOOKMARKS)
                }
            }
        }
    }

    /** 웹 unsave(985~1005): 해제 → 다시 받기 → "북마크를 해제했습니다" + "되돌리기"(지운 북마크의 폴더로 다시 저장) */
    private fun unsave(issueId: String) {
        val version = authVersion
        val hit = BookmarkView.find(_state.value.bookmarks, issueId) ?: return
        viewModelScope.launch {
            if (!done(personal { api.deleteBookmark(issueId) }, version)) return@launch
            loadBookmarks()
            if (!currentAccount(version)) return@launch
            notify(Toast(Msg.Res(Str.TOAST_UNSAVED), Str.TOAST_UNDO)) { undo(issueId, hit.folderId, version) }
        }
    }

    /** "되돌리기"(웹 995~1003): 그 계정 그대로면 같은 폴더(folderId)로 다시 저장 → 다시 받기(토스트 없음) */
    private fun undo(issueId: String, folderId: String, version: Int) {
        if (!currentAccount(version)) return
        viewModelScope.launch {
            if (!done(personal { api.saveBookmark(issueId, folderId) }, version)) return@launch
            loadBookmarks()
        }
    }

    /** 북마크 화면 폴더 칩(웹 data-folder) */
    fun selectFolder(folderId: String) = _state.update { it.copy(folderFilter = folderId) }

    /** 칩 줄 끝 "폴더 삭제"(웹 openFolderDelete 1035~1045): 그 폴더가 있고 창이 닫혀 있을 때만 */
    fun requestFolderDelete(folderId: String) {
        val s = _state.value
        val folder = s.folders.firstOrNull { it.id == folderId } ?: return
        if (s.folderDelete != null) return
        _state.update { it.copy(folderDelete = FolderDelete(folder)) }
    }

    /** "취소" · 뒤로 · 바깥(웹 cancelFolderDelete): 요청 0, 보내는 중이면 무시 */
    fun cancelFolderDelete() = _state.update { s -> if (s.folderDelete?.busy == true) s else s.copy(folderDelete = null) }

    /**
     * "삭제"(웹 deleteFolder 1052~1088): 두 단추 막음 → DELETE 1번 → 응답이 오면 먼저 창을 닫음 → 계정이 바뀌었거나 401이면 끝 →
     * 실패 = 서버 글 토스트(404면 다시 받기) · 성공 = 그 폴더 · 그 폴더 북마크를 바로 빼고 전체 칩 → 다시 받기 → "‘이름’ 폴더를 삭제했습니다".
     */
    fun confirmFolderDelete() {
        val d = _state.value.folderDelete ?: return
        if (d.busy) return
        val version = authVersion
        val sending = d.copy(busy = true)
        _state.update { it.copy(folderDelete = sending) }
        viewModelScope.launch {
            val r = personal { api.deleteFolder(d.folder.id) }
            _state.update { if (it.folderDelete === sending) it.copy(folderDelete = null) else it }
            if (r == null || !currentAccount(version)) return@launch
            if (r is ApiResult.Fail) {
                notify(Messages.toast(r.error))
                if (r.error.kind == ErrorKind.NOT_FOUND) loadBookmarks()
                return@launch
            }
            _state.update { s ->
                s.copy(
                    folders = s.folders.filter { it.id != d.folder.id },
                    bookmarks = s.bookmarks.filter { it.folderId != d.folder.id },
                    folderFilter = BookmarkView.ALL,
                )
            }
            loadBookmarks()
            if (currentAccount(version)) notify(Toast(Msg.Res(Str.TOAST_FOLDER_DELETED, listOf(d.folder.name))))
        }
    }

    /** 토스트 하나(웹 toast) */
    fun notify(toast: Toast, onAction: (() -> Unit)? = null) {
        _notices.trySend(Notice(toast, onAction))
    }
}
