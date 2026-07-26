package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import androidx.core.content.edit

object GatewayPrefs {
    const val DEFAULT_PORT = 8080

    private const val PREFS_NAME = "gateway_prefs"
    private const val KEY_ENABLED = "gateway_enabled"
    private const val KEY_HOST = "gateway_host"
    private const val KEY_PORT = "gateway_port"
    private const val KEY_API_KEY = "gateway_api_key"
    private const val KEY_UA = "custom_ua"
    private const val KEY_TEXT_ZOOM = "text_zoom"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun getHost(context: Context): String =
        prefs(context).getString(KEY_HOST, "0.0.0.0") ?: "0.0.0.0"

    fun setHost(context: Context, host: String) {
        prefs(context).edit { putString(KEY_HOST, host) }
    }

    fun getPort(context: Context): String =
        prefs(context).getString(KEY_PORT, DEFAULT_PORT.toString()) ?: DEFAULT_PORT.toString()

    fun getPortNumber(context: Context): Int =
        getPort(context).toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_PORT

    fun setPort(context: Context, port: String) {
        prefs(context).edit { putString(KEY_PORT, port) }
    }

    fun getApiKey(context: Context): String = prefs(context).getString(KEY_API_KEY, "") ?: ""

    fun setApiKey(context: Context, apiKey: String) {
        prefs(context).edit { putString(KEY_API_KEY, apiKey) }
    }

    fun getUserAgent(context: Context): String = prefs(context).getString(
        KEY_UA,
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    ) ?: "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    fun setUserAgent(context: Context, ua: String) {
        prefs(context).edit { putString(KEY_UA, ua) }
    }

    fun getTextZoom(context: Context): Int = prefs(context).getInt(KEY_TEXT_ZOOM, 100)

    fun setTextZoom(context: Context, zoom: Int) {
        prefs(context).edit { putInt(KEY_TEXT_ZOOM, zoom) }
    }
}
