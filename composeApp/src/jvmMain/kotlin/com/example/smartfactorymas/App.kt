package com.example.smartfactorymas

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/**
 * Application root — applies the theme and delegates to [MainScreen].
 */
@Composable
@Preview
fun App() {
    SmartFactoryTheme {
        MainScreen()
    }
}