package io.github.jwavascript.issuerank

import io.github.jwavascript.issuerank.data.ApiError
import io.github.jwavascript.issuerank.data.ApiJson
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.AppRoutes
import io.github.jwavascript.issuerank.data.ErrorKind
import io.github.jwavascript.issuerank.data.KstDate
import io.github.jwavascript.issuerank.data.WidgetPayload
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 위젯 미리보기 상태(검증 11): 고정 JSON 4가지 + live(지금 서버에서 한 번 — DebugToolsModel). 위젯과 같은 resolve 길로 WidgetView를 만듦.
 * 줄 = 가짜 서버 씨앗 buildWidget과 같은 순서 · 제목(단위 시험 fixture widget-d0.json · widget-d1.json과 대조 — 그 fixture는 a5-fake-server-check가 서버와 대조)이고,
 * 날짜 = 보는 날의 오늘 · 어제(KST)라 줄을 누르면 가짜 서버의 그 이슈 상세가 열림.
 * ok30 = 오늘 30줄 · ok3 = 앞 3줄(줄이 항목 수에서 멈춤) · cache = 받기 실패 + 어제 받아 둔 캐시 · fail = 받기 실패 + 캐시 없음("불러오지 못했습니다").
 */
object DebugWidgetStates {
    enum class State(val key: String) {
        OK30("ok30"), OK3("ok3"), CACHE("cache"), FAIL("fail"), LIVE("live");

        companion object {
            fun of(key: String): State? = entries.firstOrNull { it.key == key }
        }
    }

    /** 고정 상태 → 위젯이 그릴 것. LIVE는 서버에서 받아야 해서 null */
    fun view(state: State, today: String): WidgetView? = when (state) {
        State.OK30 -> received(json(today, TODAY, "04:31:27", "오전 4:31"))
        State.OK3 -> received(json(today, TODAY.take(3), "04:31:27", "오전 4:31"))
        State.CACHE -> WidgetRules.resolve(OFFLINE, json(KstDate.addDays(today, -1), YESTERDAY, "04:28:05", "오전 4:28")).view
        State.FAIL -> WidgetRules.resolve(OFFLINE, null).view
        State.LIVE -> null
    }

    private val OFFLINE = ApiResult.Fail(ApiError(ErrorKind.NETWORK))

    private fun received(json: String): WidgetView = WidgetRules.resolve(ApiResult.Ok(WidgetPayload(ApiJson.widget(json), json)), null).view

    /** 가짜 서버 /api/widget 꼴: 줄 id = {date + 그 줄의 날 차이}-{섹션}-{번호} · path = /?issue=id · morePath /#list · updatedLabel "M.D 오전 h:mm 수집" */
    private fun json(date: String, items: List<Item>, updatedTime: String, labelTime: String): String {
        val d = LocalDate.parse(date)
        val list = JSONArray()
        for (item in items) {
            val id = "${KstDate.addDays(date, item.dayOffset)}-${item.tail}"
            list.put(JSONObject().put("id", id).put("section", item.tail.substringBefore('-')).put("title", item.title).put("path", AppRoutes.issuePath(id)))
        }
        return JSONObject()
            .put("date", date)
            .put("updatedAt", "${date}T$updatedTime+09:00")
            .put("updatedLabel", "${d.monthValue}.${d.dayOfMonth} $labelTime 수집")
            .put("morePath", AppRoutes.LIST)
            .put("items", list)
            .toString()
    }

    /** 위젯 한 줄 = 날 차이(그 위젯 date 기준) · "{섹션}-{번호}" · 제목 */
    private class Item(val dayOffset: Long, val tail: String, val title: String)

