package com.qtwl.YitongAIzhuanzhan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.qtwl.YitongAIzhuanzhan.GatewayPrefs
import android.os.Build

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            if (GatewayPrefs.isEnabled(context)) {
                GatewayService.start(context)
            }
        }
    }
}