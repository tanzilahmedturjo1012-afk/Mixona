package com.example.engine

import com.example.model.DrawRecord
import com.example.model.GokuAnalysis
import com.example.model.HidanAnalysis
import com.example.model.HidanSignals
import kotlin.math.abs
import kotlin.math.min

object HidanEngine {

    fun predict(history: List<DrawRecord>): HidanAnalysis {
        val nums = history.map { it.number }
        var up = 0
        var down = 0
        val span = min(18, nums.size)
        for (i in 1 until span) {
            if (nums[i - 1] > nums[i]) up++
            else if (nums[i - 1] < nums[i]) down++
        }
        val crm = if (up >= down) "BIG" else "SMALL"

        val last12 = nums.take(12)
        val big12 = last12.count { it >= 5 }
        val small12 = last12.size - big12
        val psm = when {
            big12 - small12 >= 3 -> "SMALL"
            small12 - big12 >= 3 -> "BIG"
            big12 >= small12 -> "BIG"
            else -> "SMALL"
        }

        var lastMirror: Int? = null
        for (i in 1 until nums.size) {
            if (abs(nums[i - 1] - nums[i]) == 5) {
                lastMirror = nums[i - 1]
                break
            }
        }
        val mdr = if (lastMirror == null) {
            if (nums[0] >= 5) "SMALL" else "BIG"
        } else {
            if (lastMirror >= 5) "SMALL" else "BIG"
        }

        val prediction = if (listOf(crm, psm, mdr).count { it == "BIG" } >= 2) "BIG" else "SMALL"
        var conf = 45
        if (crm == prediction) conf += 18
        if (psm == prediction) conf += 18
        if (mdr == prediction) conf += 18

        val lastIndex = IntArray(10) { 999 }
        for (i in nums.indices.reversed()) {
            lastIndex[nums[i]] = i
        }
        var coldest = 0
        var hottest = 0
        for (n in 1..9) {
            if (lastIndex[n] > lastIndex[coldest]) coldest = n
            if (lastIndex[n] < lastIndex[hottest]) hottest = n
        }
        val n1 = coldest
        val n2 = if (hottest != n1) hottest else (n1 + 5) % 10

        val oppPool = if (prediction == "BIG") listOf(0, 1, 2, 3, 4) else listOf(5, 6, 7, 8, 9)
        val recent10 = nums.take(10)
        val opposite = oppPool.firstOrNull { recent10.contains(it) } ?: oppPool[0]

        return HidanAnalysis(
            prediction = prediction,
            confidencePct = min(conf, 99),
            signals = HidanSignals(crm = crm, psm = psm, mdr = mdr),
            numbers = listOf(n1, n2),
            opposite = opposite,
        )
    }
}

object GokuAuraEngine {

