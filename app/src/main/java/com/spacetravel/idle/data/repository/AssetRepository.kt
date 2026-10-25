package com.spacetravel.idle.data.repository

import android.content.Context
import com.spacetravel.idle.domain.model.*
import kotlinx.serialization.json.Json

class AssetRepository(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val balance: BalanceConfig by lazy {
        loadJsonFromAsset("balance.json") { json.decodeFromString<BalanceConfig>(it) }
            ?: BalanceConfig()
    }

    val bodies: List<CelestialBody> by lazy {
        loadJsonFromAsset("bodies.json") { json.decodeFromString<List<CelestialBody>>(it) }
            ?: emptyList()
    }

    val idleLogs: List<IdleLog> by lazy {
        loadJsonFromAsset("idle_logs.json") { json.decodeFromString<IdleLogsData>(it).logs }
            ?: emptyList()
    }

    val cosmetics: CosmeticsData by lazy {
        loadJsonFromAsset("cosmetics.json") { json.decodeFromString<CosmeticsData>(it) }
            ?: CosmeticsData()
    }

    private fun <T> loadJsonFromAsset(fileName: String, parser: (String) -> T): T? {
        return try {
            val content = context.assets.open(fileName).bufferedReader().use { it.readText() }
            parser(content)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
