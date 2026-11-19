package com.example.engine

import com.example.model.DrawRecord
import com.example.model.EngineStats
import com.example.model.EngineVote
import com.example.model.LearnState
import com.example.model.LedgerEntry
import com.example.model.MasterAnalysis
import com.example.model.MistakeSignature
import com.example.model.PhantomAnalysis
import com.example.model.RankedNumber
import com.example.model.StreakStats
import com.example.model.TrendSignature
import java.math.BigInteger
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object MasterEnsemble {

    private val ENGINE_IDS = listOf("phantom", "renox", "hidan", "goku")

    fun normalizeWeights(weights: Map<String, Double>): Map<String, Double> {
        val clamped = weights.toMutableMap()
        var sum = 0.0
        for (k in ENGINE_IDS) {
            val v = max(0.10, min(0.45, clamped[k] ?: 0.25))
            clamped[k] = v
            sum += v
        }
        val denom = if (sum == 0.0) 1.0 else sum
        for (k in ENGINE_IDS) {
            clamped[k] = clamped.getValue(k) / denom
        }
        return clamped
    }

    fun buildTrendSignature(phantom: PhantomAnalysis, records: List<DrawRecord>): TrendSignature {
        val pat3 = records.take(3).joinToString("") { if (it.size == "BIG") "B" else "S" }
        val shortDir = when {
            phantom.shortTrendP >= 0.56 -> "BIG_SHORT"
            phantom.shortTrendP <= 0.44 -> "SMALL_SHORT"
            else -> "MIX_SHORT"
        }
        val longDir = when {
            phantom.longTrendP >= 0.55 -> "BIG_LONG"
            phantom.longTrendP <= 0.45 -> "SMALL_LONG"
            else -> "MIX_LONG"
        }
        val trendKey = "${phantom.regime}|$shortDir|$pat3"
        val regimeKey = "${phantom.regime}|$shortDir"
        return TrendSignature(
            pat3 = pat3,
            shortDir = shortDir,
            longDir = longDir,
            trendKey = trendKey,
            regimeKey = regimeKey,
        )
    }

    fun runMasterEnsemble(
        records: List<DrawRecord>,
        learnState: LearnState,
        jackpotThreshold: Double,
    ): MasterAnalysis {
        val phantom = PhantomEngine.analyse(records)
        val renox = RenoxEngine.predict(records, jackpotThreshold)
        val hidan = HidanEngine.predict(records)
        val goku = GokuAuraEngine.predict(records)
        val sig = buildTrendSignature(phantom, records)

        val wPhantom = learnState.weights["phantom"] ?: 0.28
        val wRenox = learnState.weights["renox"] ?: 0.27
        val wHidan = learnState.weights["hidan"] ?: 0.20
        val wGoku = learnState.weights["goku"] ?: 0.25

        val engineVotes = listOf(
            EngineVote("phantom", "Phantom Core", phantom.prediction, (50.0 + phantom.confidence * 50.0).roundToInt(), wPhantom, phantom.numbers),
            EngineVote("renox", "Renox v2.0", renox.prediction, renox.confidencePct, wRenox, renox.numbers),
            EngineVote("hidan", "Hidan Trio", hidan.prediction, hidan.confidencePct, wHidan, hidan.numbers),
            EngineVote("goku", "Goku + Aura", goku.prediction, goku.confidencePct, wGoku, goku.numbers),
        )

        val shortCall = if (phantom.shortTrendP >= 0.5) "BIG" else "SMALL"
        val longCall = if (phantom.longTrendP >= 0.5) "BIG" else "SMALL"
        val trendAligned = shortCall == longCall

        var bigWeight = 0.0
        var smallWeight = 0.0
        for (v in engineVotes) {
            val s = v.weight * (v.conf / 100.0)
            if (v.pred == "BIG") bigWeight += s else smallWeight += s
        }
        if (trendAligned) {
            if (shortCall == "BIG") bigWeight += 0.05 else smallWeight += 0.05
        }

        val totalW = if (bigWeight + smallWeight == 0.0) 1.0 else bigWeight + smallWeight
        val bigProb = bigWeight / totalW
        val rawMasterSize = if (bigProb >= 0.5) "BIG" else "SMALL"
        val agreementCount = engineVotes.count { it.pred == rawMasterSize }
        val masterConfidence = min(
            98,
            (max(bigProb, 1.0 - bigProb) * 100.0 + if (agreementCount == 4) 6.0 else 0.0).roundToInt(),
        )

        val exactKey = "${sig.trendKey}|$rawMasterSize"
        val broadKey = "${sig.regimeKey}|$rawMasterSize"
        val mExact = learnState.mistakes[exactKey]
        val mBroad = learnState.mistakes[broadKey]
        val guardTriggered = (mExact != null && mExact.cooldown > 0) || (mBroad != null && mBroad.cooldown > 0)

        var action = rawMasterSize
        var guardReason = ""
        if (guardTriggered) {
            action = "SKIP"
            val ref = if (mExact != null && mExact.cooldown > 0) mExact else mBroad!!
            guardReason = "Self-Learn Guard: \"${sig.regimeKey}\" (${sig.pat3}) caused ${ref.losses} recent loss(es) on $rawMasterSize — skipping repeat trade until trend shifts."
        }

        val fusedScores = DoubleArray(10)
        phantom.rank.forEach { item -> fusedScores[item.n] += item.p * 32.0 }
        if (renox.numbers.size >= 2) {
            fusedScores[renox.numbers[0]] += 5.4 * (wRenox / 0.25)
            fusedScores[renox.numbers[1]] += 3.8 * (wRenox / 0.25)
        }
        renox.rangeScores.forEach { r -> fusedScores[r.n] += r.score * 0.65 }
        fusedScores[hidan.numbers[0]] += 4.0 * (wHidan / 0.25)
        fusedScores[hidan.numbers[1]] += 3.2 * (wHidan / 0.25)
        fusedScores[goku.numbers[0]] += 4.5 * (wGoku / 0.25)
        fusedScores[goku.numbers[1]] += 3.5 * (wGoku / 0.25)

        for (n in 0..9) {
            if (agreementCount >= 3 && ((rawMasterSize == "BIG" && n >= 5) || (rawMasterSize == "SMALL" && n < 5))) {
                fusedScores[n] += 1.4
            }
        }
        val sumScores = fusedScores.sum()
        val totalScore = if (sumScores == 0.0) 1.0 else sumScores
        val rankedNumbers = fusedScores.mapIndexed { n, s ->
            RankedNumber(n = n, pct = (s / totalScore) * 100.0)
        }.sortedByDescending { it.pct }

        val backtests = PhantomEngine.backtest(phantom.nums)

        return MasterAnalysis(
            action = action,
            rawMasterSize = rawMasterSize,
            masterConfidence = masterConfidence,
            agreementCount = agreementCount,
            guardTriggered = guardTriggered,
            guardReason = guardReason,
            exactKey = exactKey,
            broadKey = broadKey,
            sig = sig,
            shortCall = shortCall,
            longCall = longCall,
            trendAligned = trendAligned,
            isJackpotSignal = !guardTriggered && (renox.isJackpot || (agreementCount == 4 && masterConfidence >= (jackpotThreshold * 100).roundToInt())),
            masterNumbers = listOf(rankedNumbers[0].n, rankedNumbers[1].n),
            rankedNumbers = rankedNumbers,
            oppositeNumber = hidan.opposite,
            phantom = phantom,
            renox = renox,
            hidan = hidan,
            goku = goku,
            engineVotes = engineVotes,
            backtests = backtests,
        )
    }

    fun learnFromSettledRound(
        entry: LedgerEntry,
        actualSize: String,
        learnState: LearnState,
        nowMs: Long = System.currentTimeMillis(),
    ): Pair<String, LearnState> {
        val updatedMistakes = learnState.mistakes.mapValues { (_, sig) ->
            if (sig.cooldown > 0) sig.copy(cooldown = max(0, sig.cooldown - 1)) else sig
        }.toMutableMap()

        val updatedStats = learnState.stats.toMutableMap()
        val updatedWeights = learnState.weights.toMutableMap()

        for (id in ENGINE_IDS) {
            val pred = entry.engineCalls[id] ?: continue
            val prevStat = updatedStats[id] ?: EngineStats()
            val prevWeight = updatedWeights[id] ?: 0.25
            if (pred == actualSize) {
                updatedStats[id] = prevStat.copy(w = prevStat.w + 1)
                updatedWeights[id] = prevWeight * 1.07
            } else {
                updatedStats[id] = prevStat.copy(l = prevStat.l + 1)
                updatedWeights[id] = prevWeight * 0.89
            }
        }
        val normalizedWeights = normalizeWeights(updatedWeights)

        var guardSaves = learnState.guardSaves
        var guardTotal = learnState.guardTotal

        val outcome = if (entry.action == "SKIP") {
            guardTotal++
            if (entry.rawMasterSize != actualSize) {
                guardSaves++
                "SAVED"
            } else {
                updatedMistakes[entry.exactKey]?.let { updatedMistakes[entry.exactKey] = it.copy(cooldown = 0) }
                updatedMistakes[entry.broadKey]?.let { updatedMistakes[entry.broadKey] = it.copy(cooldown = 0) }
                "SKIPPED_WIN"
            }
        } else {
            val won = entry.rawMasterSize == actualSize
            if (!won) {
                val prevExact = updatedMistakes[entry.exactKey] ?: MistakeSignature(
                    key = entry.exactKey,
                    side = entry.rawMasterSize,
                    losses = 0,
                    cooldown = 0,
                )
                updatedMistakes[entry.exactKey] = prevExact.copy(
                    key = entry.exactKey,
                    side = entry.rawMasterSize,
                    losses = prevExact.losses + 1,
                    cooldown = 3,
                    lastAt = nowMs,
                )
                val prevBroad = updatedMistakes[entry.broadKey] ?: MistakeSignature(
                    key = entry.broadKey,
                    side = entry.rawMasterSize,
                    losses = 0,
                    cooldown = 0,
                )
                updatedMistakes[entry.broadKey] = prevBroad.copy(
                    key = entry.broadKey,
                    side = entry.rawMasterSize,
                    losses = prevBroad.losses + 1,
                    cooldown = 1,
                    lastAt = nowMs,
                )
                "LOSS"
            } else {
                updatedMistakes[entry.exactKey]?.let { updatedMistakes[entry.exactKey] = it.copy(cooldown = 0) }
                updatedMistakes[entry.broadKey]?.let { updatedMistakes[entry.broadKey] = it.copy(cooldown = 0) }
                "WIN"
            }
        }

        return outcome to LearnState(
            weights = normalizedWeights,
            stats = updatedStats,
            mistakes = updatedMistakes,
            guardSaves = guardSaves,
            guardTotal = guardTotal,
        )
    }

    fun computeStreakStats(ledger: List<LedgerEntry>): StreakStats {
        val activeSettled = ledger.reversed().filter {
            it.actualNum != null && (it.status == "SURE SHOT" || it.status == "JACKPOT" || it.status == "LOSS")
        }

        var wins = 0
        var losses = 0
        var jackpots = 0
        var curWin = 0
        var curLoss = 0
        var maxWin = 0
        var maxLoss = 0

        for (item in activeSettled) {
            if (item.status == "JACKPOT") jackpots++
            if (item.status == "SURE SHOT" || item.status == "JACKPOT") {
                wins++
                curWin++
                curLoss = 0
                if (curWin > maxWin) maxWin = curWin
            } else {
                losses++
                curLoss++
                curWin = 0
                if (curLoss > maxLoss) maxLoss = curLoss
            }
        }

        val currentStreak = when {
            curWin > 0 -> "${curWin}W"
            curLoss > 0 -> "${curLoss}L"
            else -> "—"
        }
        val winRate = if (activeSettled.isNotEmpty()) ((wins.toDouble() / activeSettled.size) * 100.0).roundToInt() else 0

        return StreakStats(
            settled = activeSettled.size,
            wins = wins,
            losses = losses,
            jackpots = jackpots,
            winRate = winRate,
            maxWin = maxWin,
            maxLoss = maxLoss,
            currentStreak = currentStreak,
        )
    }

    fun computeNextPeriod(period: String): String {
        if (period.isEmpty()) return "NEXT"
        return try {
            if (period.all { it.isDigit() }) {
                (BigInteger(period) + BigInteger.ONE).toString()
            } else {
                val match = Regex("^(.*?)(\\d+)$").find(period)
                if (match != null) {
                    match.groupValues[1] + (match.groupValues[2].toLong() + 1L).toString()
                } else {
                    "$period+1"
                }
            }
        } catch (_: Exception) {
            "$period+1"
        }
    }
}
