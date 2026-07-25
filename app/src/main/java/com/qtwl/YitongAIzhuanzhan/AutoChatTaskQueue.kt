package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/** One independent web-AI task processed by [AutoChatTaskQueue]. */
data class AutoChatTask(
    val id: Int = 0,
    val platform: String,
    val url: String,
    val message: String,
    val callback: ((AutoChatResult) -> Unit)? = null
)

data class AutoChatResult(
    val taskId: Int,
    val success: Boolean,
    val message: String,
    val detail: String = "",
    val response: String = ""
)

/**
 * Serial task queue retained for callers that need independent jobs rather than
 * output-to-input chaining. It now uses a platform-specific WebView, navigates
 * to the requested service, and waits for a completed assistant reply.
 *
 * Multi-stage chaining is implemented by [MultiAiPipelineRunner].
 */
object AutoChatTaskQueue {
    private val taskQueue = ConcurrentLinkedQueue<AutoChatTask>()
    private val isRunning = AtomicBoolean(false)
    private val taskIdCounter = AtomicInteger(0)
    private val handler = Handler(Looper.getMainLooper())
    private var contextRef: WeakReference<Context>? = null
    private var activeHandle: AutomationHandle? = null

    var onTaskStarted: ((AutoChatTask) -> Unit)? = null
    var onTaskCompleted: ((AutoChatResult) -> Unit)? = null
    var onTaskFailed: ((AutoChatResult, Exception) -> Unit)? = null
    var onQueueEmpty: (() -> Unit)? = null

    fun initialize(context: Context) {
        contextRef = WeakReference(context)
    }

    fun enqueue(task: AutoChatTask): Int {
        val id = taskIdCounter.incrementAndGet()
        taskQueue.add(task.copy(id = id))
        return id
    }

    fun addTask(
        platform: String,
        url: String,
        message: String,
        callback: ((AutoChatResult) -> Unit)? = null
    ): Int = enqueue(
        AutoChatTask(
            platform = platform,
            url = url,
            message = message,
            callback = callback
        )
    )

    fun start() {
        if (isRunning.compareAndSet(false, true)) processNext()
    }

    fun stop() {
        isRunning.set(false)
        activeHandle?.cancel()
        activeHandle = null
        taskQueue.clear()
    }

    fun pause() {
        isRunning.set(false)
        activeHandle?.cancel()
        activeHandle = null
    }

    fun resume() {
        if (taskQueue.isNotEmpty() && isRunning.compareAndSet(false, true)) processNext()
    }

    private fun processNext() {
        if (!isRunning.get()) return
        val task = taskQueue.poll()
        if (task == null) {
            isRunning.set(false)
            onQueueEmpty?.invoke()
            return
        }

        onTaskStarted?.invoke(task)
        executeTask(task) { result ->
            task.callback?.invoke(result)
            onTaskCompleted?.invoke(result)
            if (!isRunning.get()) return@executeTask
            handler.postDelayed(::processNext, 1_000L)
        }
    }

    private fun executeTask(task: AutoChatTask, onResult: (AutoChatResult) -> Unit) {
        val context = contextRef?.get()
        if (context == null) {
            onResult(
                AutoChatResult(
                    taskId = task.id,
                    success = false,
                    message = "Queue is not initialised",
                    detail = "Call AutoChatTaskQueue.initialize(context) before starting jobs"
                )
            )
            return
        }

        try {
            val configured = AiPlatformRegistry.get(task.platform)
            val detected = AiPlatformRegistry.detect(task.url)
            val platform = configured ?: detected.takeUnless { it.id == "generic" }
                ?: AiPlatformRegistry.generic().copy(
                    id = task.platform.ifBlank { "generic" },
                    url = task.url.ifBlank { "about:blank" }
                )
            val tab = WebViewManager.getOrCreatePlatformTab(context, platform)
            if (task.url.isNotBlank() && tab.url != task.url) {
                tab.url = task.url
                tab.webView?.loadUrl(task.url)
            }
            WebViewManager.switchToTabId(tab.id)
            val webView = WebViewManager.initWebView(context, tab.id)
            if (webView == null) {
                onResult(
                    AutoChatResult(
                        taskId = task.id,
                        success = false,
                        message = "WebView unavailable",
                        detail = "Platform: ${task.platform}"
                    )
                )
                return
            }

            handler.postDelayed({
                activeHandle = JsInjector.sendAndAwaitReply(
                    platformId = platform.id,
                    webView = webView,
                    message = task.message
                ) { automation ->
                    activeHandle = null
                    onResult(
                        AutoChatResult(
                            taskId = task.id,
                            success = automation.success,
                            message = if (automation.success) "Completed" else "Failed",
                            detail = automation.detail,
                            response = automation.response
                        )
                    )
                }
            }, 350L)
        } catch (error: Exception) {
            val result = AutoChatResult(
                taskId = task.id,
                success = false,
                message = "Execution error",
                detail = error.message ?: "Unknown error"
            )
            onTaskFailed?.invoke(result, error)
            onResult(result)
        }
    }

    fun getQueueSize(): Int = taskQueue.size
    fun isBusy(): Boolean = isRunning.get()
    fun clear() = taskQueue.clear()
}
