package com.example.engine

import com.example.model.BacktestResult
import com.example.model.ContextStat
import com.example.model.DrawRecord
import com.example.model.FactorScore
import com.example.model.NumberProb
import com.example.model.PhantomAnalysis
import com.example.model.PhantomComponent
import com.example.model.RangeScore
import com.example.model.RateWindow
import com.example.model.RenoxAnalysis
import com.example.model.RenoxStreak
import com.example.model.TransitionStat
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private fun clamp(x: Double, a: Double = 0.0, b: Double = 1.0): Double = max(a, min(b, x))

object PhantomEngine {

    private data class FingerprintResult(
        val key: String,
        val n: Int,
        val p: Double?,
        val all: Int,
    )

    private fun entropy(bits: List<Int>): Double {
        if (bits.isEmpty()) return 0.0
        val p = bits.count { it != 0 }.toDouble() / bits.size
        if (p == 0.0 || p == 1.0) return 0.0
        return -(p * log2(p) + (1.0 - p) * log2(1.0 - p))
    }

    private fun fingerprintStats(bits: List<Int>, k: Int): FingerprintResult {
        val map = LinkedHashMap<String, MutableList<Int>>()
        var i = 1
        while (i + k <= bits.size) {
            val key = bits.subList(i, i + k).joinToString("")
            map.getOrPut(key) { mutableListOf() }.add(if (bits[i - 1] != 0) 1 else 0)
            i++
        }
        val cur = bits.take(k).joinToString("")
        val vals = map[cur].orEmpty()
        return FingerprintResult(
            key = cur,
            n = vals.size,
            p = if (vals.isNotEmpty()) vals.sum().toDouble() / vals.size else null,
            all = map.size,
        )
    }

    private fun weightedRate(bits: List<Int>, window: Int): Double {
        val s = bits.take(window)
        if (s.isEmpty()) return 0.5
        var a = 0.0
        var b = 0.0
        for (i in s.indices) {
            val w = exp(-i.toDouble() / max(2.0, window.toDouble() / 3.0))
            a += s[i] * w
            b += w
        }
        return a / b
    }

    private fun transitionRate(bits: List<Int>, k: Int): Pair<Double, Int> {
        var hit = 0
        var total = 0
        val cur = bits.take(k).joinToString("")
        var i = 1
        while (i + k <= bits.size) {
            val key = bits.subList(i, i + k).joinToString("")
            if (key == cur) {
                hit += if (bits[i - 1] != 0) 1 else 0
                total++
            }
            i++
        }
        return (if (total > 0) hit.toDouble() / total else 0.5) to total
    }

