package com.spacetravel.idle.domain.logic

import com.spacetravel.idle.domain.model.*
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class DataIntegrityTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testBodiesData_AC09_AC10() {
        val bodiesFile = File("src/main/assets/bodies.json")
        assertTrue("bodies.json should exist at ${bodiesFile.absolutePath}", bodiesFile.exists())

        val content = bodiesFile.readText()
        val bodies = json.decodeFromString<List<CelestialBody>>(content)

        // 1. ちょうど100件であること
        assertEquals(100, bodies.size)

        // 2. AC-09: 全100件に画像、日英名称、日英説明があること
        for (b in bodies) {
            assertTrue("Body ${b.id} should have image", b.image.isNotBlank())
            assertTrue("Body ${b.id} should have ja name", b.name.ja.isNotBlank())
            assertTrue("Body ${b.id} should have en name", b.name.en.isNotBlank())
            assertTrue("Body ${b.id} should have ja description", b.description.ja.isNotBlank())
            assertTrue("Body ${b.id} should have en description", b.description.en.isNotBlank())
            assertTrue("Body ${b.id} rarity must be 1..5", b.rarity in 1..5)
        }

        // 3. レア度配分 (★1:25, ★2:30, ★3:25, ★4:15, ★5:5)
        val r1 = bodies.count { it.rarity == 1 }
        val r2 = bodies.count { it.rarity == 2 }
        val r3 = bodies.count { it.rarity == 3 }
        val r4 = bodies.count { it.rarity == 4 }
        val r5 = bodies.count { it.rarity == 5 }
        assertEquals(25, r1)
        assertEquals(30, r2)
        assertEquals(25, r3)
        assertEquals(15, r4)
        assertEquals(5, r5)

        // 4. AC-10: 全ティアで重みが0より大きいレア度すべてに候補があること
        val balanceFile = File("src/main/assets/balance.json")
        assertTrue("balance.json should exist", balanceFile.exists())
        val balance = json.decodeFromString<BalanceConfig>(balanceFile.readText())

        val tierRankMap = mapOf(
            Tier.T0 to 0, Tier.T1 to 1, Tier.T2 to 2, Tier.T3 to 3,
            Tier.T4 to 4, Tier.T5 to 5, Tier.T6 to 6
        )

        for ((tierName, rarityWeights) in balance.rarity_weights) {
            val tier = Tier.valueOf(tierName)
            val currentRank = tierRankMap[tier] ?: 0

            for ((rarityStr, weight) in rarityWeights) {
                if (weight > 0) {
                    val rarity = rarityStr.toInt()
                    val candidates = bodies.filter {
                        it.category != Category.probe &&
                        it.rarity == rarity &&
                        (tierRankMap[it.min_tier] ?: 0) <= currentRank
                    }
                    assertTrue("Tier $tierName rarity $rarity has non-zero weight ($weight) but NO candidates!", candidates.isNotEmpty())
                }
            }
        }
    }
}
