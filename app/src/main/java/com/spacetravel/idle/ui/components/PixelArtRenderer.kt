package com.spacetravel.idle.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spacetravel.idle.domain.model.Category
import com.spacetravel.idle.domain.model.CelestialBody
import com.spacetravel.idle.ui.theme.getRarityColor
import kotlin.random.Random

/**
 * 宇宙船のドット絵レンダラー (UI-001, UI-002)
 * 船体スキン、炎フレーム、窓ライトをドット調で描画
 */
@Composable
fun SpaceShipPixelArt(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    skinPrimary: Color = Color(0xFFF2F4F7),
    skinSecondary: Color = Color(0xFF5BC8FF),
    flameColor: Color = Color(0xFFFFB454),
    lightColor: Color = Color(0xFF7CE7FF),
    isCruising: Boolean = false
) {
    // 炎アニメーション (4フレーム)
    val infiniteTransition = rememberInfiniteTransition(label = "flame")
    val flameStep by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flameStep"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pixel = (w / 16f).coerceAtLeast(1f) // 16x16 グリッド

        fun drawDot(x: Int, y: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(x * pixel, y * pixel),
                size = Size(pixel, pixel)
            )
        }

        // --- 船体 (中央 16x16) ---
        // ノーズコーン (y: 2..4)
        drawDot(7, 2, skinSecondary); drawDot(8, 2, skinSecondary)
        drawDot(7, 3, skinPrimary); drawDot(8, 3, skinPrimary)
        drawDot(6, 4, skinSecondary); drawDot(7, 4, skinPrimary); drawDot(8, 4, skinPrimary); drawDot(9, 4, skinSecondary)

        // キャビン & コックピット (y: 5..7)
        for (y in 5..7) {
            for (x in 6..9) {
                val c = if (y == 5 && (x == 7 || x == 8)) lightColor else skinPrimary
                drawDot(x, y, c)
            }
            drawDot(5, y, skinSecondary); drawDot(10, y, skinSecondary)
        }

        // 胴体 (y: 8..11)
        for (y in 8..11) {
            for (x in 5..10) {
                val c = if (x == 5 || x == 10) skinSecondary else skinPrimary
                drawDot(x, y, c)
            }
        }

        // 翼 (y: 9..12)
        drawDot(4, 9, skinSecondary); drawDot(11, 9, skinSecondary)
        drawDot(3, 10, skinSecondary); drawDot(4, 10, skinPrimary); drawDot(11, 10, skinPrimary); drawDot(12, 10, skinSecondary)
        drawDot(2, 11, skinSecondary); drawDot(3, 11, skinPrimary); drawDot(12, 11, skinPrimary); drawDot(13, 11, skinSecondary)
        drawDot(2, 12, skinSecondary); drawDot(13, 12, skinSecondary)

        // エンジンノズル (y: 12)
        drawDot(6, 12, Color(0xFF404854)); drawDot(7, 12, Color(0xFF2A3038)); drawDot(8, 12, Color(0xFF2A3038)); drawDot(9, 12, Color(0xFF404854))

        // --- エンジンの炎 (航行中または停泊中のアイドリング) ---
        val step = flameStep.toInt() % 4
        val flameLength = if (isCruising) 3 + (step % 2) else 1 + (step % 2)
        for (fy in 0..flameLength) {
            val y = 13 + fy
            if (y < 16) {
                val c = if (fy == 0) Color.White else flameColor
                drawDot(7, y, c)
                drawDot(8, y, c)
                if (isCruising && fy == 1) {
                    drawDot(6, y, flameColor.copy(alpha = 0.8f))
                    drawDot(9, y, flameColor.copy(alpha = 0.8f))
                }
            }
        }
    }
}

/**
 * 天体のドット絵レンダラー (UI-001, UI-002, UI-003)
 * カテゴリ・レア度・天体IDに応じた16x16ドットアートを描画
 */
