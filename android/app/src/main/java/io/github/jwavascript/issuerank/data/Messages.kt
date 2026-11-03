package io.github.jwavascript.issuerank.data

/** 오류 → 문구(웹 규칙 그대로). 화면은 Str/Msg를 글로 바꾸기만 한다 */
object Messages {
    /** index.html 1420~1427 authMessage(code, status, signingIn) — 이메일 가입 · 로그인 오류(창 안 글) */
    fun authMessage(code: String?, status: Int?, signingIn: Boolean): Str = when {
        signingIn && status == 401 -> Str.AUTH_ERR_CREDENTIALS
        status == 422 -> Str.AUTH_ERR_EXISTS
        code == "PASSWORD_TOO_SHORT" || code == "PASSWORD_TOO_LONG" -> Str.AUTH_ERR_PASSWORD_LENGTH
        status == 400 -> Str.AUTH_ERR_INVALID
        status == 429 -> Str.TRY_LATER
        else -> Str.AUTH_ERR_GENERIC
    }

    /** 네트워크 실패 · 모양 틀림(웹: status 없음)도 그대로 → 그 밖 문구 */
    fun auth(error: ApiError, signingIn: Boolean): Str =
        authMessage(error.code, error.status.takeIf { error.kind != ErrorKind.BAD_RESPONSE && error.kind != ErrorKind.NETWORK }, signingIn)

    /** 1495~1499 googleStartMessage(status, code) */
    fun googleStartMessage(status: Int?, code: String?): Str = when {
        status == 404 && code == "PROVIDER_NOT_FOUND" -> Str.GOOGLE_UNAVAILABLE
        status == 429 -> Str.TRY_LATER
        else -> Str.AUTH_ERR_GENERIC
    }

    /** 1597~1604 googleReturn의 error 값 → 문구 */
    fun googleReturnMessage(error: String): Str = when (error) {
        "access_denied" -> Str.GOOGLE_CANCELLED
        "account_not_linked" -> Str.GOOGLE_NOT_LINKED
        else -> Str.GOOGLE_FAILED
    }

    /**
     * 앱 Google(ID 토큰) 로그인의 서버 오류. 웹 흐름(시작 → Google → 돌아옴)의 두 규칙을 앱 순서에 맞춤:
     * 401 = 돌아옴 단계(같은 이메일 = OAUTH_LINK_ERROR "account not linked" → 웹 error 값 account_not_linked, 그 밖 토큰 거절 = 완료 못 함),
     * 그 밖(404 PROVIDER_NOT_FOUND · 429 · 네트워크 · 5xx · 403 · 400) = 시작 단계 문구.
     */
    fun google(error: ApiError): Str = when {
        error.kind == ErrorKind.UNAUTHORIZED ->
            googleReturnMessage(if (error.code == "OAUTH_LINK_ERROR") (error.message ?: "").split(" ").joinToString("_") else "")
        else -> googleStartMessage(error.status.takeIf { error.kind != ErrorKind.BAD_RESPONSE }, error.code)
    }

    /**
     * 웹 api() · loadRanking · save · unsave · createFolderAndSave · deleteFolder의 toast(e.message):
     * 서버 본문 message 그대로. 본문에 없으면 웹은 statusText(HTTP/2 = 빈 글) · 브라우저 오류 글(영어)이라 앱은 웹 문구로 —
     * 네트워크 = 오프라인 안내 제목, 그 밖 = "잠시 후 다시 시도해 주세요".
     */
    fun toast(error: ApiError): Toast = Toast(
        error.message?.let { Msg.Server(it) }
            ?: Msg.Res(if (error.kind == ErrorKind.NETWORK) Str.OFFLINE_TITLE else Str.TRY_LATER),
    )

    /** 개인 API(북마크 · 폴더) 401: 웹 api() 592~596 — 로그아웃 상태로 + "로그인이 필요합니다" · 단추 "로그인" */
    val loginRequired: Toast = Toast(Msg.Res(Str.LOGIN_REQUIRED), Str.LOGIN)

    /** 비로그인으로 북마크 단추(1537) */
    val bookmarkNeedsLogin: Toast = Toast(Msg.Res(Str.TOAST_BM_LOGIN), Str.LOGIN)

    /** 개인 API 실패 처리 하나로: 401이면 LoginRequired, 아니면 토스트 */
    fun personal(error: ApiError): PersonalFailure =
        if (error.kind == ErrorKind.UNAUTHORIZED) PersonalFailure.LoginRequired else PersonalFailure.Show(toast(error))

    /** restoreSession(1443~1447): get-session 실패 */
    val sessionFailed: Toast = Toast(Msg.Res(Str.TOAST_SESSION_FAILED))

    /** 로그아웃 뒤 토스트. 앱은 서버가 실패해도 기기 세션을 지우므로(계획 3절) 늘 "로그아웃했습니다" */
    val signedOut: Toast = Toast(Msg.Res(Str.TOAST_SIGNED_OUT))

    val signedIn: Toast = Toast(Msg.Res(Str.TOAST_SIGNED_IN))
}

sealed interface PersonalFailure {
    /** applyUser(null) + Messages.loginRequired 토스트(단추 = 로그인 창) */
    data object LoginRequired : PersonalFailure
    data class Show(val toast: Toast) : PersonalFailure
}
