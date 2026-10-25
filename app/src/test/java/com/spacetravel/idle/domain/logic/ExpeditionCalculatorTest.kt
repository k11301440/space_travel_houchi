package com.spacetravel.idle.domain.logic

import com.spacetravel.idle.domain.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import kotlin.random.Random

class ExpeditionCalculatorTest {

    private lateinit var calculator: ExpeditionCalculator
    private lateinit var testBodies: List<CelestialBody>
    private lateinit var balance: BalanceConfig

    @Before
    fun setup() {
        balance = BalanceConfig(
            max_effective_ms = 259200000L, // 72h
            min_effective_ms = 1800000L,   // 30m
            tiers = listOf(
                TierRule(Tier.T0, 0, 1799999, 0),
                TierRule(Tier.T1, 1800000, 7199999, 1),
                TierRule(Tier.T2, 7200000, 21599999, 1),
                TierRule(Tier.T3, 21600000, 43199999, 2),
                TierRule(Tier.T4, 43200000, 86399999, 2),
                TierRule(Tier.T5, 86400000, 172799999, 3),
                TierRule(Tier.T6, 172800000, 999999999, 3)
            ),
            rarity_weights = mapOf(
                "T1" to mapOf("1" to 90, "2" to 10),
                "T2" to mapOf("1" to 60, "2" to 35, "3" to 5),
                "T3" to mapOf("1" to 30, "2" to 40, "3" to 25, "4" to 5),
                "T4" to mapOf("1" to 10, "2" to 30, "3" to 40, "4" to 18, "5" to 2),
                "T5" to mapOf("1" to 5, "2" to 15, "3" to 35, "4" to 35, "5" to 10),
                "T6" to mapOf("2" to 10, "3" to 30, "4" to 40, "5" to 20)
            ),
            probe_rates = mapOf(
                "T1" to 0.0, "T2" to 0.02, "T3" to 0.03, "T4" to 0.05, "T5" to 0.07, "T6" to 0.10
            ),
            rewards = RewardsConfig(
                first_discovery = mapOf("1" to 10L, "2" to 25L, "3" to 60L, "4" to 150L, "5" to 400L),
                reobserve_ratio = 0.20
            )
        )
        calculator = ExpeditionCalculator(balance)

        // テスト用天体セット
        testBodies = listOf(
            createBody(1, Category.minor_body, 1, Tier.T1),
            createBody(2, Category.minor_body, 2, Tier.T1),
            createBody(3, Category.planet, 1, Tier.T2),
            createBody(4, Category.planet, 2, Tier.T2),
            createBody(5, Category.planet, 3, Tier.T2),
            createBody(6, Category.star, 3, Tier.T3),
            createBody(7, Category.star, 4, Tier.T3),
            createBody(8, Category.nebula, 4, Tier.T4),
            createBody(9, Category.nebula, 5, Tier.T4),
            createBody(10, Category.compact, 5, Tier.T5),
            createBody(11, Category.probe, 2, Tier.T2, fictional = true)
        )
    }

    private fun createBody(id: Int, cat: Category, rarity: Int, minTier: Tier, fictional: Boolean = false): CelestialBody {
        return CelestialBody(
            id = id,
            category = cat,
            rarity = rarity,
            min_tier = minTier,
            fictional = fictional,
            name = LocalizedText("天体$id", "Body$id"),
            description = LocalizedText("説明$id", "Desc$id"),
            image = "body_$id"
        )
    }

    @Test
    fun testTierBoundaries_AC01() {
        // 29分59秒 (1,799,000 ms) -> T0
        assertEquals(Tier.T0, calculator.determineTier(1799000L))

        // 30分00秒 (1,800,000 ms) -> T1
        assertEquals(Tier.T1, calculator.determineTier(1800000L))

        // 2時間 (7,200,000 ms) -> T2
        assertEquals(Tier.T2, calculator.determineTier(7200000L))

        // 6時間 (21,600,000 ms) -> T3
        assertEquals(Tier.T3, calculator.determineTier(21600000L))

        // 12時間 (43,200,000 ms) -> T4
        assertEquals(Tier.T4, calculator.determineTier(43200000L))

        // 24時間 (86,400,000 ms) -> T5
        assertEquals(Tier.T5, calculator.determineTier(86400000L))

        // 48時間 (172,800,000 ms) -> T6
        assertEquals(Tier.T6, calculator.determineTier(172800000L))
    }

    @Test
    fun testEffectiveDurationCap_AC02() {
        // 72時間 (259,200,000 ms)
        assertEquals(259200000L, calculator.calculateEffectiveDuration(259200000L))

        // 100時間 (360,000,000 ms) -> 72時間にキャップ
        assertEquals(259200000L, calculator.calculateEffectiveDuration(360000000L))
    }

    @Test
    fun testDrawDiscoveries_T0YieldsEmpty() {
        val results = calculator.drawDiscoveries(Tier.T0, testBodies, emptyMap())
        assertTrue(results.isEmpty())
    }

    @Test
    fun testDrawDiscoveries_T1Yields1Discovery() {
        val results = calculator.drawDiscoveries(Tier.T1, testBodies, emptyMap(), Random(42))
        assertEquals(1, results.size)
        assertTrue(results[0].body.rarity in 1..2)
    }

    @Test
    fun testNoDuplicatesInSingleExpedition() {
        // T5 (3件発見) で重複がないこと
        for (seed in 1..20) {
            val results = calculator.drawDiscoveries(Tier.T5, testBodies, emptyMap(), Random(seed))
            val ids = results.map { it.body.id }
            assertEquals(ids.size, ids.toSet().size)
        }
    }

    @Test
    fun testReobservationRewards() {
        val existing = mapOf(
            1 to DiscoveryRecord(bodyId = 1, firstFoundAt = 1000L, observeCount = 1, lastObservedAt = 1000L)
        )
        // bodyId 1 (★1, 初発見10クレジット) が再観測された場合、20% = 2クレジット
        val results = calculator.drawDiscoveries(Tier.T1, testBodies.filter { it.id == 1 }, existing, Random(1))
        if (results.isNotEmpty()) {
            val res = results[0]
            assertFalse(res.isNew)
            assertEquals(2L, res.creditsEarned)
        }
    }
}
