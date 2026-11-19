package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ApiFeedMode
import com.example.model.EngineStats
import com.example.model.LearnState
import com.example.model.LedgerEntry
import com.example.model.MistakeSignature
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

private val Context.mixonaDataStore: DataStore<Preferences> by preferencesDataStore(name = "mixona_prefs")

data class StoredAppState(
    val isDarkTheme: Boolean,
    val jackpotThreshold: Double,
    val apiFeedMode: ApiFeedMode,
    val ledger: List<LedgerEntry>,
    val learnState: LearnState,
)

class MixonaPreferences(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val JACKPOT_THRESHOLD = doublePreferencesKey("jackpot_threshold")
        val API_MODE = stringPreferencesKey("api_mode")
        val LEDGER_JSON = stringPreferencesKey("ledger_json")
        val LEARN_JSON = stringPreferencesKey("learn_json")
    }

    suspend fun loadInitialState(): StoredAppState {
        val prefs = context.mixonaDataStore.data.first()
        return StoredAppState(
            isDarkTheme = prefs[Keys.DARK_THEME] ?: true,
            jackpotThreshold = prefs[Keys.JACKPOT_THRESHOLD] ?: 0.75,
            apiFeedMode = ApiFeedMode.fromId(prefs[Keys.API_MODE] ?: "auto"),
            ledger = decodeLedger(prefs[Keys.LEDGER_JSON]),
            learnState = decodeLearn(prefs[Keys.LEARN_JSON]),
        )
    }

    suspend fun saveTheme(isDark: Boolean) {
        context.mixonaDataStore.edit { it[Keys.DARK_THEME] = isDark }
    }

    suspend fun saveJackpotThreshold(value: Double) {
        context.mixonaDataStore.edit { it[Keys.JACKPOT_THRESHOLD] = value }
    }

    suspend fun saveApiMode(mode: ApiFeedMode) {
        context.mixonaDataStore.edit { it[Keys.API_MODE] = mode.id }
    }

    suspend fun saveLedger(ledger: List<LedgerEntry>) {
        context.mixonaDataStore.edit { it[Keys.LEDGER_JSON] = encodeLedger(ledger.take(300)) }
    }

    suspend fun saveLearnState(learn: LearnState) {
        context.mixonaDataStore.edit { it[Keys.LEARN_JSON] = encodeLearn(learn) }
    }

    private fun encodeLedger(ledger: List<LedgerEntry>): String {
        val arr = JSONArray()
        for (e in ledger) {
            val obj = JSONObject()
            obj.put("period", e.period)
            obj.put("action", e.action)
            obj.put("rawMasterSize", e.rawMasterSize)
            obj.put("masterNums", JSONArray(e.masterNums))
            if (e.oppNum != null) obj.put("oppNum", e.oppNum) else obj.put("oppNum", JSONObject.NULL)
            obj.put("trendContext", e.trendContext)
            obj.put("exactKey", e.exactKey)
            obj.put("broadKey", e.broadKey)
            obj.put("engineCalls", JSONObject(e.engineCalls))
            if (e.actualNum != null) obj.put("actualNum", e.actualNum) else obj.put("actualNum", JSONObject.NULL)
            if (e.actualSize != null) obj.put("actualSize", e.actualSize) else obj.put("actualSize", JSONObject.NULL)
            obj.put("status", e.status)
            obj.put("at", e.at)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun decodeLedger(json: String?): List<LedgerEntry> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            List(arr.length()) { i ->
                val obj = arr.getJSONObject(i)
                val numsArr = obj.optJSONArray("masterNums") ?: JSONArray()
                val nums = List(numsArr.length()) { idx -> numsArr.optInt(idx) }
                val callsObj = obj.optJSONObject("engineCalls") ?: JSONObject()
                val calls = mutableMapOf<String, String>()
                callsObj.keys().forEach { k -> calls[k] = callsObj.optString(k) }
                LedgerEntry(
                    period = obj.optString("period"),
                    action = obj.optString("action"),
                    rawMasterSize = obj.optString("rawMasterSize"),
                    masterNums = nums,
                    oppNum = if (obj.isNull("oppNum")) null else obj.optInt("oppNum"),
                    trendContext = obj.optString("trendContext"),
                    exactKey = obj.optString("exactKey"),
                    broadKey = obj.optString("broadKey"),
                    engineCalls = calls,
                    actualNum = if (obj.isNull("actualNum")) null else obj.optInt("actualNum"),
                    actualSize = if (obj.isNull("actualSize")) null else obj.optString("actualSize"),
                    status = obj.optString("status", "WAIT"),
                    at = obj.optLong("at", 0L),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun encodeLearn(learn: LearnState): String {
        val root = JSONObject()
        root.put("weights", JSONObject(learn.weights))
        val statsObj = JSONObject()
        for ((k, v) in learn.stats) {
            statsObj.put(k, JSONObject().put("w", v.w).put("l", v.l))
        }
        root.put("stats", statsObj)
        val mistakesObj = JSONObject()
        for ((k, m) in learn.mistakes) {
            mistakesObj.put(
                k,
                JSONObject()
                    .put("key", m.key)
                    .put("side", m.side)
                    .put("losses", m.losses)
                    .put("cooldown", m.cooldown)
                    .put("lastAt", m.lastAt),
            )
        }
        root.put("mistakes", mistakesObj)
        root.put("guardSaves", learn.guardSaves)
        root.put("guardTotal", learn.guardTotal)
        return root.toString()
    }

    private fun decodeLearn(json: String?): LearnState {
        if (json.isNullOrBlank()) return LearnState()
        return try {
            val root = JSONObject(json)
            val defaults = LearnState()
            val wObj = root.optJSONObject("weights")
            val weights = defaults.weights.toMutableMap()
            wObj?.keys()?.forEach { k -> weights[k] = wObj.optDouble(k, weights[k] ?: 0.25) }

            val sObj = root.optJSONObject("stats")
            val stats = defaults.stats.toMutableMap()
            sObj?.keys()?.forEach { k ->
                val item = sObj.optJSONObject(k)
                if (item != null) {
                    stats[k] = EngineStats(w = item.optInt("w", 0), l = item.optInt("l", 0))
                }
            }

            val mObj = root.optJSONObject("mistakes")
            val mistakes = mutableMapOf<String, MistakeSignature>()
            mObj?.keys()?.forEach { k ->
                val item = mObj.optJSONObject(k)
                if (item != null) {
                    mistakes[k] = MistakeSignature(
                        key = item.optString("key", k),
                        side = item.optString("side", "BIG"),
                        losses = item.optInt("losses", 0),
                        cooldown = item.optInt("cooldown", 0),
                        lastAt = item.optLong("lastAt", 0L),
                    )
                }
            }

            LearnState(
                weights = weights,
                stats = stats,
                mistakes = mistakes,
                guardSaves = root.optInt("guardSaves", 0),
                guardTotal = root.optInt("guardTotal", 0),
            )
        } catch (_: Exception) {
            LearnState()
        }
    }
}
