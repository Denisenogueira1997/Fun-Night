package com.example.movienight.ui.theme

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.google.android.material.color.utilities.CorePalette

private val userBaseColor = 0xFF263583.toInt()

@SuppressLint("RestrictedApi")
@Composable
fun CustomColor(content: @Composable () -> Unit) {
    val palette = CorePalette.of(userBaseColor)


    val colorScheme = lightColorScheme(
        primary = Color(palette.a1.tone(40)),
        onPrimary = Color(palette.a1.tone(100)),
        secondary = Color(palette.a2.tone(40)),
        onSecondary = Color(palette.a2.tone(100)),
        background = Color(palette.n1.tone(99)),
        onBackground = Color(palette.n1.tone(10)),
        surface = Color(palette.n1.tone(98)),
        onSurface = Color(palette.n1.tone(10))
    )

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography(), content = content
    )


}