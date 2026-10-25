package com.spacetravel.idle.domain.logic

import com.spacetravel.idle.domain.model.BalanceConfig
import com.spacetravel.idle.domain.model.TimeGuardRecord
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimeGuardTest {

    private lateinit var evaluator: TimeGuardEvaluator
    private lateinit var balance: BalanceConfig

    @Before
    fun setup() {
        balance = BalanceConfig(
            time_rewind_threshold_ms = 86400000L, // 24時間
            penalty_rate = 0.10,
            penalty_max = 500L
        )
        evaluator = TimeGuardEvaluator(balance)
    }

    @Test
    fun testNormalClockProgression_NoRewind() {
        val lastWall = 100000000L
        val currentWall = 100050000L // 50秒進んだ
        val tg = TimeGuardRecord(lastWallMs = lastWall, lastElapsedMs = 5000L, bootCount = 1)

        val result = evaluator.evaluate(currentWall, 10000L, tg, 1000L)
        assertFalse(result.isRewound)
        assertEquals(0L, result.penaltyCredits)
        assertEquals(currentWall, result.newTimeGuard.lastWallMs)
    }

    @Test
    fun testSmallRewindUnderThreshold_NoDetection() {
        val lastWall = 100000000L
        // 23時間59分戻る (86,340,000 ms 戻る)
        val currentWall = lastWall - 86340000L
        val tg = TimeGuardRecord(lastWallMs = lastWall, lastElapsedMs = 5000L, bootCount = 1)

        val result = evaluator.evaluate(currentWall, 10000L, tg, 1000L)
        assertFalse(result.isRewound)
        assertEquals(0L, result.penaltyCredits)
    }

    @Test
    fun test24HoursRewind_Detected_AC04() {
        val lastWall = 200000000L
        // ちょうど24時間戻る (86,400,000 ms)
        val currentWall = lastWall - 86400000L
        val tg = TimeGuardRecord(lastWallMs = lastWall, lastElapsedMs = 5000L, bootCount = 1)

        // 3000 クレジット保有時: 10% = 300 (上限500以下)
        val result1 = evaluator.evaluate(currentWall, 10000L, tg, 3000L)
        assertTrue(result1.isRewound)
        assertEquals(300L, result1.penaltyCredits)
        assertEquals(currentWall, result1.newTimeGuard.lastWallMs)

        // 10,000 クレジット保有時: 10% = 1000 -> 上限500でキャップ
        val result2 = evaluator.evaluate(currentWall, 10000L, tg, 10000L)
        assertTrue(result2.isRewound)
        assertEquals(500L, result2.penaltyCredits)
    }
}
