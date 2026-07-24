package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import com.qtwl.YitongAIzhuanzhan.ui.screens.AboutScreen
import com.qtwl.YitongAIzhuanzhan.ui.screens.BookmarkEditScreen
import com.qtwl.YitongAIzhuanzhan.ui.screens.BrowserScreen
import com.qtwl.YitongAIzhuanzhan.ui.screens.SettingsScreen
import com.qtwl.YitongAIzhuanzhan.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocaleManager.applyLocale(this)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("browser") }

                when (currentScreen) {
                    "settings" -> {
                        SettingsScreen(onBack = { currentScreen = "about" })
                    }
                    "bookmarks" -> {
                        BookmarkEditScreen(onBack = { currentScreen = "about" })
                    }
                    "about" -> {
                        AboutScreen(
                            onBack = { currentScreen = "browser" },
                            onLanguageChanged = { recreate() },
                            onNavigateToSettings = { currentScreen = "settings" },
                            onNavigateToBookmarks = { currentScreen = "bookmarks" }
                        )
                    }
                    else -> {
                        BrowserScreen(onNavigateToAbout = { currentScreen = "about" })
                    }
                }
            }
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.applyLocale(newBase))
    }
}