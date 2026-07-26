package com.qtwl.YitongAIzhuanzhan

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {
    private const val CH = "qitong_status"
    private const val ID = 2001

    fun init(ctx: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val c = android.app.NotificationChannel(
                CH, "綦桐状态", android.app.NotificationManager.IMPORTANCE_LOW
            )
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(c)
        }
    }

    fun update(ctx: Context, title: String, text: String) {
        val n = NotificationCompat.Builder(ctx, CH)
            .setContentTitle(title).setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW).build()
        ctx.getSystemService(NotificationManager::class.java)?.notify(ID, n)
    }

    fun showReply(ctx: Context, source: String, content: String) =
        update(ctx, "綦桐AI转站 · $source", "收到回复 (${content.length}字)")
}