package io.github.jwavascript.issuerank.ui

import io.github.jwavascript.issuerank.Content
import io.github.jwavascript.issuerank.UiState
import io.github.jwavascript.issuerank.data.AuthUser
import io.github.jwavascript.issuerank.data.Bookmark
import io.github.jwavascript.issuerank.data.BookmarkView
import io.github.jwavascript.issuerank.data.Folder
import io.github.jwavascript.issuerank.data.Issue
import io.github.jwavascript.issuerank.data.Ranking
import io.github.jwavascript.issuerank.data.RankingSection
import io.github.jwavascript.issuerank.data.SectionKey
import io.github.jwavascript.issuerank.data.View
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * 미리보기 가짜 데이터 = 가짜 서버 씨앗 D0(android/.data/fake-server/README.md 3절 — 2026-11-03, 생활·문화는 D-1 대체 섹션).
 * 미리보기에서만 씀(앱 코드가 부르지 않아 release R8이 지움).
 */
internal object PreviewData {
    const val TODAY = "2026-11-03"
    const val YESTERDAY = "2026-11-02"

    private class Row(val id: String, val count: Int, val title: String, val summary: List<String>?)

    private val rows: Map<SectionKey, List<Row>> = mapOf(
        SectionKey.POL to listOf(
            Row("$TODAY-pol-21", 120, "여야, 내년도 예산안 처리 시한 앞두고 막판 협상", listOf("여야 원내대표가 예산안 처리 시한을 앞두고 다시 만났다.", "지역 사업 예산과 복지 예산 규모가 마지막 쟁점으로 남았다.", "합의가 늦어지면 정부 원안이 본회의에 자동 상정된다.")),
            Row("$TODAY-pol-22", 120, "국회 상임위, 민생법안 30여 건 일괄 상정", null),
            Row("$TODAY-pol-9", 120, "정부, 지방 이전 공공기관 2차 명단 발표", listOf("이전 대상 기관과 지역 배정 원칙이 함께 공개됐다.", "지자체들은 유치 경쟁에 다시 나설 채비를 하고 있다.", "정부는 연내 세부 이전 계획을 확정할 방침이다.")),
            Row("$TODAY-pol-10", 120, "선관위, 다음 달 재보궐선거 일정 확정", listOf("선거일과 후보 등록 기간이 공고됐다.", "사전투표는 선거일 닷새 전부터 이틀간 진행된다.", "선관위는 투표소 위치를 이달 말 공개한다.")),
            Row("$TODAY-pol-3", 64, "외교부, 한일 외교장관 회담 개최 조율", listOf("양국 외교 당국이 연내 회담 일정을 조율하고 있다.", "공급망 협력과 인적 교류 확대가 의제로 거론된다.", "회담 장소는 이르면 다음 주 발표될 예정이다.")),
        ),
        SectionKey.ECO to listOf(
            Row(
                "$TODAY-eco-20", 199,
                "기준금리 동결 이후 가계대출 · 전세대출 · 자영업자 대출 금리 흐름과 주택 시장, 소비 심리, 수출 경기까지 이어지는 영향을 한꺼번에 짚은 경제 부처 합동 브리핑 주요 내용 정리",
                listOf("경제 부처들은 금리 동결이 가계 · 자영업자 대출 이자 부담과 주택 거래, 소비 심리, 수출 기업의 자금 사정에 미치는 영향을 차례로 설명하며 당분간 시장 상황을 면밀히 지켜보겠다고 밝혔다.", "전세대출 금리는 지난달보다 소폭 내렸다.", "정부는 다음 달 추가 점검 결과를 발표한다."),
            ),
            Row("$TODAY-eco-1", 183, "반도체 수출 석 달 연속 증가세", listOf("9월 반도체 수출이 전년 대비 두 자릿수 증가했다.", "AI 서버용 고대역폭 메모리 수요가 증가를 이끌었다.", "전체 수출도 함께 늘며 무역수지 흑자가 이어졌다.")),
            Row("$TODAY-eco-4", 172, "가계부채 증가세, 2금융권으로 이동", listOf("은행권 대출 증가폭은 줄었으나 2금융권은 늘었다.", "금융당국은 풍선효과 차단을 위한 점검에 나섰다.", "추가 규제 여부는 이달 말 결정된다.")),
            Row("$TODAY-eco-5", 47, "소비자물가 2%대 초반 유지", listOf("농산물 가격 하락이 물가 안정에 기여했다.", "외식·서비스 물가는 여전히 높은 수준이다.", "정부는 연말 성수품 수급 관리에 들어간다.")),
            Row("$TODAY-eco-6", 29, "코스피 외국인 순매수 전환", listOf("반도체·이차전지 중심으로 외국인 자금이 유입됐다.", "지수는 2주 만에 박스권 상단을 돌파했다.", "증권가는 실적 시즌 결과에 주목하고 있다.")),
        ),
        SectionKey.SOC to listOf(
            Row("$TODAY-soc-2", 174, "전세사기 피해 지원 특별법 개정 추진", listOf("피해자 인정 요건을 완화하는 개정안이 발의됐다.", "경매 유예 기간 연장도 함께 담겼다.", "정부는 재정 부담을 이유로 신중한 입장이다.")),
            Row("$TODAY-soc-5", 97, "고령 운전자 면허 갱신 기준 강화 논의", listOf("인지기능 검사 주기를 단축하는 방안이 검토된다.", "조건부 면허 도입에 대한 공청회가 열렸다.", "이동권 보장 대책이 함께 필요하다는 지적이 나왔다.")),
            Row("$TODAY-soc-3", 85, "학교폭력 대응 전담 조사관 제도 확대", listOf("교사 대신 외부 조사관이 사안을 조사하는 제도다.", "시행 첫 학기 교사 업무 부담이 줄었다는 평가가 나왔다.", "내년부터 전국 모든 교육지원청으로 확대된다.")),
            Row("$TODAY-soc-4", 66, "가을 태풍 북상, 남부지방 강풍 대비", listOf("기상청은 주말 남해안에 강한 비바람을 예보했다.", "지자체는 해안가 시설물 점검에 들어갔다.", "항공·여객선 일부 운항 차질이 예상된다.")),
            Row("$TODAY-soc-6", 36, "저출생 대책, 육아휴직 급여 상한 인상", null),
        ),
        SectionKey.CUL to listOf(
            Row("$YESTERDAY-cul-5", 112, "한글날 앞두고 우리말 바로 쓰기 캠페인", listOf("공공기관 보도자료의 외국어 남용 실태가 공개됐다.", "대체어 목록이 새로 배포됐다.", "시민 참여형 공모전도 함께 열린다.")),
            Row("$YESTERDAY-cul-0", 40, "가을 축제 시즌, 지역 관광객 증가", listOf("주요 지역 축제 방문객이 지난해보다 늘었다.", "숙박·음식점 매출 증가로 지역 경제에 활기가 돌았다.", "일부 축제는 바가지요금 단속을 강화했다.")),
            Row("$YESTERDAY-cul-4", 31, "K-팝 월드투어 공연 매출 사상 최대", listOf("해외 공연 매출이 상반기 기준 최대치를 기록했다.", "북미·유럽 대형 공연장 매진이 이어졌다.", "암표 거래 대책 마련 요구도 커지고 있다.")),
            Row("$YESTERDAY-cul-3", 16, "도서관 야간 개방 확대 시범 운영", listOf("일부 공공도서관이 밤 10시까지 문을 연다.", "직장인 이용률이 크게 늘었다는 분석이다.", "내년 전국 확대 여부를 검토한다.")),
            Row("$YESTERDAY-cul-1", 14, "국내 영화 관객 수 회복세", listOf("추석 연휴 개봉작 흥행으로 관객 수가 반등했다.", "중예산 영화들이 입소문을 타며 장기 상영 중이다.", "업계는 연말까지 회복세가 이어질지 주목한다.")),
        ),
        SectionKey.WOR to listOf(
            Row("$TODAY-wor-2", 128, "중동 휴전 협상 재개", listOf("중재국 주도로 협상단이 다시 마주 앉았다.", "인질 석방과 인도적 지원 통로가 핵심 쟁점이다.", "협상 결과는 이번 주말 윤곽이 드러날 전망이다.")),
            Row("$TODAY-wor-1", 49, "유럽, 에너지 가격 안정 대책 발표", listOf("겨울철을 앞두고 가스 비축 목표를 상향했다.", "취약계층 에너지 요금 지원도 연장됐다.", "회원국 간 공동 구매 확대가 논의됐다.")),
            Row("$TODAY-wor-0", 22, "미 연준 위원들, 추가 인하 속도 두고 이견", listOf("일부 위원은 고용 둔화를 이유로 인하를 지지했다.", "다른 위원들은 물가 재상승 위험을 경고했다.", "시장은 다음 회의 결과에 촉각을 곤두세우고 있다.")),
            Row("$TODAY-wor-6", 20, "우크라이나 재건 회의, 민간 투자 유치", listOf("재건 사업에 민간 자본 참여 방안이 논의됐다.", "에너지·교통 인프라가 우선 분야로 꼽혔다.", "국내 기업들도 참여 의향을 밝혔다.")),
            Row("$TODAY-wor-3", 16, "일본 새 내각 출범, 경제 정책 방향 주목", listOf("새 내각은 임금 인상과 투자 확대를 강조했다.", "엔화 약세 대응 방안에 시장 관심이 쏠렸다.", "한일 관계 기조는 유지될 것으로 보인다.")),
        ),
        SectionKey.IT to listOf(
            Row("$TODAY-it-3", 121, "누리호 5차 발사 준비 착수", listOf("발사체 조립이 시작돼 연말 발사를 목표로 한다.", "실용위성 여러 기를 함께 실을 예정이다.", "민간 기업 주도 체제로 전환하는 첫 사례다.")),
            Row("$TODAY-it-1", 113, "차세대 HBM 양산 경쟁 본격화", listOf("국내 업체들이 차세대 고대역폭 메모리 샘플 공급을 시작했다.", "주요 GPU 업체의 인증 일정이 관건이다.", "내년 상반기 양산 체제 전환이 목표다.")),
            Row("$TODAY-it-2", 82, "통신 3사 요금제 개편, 데이터 중간 구간 신설", listOf("20~50GB 구간 요금제가 새로 나왔다.", "평균 요금이 소폭 낮아질 것으로 분석됐다.", "알뜰폰 업계는 경쟁력 약화를 우려했다.")),
            Row("$TODAY-it-4", 33, "개인정보 유출 사고, 과징금 부과", listOf("대형 플랫폼에 역대 최대 규모 과징금이 부과됐다.", "접근 통제 미흡이 주요 원인으로 지목됐다.", "피해자 보상 방안은 별도로 논의된다.")),
            Row("$TODAY-it-5", 17, "양자컴퓨팅 국가 연구 로드맵 발표", listOf("2030년까지 국산 양자컴퓨터 개발 목표가 제시됐다.", "인력 양성 프로그램도 함께 추진된다.", "민간 협력 컨소시엄이 꾸려졌다.")),
        ),
    )

