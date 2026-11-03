package io.github.jwavascript.issuerank

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.jwavascript.issuerank.DebugWidgetStates.State
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.WidgetPayload
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView
import io.github.jwavascript.issuerank.ui.IssueRankTheme
import io.github.jwavascript.issuerank.ui.WebTheme
import io.github.jwavascript.issuerank.ui.WebType
import io.github.jwavascript.issuerank.widget.WidgetSizes
import io.github.jwavascript.issuerank.widget.WidgetTargets
import io.github.jwavascript.issuerank.widget.WidgetViews
import kotlinx.coroutines.launch

/**
 * debug 전용 도구(계획 1 · 4절 · 검증 11 · 12 — debug APK에만, release 0):
 * - 서버 주소: 가짜 서버 · 운영 · 기본 + 직접 입력 → DebugServer prefs(AppGraph.serverUrl이 요청마다 읽음 — 앱 · 위젯에 바로 적용).
 * - 위젯 미리보기: 위젯과 같은 WidgetViews.forRows → RemoteViews.apply를 정한 높이 틀(276dp × h) 안에. 상태(고정 ok30 · ok3 · cache · fail · live) ·
 *   높이(66 · 117 · 150 · 220 · 300 + 직접) · 밝게/어둡게 · 글자 배율 = 설정 문맥을 덮어 그림(위젯 색 = 같은 @color).
 * adb `am start -n <패키지>/io.github.jwavascript.issuerank.DebugToolsActivity` + Extra(DebugExtras 허용 목록)로 화면 손대지 않고 돎.
 */
class DebugToolsActivity : ComponentActivity() {
    private val model: DebugToolsModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph = AppGraph.get(applicationContext)
        // 새로 열 때만 Extra를 따름(돌리기 = 모델이 남음 — click이 다시 눌리지 않게)
        if (savedInstanceState == null) model.take(DebugExtras.parse(intent.typedExtras()), this, graph) else model.refreshServer(graph)
        setContent { IssueRankTheme { DebugToolsScreen(model, graph) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        model.take(DebugExtras.parse(intent.typedExtras()), this, AppGraph.get(applicationContext))
    }
}

/** Extra → 이름마다 그 이름의 형으로 꺼낸 값. 형이 다르면 null(기본값 둘로 꺼내 보아 다르면 = 그 형이 아님) */
private fun Intent.typedExtras(): Map<String, Any?> {
    val b = extras ?: return emptyMap()
    return b.keySet().associateWith { key ->
        when (key) {
            DebugExtras.STATE, DebugExtras.SERVER -> b.getString(key)
            DebugExtras.HEIGHT, DebugExtras.CLICK -> b.getInt(key, 0).takeIf { it == b.getInt(key, 1) }
            DebugExtras.DARK -> b.getBoolean(key, false).takeIf { it == b.getBoolean(key, true) }
            DebugExtras.SCALE -> b.getFloat(key, 0f).takeIf { it == b.getFloat(key, 1f) }
            else -> null
        }
    }
}

/** debug 서버 주소 쓰기. null = 덮어쓰기 지움(기본 = BuildConfig.SERVER_URL = 가짜 서버) */
object DebugServers {
    fun save(context: Context, url: String?) {
        val editor = context.getSharedPreferences(DebugServer.PREFS, Context.MODE_PRIVATE).edit()
        if (url == null) editor.remove(DebugServer.KEY_SERVER_URL) else editor.putString(DebugServer.KEY_SERVER_URL, url)
        editor.apply()
    }
}

/** 계측 시험이 찾는 이름(debug 화면만 — 제품 TestTags와 따로) */
object DebugTags {
    const val SERVER_NOW = "debug-server-now"
    const val ROWS = "debug-rows"
    const val LIVE = "debug-live"
    const val CLICKED = "debug-clicked"
    const val IGNORED = "debug-ignored"
}

/** 화면 상태(돌려도 남음). click은 snapshot 상태라 미리보기가 그린 뒤 한 번 보냄 */
class DebugToolsModel : ViewModel() {
    var state by mutableStateOf(State.OK30)
        private set
    var heightDp by mutableIntStateOf(WidgetSizes.DEFAULT_HEIGHT_DP)
    var dark by mutableStateOf<Boolean?>(null)
    var scale by mutableStateOf<Float?>(null)
    var server by mutableStateOf("")
        private set
    var ignored by mutableStateOf(emptyList<IgnoredExtra>())
        private set
    var clickRequest by mutableStateOf<Int?>(null)
        private set
    var clicked by mutableStateOf<Pair<Int, String>?>(null)
        private set
    var live by mutableStateOf<ApiResult<WidgetPayload>?>(null)
        private set
    var liveLoading by mutableStateOf(false)
        private set

