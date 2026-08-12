package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.ui.GameScreen
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        val language = newBase.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .getString(LANGUAGE_KEY, null)
        if (language == null) {
            super.attachBaseContext(newBase)
            return
        }

        val configuration = Configuration(newBase.resources.configuration).apply {
            setLocale(Locale.forLanguageTag(language))
        }
        super.attachBaseContext(newBase.createConfigurationContext(configuration))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var selectedLanguage by remember { mutableStateOf(getSelectedLanguage()) }
            var backgroundMusicEnabled by remember { mutableStateOf(getBackgroundMusicEnabled()) }
            val localizedConfiguration = remember(selectedLanguage) {
                Configuration(resources.configuration).apply {
                    setLocale(selectedLanguage?.let(Locale::forLanguageTag) ?: Locale.getDefault())
                }
            }
            val localizedContext = remember(selectedLanguage) {
                createConfigurationContext(localizedConfiguration)
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration
            ) {
                MyApplicationTheme {
                    GameScreen(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelected = { language ->
                            saveSelectedLanguage(language)
                            selectedLanguage = language
                        },
                        backgroundMusicEnabled = backgroundMusicEnabled,
                        onBackgroundMusicChanged = { enabled ->
                            backgroundMusicEnabled = enabled
                            saveBackgroundMusicEnabled(enabled)
                        }
                    )
                }
            }
        }
    }

    private fun getSelectedLanguage(): String? =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(LANGUAGE_KEY, null)

    private fun saveSelectedLanguage(language: String?) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .apply {
                if (language == null) remove(LANGUAGE_KEY) else putString(LANGUAGE_KEY, language)
            }
            .apply()
    }

    private fun getBackgroundMusicEnabled(): Boolean =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getBoolean(BACKGROUND_MUSIC_KEY, true)

    private fun saveBackgroundMusicEnabled(enabled: Boolean) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putBoolean(BACKGROUND_MUSIC_KEY, enabled)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "settings"
        const val LANGUAGE_KEY = "language"
        const val BACKGROUND_MUSIC_KEY = "backgroundMusic"
    }
}
