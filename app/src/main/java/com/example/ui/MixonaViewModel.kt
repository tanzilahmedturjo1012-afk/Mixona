package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MixonaNetwork
import com.example.data.MixonaPreferences
import com.example.engine.MasterEnsemble
import com.example.model.ApiFeedMode
import com.example.model.ConnectionState
import com.example.model.DrawRecord
import com.example.model.HistoryFilter
import com.example.model.LearnState
import com.example.model.LedgerEntry
import com.example.model.MasterAnalysis
import com.example.model.StreakStats
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ToastEvent(
    val id: Long,
    val title: String,
    val detail: String,
    val tone: String,
)

data class MixonaUiState(
    val isDarkTheme: Boolean = true,
    val connectionState: ConnectionState = ConnectionState.CONNECTING,
    val statusMessage: String = "Starting automatic 100-result sync…",
    val clockText: String = "--:--:--",
    val countdownSeconds: Int = 30,
    val apiMode: ApiFeedMode = ApiFeedMode.AUTO,
    val records: List<DrawRecord> = emptyList(),
    val nextPeriod: String = "-----",
    val analysis: MasterAnalysis? = null,
    val ledger: List<LedgerEntry> = emptyList(),
    val learnState: LearnState = LearnState(),
    val streaks: StreakStats = StreakStats(),
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val ledgerSearch: String = "",
    val jackpotThreshold: Double = 0.75,
    val toast: ToastEvent? = null,
)

class MixonaViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = MixonaPreferences(application)
    private val _uiState = MutableStateFlow(MixonaUiState())
    val uiState: StateFlow<MixonaUiState> = _uiState.asStateFlow()

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.UK)
    private var isFetching = false
    private var lastPeriod = ""
    private var isInitialized = false

    init {
        viewModelScope.launch {
            val saved = prefs.loadInitialState()
            _uiState.update {
                it.copy(
                    isDarkTheme = saved.isDarkTheme,
                    jackpotThreshold = saved.jackpotThreshold,
                    apiMode = saved.apiFeedMode,
                    ledger = saved.ledger,
                    learnState = saved.learnState,
                    streaks = MasterEnsemble.computeStreakStats(saved.ledger),
                )
            }
            isInitialized = true
            autoSync()
        }

        viewModelScope.launch {
            while (true) {
                val now = Calendar.getInstance()
                val sec = now.get(Calendar.SECOND)
                val rem = 30 - (sec % 30)
                _uiState.update {
                    it.copy(
                        clockText = timeFormatter.format(now.time),
                        countdownSeconds = rem,
                    )
                }
                if (isInitialized && rem == 30) {
                    autoSync()
                }
                delay(1000L)
            }
        }

        viewModelScope.launch {
            while (true) {
                delay(5000L)
                if (isInitialized) {
                    autoSync()
                }
            }
        }
    }

    fun autoSync() {
        if (isFetching) return
        isFetching = true
        viewModelScope.launch {
            try {
                val result = MixonaNetwork.fetchFromSource(_uiState.value.apiMode)
                if (result.data.size < 20) {
                    throw IllegalStateException("Fewer than 20 valid records returned.")
                }
                processRecords(result.data)
                val timeStr = timeFormatter.format(Date())
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.LIVE,
                        statusMessage = "Auto-synced $timeStr · ${result.data.size} draws · ${result.source}",
                    )
                }
            } catch (err: Exception) {
                _uiState.update {
                    it.copy(
                        connectionState = ConnectionState.ERROR,
                        statusMessage = "${err.message ?: "Sync failed"} · Auto-retrying every 5s (or tap Demo Preview).",
                    )
                }
            } finally {
                isFetching = false
            }
        }
    }

    fun loadDemoData() {
        val demo = MixonaNetwork.generateDemoDraws()
        processRecords(demo)
        _uiState.update {
            it.copy(
                connectionState = ConnectionState.DEMO,
                statusMessage = "100-draw synthetic demo running automatically across all 4 engines + Self-Learner.",
            )
        }
        showToast("Demo Data Loaded", "All engines & Self-Learning Guard updated.", "gold")
    }

    private fun processRecords(data: List<DrawRecord>) {
        val periodMap = data.filter { it.period.isNotEmpty() }.associateBy { it.period }
        var currentLearn = _uiState.value.learnState
        var settledAny = false

        val updatedLedger = _uiState.value.ledger.map { entry ->
            val actual = if (entry.actualNum == null && entry.period.isNotEmpty()) periodMap[entry.period] else null
            if (actual == null) {
                entry
            } else {
                val (learnOutcome, newLearn) = MasterEnsemble.learnFromSettledRound(
                    entry = entry,
                    actualSize = actual.size,
                    learnState = currentLearn,
                )
                currentLearn = newLearn
                settledAny = true

                val status = if (entry.action == "SKIP") {
                    if (learnOutcome == "SAVED") "SAVED" else "GUARDED"
                } else {
                    val numHit = entry.masterNums.contains(actual.number) || entry.oppNum == actual.number
                    when {
                        numHit -> "JACKPOT"
                        entry.rawMasterSize == actual.size -> "SURE SHOT"
                        else -> "LOSS"
                    }
                }

                entry.copy(
                    actualNum = actual.number,
                    actualSize = actual.size,
                    status = status,
                )
            }
        }.toMutableList()

        val threshold = _uiState.value.jackpotThreshold
        val analysis = MasterEnsemble.runMasterEnsemble(data, currentLearn, threshold)

        val latestPeriod = data.firstOrNull()?.period.orEmpty()
        val nextPeriod = MasterEnsemble.computeNextPeriod(latestPeriod)
        var addedNew = false
        if (latestPeriod.isNotEmpty() && nextPeriod != lastPeriod) {
            lastPeriod = nextPeriod
            if (updatedLedger.firstOrNull()?.period != nextPeriod) {
                updatedLedger.add(
                    0,
                    LedgerEntry(
                        period = nextPeriod,
                        action = analysis.action,
                        rawMasterSize = analysis.rawMasterSize,
                        masterNums = analysis.masterNumbers,
                        oppNum = analysis.oppositeNumber,
                        trendContext = "${analysis.phantom.regime} · ${analysis.sig.pat3}",
                        exactKey = analysis.exactKey,
                        broadKey = analysis.broadKey,
                        engineCalls = mapOf(
                            "phantom" to analysis.phantom.prediction,
                            "renox" to analysis.renox.prediction,
                            "hidan" to analysis.hidan.prediction,
                            "goku" to analysis.goku.prediction,
                        ),
                        actualNum = null,
                        actualSize = null,
                        status = "WAIT",
                        at = System.currentTimeMillis(),
                    ),
                )
                addedNew = true
            }
        }

        val trimmedLedger = updatedLedger.take(300)
        val streaks = MasterEnsemble.computeStreakStats(trimmedLedger)

        _uiState.update {
            it.copy(
                records = data,
                nextPeriod = nextPeriod,
                analysis = analysis,
                ledger = trimmedLedger,
                learnState = currentLearn,
                streaks = streaks,
            )
        }

        if (settledAny || addedNew) {
            viewModelScope.launch {
                prefs.saveLedger(trimmedLedger)
                if (settledAny) {
                    prefs.saveLearnState(currentLearn)
                }
            }
        }
    }

    fun setApiMode(mode: ApiFeedMode) {
        _uiState.update { it.copy(apiMode = mode) }
        viewModelScope.launch { prefs.saveApiMode(mode) }
        autoSync()
    }

    fun toggleTheme() {
        val next = !_uiState.value.isDarkTheme
        _uiState.update { it.copy(isDarkTheme = next) }
        viewModelScope.launch { prefs.saveTheme(next) }
    }

    fun setHistoryFilter(filter: HistoryFilter) {
        _uiState.update { it.copy(historyFilter = filter) }
    }

    fun setLedgerSearch(query: String) {
        _uiState.update { it.copy(ledgerSearch = query) }
    }

    fun setJackpotThreshold(value: Double) {
        _uiState.update { it.copy(jackpotThreshold = value) }
        viewModelScope.launch { prefs.saveJackpotThreshold(value) }
        val records = _uiState.value.records
        if (records.isNotEmpty()) {
            val updatedAnalysis = MasterEnsemble.runMasterEnsemble(records, _uiState.value.learnState, value)
            _uiState.update { it.copy(analysis = updatedAnalysis) }
        }
    }

    fun clearLedger() {
        _uiState.update {
            it.copy(
                ledger = emptyList(),
                streaks = MasterEnsemble.computeStreakStats(emptyList()),
            )
        }
        viewModelScope.launch { prefs.saveLedger(emptyList()) }
        showToast("Ledger Cleared", "Round history and streak counters reset.", "info")
    }

    fun resetLearning() {
        val baseline = LearnState()
        _uiState.update { it.copy(learnState = baseline) }
        viewModelScope.launch { prefs.saveLearnState(baseline) }
        val records = _uiState.value.records
        if (records.isNotEmpty()) {
            processRecords(records)
        }
        showToast("Self-Learning Reset", "Engine weights restored to baseline.", "info")
    }

    fun dismissToast() {
        _uiState.update { it.copy(toast = null) }
    }

    private fun showToast(title: String, detail: String, tone: String) {
        val id = System.currentTimeMillis()
        _uiState.update { it.copy(toast = ToastEvent(id, title, detail, tone)) }
        viewModelScope.launch {
            delay(3000L)
            if (_uiState.value.toast?.id == id) {
                _uiState.update { it.copy(toast = null) }
            }
        }
    }
}
