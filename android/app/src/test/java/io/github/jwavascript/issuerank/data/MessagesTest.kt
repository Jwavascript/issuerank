package io.github.jwavascript.issuerank.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 오류 → 문구 키: 웹 authMessage · googleStartMessage · googleReturn을 vm으로 돌린 글(web/text.json)과 리소스 글이 같음 */
class MessagesTest {
    private val s = XmlStrings
    private fun text(o: org.json.JSONObject, key: String): String? = o.text(key)
    private fun status(o: org.json.JSONObject): Int? = if (o.isNull("status")) null else o.getInt("status")

    @Test
    fun authMessageEqualsWeb() {
        val cases = TestFiles.webCases("text.json", "authMessage")
        assertEquals(9 * 9 * 2, cases.size)
        for (c in cases) {
            val got = s[Messages.authMessage(text(c, "code"), status(c), c.getBoolean("signingIn"))]
            assertEquals(c.toString(), c.getString("text"), got)
        }
    }

    @Test
    fun googleStartMessageEqualsWeb() {
        for (c in TestFiles.webCases("text.json", "googleStartMessage")) {
            assertEquals(c.toString(), c.getString("text"), s[Messages.googleStartMessage(status(c), text(c, "code"))])
        }
    }

    @Test
    fun googleReturnMessageEqualsWeb() {
        val cases = TestFiles.webCases("text.json", "googleReturn")
        assertEquals(6, cases.size)
        for (c in cases) assertEquals(c.toString(), c.getString("text"), s[Messages.googleReturnMessage(c.getString("error"))])
        assertEquals(TestFiles.webCases("text.json", "googleReturnSignedIn").single().getString("toast"), Messages.signedIn.message.text(s))
    }

    @Test
    fun authFromApiErrors() {
        assertEquals("이메일 또는 비밀번호를 확인해 주세요", s[Messages.auth(ApiError(ErrorKind.UNAUTHORIZED, 401, "INVALID_EMAIL_OR_PASSWORD"), signingIn = true)])
        assertEquals("인증 요청을 처리하지 못했습니다. 다시 시도해 주세요", s[Messages.auth(ApiError(ErrorKind.UNAUTHORIZED, 401), signingIn = false)])
        assertEquals("이미 가입된 이메일입니다", s[Messages.auth(ApiError(ErrorKind.UNPROCESSABLE, 422, "USER_ALREADY_EXISTS_USE_ANOTHER_EMAIL"), false)])
        assertEquals("비밀번호는 8~128자로 입력해 주세요", s[Messages.auth(ApiError(ErrorKind.BAD_REQUEST, 400, "PASSWORD_TOO_SHORT"), false)])
        assertEquals("이름, 이메일과 비밀번호를 확인해 주세요", s[Messages.auth(ApiError(ErrorKind.BAD_REQUEST, 400, "INVALID_EMAIL"), false)])
        assertEquals("잠시 후 다시 시도해 주세요", s[Messages.auth(ApiError(ErrorKind.TOO_MANY_REQUESTS, 429), true)])
        // 네트워크 · 200인데 user 없음(웹 throw new Error() — status 없음) · 403 Origin
        for (e in listOf(ApiError(ErrorKind.NETWORK), ApiError(ErrorKind.BAD_RESPONSE, 200), ApiError(ErrorKind.FORBIDDEN, 403, "INVALID_ORIGIN"))) {
            assertEquals("$e", "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요", s[Messages.auth(e, true)])
        }
    }

