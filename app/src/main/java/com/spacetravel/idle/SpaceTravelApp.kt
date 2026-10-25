package com.spacetravel.idle

import android.app.Application
import com.spacetravel.idle.data.repository.AppDatabaseHelper
import com.spacetravel.idle.data.repository.AssetRepository
import com.spacetravel.idle.data.repository.GameRepository
import com.spacetravel.idle.data.repository.PreferencesRepository
import com.spacetravel.idle.domain.time.DebuggableClock

class SpaceTravelApp : Application() {

    lateinit var clock: DebuggableClock
    lateinit var dbHelper: AppDatabaseHelper
    lateinit var prefs: PreferencesRepository
    lateinit var assets: AssetRepository
    lateinit var repository: GameRepository

    override fun onCreate() {
        super.onCreate()
        clock = DebuggableClock()
        dbHelper = AppDatabaseHelper(this)
        prefs = PreferencesRepository(this)
        assets = AssetRepository(this)
        repository = GameRepository(this, clock, dbHelper, prefs, assets)
    }
}
