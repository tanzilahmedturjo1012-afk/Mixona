package com.example.data

import com.example.model.ApiFeedMode
import com.example.model.DrawRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random

data class FetchResult(
    val data: List<DrawRecord>,
    val source: String,
)

object MixonaNetwork {

    private const val SKY_30S_URL =
        "https://sky-predictor-1012593186417.asia-southeast1.run.app/api/wingo-history-30s-100"
    private const val AR_1M_URL =
        "https://draw.ar-lottery01.com/WinGo/WinGo_1M/GetHistoryIssuePage.json"

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun getJson(url: String): Any {
        val request = Request.Builder()
            .url(url)
            .cacheControl(CacheControl.FORCE_NETWORK)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}")
            }
            val body = response.body?.string() ?: throw IOException("Empty response")
            return JSONTokener(body).nextValue()
        }
    }

    fun normalizePayload(payload: Any?): List<DrawRecord> {
        val arr: JSONArray? = when (payload) {
            is JSONArray -> payload
            is JSONObject -> {
                val dataObj = payload.opt("data")
                val resultObj = payload.opt("result")
                when {
                    dataObj is JSONObject && dataObj.opt("list") is JSONArray -> dataObj.optJSONArray("list")
                    dataObj is JSONObject && dataObj.opt("records") is JSONArray -> dataObj.optJSONArray("records")
                    dataObj is JSONArray -> dataObj
                    payload.opt("list") is JSONArray -> payload.optJSONArray("list")
                    payload.opt("records") is JSONArray -> payload.optJSONArray("records")
                    resultObj is JSONObject && resultObj.opt("list") is JSONArray -> resultObj.optJSONArray("list")
                    else -> null
                }
            }
            else -> null
        }

        if (arr == null) return emptyList()

        val result = ArrayList<DrawRecord>(minOf(arr.length(), 100))
        val digitRegex = Regex("\\d")

        for (i in 0 until arr.length()) {
            if (result.size >= 100) break
            val item = arr.opt(i)
            when (item) {
                is Number -> {
                    val n = item.toInt()
                    if (n in 0..9 && item.toDouble() == n.toDouble()) {
                        result.add(DrawRecord(n, if (n >= 5) "BIG" else "SMALL", ""))
                    }
                }
                is String -> {
                    val n = item.toIntOrNull()
                    if (n != null && n in 0..9) {
                        result.add(DrawRecord(n, if (n >= 5) "BIG" else "SMALL", ""))
                    }
                }
                is JSONObject -> {
                    val raw = item.optFirstNonNull("number", "openNumber", "result", "code", "value", "open_code", "openCode")
                    val match = digitRegex.find(raw?.toString().orEmpty()) ?: continue
                    val n = match.value.toIntOrNull() ?: continue
                    if (n !in 0..9) continue
                    val period = item.optFirstNonNull("issueNumber", "issue", "issue_number", "period", "drawNo", "id")
                        ?.toString()
                        .orEmpty()
                    result.add(DrawRecord(number = n, size = if (n >= 5) "BIG" else "SMALL", period = period))
                }
            }
        }
        return result
    }

    private fun JSONObject.optFirstNonNull(vararg keys: String): Any? {
        for (key in keys) {
            if (has(key) && !isNull(key)) {
                return opt(key)
            }
        }
        return null
    }

    suspend fun fetchFromSource(mode: ApiFeedMode): FetchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        fun trySky(): List<DrawRecord> {
            return try {
                normalizePayload(getJson("$SKY_30S_URL?_=$now"))
            } catch (e: IOException) {
                throw IOException("Sky 30s ${e.message}", e)
            }
        }

        fun tryAr(): List<DrawRecord> {
            return try {
                normalizePayload(getJson("$AR_1M_URL?ts=$now"))
            } catch (e: IOException) {
                throw IOException("AR 1M ${e.message}", e)
            }
        }

        when (mode) {
            ApiFeedMode.SKY_30S -> FetchResult(trySky(), "Sky WinGo 30s")
            ApiFeedMode.AR_1M -> FetchResult(tryAr(), "AR-Lottery 1M")
            ApiFeedMode.AUTO -> {
                try {
                    val sky = trySky()
                    if (sky.size >= 20) {
                        FetchResult(sky, "Sky WinGo 30s")
                    } else {
                        throw IOException("Insufficient Sky records")
                    }
                } catch (_: Exception) {
                    val ar = tryAr()
                    FetchResult(ar, "AR-Lottery 1M")
                }
            }
        }
    }

    fun generateDemoDraws(): List<DrawRecord> {
        var seed = (System.currentTimeMillis() % 100000L).toInt()
        val base = 202611170100L + Random.nextInt(500)
        return List(100) { i ->
            seed = (seed * 9301 + 49297) % 233280
            val number = ((seed.toDouble() / 233280.0) * 10.0).toInt().coerceIn(0, 9)
            DrawRecord(
                number = number,
                size = if (number >= 5) "BIG" else "SMALL",
                period = (base - i).toString(),
            )
        }
    }
}
