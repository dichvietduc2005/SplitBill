package com.example.splitbill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.splitbill.data.SettingsManager
import com.example.splitbill.theme.SplitBillTheme
import com.example.splitbill.ui.localization.AppLanguage
import com.example.splitbill.ui.localization.LocalAppLanguage

import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val settingsManager = remember { SettingsManager(applicationContext) }
      val themeMode by settingsManager.themeMode.collectAsState(initial = "system")
      val fontScale by settingsManager.fontScale.collectAsState(initial = 1.0f)
      val languageCode by settingsManager.language.collectAsState(initial = "vi")

      val darkTheme = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
      }

      val appLanguage = when (languageCode) {
        "en" -> AppLanguage.ENGLISH
        else -> AppLanguage.VIETNAMESE
      }

      CompositionLocalProvider(
        LocalAppLanguage provides appLanguage,
        LocalDensity provides Density(
          density = LocalDensity.current.density,
          fontScale = fontScale
        )
      ) {
        SplitBillTheme(darkTheme = darkTheme) {
          Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
          ) {
            MainNavigation(settingsManager)
          }
        }
      }
    }
  }
}

