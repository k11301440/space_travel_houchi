package com.spacetravel.idle.ui.theme

import androidx.compose.ui.graphics.Color

val DarkBg = Color(0xFF000000)
val DarkSurface = Color(0xFF0B0D10)
val DarkSurfaceVariant = Color(0xFF14181D)
val DarkBorder = Color(0xFF2A3038)
val DarkTextPrimary = Color(0xFFF2F4F7)
val DarkTextSecondary = Color(0xFF9AA4AF)

val LightBg = Color(0xFFF4F6F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE6EAEE)
val LightBorder = Color(0xFFC8CFD6)
val LightTextPrimary = Color(0xFF0B0D10)
val LightTextSecondary = Color(0xFF55606B)

val AccentBlue = Color(0xFF5BC8FF)
val AccentBlueLight = Color(0xFF076AA0)
val AccentWarm = Color(0xFFFFB454)
val AccentWarmLight = Color(0xFFA85F00)

val Rarity1 = Color(0xFF9AA4AF) // ★1
val Rarity2 = Color(0xFF7FE3A1) // ★2
val Rarity3 = Color(0xFF5BC8FF) // ★3
val Rarity4 = Color(0xFFC58BFF) // ★4
val Rarity5 = Color(0xFFFFB454) // ★5

fun getRarityColor(rarity: Int): Color = when (rarity) {
    1 -> Rarity1
    2 -> Rarity2
    3 -> Rarity3
    4 -> Rarity4
    5 -> Rarity5
    else -> Rarity1
}
