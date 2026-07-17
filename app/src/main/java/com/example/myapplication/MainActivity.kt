package com.example.myapplication

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import java.util.Locale
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
            MyApplicationTheme {
                GameScreen(
                    selectedLanguage = getSelectedLanguage(),
                    onLanguageSelected = ::setSelectedLanguage
                )
            }
        }
    }

    private fun getSelectedLanguage(): String? =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(LANGUAGE_KEY, null)

    private fun setSelectedLanguage(language: String?) {
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)
            .edit()
            .apply {
                if (language == null) remove(LANGUAGE_KEY) else putString(LANGUAGE_KEY, language)
            }
            .apply()
        recreate()
    }

    private companion object {
        const val PREFERENCES_NAME = "settings"
        const val LANGUAGE_KEY = "language"
    }
}