    private val names = mapOf(
        SectionKey.POL to "정치", SectionKey.ECO to "경제", SectionKey.SOC to "사회",
        SectionKey.CUL to "생활·문화", SectionKey.WOR to "세계", SectionKey.IT to "IT·과학",
    )

    val ranking: Ranking = Ranking(
        date = TODAY,
        updatedAt = "${TODAY}T04:31:27+09:00",
        sections = SectionKey.entries.map { key ->
            RankingSection(key, names.getValue(key), rows.getValue(key).mapIndexed { i, r ->
                Issue(r.id, r.id.substring(0, 10), key, i + 1, r.title, r.summary, r.count, "https://news.naver.com/")
            })
        },
    )

    /** 지난 날(D-1) 화면: 같은 카드 · 날짜만 어제(미리보기용 — 실제 D-1 내용과 다름) */
    val yesterday: Ranking = ranking.copy(date = YESTERDAY, updatedAt = "${YESTERDAY}T04:28:05+09:00")

    /** "n분 전 저장"의 지금 = 2026-11-03 12:00 KST */
    val clock: Clock = Clock.fixed(Instant.parse("${TODAY}T03:00:00Z"), ZoneOffset.UTC)

    val user = AuthUser("preview-user", "앱 사용자", "app.user@example.invalid")

