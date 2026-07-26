package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class GatewayServer(
    private val context: Context,
    private val port: Int = 7773
) : NanoHTTPD(port) {

    var onRequestReceived: ((String) -> Unit)? = null
    var onReplyReady: ((String) -> Unit)? = null

    private val replyLatches = ConcurrentHashMap<String, java.util.concurrent.CountDownLatch>()

    override fun serve(session: NanoHTTPD.IHTTPSession): NanoHTTPD.Response {
        val uri = session.uri
        // API Key 校验
        val savedKey = GatewayPrefs.getApiKey(context)
        if (savedKey.isNotEmpty()) {
            val auth = session.headers["authorization"] ?: ""
            if (!auth.equals("Bearer $savedKey", ignoreCase = true)) {
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json", """{"error":"unauthorized"}""")
            }
        }
        return when {
            uri == "/v1/models" && session.method == Method.GET -> {
                val json = JSONObject().apply {
                    put("object", "list")
                    put("data", JSONArray().apply {
                        put(JSONObject().apply {
                            put("id", "qtllq"); put("object", "model"); put("owned_by", "qitong")
                        })
                    })
                }
                jsonResponse(json.toString())
            }
            uri == "/v1/chat/completions" && session.method == Method.POST -> handleChat(session)
            else -> newFixedLengthResponse(Response.Status.NOT_FOUND, "application/json", """{"error":"not found"}""")
        }
    }

    private fun handleChat(session: NanoHTTPD.IHTTPSession): Response {
        val body = readBody(session)
        val json = JSONObject(body)
        val stream = json.optBoolean("stream", false)
        val msgs = json.getJSONArray("messages")

        var lastUser = ""
        for (i in msgs.length() - 1 downTo 0) {
            val m = msgs.getJSONObject(i)
            if (m.getString("role") == "user") { lastUser = m.getString("content"); break }
        }
        if (lastUser.isEmpty()) return jsonResponse("""{"error":"no user msg"}""")

        onRequestReceived?.invoke(lastUser)

        val wv = try { WebViewManager.getCurrentTab()?.webView } catch (e: Exception) { null }
        if (wv == null) return jsonResponse("""{"error":"webview not ready"}""")

        val latch = java.util.concurrent.CountDownLatch(1)
        val key = lastUser.hashCode().toString()
        replyLatches[key] = latch
        var reply = ""

        try {
            JsInjector.autoSendMessage(wv, lastUser) { success, result ->
                reply = if (success) result else "$result"
                replyLatches.remove(key)
                latch.countDown()
            }
        } catch (e: Exception) {
            replyLatches.remove(key)
            latch.countDown()
        }

        latch.await(60, java.util.concurrent.TimeUnit.SECONDS)
        onReplyReady?.invoke(reply)

        return if (!stream) {
            val resp = JSONObject().apply {
                put("id", "chatcmpl-qtllq-${System.currentTimeMillis()}")
                put("object", "chat.completion")
                put("model", "qtllq")
                put("choices", JSONArray().apply {
                    put(JSONObject().apply {
                        put("index", 0)
                        put("message", JSONObject().apply { put("role", "assistant"); put("content", reply) })
                        put("finish_reason", "stop")
                    })
                })
            }
            jsonResponse(resp.toString())
        } else {
            val sse = "data: ${JSONObject().apply {
                put("choices", JSONArray().apply {
                    put(JSONObject().apply { put("delta", JSONObject().apply { put("content", reply) }) })
                })
            }.toString()}\n\ndata: [DONE]\n\n"
            newFixedLengthResponse(Response.Status.OK, "text/event-stream", sse)
        }
    }

    private fun readBody(session: NanoHTTPD.IHTTPSession): String {
        val len = session.headers["content-length"]?.toIntOrNull() ?: 0
        if (len <= 0) return ""
        val buf = ByteArray(len)
        session.inputStream.read(buf, 0, len)
        return String(buf, Charsets.UTF_8)
    }

    private fun jsonResponse(json: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json)

    fun startServer() {
        try {
            start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            Log.i("Gateway", "qtllq gateway started on port $port")
        } catch (e: Exception) { Log.e("Gateway", "Gateway start failed: ${e.message}") }
    }
}