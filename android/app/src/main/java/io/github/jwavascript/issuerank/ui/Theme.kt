package io.github.jwavascript.issuerank.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.jwavascript.issuerank.R
import io.github.jwavascript.issuerank.data.SectionKey

/**
 * 웹 CSS 토큰(index.html :root 25~62) = res/values(-night)/colors.xml(A1 표, 한 곳 — 값은 바꾸지 않음). 리소스라 기기 다크 · 미리보기 uiMode ·
 * 시험 DarkMode 덮어쓰기를 그대로 따름. 글자 규칙(대비 4.5:1 — 계획 5절 · C11): 웹이 faint로 쓴 글자 · 아이콘은 muted로(faint = 선 · 점 장식만),
 * 웹이 토큰을 섞어 만드는 칠(타일 · 숫자 칸 · 반투명 글자)은 [Contrast]가 4.5가 될 만큼만 옮김.
 */
@Immutable
class WebColors(
    val bg: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val line: Color,
    val hover: Color,
    val ink: Color,
    val inkText: Color,
    val tileText: Color,
    val shadow: Color,
    private val sections: List<Color>,
) {
    /** 섹션 색(--s-pol …) */
    fun section(key: SectionKey): Color = sections[key.ordinal]
}

private val LocalWebColors = staticCompositionLocalOf<WebColors> { error("IssueRankTheme 밖에서 색을 읽음") }

object WebTheme {
    val colors: WebColors
        @Composable @ReadOnlyComposable get() = LocalWebColors.current
}

/** 웹 :root 뒤 가림막(.scrim rgba(0,0,0,.18)) — 토큰 밖 고정 색(A1 9절 목록) */
val ScrimColor = Color(0x2E000000)

@Composable
private fun webColors() = WebColors(
    bg = colorResource(R.color.bg),
    surface = colorResource(R.color.surface),
    text = colorResource(R.color.text),
    muted = colorResource(R.color.muted),
    faint = colorResource(R.color.faint),
    line = colorResource(R.color.line),
    hover = colorResource(R.color.hover),
    ink = colorResource(R.color.ink),
    inkText = colorResource(R.color.ink_text),
    tileText = colorResource(R.color.tile_text),
    shadow = colorResource(R.color.shadow),
    sections = listOf(
        colorResource(R.color.s_pol), colorResource(R.color.s_eco), colorResource(R.color.s_soc),
        colorResource(R.color.s_cul), colorResource(R.color.s_wor), colorResource(R.color.s_it),
    ),
)

/**
 * colorScheme.outline = Material OutlinedTextField의 초점 없는 테두리. 이 앱에서 outline을 읽는 부품은 달력 창 입력 모드 칸뿐
 * (Switch · OutlinedCard · 시간 고르기 등은 안 씀, 아래 창 끌기 손잡이는 끔): 웹 line을 칸이 놓인 달력 창 바탕(surface — 칸 속은 투명)과
 * 3:1이 될 만큼만 text 쪽으로(C12 ⑥ 입력 칸 경계). 구분선(outlineVariant)은 line 그대로.
 */
internal fun inputOutline(c: WebColors): Color = Color(Contrast.inputBorder(c.line.toArgb(), c.text.toArgb(), c.surface.toArgb()))

