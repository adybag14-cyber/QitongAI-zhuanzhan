package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {
    private const val PREFS_NAME = "locale_prefs"
    private const val KEY_LANG = "selected_language"

    // 0=跟随系统, 1=简体中文, 2=繁体台湾, 3=繁体香港, 4=English
    private var currentLangIndex: Int = 0

    fun getLanguageIndex(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentLangIndex = prefs.getInt(KEY_LANG, 0)
        return currentLangIndex
    }

    fun setLanguageIndex(context: Context, index: Int) {
        currentLangIndex = index
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_LANG, index).commit() // 同步写入，确保recreate前已保存
    }

    fun getLocale(context: Context): Locale {
        val index = getLanguageIndex(context)
        return when (index) {
            1 -> Locale("zh", "CN")
            2 -> Locale("zh", "TW")
            3 -> Locale("zh", "HK")
            4 -> Locale("en")
            else -> Locale.getDefault()
        }
    }

    fun applyLocale(context: Context): Context {
        val index = getLanguageIndex(context)
        if (index == 0) return context // 跟随系统

        val locale = getLocale(context)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}