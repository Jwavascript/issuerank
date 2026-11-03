package io.github.jwavascript.issuerank.widget

import io.github.jwavascript.issuerank.data.ApiError
import io.github.jwavascript.issuerank.data.ApiResult
import io.github.jwavascript.issuerank.data.ErrorKind
import io.github.jwavascript.issuerank.data.WidgetPayload
import io.github.jwavascript.issuerank.data.WidgetRules
import io.github.jwavascript.issuerank.data.WidgetView
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** 성공한 응답 원문을 두는 곳(WidgetRules.resolve의 writeCache — 받은 글자 그대로, 읽을 때 resolve가 다시 검사) */
interface WidgetCache {
    fun read(): String?
    fun write(json: String)
}

/** 다음에 받을 시각(widget.js refreshAfter — epoch ms) */
interface RefreshSchedule {
    fun nextAt(): Long?
    fun setNextAt(atMs: Long)
}

/** 캐시 = 앱 전용 파일 하나. 임시 파일에 쓰고 바꿔 끼움(반쯤 쓴 파일 0). 쓰기 실패는 버림(widget.js 40 — 다음 성공 때 다시) */
class FileWidgetCache(private val file: File) : WidgetCache {
    override fun read(): String? = try {
        if (file.isFile) file.readText(Charsets.UTF_8) else null
    } catch (e: IOException) {
        null
    }

    override fun write(json: String) {
        val tmp = File(file.path + ".tmp")
        try {
            tmp.writeText(json, Charsets.UTF_8)
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (e: IOException) {
            tmp.delete()
        }
    }

    fun clear() {
        file.delete()
    }
}

/**
 * 위젯 데이터 한 번(03-data 예시 91~93줄 그대로 + 시간 한도). 순수(Android 0 — JVM 시험), 받기 · 캐시 · 다음 시각 · 시계는 넣어 줌.
 * - [update]: 받을 때가 됐으면(dueForRefresh) 받음 — 연결 5초 + 읽기 4초(= 9초)를 넘으면 끊고 실패로(방송 10초 한도 안).
 *   결과 = resolve(받은 것, 캐시): 성공이면 그것, 실패면 캐시, 캐시도 없으면 실패 안내. 캐시에는 성공한 원문만 씀.
 *   받았으면(성공 · 실패 모두) 다음 시각 = refreshAfter(보이는 date, 지금) — widget.js가 load 뒤 refreshAfter(data)를 정하는 것과 같음.
 * - [cached]: 받지 않고 지금 캐시로(크기 바뀜).
 */
class WidgetRefresh(
    private val fetch: suspend () -> ApiResult<WidgetPayload>,
    private val cache: WidgetCache,
    private val schedule: RefreshSchedule,
    private val now: () -> Long,
) {
    suspend fun update(): WidgetView {
        val fetched = if (WidgetRules.dueForRefresh(now(), schedule.nextAt())) {
            withTimeoutOrNull(FETCH_BUDGET_MS) { fetch() } ?: ApiResult.Fail(ApiError(ErrorKind.NETWORK))
        } else {
            null
        }
        val res = WidgetRules.resolve(fetched, cache.read())
        res.writeCache?.let(cache::write)
        if (fetched != null) schedule.setNextAt(WidgetRules.refreshAfter(res.view.widget?.date, now()))
        return res.view
    }

    fun cached(): WidgetView = WidgetRules.resolve(null, cache.read()).view

    companion object {
        /** 받기 한도 = 연결 5초 + 읽기 4초(WidgetRules). 넘으면 끊음(UrlConnectionTransport가 취소에 disconnect) → 실패 → 캐시 */
        const val FETCH_BUDGET_MS = (WidgetRules.CONNECT_TIMEOUT_MS + WidgetRules.READ_TIMEOUT_MS).toLong()
    }
}
