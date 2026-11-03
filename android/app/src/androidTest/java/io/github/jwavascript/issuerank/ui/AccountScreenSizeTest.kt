package io.github.jwavascript.issuerank.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DarkMode
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.AuthForm
import io.github.jwavascript.issuerank.FolderDelete
import io.github.jwavascript.issuerank.FolderPicker
import io.github.jwavascript.issuerank.MainViewModel
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.Folder
import io.github.jwavascript.issuerank.data.Str
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 검증 8(화면 크기 · 접근성 — 계정 화면, 계획 5절): 폰 360dp(ForcedSize) · 어둡게 · 글자 1.3배에서 로그인 창 · 폴더 고르기 · 폴더 삭제 확인 · 북마크 화면 →
 * 가로 넘침 0 · 누를 곳 48dp 이상(이웃을 덮지 않음). 창(Dialog)은 따로 창이라 ForcedSize를 따르지 않아 내용을 화면 안에 그대로 그려 잼.
 * 로그인 창은 어둡게에서도 흰 바탕(웹 늘 밝은 색). 지금은 컴파일만(C2).
 */
@RunWith(AndroidJUnit4::class)
class AccountScreenSizeTest {
    @get:Rule(order = 0)
    val server = FakeServerRule()

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val frame = "size-frame"

    private fun phone(content: @Composable () -> Unit) {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)) then
                    DeviceConfigurationOverride.DarkMode(true) then
                    DeviceConfigurationOverride.FontScale(1.3f),
            ) {
                IssueRankTheme {
                    Box(Modifier.fillMaxSize().testTag(frame), contentAlignment = Alignment.Center) { content() }
                }
            }
        }
    }

    @Test
    fun loginDialog_signUpWithError_fitsAt360Dark13_andStaysLight() {
        phone {
            AuthDialogContent(
                AuthForm(open = true, signUp = true, name = "아주 긴 이름을 가진 시험 사용자", email = "a.very.long.address.for.overflow@example.invalid", error = Str.AUTH_ERR_INVALID),
                PreviewActions, autoFocus = false,
            )
        }
        compose.waitForTag(TestTags.AUTH_DIALOG)
        compose.assertNoHorizontalOverflowIn("로그인 창", frame)
        compose.assertTouchTargetsIn("로그인 창", frame)
        // 어둡게에서도 창 바탕 = 흰색(위 여백 칸)
        val bitmap = compose.onNodeWithTag(TestTags.AUTH_DIALOG).captureToImage().asAndroidBitmap()
        val inset = with(compose.density) { 12.dp.toPx() }.toInt()
        assertEquals(0xFFFFFFFF.toInt(), bitmap.getPixel(bitmap.width / 2, inset))
    }

    @Test
    fun loginDialog_busy_fitsAt360Dark13() {
        phone { AuthDialogContent(AuthForm(open = true, email = "busy@example.invalid", busy = true), PreviewActions, autoFocus = false) }
        compose.waitForTag(TestTags.AUTH_DIALOG)
        compose.assertNoHorizontalOverflowIn("로그인 창 보내는 중", frame)
    }

    @Test
    fun folderPickerAndDeleteDialog_fitAt360Dark13() {
        val folders = listOf(Folder("f1", "반도체·AI", 12), Folder("f2", "부동산·금리 그리고 전세 대출", 3), Folder("f3", "나중에 읽기", 0))
        phone { FolderPickerContent(FolderPicker("${server.today}-pol-21", name = "가나다라마바사아자차카타파하가나"), folders, PreviewActions) }
        compose.waitForTag(TestTags.FOLDER_PICKER)
        compose.assertNoHorizontalOverflowIn("폴더 고르기", frame)
        compose.assertTouchTargetsIn("폴더 고르기", frame)
    }

    @Test
    fun folderDeleteDialog_fitsAt360Dark13() {
        phone { FolderDeleteContent(FolderDelete(Folder("f2", "부동산·금리 그리고 전세 대출", 3)), PreviewActions) }
        compose.waitForTag(TestTags.FOLDER_DELETE_DIALOG)
        compose.assertNoHorizontalOverflowIn("폴더 삭제 확인", frame)
        compose.assertTouchTargetsIn("폴더 삭제 확인", frame)
    }

    @Test
    fun bookmarksScreen_signedIn_chipsAndCards_fitAt360Dark13() {
        val email = "size.check@example.invalid"
        val cookie = FakeAccounts.signUp("크기 시험", email)
        val long = FakeAccounts.createFolder(cookie, "부동산·금리 그리고 전세 대출")
        val other = FakeAccounts.createFolder(cookie, "반도체·AI")
        FakeAccounts.saveBookmark(cookie, "${server.today}-eco-20", long) // 아주 긴 제목 · 긴 요약
        FakeAccounts.saveBookmark(cookie, "${server.today}-pol-22", other) // 요약 없음
        FakeAccounts.saveBookmark(cookie, "${server.day(-1)}-cul-5", long)
        val graph = AppGraph.get(compose.activity)
        assertTrue(runBlocking { graph.api.signInEmail(email, FakeAccounts.PASSWORD) } is ApiResult.Ok) // 앱 세션(쿠키)으로 시작
        val vm = MainViewModel(SavedStateHandle(), graph.api, graph.clock)
        compose.runOnUiThread { vm.start("/#bookmarks") }
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(DpSize(360.dp, 800.dp)) then
                    DeviceConfigurationOverride.DarkMode(true) then
                    DeviceConfigurationOverride.FontScale(1.3f),
            ) { IssueRankApp(vm) }
        }
        compose.waitForTag(TestTags.BM_SUB)
        compose.waitForTag(TestTags.BM_USER) // 640 이하 = 북마크 화면 로그아웃
        compose.assertNoHorizontalOverflowIn("북마크 화면", TestTags.APP)
        compose.assertTouchTargetsIn("북마크 화면", TestTags.APP)
        compose.onNodeWithTag(TestTags.folderChip(long)).performClick()
        compose.waitForTag(TestTags.FOLDER_DELETE)
        compose.assertNoHorizontalOverflowIn("북마크 화면 폴더 칩", TestTags.APP)
        compose.assertTouchTargetsIn("북마크 화면 폴더 칩", TestTags.APP)
    }
}
