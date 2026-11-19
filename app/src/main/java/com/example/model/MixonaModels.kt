package com.example.model

enum class ApiFeedMode(val id: String, val label: String) {
    AUTO("auto", "Auto Feed: Sky 30s + AR 1M"),
    SKY_30S("sky30s", "Sky WinGo 30s"),
    AR_1M("ar1m", "AR-Lottery 1M");

    companion object {
        fun fromId(id: String): ApiFeedMode = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

enum class ConnectionState {
    CONNECTING,
    LIVE,
    DEMO,
    ERROR,
}

enum class HistoryFilter(val label: String) {
    ALL("All"),
    BIG("Big (5–9)"),
    SMALL("Small (0–4)"),
}

data class DrawRecord(
    val number: Int,
    val size: String,
    val period: String,
)

data class RateWindow(
    val w: Int,
    val p: Double,
)

data class ContextStat(
    val key: String,
    val n: Int,
    val p: Double?,
    val all: Int,
    val k: Int,
    val shrunk: Double,
)

data class TransitionStat(
    val k: Int,
    val p: Double,
    val n: Int,
)

data class PhantomComponent(
    val name: String,
    val p: Double,
    val w: Double,
)

data class NumberProb(
    val n: Int,
    val p: Double,
)

data class BacktestResult(
    val name: String,
    val correct: Int,
    val tested: Int,
    val rate: Double,
)

data class PhantomAnalysis(
    val nums: List<Int>,
    val bits: List<Int>,
    val rates: List<RateWindow>,
    val shortTrendP: Double,
    val longTrendP: Double,
    val r5: Double,
    val r10: Double,
    val r30: Double,
    val r50: Double,
    val r100: Double,
    val streak: Int,
    val alternation: Double,
    val trendStrength: Double,
    val regime: String,
    val entropyVal: Double,
    val ctx: List<ContextStat>,
    val trans: List<TransitionStat>,
    val components: List<PhantomComponent>,
    val p: Double,
    val rank: List<NumberProb>,
    val prediction: String,
    val confidence: Double,
    val numbers: List<Int>,
    val totalCount: Int,
)

data class FactorScore(
    val big: Double,
    val small: Double,
)

data class RenoxStreak(
    val count: Int,
    val side: String,
)

data class RangeScore(
    val n: Int,
    val score: Double,
)

data class RenoxAnalysis(
    val prediction: String,
    val bigProb: Double,
    val smallProb: Double,
    val confidence: Double,
    val confidencePct: Int,
    val isJackpot: Boolean,
    val numbers: List<Int>,
    val rangeScores: List<RangeScore>,
    val numberReason: String,
    val factors: Map<String, FactorScore>,
    val agreement: Int,
    val streak: RenoxStreak,
    val hotNumber: Int,
    val coldNumber: Int,
)

data class HidanSignals(
    val crm: String,
    val psm: String,
    val mdr: String,
)

data class HidanAnalysis(
    val prediction: String,
    val confidencePct: Int,
    val signals: HidanSignals,
    val numbers: List<Int>,
    val opposite: Int,
)

data class GokuAnalysis(
    val prediction: String,
    val confidencePct: Int,
    val name: String,
    val source: String,
    val numbers: List<Int>,
)

data class EngineStats(
    val w: Int = 0,
    val l: Int = 0,
)

data class MistakeSignature(
    val key: String,
    val side: String,
    val losses: Int,
    val cooldown: Int,
    val lastAt: Long = 0L,
)

data class LearnState(
    val weights: Map<String, Double> = mapOf(
        "phantom" to 0.28,
        "renox" to 0.27,
        "hidan" to 0.20,
        "goku" to 0.25,
    ),
    val stats: Map<String, EngineStats> = mapOf(
        "phantom" to EngineStats(),
        "renox" to EngineStats(),
        "hidan" to EngineStats(),
        "goku" to EngineStats(),
    ),
    val mistakes: Map<String, MistakeSignature> = emptyMap(),
    val guardSaves: Int = 0,
    val guardTotal: Int = 0,
)

data class TrendSignature(
    val pat3: String,
    val shortDir: String,
    val longDir: String,
    val trendKey: String,
    val regimeKey: String,
)

data class EngineVote(
    val id: String,
    val label: String,
    val pred: String,
    val conf: Int,
    val weight: Double,
    val nums: List<Int>,
)

data class RankedNumber(
    val n: Int,
    val pct: Double,
)

data class MasterAnalysis(
    val action: String,
    val rawMasterSize: String,
    val masterConfidence: Int,
    val agreementCount: Int,
    val guardTriggered: Boolean,
    val guardReason: String,
    val exactKey: String,
    val broadKey: String,
    val sig: TrendSignature,
    val shortCall: String,
    val longCall: String,
    val trendAligned: Boolean,
    val isJackpotSignal: Boolean,
    val masterNumbers: List<Int>,
    val rankedNumbers: List<RankedNumber>,
    val oppositeNumber: Int,
    val phantom: PhantomAnalysis,
    val renox: RenoxAnalysis,
    val hidan: HidanAnalysis,
    val goku: GokuAnalysis,
    val engineVotes: List<EngineVote>,
    val backtests: List<BacktestResult>,
)

data class LedgerEntry(
    val period: String,
    val action: String,
    val rawMasterSize: String,
    val masterNums: List<Int>,
    val oppNum: Int?,
    val trendContext: String,
    val exactKey: String,
    val broadKey: String,
    val engineCalls: Map<String, String>,
    val actualNum: Int?,
    val actualSize: String?,
    val status: String,
    val at: Long,
)

data class StreakStats(
    val settled: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val jackpots: Int = 0,
    val winRate: Int = 0,
    val maxWin: Int = 0,
    val maxLoss: Int = 0,
    val currentStreak: String = "—",
)