    /** 폴더 만들기 실패 토스트 = 서버 400 본문 message(ex12 server/schemas.ts FOLDER_NAME — 앱 문구가 아니라 서버가 주는 글) */
    const val FOLDER_NAME_ERROR = "폴더 이름은 1~16자로 입력해 주세요"

    /** 폴더(만든 순 · 개수 = 아래 북마크) — 16자 이름 하나(넘침 확인) */
    val folders = listOf(Folder("f1", "반도체·AI", 2), Folder("f2", "부동산·금리 그리고 전세 대출", 1), Folder("f3", "나중에 읽기", 0))

    /** 최근 저장 순: 오늘 카드 둘(한 묶음) → 어제 카드(대체 섹션 · 다른 묶음) */
    val bookmarks: List<Bookmark> = listOf(
        Bookmark(ranking.issueById("$TODAY-it-1")!!, "f1", "${TODAY}T02:58:00Z"),
        Bookmark(ranking.issueById("$TODAY-eco-20")!!, "f1", "${TODAY}T01:40:00Z"),
        Bookmark(ranking.issueById("$YESTERDAY-cul-5")!!, "f2", "${YESTERDAY}T12:40:00Z"),
    )

    fun state(
        view: View = View.MAP,
        ranking: Ranking? = PreviewData.ranking,
        content: Content = if (ranking != null) Content.READY else Content.NO_DATA,
        zoom: SectionKey? = null,
        selectedId: String? = null,
        expanded: Boolean = true,
    ) = UiState(
        view = view, date = ranking?.date ?: TODAY, today = TODAY, ranking = ranking, content = content,
        zoom = zoom, selectedId = selectedId, expanded = expanded,
    )

    /** 로그인 + 북마크 · 폴더 */
    fun signedIn(view: View = View.BOOKMARKS, folderFilter: String = BookmarkView.ALL) =
        state(view).copy(user = user, bookmarks = bookmarks, folders = folders, folderFilter = folderFilter)
}