@Composable
fun CelestialBodyPixelArt(
    body: CelestialBody?,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    isDiscovered: Boolean = true
) {
    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val pixel = (w / 16f).coerceAtLeast(1f)

        fun drawDot(x: Int, y: Int, color: Color) {
            drawRect(
                color = color,
                topLeft = Offset(x * pixel, y * pixel),
                size = Size(pixel, pixel)
            )
        }

        if (body == null || !isDiscovered) {
            // 未発見：暗いシルエット (FR-030)
            val silColor = Color(0xFF1E232A)
            for (y in 3..12) {
                for (x in 3..12) {
                    val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                    if (distSq <= 20f) {
                        drawDot(x, y, silColor)
                    }
                }
            }
            return@Canvas
        }

        // 天体固有のパレットシード
        val rng = Random(body.id * 31 + body.rarity)
        val mainColor = when (body.category) {
            Category.minor_body -> Color(0xFFA0C4FF)
            Category.asteroid -> Color(0xFF9E8E81)
            Category.planet -> Color(0xFF5BC8FF)
            Category.star -> Color(0xFFFFB454)
            Category.nebula -> Color(0xFFC58BFF)
            Category.compact -> Color(0xFFE63946)
            Category.probe -> Color(0xFF00F5D4)
        }
        val subColor = getRarityColor(body.rarity)

        when (body.category) {
            Category.minor_body -> {
                // 彗星（尾を引く）
                for (y in 4..11) {
                    for (x in 4..11) {
                        val distSq = (x - 6.5f) * (x - 6.5f) + (y - 8.5f) * (y - 8.5f)
                        if (distSq <= 8f) drawDot(x, y, mainColor)
                    }
                }
                // 尾
                drawDot(9, 6, subColor.copy(alpha = 0.7f)); drawDot(10, 5, subColor.copy(alpha = 0.5f))
                drawDot(11, 4, subColor.copy(alpha = 0.4f)); drawDot(12, 3, subColor.copy(alpha = 0.3f))
                drawDot(9, 7, subColor.copy(alpha = 0.5f)); drawDot(10, 6, subColor.copy(alpha = 0.3f))
            }
            Category.asteroid -> {
                // デコボコの小惑星
                for (y in 4..11) {
                    for (x in 4..11) {
                        val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                        val noise = (rng.nextInt(5) - 2) * 0.8f
                        if (distSq + noise <= 14f) {
                            val c = if ((x + y + body.id) % 3 == 0) subColor else mainColor
                            drawDot(x, y, c)
                        }
                    }
                }
            }
            Category.planet -> {
                // 環や縞模様のある球体
                for (y in 3..12) {
                    for (x in 3..12) {
                        val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                        if (distSq <= 18f) {
                            val c = if (y % 2 == 0) mainColor else subColor
                            drawDot(x, y, c)
                        }
                    }
                }
                // 土星型などの環（IDが偶数の場合）
                if (body.id % 2 == 0) {
                    for (x in 1..14) {
                        if (x !in 6..9) {
                            drawDot(x, 7, Color.White.copy(alpha = 0.8f))
                        }
                    }
                }
            }
            Category.star -> {
                // 輝く恒星（中心は明るく、外周にコロナ）
                for (y in 3..12) {
                    for (x in 3..12) {
                        val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                        if (distSq <= 16f) {
                            val c = if (distSq <= 6f) Color.White else mainColor
                            drawDot(x, y, c)
                        }
                    }
                }
                // フレア十字
                drawDot(7, 1, subColor); drawDot(8, 1, subColor)
                drawDot(7, 14, subColor); drawDot(8, 14, subColor)
                drawDot(1, 7, subColor); drawDot(1, 8, subColor)
                drawDot(14, 7, subColor); drawDot(14, 8, subColor)
            }
            Category.nebula -> {
                // 幻想的な星雲の散光
                for (y in 2..13) {
                    for (x in 2..13) {
                        val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                        if (distSq <= 24f && rng.nextFloat() > 0.35f) {
                            val c = if (rng.nextBoolean()) mainColor else subColor
                            drawDot(x, y, c.copy(alpha = 0.85f))
                        }
                    }
                }
            }
            Category.compact -> {
                // ブラックホール/パルサー（中心の暗黒 + 光輪）
                for (y in 3..12) {
                    for (x in 3..12) {
                        val distSq = (x - 7.5f) * (x - 7.5f) + (y - 7.5f) * (y - 7.5f)
                        if (distSq in 6f..18f) {
                            drawDot(x, y, subColor)
                        } else if (distSq < 6f) {
                            drawDot(x, y, Color.Black)
                        }
                    }
                }
            }
            Category.probe -> {
                // 幾何学的探査機
                drawDot(7, 3, subColor); drawDot(8, 3, subColor)
                for (y in 4..10) {
                    drawDot(7, y, mainColor); drawDot(8, y, mainColor)
                }
                // 太陽電池パネル翼
                for (x in 2..5) { drawDot(x, 7, Color(0xFF5BC8FF)) }
                for (x in 10..13) { drawDot(x, 7, Color(0xFF5BC8FF)) }
                // アンテナ
                drawDot(5, 4, Color.White); drawDot(10, 4, Color.White)
            }
        }
    }
}

/**
 * 星空背景 (UI-004)
 */
@Composable
fun StarfieldBackground(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stars")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "starTwinkle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 固定の星空パターン
        val random = Random(12345)
        for (i in 0..70) {
            val sx = random.nextFloat() * w
            val sy = random.nextFloat() * h
            val sz = if (i % 5 == 0) 3f else 1.8f
            val alpha = if (i % 2 == 0) alphaAnim else 0.7f
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = sz,
                center = Offset(sx, sy)
            )
        }
    }
}
