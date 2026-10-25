package com.spacetravel.idle.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.spacetravel.idle.R
import com.spacetravel.idle.domain.model.SettlementResult
import com.spacetravel.idle.ui.theme.AccentBlue
import com.spacetravel.idle.ui.theme.AccentWarm
import com.spacetravel.idle.ui.theme.LocalUiScale
import com.spacetravel.idle.ui.theme.getRarityColor
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * 帰還ワープ演出 (FR-021)
 * 約3秒で自動完了、スキップボタン対応
 */
@Composable
fun ReturnAnimationOverlay(
    onFinished: () -> Unit
) {
    val uiScale = LocalUiScale.current
    val infiniteTransition = rememberInfiniteTransition(label = "warp")
    val warpProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "warpSpeed"
    )

    LaunchedEffect(Unit) {
        delay(3000) // 約3秒の演出
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f)),
        contentAlignment = Alignment.Center
    ) {
        // ワープ星線アニメーション
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val random = Random(42)
            for (i in 0..100) {
                val angle = random.nextFloat() * 2 * Math.PI.toFloat()
                val dist = (warpProgress + random.nextFloat()) % 1f * size.width * 0.7f
                val length = (dist * 0.35f).coerceAtLeast(10f)

                val startX = center.x + Math.cos(angle.toDouble()).toFloat() * dist
                val startY = center.y + Math.sin(angle.toDouble()).toFloat() * dist
                val endX = center.x + Math.cos(angle.toDouble()).toFloat() * (dist + length)
                val endY = center.y + Math.sin(angle.toDouble()).toFloat() * (dist + length)

                drawLine(
                    color = Color(0xFF5BC8FF).copy(alpha = (dist / (size.width * 0.7f)).coerceIn(0.2f, 1f)),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 3f
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "HYPERSPACE RE-ENTRY",
                style = MaterialTheme.typography.titleLarge.copy(
                    color = AccentBlue,
                    fontSize = (20 * uiScale).sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "母港へ帰還中...",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White,
                    fontSize = (13 * uiScale).sp
                )
            )
        }

        // スキップボタン
        Button(
            onClick = onFinished,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Text(stringResource(R.string.btn_skip), fontSize = (12 * uiScale).sp)
        }
    }
}

/**
 * 探査報告書ダイアログ (FR-021〜FR-023, FR-054)
 */
@Composable
fun SettlementReportDialog(
    report: SettlementResult,
    lang: String,
    onDismiss: () -> Unit
) {
    val uiScale = LocalUiScale.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (report.isTimeRewound) Color.Red else AccentBlue
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ヘッダー
                Text(
                    text = if (report.isTimeRewound) stringResource(R.string.time_warp_detected) else stringResource(R.string.report_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (18 * uiScale).sp,
                        color = if (report.isTimeRewound) Color.Red else Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (report.isTimeRewound) {
                    // 時刻巻き戻しペナルティ表示 (FR-054)
                    Text(
                        text = stringResource(R.string.time_warp_desc, report.penaltyCredits),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = (12 * uiScale).sp,
                            color = Color(0xFFFFB4B4)
                        ),
                        lineHeight = (18 * uiScale).sp
                    )
                } else if (report.foundBodies.isNotEmpty()) {
                    // 発見物リスト
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(report.foundBodies) { found ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, getRarityColor(found.body.rarity).copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CelestialBodyPixelArt(
                                        body = found.body,
                                        size = (48 * uiScale).dp,
                                        isDiscovered = true
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = found.body.name.get(lang),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = (13 * uiScale).sp,
                                                    color = Color.White
                                                )
                                            )
                                            if (found.isNew) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    color = AccentWarm,
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(R.string.report_new_discovery),
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = Color.Black,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = (9 * uiScale).sp
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "★".repeat(found.body.rarity),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = getRarityColor(found.body.rarity),
                                                fontSize = (10 * uiScale).sp
                                            )
                                        )
                                        Text(
                                            text = "+${found.creditsEarned} クレジット",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = AccentWarm,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = (11 * uiScale).sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.report_credits_earned, report.totalCreditsEarned),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = AccentWarm,
                            fontWeight = FontWeight.Bold,
                            fontSize = (15 * uiScale).sp
                        )
                    )
                } else {
                    // 収穫なしログ (FR-023)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.report_empty_title),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * uiScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = report.emptyLog?.get(lang) ?: "周囲に観測対象は発見されなかった。",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (12 * uiScale).sp,
                                color = Color(0xFFD0D5DD)
                            ),
                            textAlign = TextAlign.Center,
                            lineHeight = (18 * uiScale).sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                ) {
                    Text(
                        text = stringResource(R.string.btn_confirm),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = (14 * uiScale).sp
                        )
                    )
                }
            }
        }
    }
}

/**
 * 初回チュートリアルダイアログ (S10, 3枚スライド)
 */
@Composable
fun TutorialDialog(
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    val uiScale = LocalUiScale.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, AccentBlue)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.tutorial_title),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (18 * uiScale).sp,
                        color = Color.White
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                when (step) {
                    1 -> {
                        Text(
                            text = stringResource(R.string.tutorial_step1_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * uiScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_step1_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (12 * uiScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            lineHeight = (18 * uiScale).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    2 -> {
                        Text(
                            text = stringResource(R.string.tutorial_step2_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * uiScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_step2_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (12 * uiScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            lineHeight = (18 * uiScale).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    3 -> {
                        Text(
                            text = stringResource(R.string.tutorial_step3_title),
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = AccentBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = (14 * uiScale).sp
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.tutorial_step3_desc),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = (12 * uiScale).sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            lineHeight = (18 * uiScale).sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        OutlinedButton(onClick = { step-- }) {
                            Text("前へ", fontSize = (11 * uiScale).sp)
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (step < 3) {
                        Button(
                            onClick = { step++ },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.Black)
                        ) {
                            Text("次へ", fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = AccentWarm, contentColor = Color.Black)
                        ) {
                            Text(stringResource(R.string.tutorial_btn_start), fontSize = (11 * uiScale).sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
