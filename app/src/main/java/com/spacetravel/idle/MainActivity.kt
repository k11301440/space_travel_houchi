package com.spacetravel.idle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.spacetravel.idle.ui.MainViewModel
import com.spacetravel.idle.ui.ScreenState
import com.spacetravel.idle.ui.components.ReturnAnimationOverlay
import com.spacetravel.idle.ui.components.SettlementReportDialog
import com.spacetravel.idle.ui.components.TutorialDialog
import com.spacetravel.idle.ui.screens.DatabaseScreen
import com.spacetravel.idle.ui.screens.HangarScreen
import com.spacetravel.idle.ui.screens.HomeScreen
import com.spacetravel.idle.ui.screens.SettingsScreen
import com.spacetravel.idle.ui.theme.AccentBlue
import com.spacetravel.idle.ui.theme.LocalUiScale
import com.spacetravel.idle.ui.theme.SpaceTravelTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as SpaceTravelApp
                return MainViewModel(app.repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val themeSetting by viewModel.theme.collectAsState()
            val uiScale by viewModel.uiScale.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()

            val isReturning by viewModel.isReturningAnimation.collectAsState()
            val report by viewModel.settlementReport.collectAsState()
            val showTutorial by viewModel.showTutorial.collectAsState()
            val lang by viewModel.language.collectAsState()

            SpaceTravelTheme(themeSetting = themeSetting, uiScale = uiScale) {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = AccentBlue,
                            tonalElevation = 4.dp
                        ) {
                            val scale = LocalUiScale.current
                            NavigationBarItem(
                                selected = currentScreen is ScreenState.Home,
                                onClick = { viewModel.navigateTo(ScreenState.Home) },
                                icon = { Icon(Icons.Default.Home, contentDescription = stringResource(R.string.nav_home)) },
                                label = { Text(stringResource(R.string.nav_home), fontSize = (11 * scale).sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen is ScreenState.Database,
                                onClick = { viewModel.navigateTo(ScreenState.Database) },
                                icon = { Icon(Icons.Default.Book, contentDescription = stringResource(R.string.nav_database)) },
                                label = { Text(stringResource(R.string.nav_database), fontSize = (11 * scale).sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen is ScreenState.Hangar,
                                onClick = { viewModel.navigateTo(ScreenState.Hangar) },
                                icon = { Icon(Icons.Default.Build, contentDescription = stringResource(R.string.nav_hangar)) },
                                label = { Text(stringResource(R.string.nav_hangar), fontSize = (11 * scale).sp) }
                            )
                            NavigationBarItem(
                                selected = currentScreen is ScreenState.Settings,
                                onClick = { viewModel.navigateTo(ScreenState.Settings) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings)) },
                                label = { Text(stringResource(R.string.nav_settings), fontSize = (11 * scale).sp) }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        when (currentScreen) {
                            is ScreenState.Home -> HomeScreen(viewModel = viewModel)
                            is ScreenState.Database -> DatabaseScreen(viewModel = viewModel)
                            is ScreenState.Hangar -> HangarScreen(viewModel = viewModel)
                            is ScreenState.Settings -> SettingsScreen(viewModel = viewModel)
                        }

                        // 帰還演出オーバーレイ (FR-021)
                        if (isReturning) {
                            ReturnAnimationOverlay(
                                onFinished = { viewModel.onReturnAnimationFinished() }
                            )
                        }

                        // 探査報告ダイアログ (FR-022)
                        report?.let { res ->
                            SettlementReportDialog(
                                report = res,
                                lang = lang,
                                onDismiss = { viewModel.dismissReport() }
                            )
                        }

                        // 初回チュートリアルダイアログ (S10)
                        if (showTutorial) {
                            TutorialDialog(
                                onDismiss = { viewModel.dismissTutorial() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // モードB: 出航ボタンを押さずにアプリを閉じた場合、onStop時刻から自動航行開始 (FR-004)
        val app = application as SpaceTravelApp
        app.repository.onAppBackgrounded()
    }
}