    private val AURA_PATTERNS: Map<String, String> = mapOf(
        "BSBSBBBBS" to "1", "SBSBBBBSB" to "1", "BSBBBBSBB" to "1", "SBBBBSBBB" to "1", "BBBBBSBBBB" to "2",
        "BBBSBBBBS" to "2", "BBSBBBBSS" to "1", "BSBBBBSSS" to "2", "SBBBBSSSS" to "2", "BBBBSSSSS" to "1",
        "BBBSSSSSS" to "2", "BBSSSSSSS" to "2", "BSSSSSSSB" to "1", "SSSSSSSBS" to "1", "SSSSSSBSB" to "1",
        "SSSSSBSBB" to "2", "SSSSBSBBS" to "2", "SSSBSBBSS" to "2", "SSBSBBSSS" to "1", "SBSBBSSSB" to "1",
        "BSBBSSSBB" to "1", "SBBSSSBBB" to "1", "BBSSSBBBS" to "1", "BSSSBBBSB" to "2", "SSSBBBSBB" to "2",
        "SSBBBSBBS" to "2", "SBBBSBBSS" to "2", "BBBSBBSSS" to "1", "BBSBBSSSB" to "2", "BBSSSBBBB" to "1",
        "BSSSBBBBB" to "1", "SSSBBBBBS" to "2", "SSBBBBBSS" to "2", "SBBBBBSSS" to "1", "BBBBBSSSS" to "2",
        "BBBBSSSSB" to "1", "BBBSSSSBB" to "2", "BBSSSSBBS" to "1", "BSSSSBBSS" to "2", "SSSSBBSSB" to "2",
        "SSSBBSSBB" to "2", "SSBBSSBBS" to "1", "SBBSSBBSS" to "1", "BBSSBBSSS" to "2", "BSSBBSSSS" to "2",
        "SSBBSSSSB" to "2", "SBBSSSSBB" to "2", "BBSSSSBBB" to "2", "BSSSSBBBS" to "2", "SSSSBBBSS" to "1",
        "SSSBBBSSS" to "1", "SSBBBSSSB" to "2", "SBBBSSSBB" to "2", "BBBSSSBBS" to "1", "BBSSSBBSB" to "2",
        "BSSSBBSBS" to "2", "SSSBBSBSB" to "1", "SSBBSBSBS" to "1", "SBBSBSBSB" to "2", "BBSBSBSBB" to "2",
        "BSBSBSBBS" to "2", "SBSBSBBSS" to "1", "BSBSBBSSB" to "2", "SBSBBSSBS" to "1", "BSBBSSBSB" to "2",
        "SBBSSBSBB" to "2", "BBSSBSBBS" to "2", "BSSBSBBSB" to "1", "SSBSBBSBS" to "1", "SBSBBSBSS" to "1",
        "BSBBSBSSB" to "1", "SBBSBSSBS" to "1", "BBSBSSBSB" to "1", "BSBSSBSBB" to "1", "SBSSBSBBS" to "1",
        "SSBSBBSBB" to "1", "SBSBBSBBS" to "1", "BSBBSBBSS" to "1", "SBBSBBSSB" to "1", "BBSBBSSBS" to "1",
        "SBBSSBSBS" to "2", "BBSSBSBSS" to "2", "BSSBSBSSB" to "1", "SSBSBSSBS" to "2", "SBSBSSBSS" to "2",
        "BSBSSBSSS" to "2", "SBSSBSSSS" to "1", "BSSBSSSSB" to "1", "SSBSSSSBB" to "2", "SBSSSSBBS" to "2",
        "SSSSBBSSS" to "2", "SSSBBSSSS" to "2", "BSSSSBBSB" to "1", "SSSSBBSBB" to "1", "SSSBBSBBB" to "2",
        "SSBBSBBBS" to "1", "SBBSBBBSS" to "2", "BBSBBBSSB" to "1", "BSBBBSSBB" to "1", "SBBBSSBBB" to "2",
        "BBBSSBBBB" to "1", "BBSSBBBBS" to "2", "BSSBBBBSS" to "2", "SSBBBBSSB" to "2", "SBBBBSSBB" to "2",
        "BBBBSSBBS" to "1", "BBBSSBBSB" to "1", "BBSSBBSBB" to "1", "BSSBBSBBB" to "2", "SBBSBBBSB" to "2",
        "BBSBBBSBS" to "2", "BSBBBSBSS" to "1", "SBBBSBSSB" to "2", "BBBSBSSBB" to "1", "BBSBSSBBB" to "2",
        "BSBSSBBBB" to "1", "SBSSBBBBS" to "1", "SSBBBBSSS" to "2", "BBBSSSSSB" to "2", "BBSSSSSBS" to "2",
        "BSSSSSBSS" to "1", "SSSSSBSSS" to "1", "SSSSBSSSB" to "1", "SSSBSSSBS" to "1", "SSBSSSBSB" to "2",
        "SBSSSBSBS" to "1", "BSSSBSBSB" to "1", "SSSBSBSBS" to "2", "SSBSBSBSS" to "2", "SBSBSBSSS" to "2",
        "BSBSBSSSS" to "2", "SBSBSSSSB" to "2", "BSBSSSSBS" to "2", "SBSSSSBSS" to "1", "BSSSSBSSB" to "2",
        "SSSSBSSBB" to "1", "SSSBSSBBS" to "2", "SSBSSBBSS" to "1", "SBSSBBSSS" to "1", "SSBBSSSSS" to "1",
        "SBBSSSSSS" to "1", "BSSSSSSSS" to "2", "SSSSSSSSB" to "1", "SSSSSSSBB" to "1", "SSSSSSBBB" to "2",
        "SSSSSBBBS" to "1", "SBBBSSSBS" to "2", "BBBSSSBSS" to "2", "BBSSSBSSS" to "1", "BSSSBSSSS" to "1",
        "SSSBSSSSB" to "2", "SSBSSSSBS" to "2", "SBSSSSBSB" to "2", "BSSSSBSBS" to "2", "SSSSBSBSS" to "1",
        "SSSBSBSSB" to "1", "SSBSBSSBB" to "1", "SBSBSSBBS" to "2", "BSBSSBBSS" to "2", "SBBSSSSSB" to "1",
        "BBSSSSSBB" to "2", "BSSSSSBBB" to "2", "SSSBBBSSB" to "2", "SSBBBSSBS" to "1", "SBBBSSBSB" to "2",
        "BBBSSBSBS" to "2", "SBSBSSBBB" to "1", "BSBSSBBBS" to "1", "SBSSBBBSB" to "1", "BSSBBBSBS" to "1",
        "SSBBBSBSS" to "1", "BBBSBSSBS" to "2", "SBSSBSBBB" to "2", "BSSBSBBBB" to "1", "SSBSBBBBS" to "2",
        "SBSBBBBSS" to "1", "BSBBBBSSB" to "2", "SBBBBSSBS" to "1", "BBBBSSBSS" to "1", "BBBSSBSSS" to "1",
        "BBSSBSSSB" to "1", "BSSBSSSBB" to "2", "SSBSSSBBS" to "1", "SBSSSBBSB" to "2", "SSSBBSBSS" to "2",
        "SSBBSBSSS" to "1", "SBBSBSSSB" to "1", "BBSBSSSBS" to "1", "BSBSSSBSB" to "1", "SBSSSBSBB" to "2",
        "BSSSBSBBS" to "1", "SSSBSBBSB" to "1", "BSBBSBBBB" to "1", "SBBSBBBBS" to "2", "BBBSSBSSB" to "2",
        "BBSSBSSBS" to "1", "BSSBSSBSB" to "1", "SSBSSBSBB" to "1", "BSSBSBBBS" to "2", "SSBSBBBSB" to "2",
        "SBSBBBSBS" to "2", "SBSSBBBBB" to "2", "BSSBBBBBB" to "1", "SSBBBBBBS" to "1", "SBBBBBBSS" to "2",
        "BBBBBBSSB" to "2", "BBBBBSSBB" to "1", "BBBBSSBBB" to "1", "BBSSBBBBB" to "2", "BBBBBBSSS" to "2",
        "BBBBBSSSB" to "1", "BBBBSSSBS" to "2", "BBBSSSBSB" to "2", "BBSSSBSBB" to "1", "BSSSBSBBB" to "1",
        "SSSBSBBBB" to "1", "SSBSBBBBB" to "2", "SBSBBBBBS" to "1", "BSBBBBBSS" to "1", "SBBBBBSSB" to "2",
        "BBBBBSSBS" to "1", "BBBBSSBSB" to "1", "BBBSSBSBB" to "2", "BBSSBSBBB" to "2", "SSBSBBBSS" to "2",
        "SBSBBBSSS" to "1", "BSBBBSSSB" to "2", "SBSSSSBSS" to "2", "BSSSSBSSS" to "2", "SSSSBSSSS" to "2",
        "SSSBSSSSS" to "2", "SSBSSSSSS" to "1", "SBSSSSSSB" to "2", "BSSSSSSBS" to "2", "SSSSSBSBB" to "1",
        "SSSSBSBBB" to "2", "SSSBSBBBS" to "2", "SSBSBBBSS" to "1", "SBSBBBSSB" to "2", "BSSBBBBBS" to "2",
        "BBBBSSSBB" to "1", "BBBSSSBBB" to "1", "SSSBBBBBB" to "2", "SBBBBBBSB" to "1", "BBBBBBSBB" to "2",
        "BBBBBSBBB" to "1", "BBBBSBBBB" to "1", "BBBSBBBBB" to "1", "BBSBBBBBB" to "1", "BSBBBBBBB" to "1",
        "SBBBBBBBS" to "1", "BBBBBBBSS" to "2", "BBBSSBBBS" to "1", "BBSSBBBSB" to "1", "SSBBBSBSB" to "2",
        "SBBBSBSBB" to "1", "BBBSBSBBB" to "2", "BBSBSBBBS" to "2", "BSBSBBBSB" to "2", "SBSBBBSBB" to "2",
        "BSBBBSBBS" to "1", "SBBBSBBSB" to "2", "BBBSBBSBS" to "1", "BBSBBSBSS" to "1", "BSBBSBSSS" to "1",
        "BBSBSSSBB" to "2", "BSBSSSBBS" to "2", "SBSSSBBSS" to "2", "BSSSBBSSB" to "1", "SBBSSBBSB" to "2",
        "BBSBBBSSS" to "1", "SBBBSSSBS" to "1", "BBSSSBSBS" to "1", "BSSSBSBSS" to "2", "SSSBSBSSS" to "2",
        "SSBSBSSSS" to "1", "SBSBSSSSB" to "1", "BSBSSSSBB" to "2", "SSSBBSSBS" to "1", "SSBBSSBSB" to "1",
        "SSSSSSSSS" to "1", "SSSSSSSBB" to "2", "SSSSSSBBS" to "1", "SSSSSBBSB" to "2", "SSSSBBSBS" to "1",
        "SSBBSBSBB" to "1", "SBBSBSBBB" to "2", "BSBSBBBSS" to "1", "BSBBBSSBS" to "1", "SSBSBSSSB" to "1",
        "SBSBSSSBB" to "2", "BSSSBBSSS" to "1", "SSSBBSSSB" to "2", "SSBBSSSBS" to "1", "SBBSSSBSS" to "1",
        "BBSSSBSSB" to "1", "BSSSBSSBB" to "1", "SBSSBBSSB" to "1", "BSSBBSSBB" to "1", "SSBBSSBBB" to "1",
        "SBBSSBBBB" to "2", "SSBBBBBBB" to "2", "BBBBBBBSB" to "2", "BBBBBSBBS" to "2", "BBBBSBBSS" to "2",
        "BSBBSSSBS" to "2", "SSSBSSBBB" to "2", "SSBSSBBBS" to "1", "SBBBSBSBS" to "1", "BBBSBSBSS" to "1",
        "BBSBSBSSB" to "1", "BSBSBSSBB" to "1", "SBSBBSSBB" to "1", "BSBBSSBBB" to "2", "SBBSSBBBS" to "1",
        "BSSBBBSBB" to "1", "SSBBBSBBB" to "2", "SBBBSBBBS" to "1", "BBBSBBBSB" to "1", "BBBSBSSBB" to "2",
        "BBSBSSBBS" to "2", "BSBSSBBSB" to "1", "SBSSBBSBB" to "2", "BSSBBSBBS" to "2", "SSBBSBBSS" to "1",
        "SBBSBBSSS" to "1", "BSBBSSSBB" to "2", "SBBSSSBBS" to "2", "BBSBSBSBS" to "2", "BSBSBSBSS" to "1",
        "SBSBSSSSS" to "2", "BSBSSSSSS" to "2", "SBSSSSSSS" to "1", "SSSSBBBSB" to "1", "SSSBBBSBS" to "1",
        "BBSBBBSBB" to "1", "BSBBBSBBB" to "1", "SBBBSBBBB" to "1", "SSSBSBSBB" to "1", "SSBSBSBBB" to "1",
        "SBSBSBBBB" to "2", "BSBSBBBBB" to "1", "SBSBBBBBB" to "1", "BBBBBBSBS" to "2", "BBBBBSBSS" to "2",
        "BBBBSBSSS" to "2", "BBBSBSSSS" to "2", "BBSBSSSSS" to "1", "BSBSSSSSB" to "1", "SBSSSSSBB" to "2",
        "SSBBBSBBS" to "1", "SBBSBSSBB" to "2", "BSSBBSSSB" to "1", "SSBBSSSBB" to "2", "BBBSBSBSB" to "2",
        "SBSBSBSSB" to "1", "SSSSSBSSB" to "1", "BBSSSBBSS" to "2", "SBBSSSBSB" to "2", "BSBBBBSBB" to "2",
        "SBBBBSBBS" to "1", "BBBBSBBSB" to "2", "BBSBBSBSB" to "1", "BSBBSBSBB" to "2", "SBBSBSBBS" to "1",
        "BBSBSBBSB" to "2", "BSBSBBSBB" to "2", "SBBSBBBBB" to "1", "SSSBSSSBB" to "1", "SSBSSSBBB" to "2",
        "SBSSSBBBS" to "1", "BSSBBBBSB" to "1", "SSBBBBSBB" to "1", "SBBBBSBBB" to "2", "BBBBSBBBS" to "2",
        "BBBSBBBSS" to "1", "SBBSSSSBS" to "2", "BBSSSSBSS" to "1", "SSSSBSSBS" to "2", "SSSBSSBSS" to "1",
        "SSBSSBSSB" to "1", "SBSSBSSBB" to "2", "BSSBSSBBS" to "1", "SSBSSBBSB" to "1", "BBSBBSSBB" to "2",
        "BSBBSSBBS" to "1", "BBSSBBSBS" to "2", "BSSBBSBSS" to "1", "SSBBSBSSB" to "1", "BBSSSSSSB" to "2",
        "SSSSSSBSS" to "1", "BBSBSSBSS" to "1", "BSBSSBSSB" to "2", "SBSSBSSBS" to "1", "BSBBBBBSB" to "1",
        "SBBBBBSBB" to "1", "SBBBBBBBB" to "2", "BBBBBBBBS" to "2", "BSSSBSSSB" to "2", "BSBBBBSBS" to "2",
        "SBBBBSBSS" to "2", "BSSSSSBBS" to "2", "SSSSSBBSS" to "1", "BBSSBBSSB" to "2", "BSSBBSSBS" to "1",
        "SSBSBBSBS" to "1", "SBSBBSBSB" to "2", "BSBBSBBSB" to "2", "SBBSBBSBS" to "1", "BSBSBBSBS" to "1",
        "BSBBSBSBS" to "2", "SBBSBSBSS" to "2", "BBSBSBSSS" to "2",
    )

