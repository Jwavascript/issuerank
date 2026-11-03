package io.github.jwavascript.issuerank.data

/**
 * 웹 문구 키 = res/values/strings_web.xml의 이름(소문자) 전부. 문구 글자는 리소스에만 있고 여기는 이름뿐.
 * 화면: platform/StrRes.kt의 Str.res(@StringRes) 또는 Strings(platform/AndroidStrings). 시험: XML을 읽은 Strings.
 * strings_web.xml에 새 문구를 넣으면 여기와 StrRes에도 넣음(WebStringsTest · StrResTest가 양쪽을 맞춤).
 */
enum class Str {
    APP_NAME, BRAND, CD_HOME, CD_VIEW_SWITCH, TAB_MAP,
    TAB_LIST, BOOKMARKS, LOGIN, USER_LOGOUT, RANK_TITLE_TODAY,
    RANK_TITLE_PAST, RANK_DESC_MAP, RANK_DESC_LIST, CD_PREV_DAY, CD_PICK_DATE,
    CD_NEXT_DAY, TODAY, DATE_TEXT_TODAY, DATE_FULL, DATE_SHORT,
    WEEKDAY_0, WEEKDAY_1, WEEKDAY_2, WEEKDAY_3, WEEKDAY_4,
    WEEKDAY_5, WEEKDAY_6, AM, PM, TIME_UPDATED,
    UPDATED_LINE, MAP_ALL_SECTIONS, CRUMB_SEPARATOR, MAP_SECTION_TOTAL, MAP_TOTAL,
    MAP_ZOOM_IN, MAP_ZOOM_OUT, RANK_N, COUNT_N, TILE_XS,
    CD_TILE, MAP_FOOTER, EMPTY_NO_DATA_TITLE, EMPTY_NO_DATA_BODY, EMPTY_NO_DATA_ACTION,
    CD_LIST, CD_SECTIONS, ALL, SECTION_POL, SECTION_ECO,
    SECTION_SOC, SECTION_CUL, SECTION_WOR, SECTION_IT, SUMMARY_TOGGLE,
    TOP5, LIST_FOOTER, RELATED_ARTICLES, SECTION_RANK, META_SEPARATOR,
    SAVED_AGO, SUMMARY_NONE, ORIGINAL_ARTICLE, SHARE, CD_SHARE_ISSUE,
    CD_UNBOOKMARK, AGO_NOW, AGO_MINUTES, AGO_HOURS, AGO_DAYS,
    BM_SUB, CD_FOLDERS, FOLDER_DELETE, CD_FOLDER_DELETE, LOGIN_REQUIRED,
    BM_LOGIN_BODY, BM_EMPTY_TITLE, BM_EMPTY_BODY, BM_EMPTY_ACTION, RANKING_OF_DATE,
    BM_FOOTER, SHARE_SITE, PRIVACY, TERMS, CONTACT,
    CD_ISSUE_DETAIL, CLOSE, DETAIL_RANK_LABEL, DETAIL_RANK_VALUE, DETAIL_COLLECTED,
    BOOKMARKED, FOLDER_SAVE_TITLE, FOLDER_NEW_HINT, FOLDER_CREATE, FOLDER_DELETE_TITLE,
    FOLDER_DELETE_NOTE, FOLDER_DELETE_NOTE_EMPTY, CANCEL, DELETE, SIGNUP,
    AUTH_SWITCH_TO_LOGIN, GOOGLE_CONTINUE, AUTH_OR_EMAIL, AUTH_NAME, AUTH_EMAIL,
    AUTH_PASSWORD, AUTH_ERR_NAME, AUTH_ERR_CREDENTIALS, AUTH_ERR_EXISTS, AUTH_ERR_PASSWORD_LENGTH,
    AUTH_ERR_INVALID, TRY_LATER, AUTH_ERR_GENERIC, GOOGLE_UNAVAILABLE, GOOGLE_CANCELLED,
    GOOGLE_NOT_LINKED, GOOGLE_FAILED, TOAST_BM_LOGIN, TOAST_SAVED, TOAST_VIEW_BOOKMARKS,
    TOAST_UNSAVED, TOAST_UNDO, TOAST_FOLDER_DELETED, TOAST_ISSUE_NOT_FOUND, TOAST_SESSION_FAILED,
    TOAST_SIGNED_IN, TOAST_SIGNED_OUT, TOAST_SIGN_OUT_FAILED, SHARE_ISSUE_TEXT, SHARE_SITE_TEXT,
    OFFLINE_TITLE, OFFLINE_NOTE, RETRY, WIDGET_FAIL;

    /** 리소스 이름(strings_web.xml) */
    val resName: String get() = name.lowercase()
}

/** 문구 키 → 글자. 인자가 있으면 리소스의 자리 표시(%1$s · %1$d)에 넣음(Android getString(id, args)와 같음) */
interface Strings {
    operator fun get(key: Str, vararg args: Any): String
}

/** 화면에 보일 글 하나: 문구 키(+ 인자) 또는 서버가 준 글(웹이 오류 응답의 message를 그대로 토스트하는 곳) */
sealed interface Msg {
    data class Res(val key: Str, val args: List<Any> = emptyList()) : Msg
    data class Server(val text: String) : Msg

    fun text(strings: Strings): String = when (this) {
        is Res -> strings.get(key, *args.toTypedArray())
        is Server -> text
    }
}

/** 토스트 = 글 + (있으면) 단추 글. 단추 동작은 화면이 정함(웹 toast(msg, action, fn)) */
data class Toast(val message: Msg, val action: Str? = null)
