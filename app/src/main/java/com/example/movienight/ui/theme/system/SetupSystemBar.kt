package com.example.movienight.ui.theme.system

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat

@Composable
fun SetupSystemBars() {
    val view = LocalView.current
    val color = MaterialTheme.colorScheme.onPrimary

    SideEffect {
        val window = (view.context as Activity).window
        window.statusBarColor = color.toArgb()
        window.navigationBarColor = color.toArgb()

        WindowInsetsControllerCompat(
            window,
            view
        ).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }
}