    private val GOKU_PATTERNS: List<List<String>> = listOf(
        listOf("BIG", "BIG", "SMALL", "BIG", "SMALL", "BIG"),
        listOf("SMALL", "BIG", "BIG", "SMALL", "BIG", "SMALL"),
        listOf("BIG", "SMALL", "BIG", "BIG", "SMALL", "SMALL"),
        listOf("SMALL", "SMALL", "BIG", "BIG", "BIG", "SMALL"),
        listOf("BIG", "BIG", "BIG", "SMALL", "SMALL", "BIG"),
        listOf("SMALL", "BIG", "SMALL", "BIG", "BIG", "SMALL"),
        listOf("BIG", "SMALL", "SMALL", "BIG", "BIG", "SMALL"),
        listOf("SMALL", "BIG", "BIG", "BIG", "SMALL", "BIG"),
        listOf("BIG", "SMALL", "BIG", "SMALL", "BIG", "BIG"),
        listOf("SMALL", "SMALL", "BIG", "SMALL", "BIG", "BIG"),
        listOf("BIG", "BIG", "BIG", "BIG", "BIG", "BIG", "BIG", "BIG"),
        listOf("SMALL", "SMALL", "SMALL", "SMALL", "SMALL"),
    )

    val totalAuraPatterns: Int = AURA_PATTERNS.size

