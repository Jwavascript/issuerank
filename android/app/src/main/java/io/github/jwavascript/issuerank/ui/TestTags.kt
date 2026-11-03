package io.github.jwavascript.issuerank.ui

import io.github.jwavascript.issuerank.data.SectionKey

/**
 * 시험이 찾는 이름(Modifier.testTag). 규칙: 소문자 케밥, 같은 종류가 여럿이면 "종류:id"(id = 이슈 id · 섹션 키 · 폴더 id),
 * 종류 접두는 *_PREFIX 상수로도 둠(개수 세기). 글자 · 순서는 문구(Str)로 확인하고 tag는 찾기에만.
 * A5도 이 꼴로 여기에 더함(예: "widget-row:<n>").
 */
object TestTags {
    /** 화면 전체(IssueRankScreen 뿌리 — 넘침 시험의 기준 폭) */
    const val APP = "app"

    // 머리
    const val HEADER = "header"
    const val BRAND = "brand"
    const val TAB_MAP = "tab-map"
    const val TAB_LIST = "tab-list"
    const val TAB_BOOKMARKS = "tab-bookmarks"
    const val ACCOUNT = "account"

    // 날짜 줄(이슈맵 · 목록 공통)
    const val RANK_TITLE = "rank-title"
    const val RANK_DESC = "rank-desc"
    const val DATE_PREV = "date-prev"
    const val DATE_PICK = "date-pick"
    const val DATE_TEXT = "date-text"
    const val DATE_NEXT = "date-next"
    const val DATE_TODAY = "date-today"
    const val DATE_DIALOG = "date-dialog"
    const val DATE_DIALOG_OK = "date-dialog-ok"
    const val UPDATED = "updated"

    // 화면(탭 내용)
    const val VIEW_MAP = "view-map"
    const val VIEW_LIST = "view-list"
    const val VIEW_BOOKMARKS = "view-bookmarks"

    // 이슈맵
    const val MAP = "map"
    const val CRUMBS = "crumbs"
    const val CRUMB_ALL = "crumb-all"
    const val TILE_PREFIX = "tile:"
    fun legend(key: SectionKey) = "legend:${key.key}"
    fun mapBox(key: SectionKey) = "map-box:${key.key}"
    fun mapLabel(key: SectionKey) = "map-label:${key.key}"
    fun tile(issueId: String) = TILE_PREFIX + issueId

    // 목록 · 카드
    const val SECTION_TABS = "section-tabs"
    const val SUMMARY_TOGGLE = "summary-toggle"
    const val CARD_PREFIX = "card:"
    fun sectionTab(key: SectionKey?) = "section-tab:${key?.key ?: "all"}"
    fun group(key: SectionKey) = "group:${key.key}"
    fun card(issueId: String) = CARD_PREFIX + issueId
    fun cardTitle(issueId: String) = "card-title:$issueId"
    fun cardSummary(issueId: String) = "card-summary:$issueId"
    fun cardOriginal(issueId: String) = "card-original:$issueId"
    fun cardShare(issueId: String) = "card-share:$issueId"
    fun cardBookmark(issueId: String) = "card-bookmark:$issueId"

    // 상세(ModalBottomSheet)
    const val DETAIL = "detail"
    const val DETAIL_META = "detail-meta"
    const val DETAIL_TITLE = "detail-title"
    const val DETAIL_SUMMARY = "detail-summary"
    const val DETAIL_COUNT = "detail-count"
    const val DETAIL_RANK = "detail-rank"
    const val DETAIL_COLLECTED = "detail-collected"
    const val DETAIL_ORIGINAL = "detail-original"
    const val DETAIL_BOOKMARK = "detail-bookmark"
    const val DETAIL_SHARE = "detail-share"
    const val DETAIL_CLOSE = "detail-close"

    // 빈 화면 · 오프라인 · 토스트 · 맨 아래 · 북마크 비로그인
    const val EMPTY_NO_DATA = "empty-no-data"
    const val EMPTY_TODAY = "empty-today"
    const val OFFLINE = "offline"
    const val OFFLINE_RETRY = "offline-retry"
    const val TOAST = "toast"
    const val TOAST_ACTION = "toast-action"
    const val SITE_FOOTER = "site-footer"
    const val SHARE_SITE = "share-site"
    const val FOOTER_PRIVACY = "footer-privacy"
    const val FOOTER_TERMS = "footer-terms"
    const val BOOKMARKS_LOGIN = "bookmarks-login"

    // 북마크 화면(A4): 640 이하 로그아웃 · 개수 줄 · 폴더 칩(id = 폴더 id 또는 all) · 폴더 삭제 · 날짜 묶음(순서 번호 — 같은 날짜가 떨어져 다시 나올 수 있음) · 빈 화면
    const val BM_USER = "bm-user"
    const val BM_SUB = "bm-sub"
    const val FOLDER_CHIPS = "folder-chips"
    const val FOLDER_CHIP_PREFIX = "folder-chip:"
    const val FOLDER_DELETE = "folder-delete"
    const val BM_GROUP_PREFIX = "bm-group:"
    const val BM_EMPTY = "bm-empty"
    const val BM_EMPTY_ACTION = "bm-empty-action"
    fun folderChip(id: String) = FOLDER_CHIP_PREFIX + id
    fun bookmarkGroup(index: Int) = BM_GROUP_PREFIX + index
    fun bookmarkMeta(issueId: String) = "bm-meta:$issueId"

    // 로그인 창(A4 — 늘 밝은 색)
    const val AUTH_DIALOG = "auth-dialog"
    const val AUTH_TITLE = "auth-title"
    const val AUTH_GOOGLE = "auth-google"
    const val AUTH_NAME = "auth-name"
    const val AUTH_EMAIL = "auth-email"
    const val AUTH_PASSWORD = "auth-password"
    const val AUTH_ERROR = "auth-error"
    const val AUTH_SUBMIT = "auth-submit"
    const val AUTH_SWITCH = "auth-switch"
    const val AUTH_CLOSE = "auth-close"
    const val AUTH_TERMS = "auth-terms"
    const val AUTH_PRIVACY = "auth-privacy"

    // 폴더 고르기 · 폴더 삭제 확인(A4)
    const val FOLDER_PICKER = "folder-picker"
    const val FOLDER_OPTION_PREFIX = "folder-option:"
    const val FOLDER_NEW_NAME = "folder-new-name"
    const val FOLDER_CREATE = "folder-create"
    const val FOLDER_DELETE_DIALOG = "folder-delete-dialog"
    const val FOLDER_DELETE_TITLE = "folder-delete-title"
    const val FOLDER_DELETE_NOTE = "folder-delete-note"
    const val FOLDER_DELETE_CANCEL = "folder-delete-cancel"
    const val FOLDER_DELETE_CONFIRM = "folder-delete-confirm"
    fun folderOption(id: String) = FOLDER_OPTION_PREFIX + id
}
