package com.spacetravel.idle.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class Category {
    minor_body,
    asteroid,
    planet,
    star,
    nebula,
    compact,
    probe
}

@Serializable
enum class Tier {
    T0, T1, T2, T3, T4, T5, T6
}

@Serializable
enum class ExpeditionStatus {
    ACTIVE,
    SETTLED,
    VOID
}

@Serializable
enum class ExpeditionMode {
    A, // 手動出航
    B, // アプリ離脱による自動出航
    C  // 使用状況アクセス（画面オフ時間のみ）
}

@Serializable
data class LocalizedText(
    val ja: String,
    val en: String
) {
    fun get(lang: String): String = if (lang.equals("en", ignoreCase = true)) en else ja
}

@Serializable
data class BodyFacts(
    val distance: String = "",
    val type: String = ""
)

@Serializable
data class CelestialBody(
    val id: Int,
    val category: Category,
    val rarity: Int, // 1〜5
    val min_tier: Tier,
    val fictional: Boolean = false,
    val name: LocalizedText,
    val description: LocalizedText,
    val image: String,
    val facts: BodyFacts = BodyFacts(),
    val source: String = ""
)

@Serializable
data class DiscoveryRecord(
    val bodyId: Int,
    val firstFoundAt: Long,
    val observeCount: Int,
    val lastObservedAt: Long
)

@Serializable
data class WalletRecord(
    val credits: Long = 0,
    val totalEarned: Long = 0,
    val penaltyCount: Int = 0
)

@Serializable
data class TimeGuardRecord(
    val lastWallMs: Long = 0,
    val lastElapsedMs: Long = 0,
    val bootCount: Int = 0
)

@Serializable
data class ExpeditionRecord(
    val id: String,
    val startAt: Long,
    val endAt: Long,
    val mode: ExpeditionMode,
    val effectiveMs: Long,
    val tier: Tier,
    val status: ExpeditionStatus,
    val resultJson: String = ""
)

@Serializable
data class FoundBodyResult(
    val body: CelestialBody,
    val isNew: Boolean,
    val creditsEarned: Long
)

@Serializable
data class SettlementResult(
    val expeditionId: String,
    val effectiveMs: Long,
    val tier: Tier,
    val foundBodies: List<FoundBodyResult>,
    val emptyLog: LocalizedText? = null,
    val isTimeRewound: Boolean = false,
    val penaltyCredits: Long = 0,
    val totalCreditsEarned: Long = 0
)

@Serializable
data class CosmeticItem(
    val id: String,
    val name_ja: String,
    val name_en: String,
    val price: Long,
    val color_primary: String = "",
    val color_secondary: String = "",
    val color: String = ""
) {
    fun name(lang: String): String = if (lang.equals("en", ignoreCase = true)) name_en else name_ja
}

@Serializable
data class CosmeticsData(
    val skins: List<CosmeticItem> = emptyList(),
    val flames: List<CosmeticItem> = emptyList(),
    val lights: List<CosmeticItem> = emptyList()
)

@Serializable
data class IdleLog(
    val id: Int,
    val ja: String,
    val en: String
)

@Serializable
data class IdleLogsData(
    val logs: List<IdleLog> = emptyList()
)

@Serializable
data class TierRule(
    val tier: Tier,
    val min_ms: Long,
    val max_ms: Long,
    val discoveries: Int
)

@Serializable
data class BalanceConfig(
    val max_effective_ms: Long = 259200000L,
    val min_effective_ms: Long = 1800000L,
    val time_rewind_threshold_ms: Long = 86400000L,
    val penalty_rate: Double = 0.10,
    val penalty_max: Long = 500L,
    val tiers: List<TierRule> = emptyList(),
    val rarity_weights: Map<String, Map<String, Int>> = emptyMap(),
    val probe_rates: Map<String, Double> = emptyMap(),
    val rewards: RewardsConfig = RewardsConfig()
)

@Serializable
data class RewardsConfig(
    val first_discovery: Map<String, Long> = mapOf("1" to 10L, "2" to 25L, "3" to 60L, "4" to 150L, "5" to 400L),
    val reobserve_ratio: Double = 0.20
)
