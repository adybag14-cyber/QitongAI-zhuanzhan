package com.qtwl.YitongAIzhuanzhan

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.os.Build

object AppHider {
    private const val PREFS = "app_hider"
    private const val KEY = "hide_when_busy"

    fun isEnabled(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false)
    fun setEnabled(ctx: Context, on: Boolean) =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply()

    fun hide(activity: Activity) {
        if (!isEnabled(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.appTasks?.firstOrNull { task ->
                    val info = task.taskInfo
                    info.baseActivity?.className?.contains("MainActivity") == true
                }?.setExcludeFromRecents(true)
            } catch (_: Exception) {}
        }
    }
    fun show(activity: Activity) {
        if (!isEnabled(activity)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val am = activity.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                am?.appTasks?.firstOrNull { task ->
                    val info = task.taskInfo
                    info.baseActivity?.className?.contains("MainActivity") == true
                }?.setExcludeFromRecents(false)
            } catch (_: Exception) {}
        }
    }
}