    /** 앱 Google(ID 토큰): 서버 응답 꼴(fake-server README 4절 · native-client.test ⑤) → 웹 문구 */
    @Test
    fun googleIdTokenErrors() {
        val linked = TestFiles.webCases("text.json", "googleReturn").first { it.getString("error") == "account_not_linked" }.getString("text")
        val failed = TestFiles.webCases("text.json", "googleReturn").first { it.getString("error") == "state_mismatch" }.getString("text")
        val cases = listOf(
            ApiError(ErrorKind.UNAUTHORIZED, 401, "OAUTH_LINK_ERROR", "account not linked") to linked,
            ApiError(ErrorKind.UNAUTHORIZED, 401, "OAUTH_LINK_ERROR", "unable to create user") to failed,
            ApiError(ErrorKind.UNAUTHORIZED, 401, "INVALID_TOKEN", "Invalid token") to failed,
            ApiError(ErrorKind.UNAUTHORIZED, 401, "USER_EMAIL_NOT_FOUND") to failed,
            ApiError(ErrorKind.NOT_FOUND, 404, "PROVIDER_NOT_FOUND") to "지금은 Google 로그인을 사용할 수 없습니다",
            ApiError(ErrorKind.NOT_FOUND, 404) to "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요",
            ApiError(ErrorKind.TOO_MANY_REQUESTS, 429) to "잠시 후 다시 시도해 주세요",
            ApiError(ErrorKind.FORBIDDEN, 403, "INVALID_ORIGIN") to "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요",
            ApiError(ErrorKind.NETWORK) to "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요",
            ApiError(ErrorKind.SERVER, 500) to "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요",
            ApiError(ErrorKind.BAD_RESPONSE, 200) to "인증 요청을 처리하지 못했습니다. 다시 시도해 주세요",
        )
        for ((e, expected) in cases) assertEquals("$e", expected, s[Messages.google(e)])
        assertEquals("이미 가입된 이메일입니다. 처음 가입한 방법으로 로그인해 주세요", linked)
    }

    @Test
    fun apiToastsUseServerMessageElseWebStrings() {
        val server = Messages.toast(ApiError(ErrorKind.CONFLICT, 409, null, "이미 북마크한 이슈입니다"))
        assertEquals(Toast(Msg.Server("이미 북마크한 이슈입니다")), server)
        assertEquals("폴더 이름은 1~16자로 입력해 주세요", Messages.toast(ApiError(ErrorKind.BAD_REQUEST, 400, null, "폴더 이름은 1~16자로 입력해 주세요")).message.text(s))
        assertEquals("인터넷에 연결되어 있지 않습니다", Messages.toast(ApiError(ErrorKind.NETWORK)).message.text(s))
        assertEquals("잠시 후 다시 시도해 주세요", Messages.toast(ApiError(ErrorKind.SERVER, 500)).message.text(s))
        assertEquals("잠시 후 다시 시도해 주세요", Messages.toast(ApiError(ErrorKind.BAD_RESPONSE, 200)).message.text(s))
        assertEquals(null, server.action)
    }

    @Test
    fun personalApiLoginRequired() {
        assertEquals(PersonalFailure.LoginRequired, Messages.personal(ApiError(ErrorKind.UNAUTHORIZED, 401, null, "로그인이 필요합니다")))
        val shown = Messages.personal(ApiError(ErrorKind.NOT_FOUND, 404, null, "폴더를 찾을 수 없습니다"))
        assertTrue(shown is PersonalFailure.Show)
        assertEquals("폴더를 찾을 수 없습니다", (shown as PersonalFailure.Show).toast.message.text(s))
        assertEquals("로그인이 필요합니다" to "로그인", Messages.loginRequired.message.text(s) to s[Messages.loginRequired.action!!])
        assertEquals("북마크하려면 로그인이 필요합니다" to "로그인", Messages.bookmarkNeedsLogin.message.text(s) to s[Messages.bookmarkNeedsLogin.action!!])
        assertEquals("로그인 상태를 확인하지 못했습니다", Messages.sessionFailed.message.text(s))
        assertEquals("로그아웃했습니다", Messages.signedOut.message.text(s))
    }

    @Test
    fun formattedToasts() {
        assertEquals("‘반도체·AI’에 저장했습니다", Msg.Res(Str.TOAST_SAVED, listOf("반도체·AI")).text(s))
        assertEquals("‘나중에 읽기’ 폴더를 삭제했습니다", Msg.Res(Str.TOAST_FOLDER_DELETED, listOf("나중에 읽기")).text(s))
        assertEquals("앱 사용자 · 로그아웃", s[Str.USER_LOGOUT, "앱 사용자"])
        assertEquals("저장한 기사 3개 · 최근 저장 순", s[Str.BM_SUB, 3])
    }
}
