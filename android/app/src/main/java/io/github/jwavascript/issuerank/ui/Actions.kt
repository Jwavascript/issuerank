package io.github.jwavascript.issuerank.ui

import android.content.Context
import io.github.jwavascript.issuerank.AppGraph
import io.github.jwavascript.issuerank.MainViewModel
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.Share
import io.github.jwavascript.issuerank.data.View
import io.github.jwavascript.issuerank.platform.AndroidStrings
import io.github.jwavascript.issuerank.platform.findActivity

/**
 * 화면 → 동작(누름 하나 = 함수 하나). 앱 = [AppActions](ViewModel + 플랫폼 걸이), 미리보기 = [PreviewActions](아무것도 안 함).
 * 공유 · 원문 = AppGraph 걸이(ACTION_SEND · Custom Tab), Google = AppGraph.googleCredentials(화면 Activity) → ViewModel.
 */
interface ScreenActions {
    fun selectView(view: View) {}
    fun prevDay() {}
    fun nextDay() {}
    fun today() {}
    fun pickDate(date: String) {}
    fun zoom(section: SectionKey?) {}
    fun selectSection(section: SectionKey?) {}
    fun setExpanded(expanded: Boolean) {}
    fun openIssue(issueId: String) {}
    fun closeIssue() {}
    fun retry() {}

    /** 원문 · 정책 링크(Custom Tab) */
    fun openUrl(url: String) {}
    fun bookmark(issue: Issue) {}
    fun share(issue: Issue) {}
    fun shareSite() {}

    /** 머리 계정 단추 */
    fun account() {}

    /** 북마크 화면 · 토스트의 "로그인" */
    fun login() {}

    /** 640 이하 북마크 화면 "{이름} · 로그아웃" */
    fun signOut() {}

    // 로그인 창
    fun closeAuth() {}
    fun switchAuthMode() {}
    fun setAuthName(value: String) {}
    fun setAuthEmail(value: String) {}
    fun setAuthPassword(value: String) {}
    fun submitAuth() {}
    fun googleSignIn() {}

    // 폴더 고르기 · 폴더 칩 · 폴더 삭제
    fun saveToFolder(folderId: String) {}
    fun setFolderName(value: String) {}
    fun createFolder() {}
    fun closePicker() {}
    fun selectFolder(folderId: String) {}
    fun requestFolderDelete(folderId: String) {}
    fun confirmFolderDelete() {}
    fun cancelFolderDelete() {}
}

object PreviewActions : ScreenActions

/** context = 화면의 Activity(LocalContext) — Custom Tab · 공유 창을 그 위에 엶 */
class AppActions(private val vm: MainViewModel, private val context: Context) : ScreenActions {
    private val graph = AppGraph.get(context)
    private val strings = AndroidStrings(context)

    override fun selectView(view: View) = vm.selectView(view)
    override fun prevDay() = vm.prevDay()
    override fun nextDay() = vm.nextDay()
    override fun today() = vm.goToday()
    override fun pickDate(date: String) = vm.pickDate(date)
    override fun zoom(section: SectionKey?) = vm.zoom(section)
    override fun selectSection(section: SectionKey?) = vm.selectSection(section)
    override fun setExpanded(expanded: Boolean) = vm.setExpanded(expanded)
    override fun openIssue(issueId: String) = vm.openIssue(issueId)
    override fun closeIssue() = vm.closeIssue()
    override fun retry() = vm.retry()
    override fun openUrl(url: String) = graph.openUrl(context, url)
    override fun bookmark(issue: Issue) = vm.onBookmark(issue)
    override fun share(issue: Issue) = graph.share(context, Share.issue(issue, strings))
    override fun shareSite() = graph.share(context, Share.site(strings))
    override fun account() = vm.onAccount()
    override fun login() = vm.requestLogin(null)
    override fun signOut() = vm.signOut()
    override fun closeAuth() = vm.closeAuth()
    override fun switchAuthMode() = vm.switchAuthMode()
    override fun setAuthName(value: String) = vm.setAuthName(value)
    override fun setAuthEmail(value: String) = vm.setAuthEmail(value)
    override fun setAuthPassword(value: String) = vm.setAuthPassword(value)
    override fun submitAuth() = vm.submitAuth()

    /** 계정 창은 화면 Activity 위에(Credential Manager 요건) — Activity가 없으면 아무것도 안 함 */
    override fun googleSignIn() {
        val activity = context.findActivity() ?: return
        vm.googleSignIn(graph.googleCredentials(activity))
    }

    override fun saveToFolder(folderId: String) = vm.saveToFolder(folderId)
    override fun setFolderName(value: String) = vm.setFolderName(value)
    override fun createFolder() = vm.createFolder()
    override fun closePicker() = vm.closePicker()
    override fun selectFolder(folderId: String) = vm.selectFolder(folderId)
    override fun requestFolderDelete(folderId: String) = vm.requestFolderDelete(folderId)
    override fun confirmFolderDelete() = vm.confirmFolderDelete()
    override fun cancelFolderDelete() = vm.cancelFolderDelete()
}
