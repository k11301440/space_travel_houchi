package com.spacetravel.idle.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spacetravel.idle.data.repository.*
import com.spacetravel.idle.domain.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ScreenState {
    object Home : ScreenState()
    object Database : ScreenState()
    object Hangar : ScreenState()
    object Settings : ScreenState()
}

class MainViewModel(
    val repository: GameRepository
) : ViewModel() {

    // 現在の画面
    private val _currentScreen = MutableStateFlow<ScreenState>(ScreenState.Home)
    val currentScreen: StateFlow<ScreenState> = _currentScreen.asStateFlow()

    // 演出・報告ダイアログ
    private val _settlementReport = MutableStateFlow<SettlementResult?>(null)
    val settlementReport: StateFlow<SettlementResult?> = _settlementReport.asStateFlow()

    private val _isReturningAnimation = MutableStateFlow(false)
    val isReturningAnimation: StateFlow<Boolean> = _isReturningAnimation.asStateFlow()

    // 経過時間（秒）
    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    // 設定
    private val _theme = MutableStateFlow(repository.prefs.theme)
    val theme: StateFlow<ThemeSetting> = _theme.asStateFlow()

    private val _uiScale = MutableStateFlow(repository.prefs.uiScale)
    val uiScale: StateFlow<Float> = _uiScale.asStateFlow()

    private val _language = MutableStateFlow(repository.prefs.language)
    val language: StateFlow<String> = _language.asStateFlow()

    private val _exploreMode = MutableStateFlow(repository.prefs.exploreMode)
    val exploreMode: StateFlow<ExploreModeSetting> = _exploreMode.asStateFlow()

    // コスメティクス装備
    private val _equippedSkin = MutableStateFlow(repository.prefs.equippedSkin)
    val equippedSkin: StateFlow<String> = _equippedSkin.asStateFlow()

    private val _equippedFlame = MutableStateFlow(repository.prefs.equippedFlame)
    val equippedFlame: StateFlow<String> = _equippedFlame.asStateFlow()

    private val _equippedLight = MutableStateFlow(repository.prefs.equippedLight)
    val equippedLight: StateFlow<String> = _equippedLight.asStateFlow()

    // チュートリアル
    private val _showTutorial = MutableStateFlow(!repository.prefs.isTutorialDone)
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    // デバッグ時間オフセット（分）
    private val _debugOffsetMinutes = MutableStateFlow(repository.clock.offsetMillis / (60 * 1000))
    val debugOffsetMinutes: StateFlow<Long> = _debugOffsetMinutes.asStateFlow()

    init {
        // タイマーループ: 1秒ごとに経過時間を更新し、15秒ごとにハートビート保存
        viewModelScope.launch {
            var counter = 0
            while (true) {
                delay(1000)
                updateElapsedTime()
                counter++
                if (counter % 15 == 0) {
                    repository.updateHeartbeat()
                }
            }
        }
    }

    private fun updateElapsedTime() {
        val start = repository.prefs.activeExpeditionStart
        if (start > 0L) {
            val now = repository.clock.currentTimeMillis()
            _elapsedSeconds.value = ((now - start) / 1000).coerceAtLeast(0L)
        } else {
            _elapsedSeconds.value = 0L
        }
    }

    fun navigateTo(screen: ScreenState) {
        _currentScreen.value = screen
    }

    fun launchExpedition() {
        repository.launchExpeditionA()
        updateElapsedTime()
    }

    fun returnExpedition() {
        // 帰還演出開始 (FR-021)
        _isReturningAnimation.value = true
    }

    fun onReturnAnimationFinished() {
        _isReturningAnimation.value = false
        val result = repository.settleExpedition()
        if (result != null) {
            _settlementReport.value = result
        }
        updateElapsedTime()
    }

    fun dismissReport() {
        _settlementReport.value = null
    }

    fun dismissTutorial() {
        repository.prefs.isTutorialDone = true
        _showTutorial.value = false
    }

    // --- カスタマイズ ---
    fun buyAndEquip(item: CosmeticItem, slotType: String): Boolean {
        val success = repository.buyCosmetic(item)
        if (success) {
            when (slotType) {
                "skin" -> equipSkin(item.id)
                "flame" -> equipFlame(item.id)
                "light" -> equipLight(item.id)
            }
        }
        return success
    }

    fun equipSkin(id: String) {
        repository.equipSkin(id)
        _equippedSkin.value = id
    }

    fun equipFlame(id: String) {
        repository.equipFlame(id)
        _equippedFlame.value = id
    }

    fun equipLight(id: String) {
        repository.equipLight(id)
        _equippedLight.value = id
    }

    // --- 設定 ---
    fun setTheme(theme: ThemeSetting) {
        repository.prefs.theme = theme
        _theme.value = theme
    }

    fun setUiScale(scale: Float) {
        repository.prefs.uiScale = scale
        _uiScale.value = scale
    }

    fun setLanguage(lang: String) {
        repository.prefs.language = lang
        _language.value = lang
    }

    fun setExploreMode(mode: ExploreModeSetting) {
        repository.prefs.exploreMode = mode
        _exploreMode.value = mode
    }

    fun resetAllData() {
        repository.resetAllData()
        _theme.value = repository.prefs.theme
        _uiScale.value = repository.prefs.uiScale
        _language.value = repository.prefs.language
        _exploreMode.value = repository.prefs.exploreMode
        _equippedSkin.value = repository.prefs.equippedSkin
        _equippedFlame.value = repository.prefs.equippedFlame
        _equippedLight.value = repository.prefs.equippedLight
        _debugOffsetMinutes.value = 0L
        _elapsedSeconds.value = 0L
        _settlementReport.value = null
        _showTutorial.value = true
        _currentScreen.value = ScreenState.Home
    }

    // --- デバッグ用時間操作メニュー ---
    fun debugFastForwardMinutes(minutes: Long) {
        repository.debugFastForward(minutes * 60 * 1000L)
        _debugOffsetMinutes.value = repository.clock.offsetMillis / (60 * 1000)
        updateElapsedTime()
    }

    fun debugRewindHours(hours: Long) {
        repository.debugRewind(hours * 3600 * 1000L)
        _debugOffsetMinutes.value = repository.clock.offsetMillis / (60 * 1000)
        updateElapsedTime()
    }

    fun debugResetOffset() {
        repository.debugResetOffset()
        _debugOffsetMinutes.value = 0L
        updateElapsedTime()
    }
}
