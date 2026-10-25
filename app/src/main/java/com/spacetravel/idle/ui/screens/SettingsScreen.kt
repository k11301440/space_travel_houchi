package com.spacetravel.idle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spacetravel.idle.R
import com.spacetravel.idle.data.repository.ExploreModeSetting
import com.spacetravel.idle.data.repository.ThemeSetting
import com.spacetravel.idle.ui.MainViewModel
import com.spacetravel.idle.ui.theme.AccentBlue
import com.spacetravel.idle.ui.theme.AccentWarm
import com.spacetravel.idle.ui.theme.LocalUiScale

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val theme by viewModel.theme.collectAsState()
    val uiScale by viewModel.uiScale.collectAsState()
    val language by viewModel.language.collectAsState()
    val exploreMode by viewModel.exploreMode.collectAsState()
    val debugOffsetMinutes by viewModel.debugOffsetMinutes.collectAsState()

    val currentUiScale = LocalUiScale.current
    var showResetDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (20 * currentUiScale).sp,
                color = Color.White
            )
        )

        // 1. 【デバッグ専用】時間加速メニュー (ユーザー指定要件)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentWarm)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = stringResource(R.string.debug_menu_title),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = AccentWarm,
                        fontWeight = FontWeight.Bold,
                        fontSize = (14 * currentUiScale).sp
                    )
                )
                Text(
                    text = stringResource(R.string.debug_menu_desc),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = (10 * currentUiScale).sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.debug_current_offset, debugOffsetMinutes),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (12 * currentUiScale).sp
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 加速ボタングリッド
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(30) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_30m), fontSize = (10 * currentUiScale).sp)
                    }
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(120) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_2h), fontSize = (10 * currentUiScale).sp)
                    }
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(360) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_6h), fontSize = (10 * currentUiScale).sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(720) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_12h), fontSize = (10 * currentUiScale).sp)
                    }
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(1440) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_24h), fontSize = (10 * currentUiScale).sp)
                    }
                    Button(
                        onClick = { viewModel.debugFastForwardMinutes(2880) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(stringResource(R.string.debug_add_48h), fontSize = (10 * currentUiScale).sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 巻き戻しテスト & オフセットリセット
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.debugRewindHours(25) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(stringResource(R.string.debug_rewind_25h), fontSize = (10 * currentUiScale).sp, color = Color(0xFFFF8A8A))
                    }
                    OutlinedButton(
                        onClick = { viewModel.debugResetOffset() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(4.dp)
                    ) {
                        Text(stringResource(R.string.debug_reset_offset), fontSize = (10 * currentUiScale).sp)
                    }
                }
            }
        }

        // 2. テーマ設定 (FR-060)
        SettingsGroup(title = stringResource(R.string.settings_theme), uiScale = currentUiScale) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeSetting.values().forEach { t ->
                    val isSelected = theme == t
                    val label = when (t) {
                        ThemeSetting.DARK -> "ダーク"
                        ThemeSetting.LIGHT -> "ライト"
                        ThemeSetting.SYSTEM -> "端末連動"
                    }
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setTheme(t) },
                        label = { Text(label, fontSize = (11 * currentUiScale).sp) }
                    )
                }
            }
        }

        // 3. UIサイズ (FR-061)
        SettingsGroup(title = stringResource(R.string.settings_ui_size), uiScale = currentUiScale) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0f to "標準 (1.0x)", 1.5f to "大 (1.5x)", 2.0f to "特大 (2.0x)").forEach { (scale, label) ->
                    FilterChip(
                        selected = uiScale == scale,
                        onClick = { viewModel.setUiScale(scale) },
                        label = { Text(label, fontSize = (11 * currentUiScale).sp) }
                    )
                }
            }
        }

        // 4. 言語設定 (FR-062)
        SettingsGroup(title = stringResource(R.string.settings_language), uiScale = currentUiScale) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("SYSTEM" to "端末連動", "ja" to "日本語", "en" to "English").forEach { (code, label) ->
                    FilterChip(
                        selected = language == code,
                        onClick = { viewModel.setLanguage(code) },
                        label = { Text(label, fontSize = (11 * currentUiScale).sp) }
                    )
                }
            }
        }

        // 5. 探索モード (FR-063)
        SettingsGroup(title = stringResource(R.string.settings_mode), uiScale = currentUiScale) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setExploreMode(ExploreModeSetting.STANDARD) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = exploreMode == ExploreModeSetting.STANDARD,
                        onClick = { viewModel.setExploreMode(ExploreModeSetting.STANDARD) }
                    )
                    Text(
                        text = stringResource(R.string.settings_mode_standard),
                        fontSize = (12 * currentUiScale).sp,
                        color = Color.White
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setExploreMode(ExploreModeSetting.HIGH_PRECISION) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = exploreMode == ExploreModeSetting.HIGH_PRECISION,
                        onClick = { viewModel.setExploreMode(ExploreModeSetting.HIGH_PRECISION) }
                    )
                    Text(
                        text = stringResource(R.string.settings_mode_high_precision),
                        fontSize = (12 * currentUiScale).sp,
                        color = Color.White
                    )
                }
            }
        }

        // 6. システム・その他 (FR-065, FR-066)
        SettingsGroup(title = "データとアプリ情報", uiScale = currentUiScale) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { showAboutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(stringResource(R.string.settings_about), fontSize = (12 * currentUiScale).sp)
                }

                Button(
                    onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B1D1D))
                ) {
                    Text(stringResource(R.string.settings_reset_data), fontSize = (12 * currentUiScale).sp, color = Color.White)
                }
            }
        }
    }

    // 初期化確認ダイアログ
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.settings_reset_data)) },
            text = { Text("すべての探索進行状況、獲得した天体図鑑、クレジット、カスタマイズが完全に初期化されます。よろしいですか？") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("初期化する")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }

    // About ダイアログ
    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("宇宙探査アプリ v0.1.0") },
            text = {
                Text(
                    "【宇宙探査アプリ（仮称）】\n" +
                    "スマホから離れた時間を宇宙船が探索する放置型天体探索アプリ。\n\n" +
                    "・完全オフライン / 広告なし / 通信権限ゼロ\n" +
                    "・天体データ出典: NASA, JAXA, 国立天文台公開資料\n" +
                    "・フォント: DotGothic16 (SIL Open Font License 1.1)\n" +
                    "・著作権: (C) 2026 Space Travel Project"
                )
            },
            confirmButton = {
                Button(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.btn_close))
                }
            }
        )
    }
}

@Composable
fun SettingsGroup(
    title: String,
    uiScale: Float,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (14 * uiScale).sp,
                    color = AccentBlue
                )
            )
            content()
        }
    }
}