    /** adb Extra 한 번: 없는 칸 = 기본값 · server = 저장 · click = 다음 그림 뒤 한 번 */
    fun take(request: DebugRequest, context: Context, graph: AppGraph) {
        request.server?.let { DebugServers.save(context, it.url) }
        refreshServer(graph)
        heightDp = request.heightDp ?: WidgetSizes.DEFAULT_HEIGHT_DP
        dark = request.dark
        scale = request.scale
        ignored = request.ignored
        clicked = null
        clickRequest = request.click
        select(request.state ?: State.OK30, graph)
    }

    fun refreshServer(graph: AppGraph) {
        server = graph.serverUrl()
    }

    fun saveServer(context: Context, graph: AppGraph, url: String?) {
        DebugServers.save(context, url)
        refreshServer(graph)
    }

    fun select(next: State, graph: AppGraph) {
        state = next
        if (next == State.LIVE) fetchLive(graph)
    }

    /** live = 지금 서버 GET /api/widget 한 번(위젯과 같은 AppGraph.api — 위젯 캐시 · 다음 시각은 건드리지 않음) */
    fun fetchLive(graph: AppGraph) {
        liveLoading = true
        viewModelScope.launch {
            live = graph.api.widget()
            liveLoading = false
        }
    }

    /** 미리보기가 [rows]줄 한 벌을 그린 뒤: n번째 줄이 보이는 줄이면(위젯과 같은 WidgetTargets) 그 줄을 실제로 누름(RemoteViews가 단 PendingIntent를 보냄), 아니면 "무시함" */
    fun click(n: Int, view: WidgetView, rows: Int, shown: View) {
        if (clickRequest != n) return
        clickRequest = null
        val target = WidgetTargets.of(view, rows).firstOrNull { it.slot == n - 1 }
        if (target != null) {
            shown.findViewById<View>(WidgetViews.ROW_IDS[n - 1]).performClick()
            clicked = n to target.path
        } else {
            ignored = ignored + IgnoredExtra(DebugExtras.CLICK, IgnoredExtra.Reason.OUT_OF_RANGE)
        }
    }
}

private const val PREVIEW_WIDTH_DP = 276
/** 미리보기의 위젯 id(AppWidgetManager.INVALID_APPWIDGET_ID = 0 — 실제 위젯 id는 1부터라 PendingIntent가 섞이지 않음) */
private const val PREVIEW_WIDGET_ID = 0
private val HEIGHTS = listOf(66, 117, 150, 220, 300)
private val SCALES = listOf(null, 0.85f, 1f, 1.15f, 1.3f, 2f)

@Composable
private fun DebugToolsScreen(model: DebugToolsModel, graph: AppGraph) {
    val c = WebTheme.colors
    Column(
        Modifier.fillMaxSize().background(c.bg).verticalScroll(rememberScrollState()).safeDrawingPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.debug_tools_title), style = WebType.h1Narrow, color = c.text)
        ServerSection(model, graph)
        WidgetSection(model, graph)
    }
}

@Composable
private fun Heading(text: String) = Text(text, style = WebType.emptyTitle, color = WebTheme.colors.text)

@Composable
private fun Note(text: String, modifier: Modifier = Modifier) = Text(text, modifier, style = WebType.label13, color = WebTheme.colors.muted)

/** 고르기 단추 줄(고른 것 = 채운 단추) */
@Composable
private fun <T> Choices(options: List<T>, selected: T, label: @Composable (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (option in options) {
            val text = label(option)
            if (option == selected) Button(onClick = { onPick(option) }) { Text(text) } else OutlinedButton(onClick = { onPick(option) }) { Text(text) }
        }
    }
}

@Composable
private fun ServerSection(model: DebugToolsModel, graph: AppGraph) {
    val context = LocalContext.current
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    Heading(stringResource(R.string.debug_server_title))
    Note(stringResource(R.string.debug_server_now, model.server), Modifier.testTag(DebugTags.SERVER_NOW))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (choice in DebugServerChoice.entries) {
            val label = when (choice) {
                DebugServerChoice.FAKE -> R.string.debug_server_fake
                DebugServerChoice.PROD -> R.string.debug_server_prod
                DebugServerChoice.DEFAULT -> R.string.debug_server_default
            }
            OutlinedButton(onClick = { model.saveServer(context, graph, choice.url) }) { Text(stringResource(label)) }
        }
    }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it; invalid = false },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.debug_server_custom)) },
        isError = invalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
    )
    Button(onClick = { DebugExtras.customServer(input)?.let { model.saveServer(context, graph, it) } ?: run { invalid = true } }) {
        Text(stringResource(R.string.debug_apply))
    }
    if (invalid) Text(stringResource(R.string.debug_server_invalid), style = WebType.label13, color = MaterialTheme.colorScheme.error)
}

