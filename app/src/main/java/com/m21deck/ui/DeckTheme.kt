package com.m21deck.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFFF9850)
val DeepBlack = Color(0xFF090A0C)
val Panel = Color(0xFF1D2024)
val Dim = Color(0xFF9D9B9C)
val Bone = Color(0xFFF5E9E1)

@Composable
fun DeckTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Amber, onPrimary = DeepBlack,
        background = DeepBlack, onBackground = Bone,
        surface = Color(0xFF131518), onSurface = Bone,
        surfaceVariant = Panel, onSurfaceVariant = Color(0xFFCBC5C1),
        outline = Color(0xFF464348)
    ), content = content)
}
