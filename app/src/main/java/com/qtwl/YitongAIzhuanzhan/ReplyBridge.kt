package com.qtwl.YitongAIzhuanzhan

import java.util.concurrent.ConcurrentHashMap

/** Routes one JavaScript reply callback to the automation request that armed it. */
object ReplyBridge {
    private val pending = ConcurrentHashMap<String, (String) -> Unit>()

    fun register(requestId: String, callback: (String) -> Unit) {
        pending[requestId] = callback
    }

    fun unregister(requestId: String) {
        pending.remove(requestId)
    }

    fun deliver(requestId: String, content: String): Boolean {
        val callback = pending.remove(requestId) ?: return false
        callback(content.trim())
        return true
    }

    internal fun pendingCount(): Int = pending.size

    internal fun clearForTests() {
        pending.clear()
    }
}
