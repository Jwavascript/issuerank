package io.github.jwavascript.issuerank.data

import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** 응답 모양이 계약(openapi · 서버 코드)과 다름 → ErrorKind.BAD_RESPONSE */
class BadResponseException(field: String) : Exception("bad field: $field")

// org.json 도우미: Android 프레임워크 구현과 JVM 시험의 Maven org.json:json에서 같게 도는 것만 쓴다.
// optString · getString · optInt · getInt는 쓰지 않음 — JSON null에 Android optString은 "null" 글자, Maven은 ""를 돌려주고
// getString · getInt도 형 바꾸기 규칙이 다름(JsonCompatTest가 고정). 칸마다 isNull을 먼저 보고 opt()의 실제 형을 확인한다.

/** 맨 위 값: JSONObject · JSONArray · String · Number · Boolean · JSONObject.NULL */
internal fun jsonValue(text: String): Any = JSONTokener(text).nextValue()

internal fun jsonObject(text: String): JSONObject = jsonValue(text) as? JSONObject ?: bad("(root)")

internal fun jsonArray(text: String): JSONArray = jsonValue(text) as? JSONArray ?: bad("(root)")

/** 문자열이 아니면(없음 · null · 다른 형) null — JS `typeof x === 'string'` */
internal fun JSONObject.strOrNull(key: String): String? = if (isNull(key)) null else opt(key) as? String

internal fun JSONObject.str(key: String): String = strOrNull(key) ?: bad(key)

/** 정수(소수점 아래 0)만. Android는 Integer/Long/Double, Maven은 Integer/Long/BigDecimal로 읽어도 같은 값 */
internal fun JSONObject.int(key: String): Int {
    val v = if (isNull(key)) null else opt(key)
    if (v !is Number) bad(key)
    val d = v.toDouble()
    if (d.isNaN() || d != Math.floor(d) || d < Int.MIN_VALUE || d > Int.MAX_VALUE) bad(key)
    return d.toInt()
}

internal fun JSONObject.objOrNull(key: String): JSONObject? = if (isNull(key)) null else opt(key) as? JSONObject

internal fun JSONObject.obj(key: String): JSONObject = objOrNull(key) ?: bad(key)

internal fun JSONObject.arrOrNull(key: String): JSONArray? = if (isNull(key)) null else opt(key) as? JSONArray

internal fun JSONObject.arr(key: String): JSONArray = arrOrNull(key) ?: bad(key)

internal fun JSONArray.objAt(i: Int): JSONObject = opt(i) as? JSONObject ?: bad("[$i]")

internal fun JSONArray.strAt(i: Int): String = opt(i) as? String ?: bad("[$i]")

internal fun <T> JSONArray.mapObjects(block: (JSONObject) -> T): List<T> = List(length()) { block(objAt(it)) }

internal fun bad(field: String): Nothing = throw BadResponseException(field)
