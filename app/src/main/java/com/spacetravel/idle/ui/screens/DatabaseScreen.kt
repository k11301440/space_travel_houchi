package com.spacetravel.idle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spacetravel.idle.R
import com.spacetravel.idle.domain.model.Category
import com.spacetravel.idle.domain.model.CelestialBody
import com.spacetravel.idle.domain.model.DiscoveryRecord
import com.spacetravel.idle.ui.MainViewModel
import com.spacetravel.idle.ui.components.CelestialBodyPixelArt
import com.spacetravel.idle.ui.theme.AccentBlue
import com.spacetravel.idle.ui.theme.AccentWarm
import com.spacetravel.idle.ui.theme.LocalUiScale
import com.spacetravel.idle.ui.theme.getRarityColor
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DatabaseScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val allBodies = viewModel.repository.assets.bodies
    val discoveries by viewModel.repository.discoveries.collectAsState()
    val uiScale = LocalUiScale.current
    val currentLang by viewModel.language.collectAsState()

    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedRarity by remember { mutableStateOf<Int?>(null) }
    var selectedBodyForDetail by remember { mutableStateOf<CelestialBody?>(null) }

    // フィルタリング
    val filteredBodies = remember(allBodies, selectedCategory, selectedRarity) {
        allBodies.filter { body ->
            (selectedCategory == null || body.category == selectedCategory) &&
            (selectedRarity == null || body.rarity == selectedRarity)
        }
    }

    val totalCount = allBodies.size
    val discoveredCount = discoveries.size
    val percentage = if (totalCount > 0) (discoveredCount.toFloat() / totalCount * 100f) else 0f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 発見率ヘッダー (FR-030)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.db_discovery_rate, discoveredCount, totalCount, percentage),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = AccentBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = (15 * uiScale).sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = AccentBlue,
                    trackColor = Color(0xFF2A3038),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // カテゴリタブ
        ScrollableTabRow(
            selectedTabIndex = if (selectedCategory == null) 0 else Category.values().indexOf(selectedCategory) + 1,
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            contentColor = AccentBlue,
            divider = {}
        ) {
            Tab(
                selected = selectedCategory == null,
                onClick = { selectedCategory = null },
                text = { Text(stringResource(R.string.category_all), fontSize = (12 * uiScale).sp) }
            )
            Category.values().forEach { cat ->
                val label = when (cat) {
                    Category.minor_body -> stringResource(R.string.category_minor)
                    Category.asteroid -> stringResource(R.string.category_asteroid)
                    Category.planet -> stringResource(R.string.category_planet)
                    Category.star -> stringResource(R.string.category_star)
                    Category.nebula -> stringResource(R.string.category_nebula)
                    Category.compact -> stringResource(R.string.category_compact)
                    Category.probe -> stringResource(R.string.category_probe)
                }
                Tab(
                    selected = selectedCategory == cat,
                    onClick = { selectedCategory = cat },
                    text = { Text(label, fontSize = (12 * uiScale).sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 天体グリッド
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filteredBodies, key = { it.id }) { body ->
                val isDiscovered = discoveries.containsKey(body.id)
                val discovery = discoveries[body.id]

                BodyGridCard(
                    body = body,
                    isDiscovered = isDiscovered,
                    discovery = discovery,
                    lang = currentLang,
                    uiScale = uiScale,
                    onClick = {
                        if (isDiscovered) {
                            selectedBodyForDetail = body
                        }
                    }
                )
            }
        }
    }

    // 詳細ダイアログ (FR-031)
    selectedBodyForDetail?.let { body ->
        val record = discoveries[body.id]
        BodyDetailDialog(
            body = body,
            record = record,
            lang = currentLang,
            uiScale = uiScale,
            onDismiss = { selectedBodyForDetail = null }
        )
    }
}

@Composable
fun BodyGridCard(
    body: CelestialBody,
    isDiscovered: Boolean,
    discovery: DiscoveryRecord?,
    lang: String,
    uiScale: Float,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(enabled = isDiscovered, onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDiscovered) getRarityColor(body.rarity).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // レア度星
            Text(
                text = if (isDiscovered) "★".repeat(body.rarity) else "★",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (9 * uiScale).sp,
                    color = if (isDiscovered) getRarityColor(body.rarity) else MaterialTheme.colorScheme.outline
                )
            )

            // ドット絵
            CelestialBodyPixelArt(
                body = body,
                size = (54 * uiScale).dp,
                isDiscovered = isDiscovered
            )

            // 名称
            Text(
                text = if (isDiscovered) body.name.get(lang) else stringResource(R.string.db_unknown_name),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = (11 * uiScale).sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BodyDetailDialog(
    body: CelestialBody,
    record: DiscoveryRecord?,
    lang: String,
    uiScale: Float,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, getRarityColor(body.rarity))
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ドット絵拡大
                CelestialBodyPixelArt(
                    body = body,
                    size = (110 * uiScale).dp,
                    isDiscovered = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // レア度 & 名前
                Text(
                    text = "★".repeat(body.rarity),
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = getRarityColor(body.rarity),
                        fontSize = (14 * uiScale).sp
                    )
                )
                Text(
                    text = body.name.get(lang),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = (18 * uiScale).sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                if (body.facts.type.isNotBlank()) {
                    Text(
                        text = "${body.facts.type} • ${body.facts.distance}",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (11 * uiScale).sp,
                            color = AccentBlue
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 説明文
                Text(
                    text = body.description.get(lang),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = (12 * uiScale).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    lineHeight = (18 * uiScale).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 観測情報
                record?.let {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.db_first_found, dateFormat.format(Date(it.firstFoundAt))),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (10 * uiScale).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = stringResource(R.string.db_observe_count, it.observeCount),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = (10 * uiScale).sp,
                                color = AccentWarm
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = stringResource(R.string.btn_close),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White,
                            fontSize = (13 * uiScale).sp
                        )
                    )
                }
            }
        }
    }
}
