package com.spacetravel.idle.data.repository

import android.content.Context
import com.spacetravel.idle.domain.logic.ExpeditionCalculator
import com.spacetravel.idle.domain.logic.TimeGuardEvaluator
import com.spacetravel.idle.domain.model.*
import com.spacetravel.idle.domain.time.DebuggableClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

class GameRepository(
    private val context: Context,
    val clock: DebuggableClock,
    val dbHelper: AppDatabaseHelper,
    val prefs: PreferencesRepository,
    val assets: AssetRepository
) {
    private val calculator = ExpeditionCalculator(assets.balance)
    private val timeGuardEvaluator = TimeGuardEvaluator(assets.balance)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // 進行状態の StateFlow
    private val _wallet = MutableStateFlow(dbHelper.getWallet())
    val wallet: StateFlow<WalletRecord> = _wallet.asStateFlow()

    private val _discoveries = MutableStateFlow(dbHelper.getAllDiscoveries())
    val discoveries: StateFlow<Map<Int, DiscoveryRecord>> = _discoveries.asStateFlow()

    private val _ownedCosmetics = MutableStateFlow(dbHelper.getOwnedCosmetics())
    val ownedCosmetics: StateFlow<Set<String>> = _ownedCosmetics.asStateFlow()

    private val _isExpeditionActive = MutableStateFlow(prefs.activeExpeditionStart > 0L)
    val isExpeditionActive: StateFlow<Boolean> = _isExpeditionActive.asStateFlow()

    init {
        // 保存されていたデバッグ時間オフセットをクロックに適用
        clock.offsetMillis = prefs.debugTimeOffsetMs
    }

    /**
     * 手動出航 (モードA: FR-003)
     */
    fun launchExpeditionA(): Long {
        val now = clock.currentTimeMillis()
        prefs.activeExpeditionStart = now
        prefs.activeExpeditionMode = ExpeditionMode.A.name
        _isExpeditionActive.value = true
        return now
    }

    /**
     * 自動出航 (モードB: FR-004)
     * 航行中でない状態でアプリがフォアグラウンドを離れた場合に開始
     */
    fun onAppBackgrounded() {
        val now = clock.currentTimeMillis()
        prefs.lastActiveAt = now
        if (prefs.activeExpeditionStart <= 0L) {
            // 出航していなければ、離脱時刻から自動出航 (モードB)
            prefs.activeExpeditionStart = now
            prefs.activeExpeditionMode = ExpeditionMode.B.name
            _isExpeditionActive.value = true
        }
    }

    /**
     * アプリ復帰時の定期アクティブ更新 (15秒ごと: FR-007)
     */
    fun updateHeartbeat() {
        prefs.lastActiveAt = clock.currentTimeMillis()
    }

    /**
     * 帰還精算を実行 (FR-010, FR-020, FR-050〜FR-054)
     * 航行中でない場合は null を返す
     */
    fun settleExpedition(): SettlementResult? {
        val startAt = prefs.activeExpeditionStart
        if (startAt <= 0L) return null

        val modeStr = prefs.activeExpeditionMode
        val mode = try { ExpeditionMode.valueOf(modeStr) } catch (e: Exception) { ExpeditionMode.A }
        val now = clock.currentTimeMillis()
        val elapsedRealtime = clock.elapsedRealtime()

        // 1. 時刻巻き戻し判定 (FR-051)
        val timeGuard = dbHelper.getTimeGuard()
        val wallet = dbHelper.getWallet()
        val tgEval = timeGuardEvaluator.evaluate(
            currentWallMs = now,
            currentElapsedMs = elapsedRealtime,
            timeGuard = timeGuard,
            currentCredits = wallet.credits
        )

        val expeditionId = UUID.randomUUID().toString()

        // 巻き戻しが検知された場合 (FR-052, FR-053)
        if (tgEval.isRewound) {
            val penalizedWallet = wallet.copy(
                credits = (wallet.credits - tgEval.penaltyCredits).coerceAtLeast(0L),
                penaltyCount = wallet.penaltyCount + 1
            )
            val voidExpedition = ExpeditionRecord(
                id = expeditionId,
                startAt = startAt,
                endAt = now,
                mode = mode,
                effectiveMs = 0L,
                tier = Tier.T0,
                status = ExpeditionStatus.VOID,
                resultJson = ""
            )

            dbHelper.commitSettlement(
                expedition = voidExpedition,
                newDiscoveries = emptyList(),
                updatedWallet = penalizedWallet,
                updatedTimeGuard = tgEval.newTimeGuard
            )

            // 状態リセット
            prefs.activeExpeditionStart = -1L
            _isExpeditionActive.value = false
            _wallet.value = penalizedWallet

            return SettlementResult(
                expeditionId = expeditionId,
                effectiveMs = 0L,
                tier = Tier.T0,
                foundBodies = emptyList(),
                emptyLog = LocalizedText(
                    ja = "【警報】時空の乱れを検知しました。航行は無効化されました。",
                    en = "[ALERT] Spacetime distortion detected. Expedition voided."
                ),
                isTimeRewound = true,
                penaltyCredits = tgEval.penaltyCredits,
                totalCreditsEarned = 0L
            )
        }

        // 2. 有効航行時間の算出
        val rawDuration = (now - startAt).coerceAtLeast(0L)
        val effectiveMs = calculator.calculateEffectiveDuration(rawDuration)
        val tier = calculator.determineTier(effectiveMs)

        // 3. 発見抽選
        val currentDiscoveries = dbHelper.getAllDiscoveries()
        val foundBodies = if (tier != Tier.T0) {
            calculator.drawDiscoveries(tier, assets.bodies, currentDiscoveries)
        } else {
            emptyList()
        }

        val totalCreditsEarned = foundBodies.sumOf { it.creditsEarned }

        // 4. 新規・再観測のレコード生成
        val updatedDiscoveries = mutableListOf<DiscoveryRecord>()
        for (f in foundBodies) {
            val existing = currentDiscoveries[f.body.id]
            if (existing != null) {
                updatedDiscoveries.add(
                    existing.copy(
                        observeCount = existing.observeCount + 1,
                        lastObservedAt = now
                    )
                )
            } else {
                updatedDiscoveries.add(
                    DiscoveryRecord(
                        bodyId = f.body.id,
                        firstFoundAt = now,
                        observeCount = 1,
                        lastObservedAt = now
                    )
                )
            }
        }

        // 5. 収穫なし時のログ (FR-023)
        val emptyLog = if (foundBodies.isEmpty()) {
            val randomLog = assets.idleLogs.randomOrNull()
            if (randomLog != null) {
                LocalizedText(ja = randomLog.ja, en = randomLog.en)
            } else {
                LocalizedText(ja = "周囲に観測対象は発見されなかった。", en = "No celestial bodies sighted in the sector.")
            }
        } else null

        // 6. DB トランザクション更新
        val updatedWallet = wallet.copy(
            credits = wallet.credits + totalCreditsEarned,
            totalEarned = wallet.totalEarned + totalCreditsEarned
        )

        val settlementResult = SettlementResult(
            expeditionId = expeditionId,
            effectiveMs = effectiveMs,
            tier = tier,
            foundBodies = foundBodies,
            emptyLog = emptyLog,
            isTimeRewound = false,
            penaltyCredits = 0L,
            totalCreditsEarned = totalCreditsEarned
        )

        val expeditionRecord = ExpeditionRecord(
            id = expeditionId,
            startAt = startAt,
            endAt = now,
            mode = mode,
            effectiveMs = effectiveMs,
            tier = tier,
            status = ExpeditionStatus.SETTLED,
            resultJson = json.encodeToString(settlementResult)
        )

        dbHelper.commitSettlement(
            expedition = expeditionRecord,
            newDiscoveries = updatedDiscoveries,
            updatedWallet = updatedWallet,
            updatedTimeGuard = tgEval.newTimeGuard
        )

        // 航行終了
        prefs.activeExpeditionStart = -1L
        _isExpeditionActive.value = false
        _wallet.value = updatedWallet
        _discoveries.value = dbHelper.getAllDiscoveries()

        return settlementResult
    }

    /**
     * コスメティクスの購入 (FR-042)
     */
    fun buyCosmetic(item: CosmeticItem): Boolean {
        val currentWallet = _wallet.value
        if (currentWallet.credits < item.price) return false

        val newCredits = currentWallet.credits - item.price
        dbHelper.updateWallet(newCredits, currentWallet.totalEarned, currentWallet.penaltyCount)
        dbHelper.unlockCosmetic(item.id, clock.currentTimeMillis())

        _wallet.value = currentWallet.copy(credits = newCredits)
        _ownedCosmetics.value = dbHelper.getOwnedCosmetics()
        return true
    }

    /**
     * コスメティクスの装備変更 (FR-043)
     */
    fun equipSkin(id: String) { prefs.equippedSkin = id }
    fun equipFlame(id: String) { prefs.equippedFlame = id }
    fun equipLight(id: String) { prefs.equippedLight = id }

    /**
     * デバッグ用: 時間を早送りする
     */
    fun debugFastForward(millis: Long) {
        clock.fastForward(millis)
        prefs.debugTimeOffsetMs = clock.offsetMillis
    }

    /**
     * デバッグ用: 時間を巻き戻す（検知テスト用）
     */
    fun debugRewind(millis: Long) {
        clock.rewind(millis)
        prefs.debugTimeOffsetMs = clock.offsetMillis
    }

    /**
     * デバッグ用: 時間オフセットをリセット
     */
    fun debugResetOffset() {
        clock.resetOffset()
        prefs.debugTimeOffsetMs = 0L
    }

    /**
     * 全データリセット (FR-065)
     */
    fun resetAllData() {
        dbHelper.resetAllData()
        prefs.clearAll()
        clock.resetOffset()
        _wallet.value = dbHelper.getWallet()
        _discoveries.value = dbHelper.getAllDiscoveries()
        _ownedCosmetics.value = dbHelper.getOwnedCosmetics()
        _isExpeditionActive.value = false
    }
}