/** 앱 테마: 웹 색 + Material 부품(달력 · 아래 창 · 단추)이 같은 색을 쓰게 색 묶음을 맞춤 */
@Composable
fun IssueRankTheme(content: @Composable () -> Unit) {
    val c = webColors()
    val base = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    val outline = remember(c.line, c.text, c.surface) { inputOutline(c) }
    val scheme = base.copy(
        primary = c.ink, onPrimary = c.inkText, primaryContainer = c.hover, onPrimaryContainer = c.text,
        secondary = c.muted, onSecondary = c.surface, secondaryContainer = c.hover, onSecondaryContainer = c.text,
        background = c.bg, onBackground = c.text, surface = c.surface, onSurface = c.text,
        surfaceVariant = c.hover, onSurfaceVariant = c.muted, surfaceTint = c.surface,
        surfaceContainerLowest = c.surface, surfaceContainerLow = c.surface, surfaceContainer = c.surface,
        surfaceContainerHigh = c.surface, surfaceContainerHighest = c.surface,
        inverseSurface = c.ink, inverseOnSurface = c.inkText, outline = outline, outlineVariant = c.line, scrim = Color.Black,
    )
    CompositionLocalProvider(LocalWebColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

/**
 * 웹 화면 구간(CSS px = dp — 계획 5절: 340 · 400 · 640 · 900 · 1200). width = 화면 폭(dp, 내림 — 웹 clientWidth는 정수).
 * 400(설치 단추)은 앱에 설치 UI가 없어 쓰지 않음.
 */
@Immutable
data class Breakpoints(val width: Int) {
    /** (max-width: 640px): 이슈맵 세로 쌓기 · 머리 좁게(로고 글자 · 계정 단추 숨김) · 카드 칸 좁게 · 여백 16 · 제목 22 */
    val narrow: Boolean get() = width <= 640

    /** (max-width: 340px): 머리 간격 8 · 전환 단추 여백 10 */
    val tiny: Boolean get() = width <= 340

    /** (max-width: 899px): 머리 오른쪽 간격 6 */
    val belowMedium: Boolean get() = width <= 899

    /** (max-width: 1199px): 머리 북마크 글자 숨김(아이콘 + 숫자, 이름은 contentDescription) */
    val belowWide: Boolean get() = width <= 1199

    /** 바깥 여백(.wrap · .wrap-wide · 머리 안쪽) */
    val gutter: Dp get() = if (narrow) 16.dp else 24.dp
}

/** 웹 글자(px → sp). 본문 15/1.55 · 글꼴은 기기 글꼴(Pretendard 묶음은 계획에 없음) */
object WebType {
    private val w650 = FontWeight(650)
    val body = TextStyle(fontSize = 15.sp, lineHeight = 1.55.em)
    val h1 = TextStyle(fontSize = 26.sp, lineHeight = 1.25.em, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
    val h1Narrow = h1.copy(fontSize = 22.sp)
    val contextDesc = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em)
    val small = TextStyle(fontSize = 12.sp, lineHeight = 1.55.em)
    val small600 = small.copy(fontWeight = FontWeight.SemiBold)
    val label13 = TextStyle(fontSize = 13.sp, lineHeight = 1.55.em)
    val pill = TextStyle(fontSize = 13.sp, lineHeight = 1.55.em, fontWeight = FontWeight.SemiBold)
    val tab = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em, fontWeight = FontWeight.SemiBold)
    val groupHead = TextStyle(fontSize = 13.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold, letterSpacing = 0.02.em)
    val cardTitle = TextStyle(fontSize = 16.sp, lineHeight = 1.45.em, fontWeight = w650, letterSpacing = (-0.01).em)
    val summary = TextStyle(fontSize = 14.sp, lineHeight = 1.6.em)
    val scoreNumber = TextStyle(fontSize = 16.sp, lineHeight = 1.em, fontWeight = FontWeight.Bold)
    val scoreLabel = TextStyle(fontSize = 10.sp, lineHeight = 1.em)
    val emptyTitle = TextStyle(fontSize = 16.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold)
    val mapLabel = TextStyle(fontSize = 12.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold, letterSpacing = 0.01.em)
    val tileTitle = TextStyle(lineHeight = 1.32.em, fontWeight = w650, letterSpacing = (-0.01).em)
    val tileFoot = TextStyle(fontSize = 11.sp, lineHeight = 1.55.em, fontWeight = FontWeight.SemiBold)
    val detailTitle = TextStyle(fontSize = 20.sp, lineHeight = 1.4.em, fontWeight = FontWeight.Bold, letterSpacing = (-0.015).em)
    val detailSummary = TextStyle(fontSize = 15.sp, lineHeight = 1.6.em)
    val button = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em, fontWeight = FontWeight.SemiBold)
    val toast = TextStyle(fontSize = 14.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Medium)
    val brand = TextStyle(fontSize = 15.sp, lineHeight = 1.55.em, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em)
    val offlineTitle = TextStyle(fontSize = 22.sp, lineHeight = 1.35.em, fontWeight = FontWeight.Bold, letterSpacing = (-0.02).em)
    val offlineBrand = TextStyle(fontSize = 14.sp, lineHeight = 1.6.em, fontWeight = FontWeight.Bold)
}
