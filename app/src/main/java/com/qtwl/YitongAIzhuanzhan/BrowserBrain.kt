package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.util.Log
import android.webkit.WebView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * 浏览器大脑 — 通过本地 GatewayServer 调用大模型，自动生成并执行浏览器脚本。
 * 最终走的路径：BrowserBrain → GatewayServer(localhost:8889) → 上游AI模型
 * 网关本身已对接好大模型，大脑只需调网关的 /v1/chat/completions 接口。
 */
object BrowserBrain {
    private const val TAG = "BrowserBrain"

    data class BrainResult(
        val success: Boolean,
        val script: String = "",
        val explanation: String = "",
        val error: String = ""
    )

    /**
     * 让大脑生成一个浏览器自动化脚本，直接执行在 WebView 中
     */
    suspend fun generateAndExecute(
        context: Context,
        webView: WebView,
        task: String,
        pageContext: String = ""
    ): BrainResult = withContext(Dispatchers.IO) {
        try {
            val baseUrl = BrowserBrainConfig.getBaseUrl(context)
            val apiKey = BrowserBrainConfig.getApiKey(context)
            val model = BrowserBrainConfig.getModel(context)

            val prompt = buildPrompt(task, pageContext)
            val script = callLlm(baseUrl, apiKey, model, prompt)

            if (script.isBlank()) {
                return@withContext BrainResult(false, error = "大脑返回空脚本")
            }

            // 在主线程执行脚本
            withContext(Dispatchers.Main) {
                webView.evaluateJavascript(script, null)
            }

            BrainResult(true, script = script, explanation = "脚本已执行")
        } catch (e: Exception) {
            Log.e(TAG, "generateAndExecute failed", e)
            BrainResult(false, error = e.message ?: "未知错误")
        }
    }

    /**
     * 只生成脚本，不执行（用于预览）
     */
    suspend fun generateOnly(
        context: Context,
        task: String,
        pageContext: String = ""
    ): BrainResult = withContext(Dispatchers.IO) {
        try {
            val baseUrl = BrowserBrainConfig.getBaseUrl(context)
            val apiKey = BrowserBrainConfig.getApiKey(context)
            val model = BrowserBrainConfig.getModel(context)

            val prompt = buildPrompt(task, pageContext)
            val script = callLlm(baseUrl, apiKey, model, prompt)

            if (script.isBlank()) {
                return@withContext BrainResult(false, error = "大脑返回空脚本")
            }
            BrainResult(true, script = script)
        } catch (e: Exception) {
            Log.e(TAG, "generateOnly failed", e)
            BrainResult(false, error = e.message ?: "未知错误")
        }
    }

    /**
     * 构建给大模型的提示词
     */
    private fun buildPrompt(task: String, pageContext: String): String {
        return """你是一个浏览器自动化专家。根据用户的任务描述，生成纯JavaScript代码。
任务：$task
${if (pageContext.isNotBlank()) "当前页面上下文：$pageContext" else ""}

要求：
1. 只输出纯JavaScript代码，不要 markdown 包裹
2. 代码可以直接在浏览器控制台执行
3. 使用标准的 DOM API（document.querySelector, .click, .value 等）
4. 完成后 console.log 返回结果
5. 如果页面有AI输入框，自动填入内容并发送
6. 等待必要的加载时间使用 setTimeout 或 Promise"""
    }

    /**
     * 调用本地 GatewayServer 的 OpenAI 兼容接口
     * 默认走 http://localhost:8889/v1/chat/completions
     */
    private fun callLlm(baseUrl: String, apiKey: String, model: String, prompt: String): String {
        val url = URL("${baseUrl.trimEnd('/')}/v1/chat/completions")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json")
        if (apiKey.isNotBlank()) {
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        conn.doOutput = true
        conn.connectTimeout = 30000
        conn.readTimeout = 60000

        val body = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply { put("role", "system"); put("content", "你是一个浏览器自动化专家，输出纯JavaScript代码。") })
                put(JSONObject().apply { put("role", "user"); put("content", prompt) })
            })
            put("stream", false)
            put("max_tokens", 4096)
            put("temperature", 0.3)
        }

        try {
            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            val responseCode = conn.responseCode
            val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val response = reader.readText()
            reader.close()

            if (responseCode !in 200..299) {
                throw Exception("API返回错误 $responseCode: $response")
            }

            val json = JSONObject(response)
            val choices = json.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val msg = choices.getJSONObject(0).optJSONObject("message")
                val content = msg?.optString("content", "") ?: ""
                return cleanScript(content)
            }
            throw Exception("API返回格式异常")
        } catch (e: Exception) {
            Log.e(TAG, "callLlm failed", e)
            throw e
        } finally {
            conn.disconnect()
        }
    }

    /**
     * 清理大模型返回的内容，提取纯JS代码
     */
    private fun cleanScript(raw: String): String {
        var s = raw.trim()
        // 去掉 markdown 代码块包裹
        if (s.startsWith("```")) {
            s = s.substringAfter("```").substringAfter("\n").substringBeforeLast("```").trim()
        }
        // 去掉 javascript 标记
        if (s.startsWith("javascript", ignoreCase = true)) {
            s = s.substringAfter("\n").trim()
        }
        return s
    }
}