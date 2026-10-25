package com.spacetravel.idle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.spacetravel.idle.domain.model.Tier
import com.spacetravel.idle.ui.MainViewModel
import com.spacetravel.idle.ui.components.SpaceShipPixelArt
import com.spacetravel.idle.ui.components.StarfieldBackground
import com.spacetravel.idle.ui.theme.*

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val isExpeditionActive by viewModel.repository.isExpeditionActive.collectAsState()
    val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
    val wallet by viewModel.repository.wallet.collectAsState()

    val skinId by viewModel.equippedSkin.collectAsState()
    val flameId by viewModel.equippedFlame.collectAsState()
    val lightId by viewModel.equippedLight.collectAsState()

    val currentSkin = viewModel.repository.assets.cosmetics.skins.firstOrNull { it.id == skinId }
    val currentFlame = viewModel.repository.assets.cosmetics.flames.firstOrNull { it.id == flameId }
    val currentLight = viewModel.repository.assets.cosmetics.lights.firstOrNull { it.id == lightId }

    val skinPrimary = try { Color(android.graphics.Color.parseColor(currentSkin?.color_primary ?: "#FFFFFF")) } catch (e: Exception) { Color.White }
    val skinSecondary = try { Color(android.graphics.Color.parseColor(currentSkin?.color_secondary ?: "#5BC8FF")) } catch (e: Exception) { AccentBlue }
    val flameColor = try { Color(android.graphics.Color.parseColor(currentFlame?.color ?: "#FFB454")) } catch (e: Exception) { AccentWarm }
    val lightColor = try { Color(android.graphics.Color.parseColor(currentLight?.color ?: "#7CE7FF")) } catch (e: Exception) { Color(0xFF7CE7FF) }

    val uiScale = LocalUiScale.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 背景の星空
        StarfieldBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 上部: クレジット表示 & ステータス
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // クレジット
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = stringResource(R.string.credits, wallet.credits),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = AccentWarm,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * uiScale).sp
                        )
                    )
                }

                // 状態タグ
                Surface(
                    color = if (isExpeditionActive) AccentBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isExpeditionActive) AccentBlue else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Text(
                        text = if (isExpeditionActive) stringResource(R.string.status_expedition) else stringResource(R.string.status_docked),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = if (isExpeditionActive) AccentBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = (13 * uiScale).sp
                        )
                    )
                }
            }

            // 中央: 宇宙船ドット絵 & 航行情報
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                SpaceShipPixelArt(
                    size = (140 * uiScale).dp,
                    skinPrimary = skinPrimary,
                    skinSecondary = skinSecondary,
                    flameColor = flameColor,
                    lightColor = lightColor,
                    isCruising = isExpeditionActive
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isExpeditionActive) {
                    val hours = elapsedSeconds / 3600
                    val minutes = (elapsedSeconds % 3600) / 60
                    val seconds = elapsedSeconds % 60
                    val timeStr = String.format("%02d:%02d:%02d", hours, minutes, seconds)

                    val effectiveMs = (elapsedSeconds * 1000).coerceAtMost(259200000L)
                    val tier = when {
                        effectiveMs < 1800000L -> Tier.T0
                        effectiveMs < 7200000L -> Tier.T1
                        effectiveMs < 21600000L -> Tier.T2
                        effectiveMs < 43200000L -> Tier.T3
                        effectiveMs < 86400000L -> Tier.T4
                        effectiveMs < 172800000L -> Tier.T5
                        else -> Tier.T6
                    }

                    Text(
                        text = stringResource(R.string.time_elapsed),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = (12 * uiScale).sp
                        )
                    )
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (32 * uiScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.current_tier, tier.name),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = (15 * uiScale).sp,
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                } else {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = (22 * uiScale).sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = stringResource(R.string.app_subtitle),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (12 * uiScale).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            // 下部: アクションボタン
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!isExpeditionActive) {
                    Button(
                        onClick = { viewModel.launchExpedition() },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height((54 * uiScale).dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_launch),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (16 * uiScale).sp
                            )
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.returnExpedition() },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height((54 * uiScale).dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentWarm,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.btn_return),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = (16 * uiScale).sp
                            )
                        )
                    }
                }
            }
        }
    }
}
