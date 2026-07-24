package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 自动对话任务队列
 * 支持多任务排队、串行/并行执行、回调通知
 */
data class AutoChatTask(
    val id: Int,
    val platform: String,      // 平台名称：doubao, yuanbao, deepseek...
    val url: String,           // 目标URL
    val message: String,       // 要发送的消息
    val callback: ((AutoChatResult) -> Unit)? = null
)

data class AutoChatResult(
    val taskId: Int,
    val success: Boolean,
    val message: String,
    val detail: String = ""
)

object AutoChatTaskQueue {
    
    private val taskQueue = ConcurrentLinkedQueue<AutoChatTask>()
    private val isRunning = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private var taskIdCounter = 0
    
    // 任务执行监听器
    var onTaskStarted: ((AutoChatTask) -> Unit)? = null
    var onTaskCompleted: ((AutoChatResult) -> Unit)? = null
    var onTaskFailed: ((AutoChatResult, Exception) -> Unit)? = null
    var onQueueEmpty: (() -> Unit)? = null
    
    /**
     * 添加任务到队列
     */
    fun enqueue(task: AutoChatTask): Int {
        val t = task.copy(id = ++taskIdCounter)
        taskQueue.add(t)
        return t.id
    }
    
    /**
     * 添加任务（简化版）
     */
    fun addTask(platform: String, url: String, message: String, callback: ((AutoChatResult) -> Unit)? = null): Int {
        val task = AutoChatTask(
            id = ++taskIdCounter,
            platform = platform,
            url = url,
            message = message,
            callback = callback
        )
        taskQueue.add(task)
        return task.id
    }
    
    /**
     * 开始执行队列
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            processNext()
        }
    }
    
    /**
     * 停止执行
     */
    fun stop() {
        isRunning.set(false)
        taskQueue.clear()
    }
    
    /**
     * 暂停执行
     */
    fun pause() {
        isRunning.set(false)
    }
    
    /**
     * 继续执行
     */
    fun resume() {
        if (!isRunning.get() && taskQueue.isNotEmpty()) {
            isRunning.set(true)
            processNext()
        }
    }
    
    /**
     * 处理下一个任务
     */
    private fun processNext() {
        if (!isRunning.get()) return
        
        val task = taskQueue.poll()
        if (task == null) {
            isRunning.set(false)
            onQueueEmpty?.invoke()
            return
        }
        
        onTaskStarted?.invoke(task)
        
        // 执行任务
        executeTask(task) { result ->
            task.callback?.invoke(result)
            onTaskCompleted?.invoke(result)
            
            // 延迟处理下一个任务（避免风控）
            handler.postDelayed({
                processNext()
            }, 1000 + (Math.random() * 2000).toLong())
        }
    }
    
    /**
     * 执行单个任务
     */
    private fun executeTask(task: AutoChatTask, onResult: (AutoChatResult) -> Unit) {
        try {
            // 获取或创建 WebView
            val webView = getWebViewForTask(task)
            if (webView == null) {
                val result = AutoChatResult(
                    taskId = task.id,
                    success = false,
                    message = "无法创建WebView",
                    detail = "平台: ${task.platform}"
                )
                onResult(result)
                return
            }
            
            // 真人化发送
            HumanLikeInput.sendLikeHuman(webView, task.message) { success, detail ->
                val result = AutoChatResult(
                    taskId = task.id,
                    success = success,
                    message = if (success) "发送成功" else "发送失败",
                    detail = detail
                )
                onResult(result)
            }
            
        } catch (e: Exception) {
            val result = AutoChatResult(
                taskId = task.id,
                success = false,
                message = "执行异常",
                detail = e.message ?: "未知错误"
            )
            onTaskFailed?.invoke(result, e)
            onResult(result)
        }
    }
    
    /**
     * 获取任务对应的 WebView
     */
    private fun getWebViewForTask(task: AutoChatTask): WebView? {
        // 这里需要从 WebViewManager 获取或创建
        // 简化实现：返回当前标签页的 WebView
        val tab = WebViewManager.getCurrentTab() ?: return null
        return tab.webView
    }
    
    /**
     * 获取队列大小
     */
    fun getQueueSize(): Int = taskQueue.size
    
    /**
     * 是否正在运行
     */
    fun isBusy(): Boolean = isRunning.get()
    
    /**
     * 清空队列
     */
    fun clear() {
        taskQueue.clear()
    }
}