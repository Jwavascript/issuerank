package io.github.jwavascript.issuerank.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.jwavascript.issuerank.AuthForm
import io.github.jwavascript.issuerank.Content
import io.github.jwavascript.issuerank.FolderDelete
import io.github.jwavascript.issuerank.FolderPicker
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Str
import io.github.jwavascript.issuerank.data.View

/**
 * 화면 미리보기 묶음(마지막 검증이 `android.exe preview render`로 PNG — 기기 없이): 폰 360 · 태블릿 세로 753 · 가로 1205 × 밝게/어둡게 + 360 글자 1.3배.
 * 데이터 = PreviewData(가짜 서버 씨앗 D0). 미리보기 도구(ui-tooling)는 debug에만.
 */
@Preview(name = "phone-360-light", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "phone-360-dark", widthDp = 360, heightDp = 800, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "tablet-753-light", widthDp = 753, heightDp = 1205, showBackground = true)
@Preview(name = "tablet-753-dark", widthDp = 753, heightDp = 1205, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "tablet-1205-light", widthDp = 1205, heightDp = 753, showBackground = true)
@Preview(name = "tablet-1205-dark", widthDp = 1205, heightDp = 753, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(name = "phone-360-font-1.3", widthDp = 360, heightDp = 800, showBackground = true, fontScale = 1.3f)
annotation class ScreenPreviews

@Composable
private fun Screen(state: UiState) {
    CompositionLocalProvider(LocalClock provides PreviewData.clock) {
        IssueRankTheme { IssueRankScreen(state, PreviewActions) }
    }
}

/** 첫 화면: 이슈맵(오늘 · 30개 · 640 이하 세로 쌓기) */
@ScreenPreviews
@Composable
fun MapScreenPreview() = Screen(PreviewData.state(View.MAP))

/** 이슈맵 확대(경제) + 고른 타일 테두리 */
@ScreenPreviews
@Composable
fun MapZoomPreview() = Screen(PreviewData.state(View.MAP, zoom = SectionKey.ECO, selectedId = "${PreviewData.TODAY}-eco-1"))

/** 랭킹 목록(전체 · 요약 펼침 — 요약 없음 카드 · 아주 긴 제목 포함) */
@ScreenPreviews
@Composable
fun ListScreenPreview() = Screen(PreviewData.state(View.LIST))

/** 랭킹 목록 요약 접음(.compact) */
@ScreenPreviews
@Composable
fun ListCompactPreview() = Screen(PreviewData.state(View.LIST, expanded = false))

/** 지난 날: "11월 2일 (월)의 주요 이슈" · "오늘" 단추 · 다음 날짜 열림 */
@ScreenPreviews
@Composable
fun PastDayPreview() = Screen(PreviewData.state(View.MAP, ranking = PreviewData.yesterday))

/** 없는 날: "해당 날짜의 랭킹 데이터가 없습니다" + "오늘 랭킹 보기" */
@ScreenPreviews
@Composable
fun NoDataPreview() = Screen(PreviewData.state(View.LIST, ranking = null, content = Content.NO_DATA))

/** 오프라인: offline.html 문구 + "다시 시도" */
@ScreenPreviews
@Composable
fun OfflinePreview() = Screen(PreviewData.state(View.MAP, ranking = null, content = Content.OFFLINE))

/** 북마크 탭 비로그인: "로그인이 필요합니다" + "로그인" */
@ScreenPreviews
@Composable
fun BookmarksPreview() = Screen(PreviewData.state(View.BOOKMARKS))

/** 북마크 화면 로그인: (640 이하) "{이름} · 로그아웃" · "저장한 기사 3개 · 최근 저장 순" · 칩 전체 · 날짜 묶음 2개(오늘 2 · 어제 1) · 카드 meta */
@ScreenPreviews
@Composable
fun BookmarksSignedInPreview() = Screen(PreviewData.signedIn())

/** 폴더 칩을 고름(16자 이름): 그 폴더 카드만 · 칩 줄 끝 "폴더 삭제" */
@ScreenPreviews
@Composable
fun BookmarksFolderChipPreview() = Screen(PreviewData.signedIn(folderFilter = "f2"))

/** 빈 화면(로그인 · 북마크 0 · 폴더 0): "북마크한 기사가 없습니다" + "이슈맵 보러 가기" */
@ScreenPreviews
@Composable
fun BookmarksEmptyPreview() = Screen(PreviewData.signedIn().copy(bookmarks = emptyList(), folders = emptyList()))

/** 로그인 창(로그인 모드 — 이메일이 남아 있음) */
@ScreenPreviews
@Composable
fun AuthSignInPreview() = DialogFrame { AuthDialogContent(AuthForm(open = true, email = PreviewData.user.email), PreviewActions, autoFocus = false) }

/** 로그인 창(회원가입 모드 — 이름 칸) */
@ScreenPreviews
@Composable
fun AuthSignUpPreview() = DialogFrame {
    AuthDialogContent(AuthForm(open = true, signUp = true, name = PreviewData.user.name, email = PreviewData.user.email), PreviewActions, autoFocus = false)
}

/** 로그인 창 오류 줄("이메일 또는 비밀번호를 확인해 주세요" — 비밀번호는 비워짐) */
@ScreenPreviews
@Composable
fun AuthErrorPreview() = DialogFrame {
    AuthDialogContent(AuthForm(open = true, email = PreviewData.user.email, error = Str.AUTH_ERR_CREDENTIALS), PreviewActions, autoFocus = false)
}

/** 로그인 창 보내는 중(단추 막힘 · Google 단추 흐림) */
@ScreenPreviews
@Composable
fun AuthBusyPreview() = DialogFrame {
    AuthDialogContent(AuthForm(open = true, email = PreviewData.user.email, password = "test-only-pw", busy = true), PreviewActions, autoFocus = false)
}

/** 폴더 고르기 — 폴더 0(새 폴더 이름 + 만들기만) */
@ScreenPreviews
@Composable
fun FolderPickerEmptyPreview() = PickerFrame { FolderPickerContent(FolderPicker("${PreviewData.TODAY}-pol-21"), emptyList(), PreviewActions) }

/** 폴더 고르기 — 폴더 여럿(서버 순서 · 개수 · 16자 이름) */
@ScreenPreviews
@Composable
fun FolderPickerPreview() = PickerFrame { FolderPickerContent(FolderPicker("${PreviewData.TODAY}-pol-21"), PreviewData.folders, PreviewActions) }

/** 폴더 고르기 — 입력 오류: 16자에서 더 안 들어감(웹 maxlength) + 만들기 실패 토스트(서버 400 글) — 창은 그대로 */
@ScreenPreviews
@Composable
fun FolderPickerInputErrorPreview() = PickerFrame(toast = PreviewData.FOLDER_NAME_ERROR) {
    FolderPickerContent(FolderPicker("${PreviewData.TODAY}-pol-21", name = "가나다라마바사아자차카타파하가나"), PreviewData.folders, PreviewActions)
}

/** 폴더 삭제 확인(함께 지워질 북마크 2개) */
@ScreenPreviews
@Composable
fun FolderDeletePreview() = DialogFrame { FolderDeleteContent(FolderDelete(PreviewData.folders[0]), PreviewActions) }

/** 밝은 창 미리보기 틀: 화면 bg + 창 뒤 어둡게(웹 ::backdrop 35%) — 창 자체(따로 창)는 그리지 않고 내용만 */
@Composable
private fun DialogFrame(content: @Composable () -> Unit) {
    IssueRankTheme {
        Box(
            Modifier.fillMaxSize().background(WebTheme.colors.bg).background(Color(LightDialog.BACKDROP_ARGB)),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}

/** 폴더 고르기 틀(뒤 어둡게 없음 — 웹 팝오버) + (있으면) 아래 토스트(웹 .toast 꼴) */
@Composable
private fun PickerFrame(toast: String? = null, content: @Composable () -> Unit) {
    IssueRankTheme {
        val c = WebTheme.colors
        Box(Modifier.fillMaxSize().background(c.bg), contentAlignment = Alignment.Center) {
            content()
            if (toast != null) {
                Box(
                    Modifier.align(Alignment.BottomCenter).padding(start = 16.dp, end = 16.dp, bottom = 28.dp)
                        .clip(RoundedCornerShape(percent = 50)).background(c.ink).padding(horizontal = 16.dp, vertical = 10.dp),
                ) { Text(toast, style = WebType.toast, color = c.inkText) }
            }
        }
    }
}

/** 상세 내용(아래 창 안 — 창 자체는 미리보기에서 그리지 않음): 정치 1위 */
@ScreenPreviews
@Composable
fun DetailPreview() = DetailFrame("${PreviewData.TODAY}-pol-21")

/** 상세: 요약 없음(정치 2위) */
@ScreenPreviews
@Composable
fun DetailNoSummaryPreview() = DetailFrame("${PreviewData.TODAY}-pol-22")

/** 상세: 대체 섹션(생활·문화 = 어제 카드 → '수집'은 날짜만) */
@ScreenPreviews
@Composable
fun DetailFallbackPreview() = DetailFrame("${PreviewData.YESTERDAY}-cul-5")

@Composable
private fun DetailFrame(issueId: String) {
    IssueRankTheme {
        val c = WebTheme.colors
        Box(Modifier.fillMaxSize().background(ScrimColor), contentAlignment = Alignment.BottomCenter) {
            Box(Modifier.widthIn(max = 640.dp).fillMaxWidth().clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)).background(c.surface)) {
                val ranking = PreviewData.ranking
                IssueDetailContent(ranking.issueById(issueId)!!, ranking, bookmarked = false, actions = PreviewActions, onClose = {})
            }
        }
    }
}