    fun analyse(data: List<DrawRecord>): PhantomAnalysis {
        val nums = data.map { it.number }
        val bits = nums.map { if (it >= 5) 1 else 0 }
        val nTotal = bits.size

        val rates = listOf(5, 10, 20, 30, 50, 100).map { w -> RateWindow(w, weightedRate(bits, w)) }
        val r5 = rates[0].p
        val r10 = rates[1].p
        val r20 = rates[2].p
        val r30 = rates[3].p
        val r50 = rates[4].p
        val r100 = rates[5].p

        var streak = if (nTotal > 0) 1 else 0
        while (streak < nTotal && bits[streak] == bits[0]) streak++

        var flips = 0
        val altSpan = max(1, min(nTotal - 1, 30))
        for (i in 0 until altSpan) {
            if (i + 1 < nTotal && bits[i] != bits[i + 1]) flips++
        }
        val alternation = flips.toDouble() / altSpan
        val trendStrength = abs(r10 - 0.5) * 2.0
        val entropyVal = if (nTotal > 0) entropy(bits.take(30)) else 0.0

        val regime = when {
            entropyVal > 0.96 && alternation > 0.42 && alternation < 0.68 -> "CHAOTIC"
            alternation > 0.72 -> "ALTERNATING"
            streak >= 4 -> "STREAK / CONTINUATION"
            abs(r10 - r100) > 0.22 -> "REGIME SHIFT"
            trendStrength > 0.24 -> "TRENDING"
            else -> "MIXED / STABLE"
        }

        val ctx = listOf(3, 4, 5, 6, 8).map { k ->
            val f = fingerprintStats(bits, k)
            val raw = f.p ?: 0.5
            val shrunk = (raw * f.n + 0.5 * 6.0) / (f.n + 6.0)
            ContextStat(
                key = f.key,
                n = f.n,
                p = f.p,
                all = f.all,
                k = k,
                shrunk = shrunk,
            )
        }

        val trans = listOf(2, 3, 4, 5).map { k ->
            val (pVal, nVal) = transitionRate(bits, k)
            TransitionStat(k = k, p = pVal, n = nVal)
        }

        val shortTrendP = r5 * 0.40 + r10 * 0.35 + r20 * 0.25
        val longTrendP = r30 * 0.40 + r50 * 0.35 + r100 * 0.25

        val b0 = bits.firstOrNull() ?: 0
        val runAltP = when {
            alternation > 0.72 -> (1 - b0) * 0.68 + b0 * 0.32
            streak >= 2 -> b0 * 0.62 + (1 - b0) * 0.38
            else -> 0.5
        }
        val macroRevP = if (abs(r10 - r100) > 0.22) {
            if (r10 > r100) 0.38 else 0.62
        } else {
            0.5
        }

        val components = listOf(
            PhantomComponent("Short Trend (5–20)", shortTrendP, 0.22),
            PhantomComponent("Long Trend (30–100)", longTrendP, 0.16),
            PhantomComponent("Context Fingerprints", ctx.sumOf { it.shrunk } / ctx.size, 0.26),
            PhantomComponent(
                "Forward Transitions",
                trans.sumOf { (it.p * it.n + 0.5 * 5.0) / (it.n + 5.0) } / trans.size,
                0.16,
            ),
            PhantomComponent("Run / Alternation", runAltP, 0.12),
            PhantomComponent("Macro Reversion", macroRevP, 0.08),
        )

        val totalW = components.sumOf { it.w }
        val p = components.sumOf { it.p * it.w } / totalW

        val score = DoubleArray(10) { 1.5 }
        for (i in 1 until nums.size) {
            val w = exp(-i.toDouble() / 38.0)
            score[nums[i - 1]] += w * 0.35
            for (k in listOf(2, 3, 4)) {
                if (i + k <= nums.size && nums.subList(i, i + k) == nums.take(k)) {
                    score[nums[i - 1]] += w * (1.2 / k.toDouble())
                }
            }
        }
        val sum = score.sum()
        val rank = score.mapIndexed { n, s -> NumberProb(n, s / sum) }
            .sortedByDescending { it.p }

        return PhantomAnalysis(
            nums = nums,
            bits = bits,
            rates = rates,
            shortTrendP = shortTrendP,
            longTrendP = longTrendP,
            r5 = r5,
            r10 = r10,
            r30 = r30,
            r50 = r50,
            r100 = r100,
            streak = streak,
            alternation = alternation,
            trendStrength = trendStrength,
            regime = regime,
            entropyVal = entropyVal,
            ctx = ctx,
            trans = trans,
            components = components,
            p = p,
            rank = rank,
            prediction = if (p >= 0.5) "BIG" else "SMALL",
            confidence = abs(p - 0.5) * 2.0,
            numbers = listOf(rank[0].n, rank[1].n),
            totalCount = nTotal,
        )
    }

    fun backtest(nums: List<Int>): List<BacktestResult> {
        val chronoBits = nums.map { if (it >= 5) 1 else 0 }.reversed()

        val methods: List<Pair<String, (List<Int>) -> Int>> = listOf(
            "Short+Long Weighted" to { h ->
                val v = weightedRate(h, min(10, h.size)) * 0.6 + weightedRate(h, min(40, h.size)) * 0.4
                if (v >= 0.5) 1 else 0
            },
            "Forward Fingerprint" to { h ->
                val c = mutableListOf<Double>()
                for (k in listOf(3, 4, 5)) {
                    val f = fingerprintStats(h, k)
                    if (f.n >= 2 && f.p != null) {
                        c.add((f.p * f.n + 1.5) / (f.n + 3.0))
                    }
                }
                val v = if (c.isNotEmpty()) c.sum() / c.size else weightedRate(h, min(15, h.size))
                if (v >= 0.5) 1 else 0
            },
            "Alternation / Streak" to { h ->
                var flips = 0
                val span = max(1, min(12, h.size - 1))
                for (i in 0 until span) {
                    if (i + 1 < h.size && h[i] != h[i + 1]) flips++
                }
                if (flips.toDouble() / span > 0.72) 1 - h[0] else h[0]
            },
        )

        return methods.map { (name, fn) ->
            var correct = 0
            var tested = 0
            for (t in 25 until chronoBits.size) {
                val pastNewestFirst = chronoBits.subList(0, t).reversed()
                val pred = fn(pastNewestFirst)
                if (pred == chronoBits[t]) correct++
                tested++
            }
            BacktestResult(
                name = name,
                correct = correct,
                tested = tested,
                rate = if (tested > 0) correct.toDouble() / tested else 0.0,
            )
        }
    }
}

