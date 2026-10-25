package com.spacetravel.idle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.spacetravel.idle.domain.model.CosmeticItem
import com.spacetravel.idle.ui.MainViewModel
import com.spacetravel.idle.ui.components.SpaceShipPixelArt
import com.spacetravel.idle.ui.theme.AccentBlue
import com.spacetravel.idle.ui.theme.AccentWarm
import com.spacetravel.idle.ui.theme.LocalUiScale

@Composable
fun HangarScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val cosmetics = viewModel.repository.assets.cosmetics
    val wallet by viewModel.repository.wallet.collectAsState()
    val ownedItems by viewModel.repository.ownedCosmetics.collectAsState()

    val equippedSkin by viewModel.equippedSkin.collectAsState()
    val equippedFlame by viewModel.equippedFlame.collectAsState()
    val equippedLight by viewModel.equippedLight.collectAsState()

    val uiScale = LocalUiScale.current
    val currentLang by viewModel.language.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: スキン, 1: 炎, 2: ライト

    val activeSkin = cosmetics.skins.firstOrNull { it.id == equippedSkin }
    val activeFlame = cosmetics.flames.firstOrNull { it.id == equippedFlame }
    val activeLight = cosmetics.lights.firstOrNull { it.id == equippedLight }

    val skinPrimary = try { Color(android.graphics.Color.parseColor(activeSkin?.color_primary ?: "#FFFFFF")) } catch (e: Exception) { Color.White }
    val skinSecondary = try { Color(android.graphics.Color.parseColor(activeSkin?.color_secondary ?: "#5BC8FF")) } catch (e: Exception) { AccentBlue }
    val flameColor = try { Color(android.graphics.Color.parseColor(activeFlame?.color ?: "#FFB454")) } catch (e: Exception) { AccentWarm }
    val lightColor = try { Color(android.graphics.Color.parseColor(activeLight?.color ?: "#7CE7FF")) } catch (e: Exception) { Color(0xFF7CE7FF) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 所持クレジット表示
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.nav_hangar),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = (20 * uiScale).sp,
                    color = Color.White
                )
            )
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
        }

        Spacer(modifier = Modifier.height(16.dp))

        // プレビューエリア
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height((160 * uiScale).dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Box(contentAlignment = Alignment.Center) {
                SpaceShipPixelArt(
                    size = (110 * uiScale).dp,
                    skinPrimary = skinPrimary,
                    skinSecondary = skinSecondary,
                    flameColor = flameColor,
                    lightColor = lightColor,
                    isCruising = false
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // スキン / 炎 / ライト タブ
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AccentBlue,
            divider = {}
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("スキン (8)", fontSize = (12 * uiScale).sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("エンジン炎 (4)", fontSize = (12 * uiScale).sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("窓ライト (3)", fontSize = (12 * uiScale).sp) }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // アイテム一覧リスト
        val itemsList = when (selectedTab) {
            0 -> cosmetics.skins
            1 -> cosmetics.flames
            else -> cosmetics.lights
        }
        val slotType = when (selectedTab) {
            0 -> "skin"
            1 -> "flame"
            else -> "light"
        }
        val currentEquippedId = when (selectedTab) {
            0 -> equippedSkin
            1 -> equippedFlame
            else -> equippedLight
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(itemsList, key = { it.id }) { item ->
                val isOwned = ownedItems.contains(item.id) || item.price == 0L
                val isEquipped = item.id == currentEquippedId

                CosmeticItemRow(
                    item = item,
                    isOwned = isOwned,
                    isEquipped = isEquipped,
                    canAfford = wallet.credits >= item.price,
                    lang = currentLang,
                    uiScale = uiScale,
                    onEquip = {
                        when (slotType) {
                            "skin" -> viewModel.equipSkin(item.id)
                            "flame" -> viewModel.equipFlame(item.id)
                            "light" -> viewModel.equipLight(item.id)
                        }
                    },
                    onBuy = {
                        viewModel.buyAndEquip(item, slotType)
                    }
                )
            }
        }
    }
}

@Composable
fun CosmeticItemRow(
    item: CosmeticItem,
    isOwned: Boolean,
    isEquipped: Boolean,
    canAfford: Boolean,
    lang: String,
    uiScale: Float,
    onEquip: () -> Unit,
    onBuy: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isEquipped) AccentBlue else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name(lang),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (13 * uiScale).sp,
                        color = Color.White
                    )
                )
                if (!isOwned) {
                    Text(
                        text = "${item.price} クレジット",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (11 * uiScale).sp,
                            color = AccentWarm
                        )
                    )
                }
            }

            if (isEquipped) {
                Surface(
                    color = AccentBlue.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(6.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue)
                ) {
                    Text(
                        text = "装備中",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = (11 * uiScale).sp
                        )
                    )
                }
            } else if (isOwned) {
                Button(
                    onClick = onEquip,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("装備", fontSize = (11 * uiScale).sp)
                }
            } else {
                Button(
                    onClick = onBuy,
                    enabled = canAfford,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentWarm,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("解放", fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