    private data class Decision(
        val size: String,
        val confidence: Int,
        val name: String,
        val source: String,
        val len: Int = 0,
    )

    fun predict(history: List<DrawRecord>): GokuAnalysis {
        if (history.size < 3) {
            return GokuAnalysis(
                prediction = "BIG",
                confidencePct = 60,
                name = "WARMUP",
                source = "TREND",
                numbers = listOf(7, 8),
            )
        }

        val sidesNewest = history.map { it.size.uppercase() }
        val chrono = sidesNewest.reversed()

        var bigStreak = 0
        var smallStreak = 0
        for (side in sidesNewest) {
            if (side == "BIG") bigStreak++ else break
        }
        for (side in sidesNewest) {
            if (side == "SMALL") smallStreak++ else break
        }

        var decision: Decision? = when {
            bigStreak in 3..8 -> Decision("BIG", min(95, 75 + bigStreak * 3), "DRAGON RIDE (${bigStreak}x BIG)", "GOKU STREAK")
            bigStreak >= 9 -> Decision("SMALL", 96, "BIG EXHAUSTION (${bigStreak}x)", "GOKU STREAK")
            smallStreak in 2..4 -> Decision("SMALL", 78 + smallStreak * 4, "SMALL FLOW (${smallStreak}x SMALL)", "GOKU STREAK")
            smallStreak >= 5 -> Decision("BIG", 94, "SMALL EXHAUSTION (${smallStreak}x)", "GOKU STREAK")
            else -> null
        }

        if (decision == null) {
            var best: Decision? = null
            for (pat in GOKU_PATTERNS) {
                var len = min(pat.size - 1, chrono.size)
                while (len >= 3) {
                    val tail = chrono.takeLast(len)
                    var ok = true
                    for (i in 0 until len) {
                        if (tail[i] != pat[i]) {
                            ok = false
                            break
                        }
                    }
                    if (ok && pat.getOrNull(len) != null && (best == null || len > best.len)) {
                        best = Decision(
                            size = pat[len],
                            confidence = 70 + len * 5,
                            name = "GOKU SEQ [$len MATCH]",
                            source = "GOKU",
                            len = len,
                        )
                    }
                    len--
                }
            }
            decision = best
        }

        if (decision == null) {
            val pat9 = sidesNewest.take(9).joinToString("") { if (it == "BIG") "B" else "S" }
            val exactVal = AURA_PATTERNS[pat9]
            if (exactVal != null) {
                decision = Decision(
                    size = if (exactVal == "1") "SMALL" else "BIG",
                    confidence = 88,
                    name = "AURA [$pat9]",
                    source = "AURA",
                )
            } else {
                val pat8 = pat9.take(8)
                for ((key, value) in AURA_PATTERNS) {
                    if (key.startsWith(pat8)) {
                        decision = Decision(
                            size = if (value == "1") "SMALL" else "BIG",
                            confidence = 82,
                            name = "AURA 8-MATCH [$pat8]",
                            source = "AURA",
                        )
                        break
                    }
                }
            }
        }

        if (decision == null) {
            var alt = true
            var i = 1
            while (i < 4 && i < sidesNewest.size) {
                if (sidesNewest[i] == sidesNewest[i - 1]) {
                    alt = false
                    break
                }
                i++
            }
            decision = if (alt) {
                Decision(
                    size = if (sidesNewest[0] == "BIG") "SMALL" else "BIG",
                    confidence = 84,
                    name = "ZIG-ZAG 1x1 FLOW",
                    source = "TREND",
                )
            } else {
                val bigs = sidesNewest.take(5).count { it == "BIG" }
                Decision(
                    size = if (bigs >= 3) "BIG" else "SMALL",
                    confidence = 72,
                    name = "MOMENTUM FLOW",
                    source = "TREND",
                )
            }
        }

        val freq = IntArray(10)
        history.take(30).forEach { h ->
            if (h.number in 0..9) freq[h.number]++
        }
        val pool = (if (decision.size == "BIG") listOf(5, 6, 7, 8, 9) else listOf(0, 1, 2, 3, 4))
            .sortedByDescending { freq[it] }

        return GokuAnalysis(
            prediction = decision.size,
            confidencePct = decision.confidence,
            name = decision.name,
            source = decision.source,
            numbers = listOf(pool[0], pool[1]),
        )
    }
}
