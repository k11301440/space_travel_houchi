package com.spacetravel.idle.domain.time

/**
 * 時刻取得インターフェース (DATA-002)
 * テストでの差し替えやデバッグ時の時間早送り・巻き戻しを可能にする。
 */
interface Clock {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}

/**
 * デバッグ用オフセット付きClock
 */
class DebuggableClock(
    private val baseClock: Clock = SystemClock(),
    var offsetMillis: Long = 0L
) : Clock {
    override fun currentTimeMillis(): Long = baseClock.currentTimeMillis() + offsetMillis
    override fun elapsedRealtime(): Long = baseClock.elapsedRealtime() + offsetMillis

    fun fastForward(millis: Long) {
        offsetMillis += millis
    }

    fun rewind(millis: Long) {
        offsetMillis -= millis
    }

    fun resetOffset() {
        offsetMillis = 0L
    }
}
