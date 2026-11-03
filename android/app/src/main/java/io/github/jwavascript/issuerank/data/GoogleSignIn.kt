package io.github.jwavascript.issuerank.data

import java.security.SecureRandom
import java.util.Base64

/** Credential Manager가 준 결과(계획 1절: 호출을 감싼 인터페이스 — 시험에서 가짜로). 토큰은 toString에 남기지 않음 */
sealed interface GoogleCredential {
    class Token(val idToken: String) : GoogleCredential {
        override fun toString(): String = "Token(<redacted>)"
    }

    /** 사용자가 계정 창을 닫음 */
    data object Cancelled : GoogleCredential

    /** 그 밖(계정 없음 · Play 서비스 · 설정 오류 등) */
    data object Failed : GoogleCredential

    /** 웹 클라이언트 ID가 아직 자리표시자(C3 — GoogleClientId)라 Credential Manager를 부르지 않음 = 웹 PROVIDER_NOT_FOUND와 같은 뜻 */
    data object Unavailable : GoogleCredential
}

/** C3: res/values/google.xml 웹 클라이언트 ID가 자리표시자(REPLACE_WITH_…)인지 — 실제 ID가 들어오면 그대로 정상 흐름 */
object GoogleClientId {
    const val PLACEHOLDER_PREFIX = "REPLACE_WITH_"

    fun isPlaceholder(id: String): Boolean = id.startsWith(PLACEHOLDER_PREFIX)
}

/** platform 쪽 구현: GetSignInWithGoogleOption(serverClientId = 웹 클라이언트 ID, nonce) */
fun interface GoogleCredentials {
    suspend fun idToken(nonce: String): GoogleCredential
}

sealed interface GoogleSignInResult {
    /** 세션 쿠키 저장됨 → 토스트 Messages.signedIn("로그인했습니다") */
    data class SignedIn(val user: AuthUser) : GoogleSignInResult
    /** 로그인 창 오류 글 */
    data class Error(val message: Str) : GoogleSignInResult
}

/**
 * "Google로 계속하기"(계획 2절 · D35): nonce → Credential Manager → POST sign-in/social(idToken + 같은 nonce).
 * 취소 = "Google 로그인을 취소했습니다", 같은 이메일 = "이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요", 그 밖 = Messages.google,
 * 자리표시자(Unavailable) = "지금은 Google 로그인을 사용할 수 없습니다"(요청 0).
 * 토큰 · nonce는 저장하지 않음.
 */
suspend fun signInWithGoogle(
    credentials: GoogleCredentials,
    api: IssueRankApi,
    nonce: () -> String = Nonce::create,
): GoogleSignInResult {
    val n = nonce()
    return when (val c = credentials.idToken(n)) {
        GoogleCredential.Cancelled -> GoogleSignInResult.Error(Messages.googleReturnMessage("access_denied"))
        GoogleCredential.Failed -> GoogleSignInResult.Error(Messages.googleReturnMessage(""))
        GoogleCredential.Unavailable -> GoogleSignInResult.Error(Messages.googleStartMessage(404, "PROVIDER_NOT_FOUND"))
        is GoogleCredential.Token -> when (val r = api.signInGoogle(c.idToken, n)) {
            is ApiResult.Ok -> GoogleSignInResult.SignedIn(r.value)
            is ApiResult.Fail -> GoogleSignInResult.Error(Messages.google(r.error))
        }
    }
}

object Nonce {
    private val random = SecureRandom()

    /** 32바이트 무작위 → base64url(패딩 없음, 43자) */
    fun create(): String = ByteArray(32).also(random::nextBytes).let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }
}
