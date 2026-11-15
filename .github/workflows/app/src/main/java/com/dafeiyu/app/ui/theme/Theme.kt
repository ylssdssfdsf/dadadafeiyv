package com.dafeiyu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FishBlue = Color(0xFF2E6BE6)
private val FishBlueDark = Color(0xFF8FB8FF)
private val BubbleLight = Color(0xFFE8F1FF)
private val BubbleDark = Color(0xFF1B2C48)

object FishColors {
    val bubbleLight = BubbleLight
    val bubbleDark = BubbleDark
}

@Composable
fun DaFeiYuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) {
        darkColorScheme(primary = FishBlueDark, secondary = FishBlueDark)
    } else {
        lightColorScheme(primary = FishBlue, secondary = FishBlue)
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
