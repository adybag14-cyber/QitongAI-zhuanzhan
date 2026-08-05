package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.content.SharedPreferences

object BrowserBrainConfig {
    private const val PREFS_NAME = "browser_brain_prefs"
    private const val KEY_ENABLED = "brain_enabled"
    private const val KEY_BASE_URL = "brain_base_url"
    private const val KEY_API_KEY = "brain_api_key"
    private const val KEY_MODEL = "brain_model"
    private const val KEY_MCP_PORT = "mcp_port"
    private const val KEY_MCP_ENABLED = "mcp_enabled"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_ENABLED, false)
    fun setEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_ENABLED, v).apply()

    fun getBaseUrl(ctx: Context): String = prefs(ctx).getString(KEY_BASE_URL, "http://localhost:7773") ?: "http://localhost:7773"
    fun setBaseUrl(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_BASE_URL, v).apply()

    fun getApiKey(ctx: Context): String = prefs(ctx).getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_API_KEY, v).apply()

    fun getModel(ctx: Context): String = prefs(ctx).getString(KEY_MODEL, "qtai-sj") ?: "qtai-sj"
    fun setModel(ctx: Context, v: String) = prefs(ctx).edit().putString(KEY_MODEL, v).apply()

    fun getMcpPort(ctx: Context): Int = prefs(ctx).getInt(KEY_MCP_PORT, 7774)
    fun setMcpPort(ctx: Context, v: Int) = prefs(ctx).edit().putInt(KEY_MCP_PORT, v).apply()

    fun isMcpEnabled(ctx: Context): Boolean = prefs(ctx).getBoolean(KEY_MCP_ENABLED, false)
    fun setMcpEnabled(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_MCP_ENABLED, v).apply()
}