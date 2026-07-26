package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.util.Log
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class GatewayServer(
    private val context: Context,
    private val port: Int = GatewayPrefs.DEFAULT_PORT
) : NanoHTTPD(port) {

    companion object {
        private const val TAG = "GatewayServer"
        private const val AUTOMATION_TIMEOUT_MS = 150_000L
        private const val HTTP_WAIT_TIMEOUT_MS = 165_000L
    }

    var onRequestReceived: ((String) -> Unit)? = null
    var onReplyReady: ((String) -> Unit)? = null
    var onRequestFailed: ((String) -> Unit)? = null

    // A WebView cannot safely type and submit two prompts at the same time.
    private val requestSlot = Semaphore(1, true)

    override fun serve(session: IHTTPSession): Response {
        val savedKey = GatewayPrefs.getApiKey(context)
        if (savedKey.isNotEmpty()) {
            val auth = session.headers["authorization"].orEmpty()
            if (!auth.equals("Bearer $savedKey", ignoreCase = true)) {
                return errorResponse(Response.Status.UNAUTHORIZED, "unauthorized")
            }
        }

        return try {
            when {
                session.uri == "/health" && session.method == Method.GET -> {
                    jsonResponse(
                        JSONObject()
                            .put("status", if (isRunning()) "ok" else "stopped")
                            .put("port", port)
                            .toString()
                    )
                }

                session.uri == "/v1/models" && session.method == Method.GET -> modelsResponse()
                session.uri == "/v1/chat/completions" && session.method == Method.POST -> handleChat(session)
                else -> errorResponse(Response.Status.NOT_FOUND, "not found")
            }
        } catch (error: Exception) {
            Log.e(TAG, "Gateway request failed", error)
            onRequestFailed?.invoke(error.message ?: "Gateway request failed")
            errorResponse(Response.Status.INTERNAL_ERROR, error.message ?: "internal error")
        }
    }

    private fun modelsResponse(): Response {
        val json = JSONObject().apply {
            put("object", "list")
            put(
                "data",
                JSONArray().put(
                    JSONObject()
                        .put("id", "qtllq")
                        .put("object", "model")
                        .put("owned_by", "qitong")
                )
            )
        }
        return jsonResponse(json.toString())
    }

    private fun handleChat(session: IHTTPSession): Response {
        if (!requestSlot.tryAcquire()) {
            return errorResponse(
                Response.Status.SERVICE_UNAVAILABLE,
                "The gateway is already processing another WebView request"
            )
        }

        try {
            val body = readBody(session)
            val request = runCatching { JSONObject(body) }.getOrElse {
                return errorResponse(Response.Status.BAD_REQUEST, "invalid JSON body")
            }
            val stream = request.optBoolean("stream", false)
            val messages = request.optJSONArray("messages")
                ?: return errorResponse(Response.Status.BAD_REQUEST, "messages must be an array")

            val lastUser = (messages.length() - 1 downTo 0)
                .asSequence()
                .mapNotNull { index -> messages.optJSONObject(index) }
                .firstOrNull { message -> message.optString("role") == "user" }
                ?.optString("content")
                ?.trim()
                .orEmpty()

            if (lastUser.isBlank()) {
                return errorResponse(Response.Status.BAD_REQUEST, "no user message")
            }

            onRequestReceived?.invoke(lastUser)

            val tab = WebViewManager.getCurrentTab()
                ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "webview tab not ready")
            val webView = tab.webView
                ?: return errorResponse(Response.Status.SERVICE_UNAVAILABLE, "webview not ready")
            val platform = tab.platformId
                ?.let(AiPlatformRegistry::get)
                ?: AiPlatformRegistry.detect(tab.url)

            val latch = CountDownLatch(1)
            val resultRef = AtomicReference<WebAutomationResult?>()
            val handle = JsInjector.sendAndAwaitReply(
                platformId = platform.id,
                webView = webView,
                message = lastUser,
                timeoutMs = AUTOMATION_TIMEOUT_MS
            ) { result ->
                resultRef.set(result)
                latch.countDown()
            }

            val completed = latch.await(HTTP_WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            if (!completed) {
                handle.cancel()
                val detail = "Timed out waiting for the AI reply"
                onRequestFailed?.invoke(detail)
                return errorResponse(Response.Status.SERVICE_UNAVAILABLE, detail)
            }

            val result = resultRef.get()
                ?: return errorResponse(Response.Status.INTERNAL_ERROR, "reply task completed without a result")
            if (!result.success || result.response.isBlank()) {
                val detail = result.detail.ifBlank { "AI reply capture failed" }
                onRequestFailed?.invoke(detail)
                return errorResponse(Response.Status.INTERNAL_ERROR, detail)
            }

            onReplyReady?.invoke(result.response)
            return if (stream) streamResponse(result.response) else completionResponse(result.response)
        } finally {
            requestSlot.release()
        }
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        try {
            session.parseBody(files)
        } catch (error: IOException) {
            throw IllegalArgumentException("Could not read request body", error)
        } catch (error: ResponseException) {
            throw IllegalArgumentException(error.message ?: "Could not parse request body", error)
        }
        return files["postData"].orEmpty()
    }

    private fun completionResponse(reply: String): Response {
        val response = JSONObject().apply {
            put("id", "chatcmpl-qtllq-${System.currentTimeMillis()}")
            put("object", "chat.completion")
            put("created", System.currentTimeMillis() / 1000L)
            put("model", "qtllq")
            put(
                "choices",
                JSONArray().put(
                    JSONObject()
                        .put("index", 0)
                        .put(
                            "message",
                            JSONObject()
                                .put("role", "assistant")
                                .put("content", reply)
                        )
                        .put("finish_reason", "stop")
                )
            )
        }
        return jsonResponse(response.toString())
    }

    private fun streamResponse(reply: String): Response {
        val chunk = JSONObject().apply {
            put("id", "chatcmpl-qtllq-${System.currentTimeMillis()}")
            put("object", "chat.completion.chunk")
            put("model", "qtllq")
            put(
                "choices",
                JSONArray().put(
                    JSONObject()
                        .put("index", 0)
                        .put("delta", JSONObject().put("content", reply))
                        .put("finish_reason", JSONObject.NULL)
                )
            )
        }
        val sse = "data: $chunk\n\ndata: [DONE]\n\n"
        return newFixedLengthResponse(Response.Status.OK, "text/event-stream; charset=utf-8", sse).apply {
            addHeader("Cache-Control", "no-cache")
            addHeader("Connection", "close")
        }
    }

    private fun jsonResponse(json: String): Response =
        newFixedLengthResponse(Response.Status.OK, "application/json; charset=utf-8", json)

    private fun errorResponse(status: Response.Status, message: String): Response =
        newFixedLengthResponse(
            status,
            "application/json; charset=utf-8",
            JSONObject().put("error", JSONObject().put("message", message)).toString()
        )

    @Throws(IOException::class)
    fun startServer() {
        start(SOCKET_READ_TIMEOUT, false)
        if (!isRunning()) throw IOException("NanoHTTPD did not enter the running state")
        Log.i(TAG, "Gateway started on port $port")
    }

    fun isRunning(): Boolean = isAlive
}
