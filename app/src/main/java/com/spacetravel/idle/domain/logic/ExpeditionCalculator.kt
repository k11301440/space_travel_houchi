package com.spacetravel.idle.domain.logic

import com.spacetravel.idle.domain.model.*
import kotlin.random.Random

class ExpeditionCalculator(
    private val balance: BalanceConfig
) {

    /**
     * 有効航行時間をミリ秒から算出し、72時間上限を適用する (FR-008)
     */
    fun calculateEffectiveDuration(durationMs: Long): Long {
        if (durationMs <= 0) return 0L
        return durationMs.coerceAtMost(balance.max_effective_ms)
    }

    /**
     * 有効航行時間からティアを判定する (FR-011)
     */
    fun determineTier(effectiveMs: Long): Tier {
        if (effectiveMs < balance.min_effective_ms) {
            return Tier.T0
        }
        val rule = balance.tiers.firstOrNull { effectiveMs in it.min_ms..it.max_ms }
        return rule?.tier ?: if (effectiveMs >= 172800000L) Tier.T6 else Tier.T0
    }

    /**
     * 航行精算の抽選を実行する (FR-012〜FR-015, FR-040)
     */
    fun drawDiscoveries(
        tier: Tier,
        allBodies: List<CelestialBody>,
        existingDiscoveries: Map<Int, DiscoveryRecord>,
        random: Random = Random.Default
    ): List<FoundBodyResult> {
        if (tier == Tier.T0) return emptyList()

        val tierRule = balance.tiers.firstOrNull { it.tier == tier } ?: return emptyList()
        val countToDraw = tierRule.discoveries
        if (countToDraw <= 0) return emptyList()

        val tierRank = getTierRank(tier)
        val selectedBodies = mutableListOf<CelestialBody>()
        val selectedIds = mutableSetOf<Int>()

        // 1. 通常枠の抽選 (probeを除く天体)
        val candidateBodies = allBodies.filter {
            it.category != Category.probe && getTierRank(it.min_tier) <= tierRank
        }

        val weightsForTier = balance.rarity_weights[tier.name] ?: emptyMap()

        for (i in 0 until countToDraw) {
            val drawnRarity = sampleRarity(weightsForTier, random) ?: 1
            // そのレア度かつ最低ティア以下で、まだ今回の航行で選ばれていない天体
            val pool = candidateBodies.filter {
                it.rarity == drawnRarity && !selectedIds.contains(it.id)
            }

            val chosen = if (pool.isNotEmpty()) {
                sampleWithUnseenBonus(pool, existingDiscoveries, random)
            } else {
                // そのレア度に未選択候補がない場合は、未選択の候補プール全体からフォールバック
                val fallbackPool = candidateBodies.filter { !selectedIds.contains(it.id) }
                if (fallbackPool.isNotEmpty()) sampleWithUnseenBonus(fallbackPool, existingDiscoveries, random) else null
            }

            chosen?.let {
                selectedBodies.add(it)
                selectedIds.add(it.id)
            }
        }

        // 2. 謎の探査機（別枠・T2以上）判定 (FR-014)
        val probeRate = balance.probe_rates[tier.name] ?: 0.0
        if (probeRate > 0.0 && random.nextDouble() < probeRate) {
            val probePool = allBodies.filter {
                it.category == Category.probe &&
                getTierRank(it.min_tier) <= tierRank &&
                !selectedIds.contains(it.id)
            }
            if (probePool.isNotEmpty()) {
                val probe = sampleWithUnseenBonus(probePool, existingDiscoveries, random)
                probe?.let {
                    selectedBodies.add(it)
                    selectedIds.add(it.id)
                }
            }
        }

        // 3. 発見結果とクレジットの計算 (FR-015, FR-040)
        return selectedBodies.map { body ->
            val isNew = !existingDiscoveries.containsKey(body.id)
            val baseCredit = balance.rewards.first_discovery[body.rarity.toString()] ?: 10L
            val earned = if (isNew) {
                baseCredit
            } else {
                (baseCredit * balance.rewards.reobserve_ratio).toLong().coerceAtLeast(1L)
            }
            FoundBodyResult(
                body = body,
                isNew = isNew,
                creditsEarned = earned
            )
        }
    }

    /**
     * 未発見天体に重み2倍を適用して1件抽選 (FR-013)
     */
    private fun sampleWithUnseenBonus(
        pool: List<CelestialBody>,
        existingDiscoveries: Map<Int, DiscoveryRecord>,
        random: Random
    ): CelestialBody? {
        if (pool.isEmpty()) return null
        val weightedList = pool.map { body ->
            val isUnseen = !existingDiscoveries.containsKey(body.id)
            val weight = if (isUnseen) 2 else 1
            body to weight
        }
        val totalWeight = weightedList.sumOf { it.second }
        if (totalWeight <= 0) return pool.random(random)

        var roll = random.nextInt(totalWeight)
        for ((body, w) in weightedList) {
            if (roll < w) return body
            roll -= w
        }
        return pool.last()
    }

    /**
     * レア度重み表からレア度(1〜5)をサンプリング
     */
    private fun sampleRarity(weights: Map<String, Int>, random: Random): Int? {
        val total = weights.values.sum()
        if (total <= 0) return null
        var roll = random.nextInt(total)
        for (r in 1..5) {
            val w = weights[r.toString()] ?: 0
            if (roll < w) return r
            roll -= w
        }
        return 1
    }

    fun getTierRank(tier: Tier): Int = when (tier) {
        Tier.T0 -> 0
        Tier.T1 -> 1
        Tier.T2 -> 2
        Tier.T3 -> 3
        Tier.T4 -> 4
        Tier.T5 -> 5
        Tier.T6 -> 6
    }
}
