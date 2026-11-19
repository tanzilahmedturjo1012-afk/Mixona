package com.example

import com.example.data.MixonaNetwork
import com.example.engine.GokuAuraEngine
import com.example.engine.HidanEngine
import com.example.engine.MasterEnsemble
import com.example.engine.PhantomEngine
import com.example.engine.RenoxEngine
import com.example.model.DrawRecord
import com.example.model.LearnState
import com.example.model.LedgerEntry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MixonaEngineTests {

    private fun sampleDraws(): List<DrawRecord> {
        val nums = listOf(
            7, 8, 2, 6, 9, 1, 4, 8, 5, 3,
            0, 7, 6, 9, 2, 4, 3, 8, 6, 5,
            1, 9, 7, 2, 0, 6, 8, 3, 5, 4,
            9, 7, 1, 2, 6, 8, 4, 0, 5, 9,
        )
        return nums.mapIndexed { idx, n ->
            DrawRecord(
                number = n,
                size = if (n >= 5) "BIG" else "SMALL",
                period = (202611170140L - idx).toString(),
            )
        }
    }

    @Test
    fun `all four engines produce valid predictions and numbers`() {
        val draws = sampleDraws()
        val phantom = PhantomEngine.analyse(draws)
        val renox = RenoxEngine.predict(draws, 0.75)
        val hidan = HidanEngine.predict(draws)
        val goku = GokuAuraEngine.predict(draws)

        assertTrue(phantom.prediction == "BIG" || phantom.prediction == "SMALL")
        assertEquals(2, phantom.numbers.size)
        assertTrue(phantom.numbers.all { it in 0..9 })

        assertTrue(renox.prediction == "BIG" || renox.prediction == "SMALL")
        assertEquals(2, renox.numbers.size)
        assertTrue(renox.numbers.all { it in 0..9 })

        assertTrue(hidan.prediction == "BIG" || hidan.prediction == "SMALL")
        assertEquals(2, hidan.numbers.size)
        assertTrue(hidan.numbers.all { it in 0..9 })

        assertTrue(goku.prediction == "BIG" || goku.prediction == "SMALL")
        assertEquals(2, goku.numbers.size)
        assertTrue(goku.numbers.all { it in 0..9 })
        assertTrue(GokuAuraEngine.totalAuraPatterns > 300)
    }

    @Test
    fun `master ensemble and mistake guard cooldown work as expected`() {
        val draws = sampleDraws()
        val initialLearn = LearnState()
        val firstRun = MasterEnsemble.runMasterEnsemble(draws, initialLearn, 0.75)
        assertFalse(firstRun.guardTriggered)
        assertEquals(firstRun.rawMasterSize, firstRun.action)

        val entry = LedgerEntry(
            period = "202611170141",
            action = firstRun.action,
            rawMasterSize = firstRun.rawMasterSize,
            masterNums = firstRun.masterNumbers,
            oppNum = firstRun.oppositeNumber,
            trendContext = "${firstRun.phantom.regime} · ${firstRun.sig.pat3}",
            exactKey = firstRun.exactKey,
            broadKey = firstRun.broadKey,
            engineCalls = mapOf(
                "phantom" to firstRun.phantom.prediction,
                "renox" to firstRun.renox.prediction,
                "hidan" to firstRun.hidan.prediction,
                "goku" to firstRun.goku.prediction,
            ),
            actualNum = null,
            actualSize = null,
            status = "WAIT",
            at = 1L,
        )

        val oppositeSize = if (firstRun.rawMasterSize == "BIG") "SMALL" else "BIG"
        val (outcome, learnedAfterLoss) = MasterEnsemble.learnFromSettledRound(entry, oppositeSize, initialLearn)
        assertEquals("LOSS", outcome)
        assertEquals(3, learnedAfterLoss.mistakes[firstRun.exactKey]?.cooldown)

        val guardedRun = MasterEnsemble.runMasterEnsemble(draws, learnedAfterLoss, 0.75)
        assertTrue(guardedRun.guardTriggered)
        assertEquals("SKIP", guardedRun.action)
    }

    @Test
    fun `normalizePayload parses nested api payloads and computeNextPeriod increments`() {
        val json = JSONObject().put(
            "data",
            JSONObject().put(
                "list",
                JSONArray()
                    .put(JSONObject().put("issueNumber", "202611190010").put("number", "8"))
                    .put(JSONObject().put("issueNumber", "202611190009").put("openCode", "3")),
            ),
        )
        val parsed = MixonaNetwork.normalizePayload(json)
        assertEquals(2, parsed.size)
        assertEquals(8, parsed[0].number)
        assertEquals("BIG", parsed[0].size)
        assertEquals("202611190010", parsed[0].period)
        assertEquals(3, parsed[1].number)
        assertEquals("SMALL", parsed[1].size)
        assertEquals("202611190011", MasterEnsemble.computeNextPeriod("202611190010"))
    }
}
