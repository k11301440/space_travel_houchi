package com.spacetravel.idle.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.spacetravel.idle.domain.model.*

class AppDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "spacetravel_idle.db"
        const val DATABASE_VERSION = 1

        const val TABLE_EXPEDITION = "expedition"
        const val TABLE_DISCOVERY = "discovery"
        const val TABLE_WALLET = "wallet"
        const val TABLE_TIME_GUARD = "time_guard"
        const val TABLE_COSMETIC_OWNED = "cosmetic_owned"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_EXPEDITION (
                id TEXT PRIMARY KEY,
                start_at INTEGER NOT NULL,
                end_at INTEGER NOT NULL,
                mode TEXT NOT NULL,
                effective_ms INTEGER NOT NULL,
                tier TEXT NOT NULL,
                status TEXT NOT NULL,
                result_json TEXT NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_DISCOVERY (
                body_id INTEGER PRIMARY KEY,
                first_found_at INTEGER NOT NULL,
                observe_count INTEGER NOT NULL,
                last_observed_at INTEGER NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_WALLET (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                credits INTEGER NOT NULL DEFAULT 0,
                total_earned INTEGER NOT NULL DEFAULT 0,
                penalty_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_TIME_GUARD (
                id INTEGER PRIMARY KEY CHECK (id = 1),
                last_wall_ms INTEGER NOT NULL DEFAULT 0,
                last_elapsed_ms INTEGER NOT NULL DEFAULT 0,
                boot_count INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE $TABLE_COSMETIC_OWNED (
                item_id TEXT PRIMARY KEY,
                unlocked_at INTEGER NOT NULL
            )
        """.trimIndent())

        // 初期レコード挿入
        db.execSQL("INSERT OR IGNORE INTO $TABLE_WALLET (id, credits, total_earned, penalty_count) VALUES (1, 0, 0, 0)")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_TIME_GUARD (id, last_wall_ms, last_elapsed_ms, boot_count) VALUES (1, 0, 0, 0)")

        // デフォルトコスメティクスを初期解放
        val now = System.currentTimeMillis()
        db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('skin_default', $now)")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('flame_blue', $now)")
        db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('light_cyan', $now)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 将来のマイグレーション用
    }

    // --- Wallet ---
    fun getWallet(): WalletRecord {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT credits, total_earned, penalty_count FROM $TABLE_WALLET WHERE id = 1", null)
        cursor.use {
            if (it.moveToFirst()) {
                return WalletRecord(
                    credits = it.getLong(0),
                    totalEarned = it.getLong(1),
                    penaltyCount = it.getInt(2)
                )
            }
        }
        return WalletRecord()
    }

    fun updateWallet(credits: Long, totalEarned: Long, penaltyCount: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("credits", credits)
            put("total_earned", totalEarned)
            put("penalty_count", penaltyCount)
        }
        db.update(TABLE_WALLET, values, "id = 1", null)
    }

    // --- TimeGuard ---
    fun getTimeGuard(): TimeGuardRecord {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT last_wall_ms, last_elapsed_ms, boot_count FROM $TABLE_TIME_GUARD WHERE id = 1", null)
        cursor.use {
            if (it.moveToFirst()) {
                return TimeGuardRecord(
                    lastWallMs = it.getLong(0),
                    lastElapsedMs = it.getLong(1),
                    bootCount = it.getInt(2)
                )
            }
        }
        return TimeGuardRecord()
    }

    fun updateTimeGuard(record: TimeGuardRecord) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("last_wall_ms", record.lastWallMs)
            put("last_elapsed_ms", record.lastElapsedMs)
            put("boot_count", record.bootCount)
        }
        db.update(TABLE_TIME_GUARD, values, "id = 1", null)
    }

    // --- Discoveries ---
    fun getAllDiscoveries(): Map<Int, DiscoveryRecord> {
        val result = mutableMapOf<Int, DiscoveryRecord>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT body_id, first_found_at, observe_count, last_observed_at FROM $TABLE_DISCOVERY", null)
        cursor.use {
            while (it.moveToNext()) {
                val record = DiscoveryRecord(
                    bodyId = it.getInt(0),
                    firstFoundAt = it.getLong(1),
                    observeCount = it.getInt(2),
                    lastObservedAt = it.getLong(3)
                )
                result[record.bodyId] = record
            }
        }
        return result
    }

    // --- Cosmetics Owned ---
    fun getOwnedCosmetics(): Set<String> {
        val set = mutableSetOf<String>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT item_id FROM $TABLE_COSMETIC_OWNED", null)
        cursor.use {
            while (it.moveToNext()) {
                set.add(it.getString(0))
            }
        }
        return set
    }

    fun unlockCosmetic(itemId: String, unlockedAt: Long) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("item_id", itemId)
            put("unlocked_at", unlockedAt)
        }
        db.insertWithOnConflict(TABLE_COSMETIC_OWNED, null, values, SQLiteDatabase.CONFLICT_IGNORE)
    }

    // --- Expeditions ---
    fun getExpedition(id: String): ExpeditionRecord? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT id, start_at, end_at, mode, effective_ms, tier, status, result_json FROM $TABLE_EXPEDITION WHERE id = ?", arrayOf(id))
        cursor.use {
            if (it.moveToFirst()) {
                return ExpeditionRecord(
                    id = it.getString(0),
                    startAt = it.getLong(1),
                    endAt = it.getLong(2),
                    mode = ExpeditionMode.valueOf(it.getString(3)),
                    effectiveMs = it.getLong(4),
                    tier = Tier.valueOf(it.getString(5)),
                    status = ExpeditionStatus.valueOf(it.getString(6)),
                    resultJson = it.getString(7)
                )
            }
        }
        return null
    }

    /**
     * トランザクション内で精算を一括コミット (DATA-004, FR-020, NFR-006)
     */
    fun commitSettlement(
        expedition: ExpeditionRecord,
        newDiscoveries: List<DiscoveryRecord>,
        updatedWallet: WalletRecord,
        updatedTimeGuard: TimeGuardRecord
    ) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            // 1. Expedition
            val expValues = ContentValues().apply {
                put("id", expedition.id)
                put("start_at", expedition.startAt)
                put("end_at", expedition.endAt)
                put("mode", expedition.mode.name)
                put("effective_ms", expedition.effectiveMs)
                put("tier", expedition.tier.name)
                put("status", expedition.status.name)
                put("result_json", expedition.resultJson)
            }
            db.insertWithOnConflict(TABLE_EXPEDITION, null, expValues, SQLiteDatabase.CONFLICT_REPLACE)

            // 2. Discoveries
            for (d in newDiscoveries) {
                val dValues = ContentValues().apply {
                    put("body_id", d.bodyId)
                    put("first_found_at", d.firstFoundAt)
                    put("observe_count", d.observeCount)
                    put("last_observed_at", d.lastObservedAt)
                }
                db.insertWithOnConflict(TABLE_DISCOVERY, null, dValues, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // 3. Wallet
            val wValues = ContentValues().apply {
                put("credits", updatedWallet.credits)
                put("total_earned", updatedWallet.totalEarned)
                put("penalty_count", updatedWallet.penaltyCount)
            }
            db.update(TABLE_WALLET, wValues, "id = 1", null)

            // 4. TimeGuard
            val tgValues = ContentValues().apply {
                put("last_wall_ms", updatedTimeGuard.lastWallMs)
                put("last_elapsed_ms", updatedTimeGuard.lastElapsedMs)
                put("boot_count", updatedTimeGuard.bootCount)
            }
            db.update(TABLE_TIME_GUARD, tgValues, "id = 1", null)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * 全データ初期化 (FR-065)
     */
    fun resetAllData() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_EXPEDITION, null, null)
            db.delete(TABLE_DISCOVERY, null, null)
            db.delete(TABLE_COSMETIC_OWNED, null, null)

            val now = System.currentTimeMillis()
            db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('skin_default', $now)")
            db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('flame_blue', $now)")
            db.execSQL("INSERT OR IGNORE INTO $TABLE_COSMETIC_OWNED (item_id, unlocked_at) VALUES ('light_cyan', $now)")

            val wValues = ContentValues().apply {
                put("credits", 0L)
                put("total_earned", 0L)
                put("penalty_count", 0)
            }
            db.update(TABLE_WALLET, wValues, "id = 1", null)

            val tgValues = ContentValues().apply {
                put("last_wall_ms", now)
                put("last_elapsed_ms", 0L)
                put("boot_count", 1)
            }
            db.update(TABLE_TIME_GUARD, tgValues, "id = 1", null)

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
