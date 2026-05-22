package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = SoftPeachPrimary,
    secondary = DeepBrownSecondary,
    tertiary = WarmPeachCard,
    background = DeepBrownSecondary,
    surface = DarkBrownText
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SoftPeachPrimary,
    secondary = DeepBrownSecondary,
    tertiary = WarmPeachCard,
    background = WarmBackground,
    surface = WhiteCard,
    onPrimary = DeepBrownSecondary,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onBackground = TextSlate,
    onSurface = TextSlate
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+ (disabling to enforce custom Natural Tones theme)
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