object RenoxEngine {

    fun predict(history: List<DrawRecord>, jackpotThreshold: Double = 0.75): RenoxAnalysis {
        val nums = history.map { it.number }.filter { it in 0..9 }

        if (nums.size < 10) {
            return RenoxAnalysis(
                prediction = "BIG",
                bigProb = 0.5,
                smallProb = 0.5,
                confidence = 0.5,
                confidencePct = 50,
                isJackpot = false,
                numbers = listOf(7, 8),
                rangeScores = emptyList(),
                numberReason = "Warmup",
                factors = emptyMap(),
                agreement = 0,
                streak = RenoxStreak(0, "—"),
                hotNumber = 0,
                coldNumber = 0,
            )
        }

        val data = nums.take(100)
        val sizes = data.map { if (it >= 5) "BIG" else "SMALL" }
        val total = data.size

        val bigFreq = sizes.count { it == "BIG" }.toDouble() / total
        val smallFreq = 1.0 - bigFreq
        var f1Big = bigFreq
        var f1Small = smallFreq
        if (bigFreq < 0.45) { f1Big += 0.10; f1Small -= 0.05 }
        if (smallFreq < 0.45) { f1Small += 0.10; f1Big -= 0.05 }
        val frequency = FactorScore(clamp(f1Big, 0.01, 0.99), clamp(f1Small, 0.01, 0.99))

        val last10 = sizes.take(10)
        val last10Big = last10.count { it == "BIG" }.toDouble() / last10.size
        val momentum = FactorScore(last10Big, 1.0 - last10Big)

        var sCount = 1
        for (i in 1 until sizes.size) {
            if (sizes[i] == sizes[0]) sCount++ else break
        }
        val streak = RenoxStreak(sCount, sizes[0])
        val f3Big = when {
            sCount >= 4 -> if (sizes[0] == "BIG") 0.25 else 0.75
            sCount == 3 -> if (sizes[0] == "BIG") 0.38 else 0.62
            sCount == 2 -> if (sizes[0] == "BIG") 0.58 else 0.42
            else -> if (sizes[0] == "BIG") 0.52 else 0.48
        }
        val streakFactor = FactorScore(f3Big, 1.0 - f3Big)

        var b2b = 0
        var b2s = 0
        var s2b = 0
        var s2s = 0
        for (i in 1 until sizes.size) {
            if (sizes[i] == "BIG") {
                if (sizes[i - 1] == "BIG") b2b++ else b2s++
            } else {
                if (sizes[i - 1] == "BIG") s2b++ else s2s++
            }
        }
        var f4Big = 0.5
        if (sizes[0] == "BIG" && b2b + b2s > 0) {
            f4Big = b2b.toDouble() / (b2b + b2s)
        } else if (sizes[0] == "SMALL" && s2b + s2s > 0) {
            f4Big = s2b.toDouble() / (s2b + s2s)
        }
        val markov1 = FactorScore(f4Big, 1.0 - f4Big)

        var f5Big = 0.5
        var f5Small = 0.5
        if (sizes.size >= 3) {
            val curPat = "${sizes[0]}-${sizes[1]}"
            var b = 0
            var s = 0
            for (i in 1 until sizes.size - 1) {
                if ("${sizes[i]}-${sizes[i + 1]}" == curPat) {
                    if (sizes[i - 1] == "BIG") b++ else s++
                }
            }
            if (b + s > 0) {
                f5Big = b.toDouble() / (b + s)
                f5Small = s.toDouble() / (b + s)
            }
        }
        val markov2 = FactorScore(f5Big, f5Small)

        var alt = 0
        for (i in 0 until sizes.size - 1) {
            if (sizes[i] != sizes[i + 1]) alt++
        }
        val altRatio = alt.toDouble() / (sizes.size - 1)
        val f6Big = when {
            altRatio > 0.65 -> if (sizes[0] == "BIG") 0.30 else 0.70
            altRatio < 0.35 -> if (sizes[0] == "BIG") 0.65 else 0.35
            else -> 0.5
        }
        val alternation = FactorScore(f6Big, 1.0 - f6Big)

        val recent50 = data.take(50)
        val counts = IntArray(10)
        recent50.forEach { counts[it]++ }
        var hotNum = 0
        var coldNum = 0
        for (n in 1..9) {
            if (counts[n] > counts[hotNum]) hotNum = n
            if (counts[n] < counts[coldNum]) coldNum = n
        }
        var f7Big = 0.5
        var f7Small = 0.5
        if (counts[hotNum] >= 5) {
            if (hotNum >= 5) f7Big += 0.08 else f7Small += 0.08
        }
        if (counts[coldNum] == 0) {
            if (coldNum >= 5) f7Big += 0.10 else f7Small += 0.10
        }
        val hotcold = FactorScore(f7Big, f7Small)

        val ma5Slice = sizes.take(5)
        val ma20Slice = sizes.take(20)
        val ma5 = ma5Slice.count { it == "BIG" }.toDouble() / ma5Slice.size
        val ma20 = ma20Slice.count { it == "BIG" }.toDouble() / ma20Slice.size
        var f8Big = ma5
        var f8Small = 1.0 - ma5
        if (ma5 > ma20 + 0.15) { f8Big += 0.08; f8Small -= 0.04 }
        if (ma5 < ma20 - 0.15) { f8Small += 0.08; f8Big -= 0.04 }
        val ma = FactorScore(max(f8Big, 0.01), max(f8Small, 0.01))

        val factors = linkedMapOf(
            "frequency" to frequency,
            "momentum" to momentum,
            "streak" to streakFactor,
            "markov1" to markov1,
            "markov2" to markov2,
            "alternation" to alternation,
            "hotcold" to hotcold,
            "ma" to ma,
        )
        val weights = mapOf(
            "frequency" to 0.18,
            "momentum" to 0.15,
            "streak" to 0.18,
            "markov1" to 0.15,
            "markov2" to 0.12,
            "alternation" to 0.10,
            "hotcold" to 0.07,
            "ma" to 0.05,
        )

        var bigScore = 0.0
        var smallScore = 0.0
        for ((k, w) in weights) {
            val f = factors.getValue(k)
            bigScore += f.big * w
            smallScore += f.small * w
        }
        val sum = bigScore + smallScore
        val bigProb = bigScore / sum
        val winner = if (bigProb >= 0.5) "BIG" else "SMALL"

        var agree = 0
        for (f in factors.values) {
            if (winner == "BIG" && f.big > f.small) agree++
            if (winner == "SMALL" && f.small > f.big) agree++
        }

        val confidence = min(max(bigProb, 1.0 - bigProb) + (agree / 8.0) * 0.10, 0.95)
        val recent20 = data.take(20)
        val range = if (winner == "BIG") listOf(5, 6, 7, 8, 9) else listOf(0, 1, 2, 3, 4)
        val rangeScores = range.map { n ->
            var sc = 0.0
            val c20 = recent20.count { it == n }
            val c50 = recent50.count { it == n }
            sc += c20 * 0.5
            if (c20 == 0) sc += 3.0
            if (c50 in 2..5) sc += 1.5
            RangeScore(n, sc)
        }.sortedByDescending { it.score }

        return RenoxAnalysis(
            prediction = winner,
            bigProb = bigProb,
            smallProb = 1.0 - bigProb,
            confidence = confidence,
            confidencePct = (confidence * 100.0).roundToInt(),
            isJackpot = confidence >= jackpotThreshold,
            numbers = listOf(rangeScores[0].n, rangeScores[1].n),
            rangeScores = rangeScores,
            numberReason = if (winner == "BIG") "BIG convergence" else "SMALL convergence",
            factors = factors,
            agreement = agree,
            streak = streak,
            hotNumber = hotNum,
            coldNumber = coldNum,
        )
    }
}
