package com.spacetravel.idle.domain.logic

import com.spacetravel.idle.domain.model.BalanceConfig
import com.spacetravel.idle.domain.model.TimeGuardRecord
import kotlin.math.min

class TimeGuardEvaluator(
    private val balance: BalanceConfig
) {
    data class EvaluationResult(
        val isRewound: Boolean,
        val penaltyCredits: Long,
        val newTimeGuard: TimeGuardRecord
    )

    /**
     * 時刻巻き戻し判定 (FR-051〜FR-053)
     */
    fun evaluate(
        currentWallMs: Long,
        currentElapsedMs: Long,
        timeGuard: TimeGuardRecord,
        currentCredits: Long
    ): EvaluationResult {
        val lastWall = timeGuard.lastWallMs

        // 初回起動時など
        if (lastWall <= 0L) {
            return EvaluationResult(
                isRewound = false,
                penaltyCredits = 0L,
                newTimeGuard = TimeGuardRecord(
                    lastWallMs = currentWallMs,
                    lastElapsedMs = currentElapsedMs,
                    bootCount = timeGuard.bootCount + 1
                )
            )
        }

        // 過去への巻き戻し判定（閾値: 24時間以上前）
        val diffMs = lastWall - currentWallMs
        val isRewound = diffMs >= balance.time_rewind_threshold_ms

        val penalty = if (isRewound) {
            val calc = (currentCredits * balance.penalty_rate).toLong()
            min(calc, balance.penalty_max).coerceAtLeast(0L)
        } else {
            0L
        }

        // 検知後は現在時刻に更新して連続没収を防止 (FR-052)
        val newWall = if (isRewound) currentWallMs else maxOf(lastWall, currentWallMs)

        return EvaluationResult(
            isRewound = isRewound,
            penaltyCredits = penalty,
            newTimeGuard = TimeGuardRecord(
                lastWallMs = newWall,
                lastElapsedMs = currentElapsedMs,
                bootCount = timeGuard.bootCount + 1
            )
        )
    }
}