    /** D0(오늘): 가짜 서버 /api/widget 30줄 그대로(생활·문화 = 대체 섹션 — 어제 줄) */
    private val TODAY = listOf(
        Item(0, "eco-20", "기준금리 동결 이후 가계대출 · 전세대출 · 자영업자 대출 금리 흐름과 주택 시장, 소비 심리, 수출 경기까지 이어지는 영향을 한꺼번에 짚은 경제 부처 합동 브리핑 주요 내용 정리"),
        Item(0, "soc-2", "전세사기 피해 지원 특별법 개정 추진"),
        Item(0, "wor-2", "중동 휴전 협상 재개"),
        Item(0, "it-3", "누리호 5차 발사 준비 착수"),
        Item(0, "pol-21", "여야, 내년도 예산안 처리 시한 앞두고 막판 협상"),
        Item(-1, "cul-5", "한글날 앞두고 우리말 바로 쓰기 캠페인"),
        Item(0, "eco-1", "반도체 수출 석 달 연속 증가세"),
        Item(0, "eco-4", "가계부채 증가세, 2금융권으로 이동"),
        Item(0, "pol-22", "국회 상임위, 민생법안 30여 건 일괄 상정"),
        Item(0, "pol-9", "정부, 지방 이전 공공기관 2차 명단 발표"),
        Item(0, "pol-10", "선관위, 다음 달 재보궐선거 일정 확정"),
        Item(0, "it-1", "차세대 HBM 양산 경쟁 본격화"),
        Item(0, "soc-5", "고령 운전자 면허 갱신 기준 강화 논의"),
        Item(0, "soc-3", "학교폭력 대응 전담 조사관 제도 확대"),
        Item(0, "it-2", "통신 3사 요금제 개편, 데이터 중간 구간 신설"),
        Item(0, "soc-4", "가을 태풍 북상, 남부지방 강풍 대비"),
        Item(0, "pol-3", "외교부, 한일 외교장관 회담 개최 조율"),
        Item(0, "wor-1", "유럽, 에너지 가격 안정 대책 발표"),
        Item(0, "eco-5", "소비자물가 2%대 초반 유지"),
        Item(-1, "cul-0", "가을 축제 시즌, 지역 관광객 증가"),
        Item(0, "soc-6", "저출생 대책, 육아휴직 급여 상한 인상"),
        Item(0, "it-4", "개인정보 유출 사고, 과징금 부과"),
        Item(-1, "cul-4", "K-팝 월드투어 공연 매출 사상 최대"),
        Item(0, "eco-6", "코스피 외국인 순매수 전환"),
        Item(0, "wor-0", "미 연준 위원들, 추가 인하 속도 두고 이견"),
        Item(0, "wor-6", "우크라이나 재건 회의, 민간 투자 유치"),
        Item(0, "it-5", "양자컴퓨팅 국가 연구 로드맵 발표"),
        Item(0, "wor-3", "일본 새 내각 출범, 경제 정책 방향 주목"),
        Item(-1, "cul-3", "도서관 야간 개방 확대 시범 운영"),
        Item(-1, "cul-1", "국내 영화 관객 수 회복세"),
    )

    /** D-1(어제): 가짜 서버 씨앗의 어제 buildWidget 30줄(cache 상태 — 받아 둔 날이 지난 캐시) */
    private val YESTERDAY = listOf(
        Item(0, "wor-0", "미 연준 위원들, 추가 인하 속도 두고 이견"),
        Item(0, "soc-3", "학교폭력 대응 전담 조사관 제도 확대"),
        Item(0, "it-4", "개인정보 유출 사고, 과징금 부과"),
        Item(0, "pol-4", "정부 조직 개편안 국무회의 의결"),
        Item(0, "eco-0", "기준금리 동결, 연내 인하 가능성 시사"),
        Item(0, "cul-5", "한글날 앞두고 우리말 바로 쓰기 캠페인"),
        Item(0, "it-2", "통신 3사 요금제 개편, 데이터 중간 구간 신설"),
        Item(0, "pol-2", "지방선거 앞두고 선거구 획정안 논의"),
        Item(0, "eco-3", "수도권 아파트 매매가 상승폭 둔화"),
        Item(0, "wor-2", "중동 휴전 협상 재개"),
        Item(0, "soc-6", "저출생 대책, 육아휴직 급여 상한 인상"),
        Item(0, "pol-6", "청년 정책 기본계획 발표"),
        Item(0, "wor-4", "기후총회 앞두고 감축 목표 상향 압박"),
        Item(0, "wor-3", "일본 새 내각 출범, 경제 정책 방향 주목"),
        Item(0, "pol-1", "국정감사 첫 주, 부처별 현안 질의 집중"),
        Item(0, "wor-6", "우크라이나 재건 회의, 민간 투자 유치"),
        Item(0, "it-0", "국내 AI 기본법 시행령 초안 공개"),
        Item(0, "soc-5", "고령 운전자 면허 갱신 기준 강화 논의"),
        Item(0, "pol-0", "내년도 예산안 국회 상임위 심사 본격화"),
        Item(0, "cul-0", "가을 축제 시즌, 지역 관광객 증가"),
        Item(0, "cul-4", "K-팝 월드투어 공연 매출 사상 최대"),
        Item(0, "soc-0", "의대 정원 후속 대책, 수련병원 지원 확대"),
        Item(0, "it-3", "누리호 5차 발사 준비 착수"),
        Item(0, "eco-2", "원·달러 환율 1,380원대 등락"),
        Item(0, "cul-3", "도서관 야간 개방 확대 시범 운영"),
        Item(0, "soc-2", "전세사기 피해 지원 특별법 개정 추진"),
        Item(0, "eco-1", "반도체 수출 석 달 연속 증가세"),
        Item(0, "cul-1", "국내 영화 관객 수 회복세"),
        Item(0, "it-1", "차세대 HBM 양산 경쟁 본격화"),
        Item(0, "eco-6", "코스피 외국인 순매수 전환"),
    )
}