@Composable
private fun WidgetSection(model: DebugToolsModel, graph: AppGraph) {
    val deviceDark = isSystemInDarkTheme()
    val deviceScale = LocalDensity.current.fontScale
    val dark = model.dark ?: deviceDark
    val scale = model.scale ?: deviceScale
    val today = remember { KstDate.today(graph.clock) }
    val view = if (model.state == State.LIVE) model.live?.let { WidgetRules.resolve(it, null).view } else DebugWidgetStates.view(model.state, today)
    val themeLight = stringResource(R.string.debug_theme_light)
    val themeDark = stringResource(R.string.debug_theme_dark)

    Heading(stringResource(R.string.debug_widget_title))
    Note(stringResource(R.string.debug_state))
    Choices(State.entries, model.state, { it.key }) { model.select(it, graph) }
    Note(stringResource(R.string.debug_height))
    Choices(HEIGHTS, model.heightDp, { it.toString() }) { model.heightDp = it }
    CustomHeight(model)
    Note(stringResource(R.string.debug_theme))
    Choices(listOf(null, false, true), model.dark, {
        when (it) { null -> stringResource(R.string.debug_theme_device); false -> themeLight; true -> themeDark }
    }) { model.dark = it }
    Note(stringResource(R.string.debug_scale))
    Choices(SCALES, model.scale, { it?.toString() ?: stringResource(R.string.debug_scale_device, deviceScale.toString()) }) { model.scale = it }

    if (model.state == State.LIVE) {
        val status = when (val r = model.live) {
            null -> stringResource(R.string.debug_live_loading)
            is ApiResult.Ok -> if (model.liveLoading) stringResource(R.string.debug_live_loading) else stringResource(R.string.debug_live_ok, r.value.widget.items.size)
            is ApiResult.Fail -> if (model.liveLoading) stringResource(R.string.debug_live_loading) else stringResource(R.string.debug_live_fail, r.error.kind.name)
        }
        Note(status, Modifier.testTag(DebugTags.LIVE))
        OutlinedButton(onClick = { model.fetchLive(graph) }) { Text(stringResource(R.string.debug_live_again)) }
    }

    val rows = WidgetRules.rows(model.heightDp.toDouble(), scale.toDouble())
    val shown = view?.widget?.let { minOf(rows, it.items.size) } ?: 0
    Note(
        stringResource(R.string.debug_rows, shown, rows, PREVIEW_WIDTH_DP, model.heightDp, scale.toString(), if (dark) themeDark else themeLight),
        Modifier.testTag(DebugTags.ROWS),
    )
    if (view != null) WidgetFrame(view, model.heightDp, dark, scale, model)
    model.clicked?.let { (n, path) -> Note(stringResource(R.string.debug_clicked, n, path), Modifier.testTag(DebugTags.CLICKED)) }
    if (model.ignored.isNotEmpty()) {
        val parts = model.ignored.map {
            when (it.reason) {
                IgnoredExtra.Reason.UNKNOWN_NAME -> stringResource(R.string.debug_ignored_unknown, it.key)
                IgnoredExtra.Reason.WRONG_TYPE -> stringResource(R.string.debug_ignored_type, it.key)
                IgnoredExtra.Reason.NOT_ALLOWED -> stringResource(R.string.debug_ignored_value, it.key)
                IgnoredExtra.Reason.OUT_OF_RANGE -> stringResource(R.string.debug_ignored_range, it.key)
            }
        }
        Note(stringResource(R.string.debug_ignored, parts.joinToString(" · ")), Modifier.testTag(DebugTags.IGNORED))
    }
}

@Composable
private fun CustomHeight(model: DebugToolsModel) {
    var input by rememberSaveable { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = input,
        onValueChange = { input = it; invalid = false },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.debug_height_custom)) },
        isError = invalid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
    Button(onClick = {
        val h = input.trim().toIntOrNull()
        if (h != null && h in DebugExtras.HEIGHTS) model.heightDp = h else invalid = true
    }) { Text(stringResource(R.string.debug_apply)) }
}

/** 위젯과 같은 RemoteViews를 설정 문맥(다크 · 글자 배율)으로 apply → 276 × h dp 틀 안에. 그린 뒤 click 요청이 있으면 그 줄을 누름 */
@Composable
private fun WidgetFrame(view: WidgetView, heightDp: Int, dark: Boolean, scale: Float, model: DebugToolsModel) {
    val base = LocalConfiguration.current
    val context = LocalContext.current
    val preview = remember(base, dark, scale) {
        val config = Configuration(base)
        config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            (if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO)
        config.fontScale = scale
        context.createConfigurationContext(config)
    }
    val rows = WidgetRules.rows(heightDp.toDouble(), scale.toDouble())
    val click = model.clickRequest
    AndroidView(
        factory = { FrameLayout(it).apply { id = R.id.debug_widget_frame } },
        modifier = Modifier.size(PREVIEW_WIDTH_DP.dp, heightDp.dp),
        update = { frame ->
            frame.removeAllViews()
            val shown = WidgetViews.forRows(preview, PREVIEW_WIDGET_ID, view, rows, scale.toDouble()).apply(preview, frame)
            frame.addView(shown)
            if (click != null) frame.post { model.click(click, view, rows, shown) }
        },
    )
}
