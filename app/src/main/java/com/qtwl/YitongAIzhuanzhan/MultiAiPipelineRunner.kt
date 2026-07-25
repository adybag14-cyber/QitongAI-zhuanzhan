package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** Android/WebView executor for [PipelineCoordinator]. */
class MultiAiPipelineRunner(
    context: Context,
    private val onSnapshot: (PipelineSnapshot) -> Unit
) {
    private val webContext = context
    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeAutomation = AtomicReference<AutomationHandle?>(null)

    private val coordinator = PipelineCoordinator(
        PipelineStepExecutor { step, renderedPrompt, callback ->
            val cancelled = AtomicBoolean(false)
            val cancellation = PipelineCancellation {
                cancelled.set(true)
                activeAutomation.getAndSet(null)?.cancel()
            }

            mainHandler.post {
                if (cancelled.get()) return@post
                try {
                    val platform = AiPlatformRegistry.require(step.platformId)
                    val tab = WebViewManager.getOrCreatePlatformTab(webContext, platform)
                    WebViewManager.switchToTabId(tab.id)
                    val webView = WebViewManager.initWebView(webContext, tab.id)
                    if (webView == null) {
                        callback(
                            PipelineExecutionResult(
                                success = false,
                                detail = "Could not create the ${platform.displayName} browser tab"
                            )
                        )
                        return@post
                    }

                    // Give Compose one frame to attach the selected WebView before DOM polling.
                    mainHandler.postDelayed({
                        if (cancelled.get()) return@postDelayed
                        val handle = JsInjector.sendAndAwaitReply(
                            platformId = platform.id,
                            webView = webView,
                            message = renderedPrompt
                        ) { result ->
                            activeAutomation.set(null)
                            if (cancelled.get()) return@sendAndAwaitReply
                            callback(
                                PipelineExecutionResult(
                                    success = result.success,
                                    output = result.response,
                                    detail = result.detail
                                )
                            )
                        }
                        activeAutomation.set(handle)
                    }, 350L)
                } catch (error: Exception) {
                    callback(
                        PipelineExecutionResult(
                            success = false,
                            detail = error.message ?: "Unexpected pipeline execution error"
                        )
                    )
                }
            }
            cancellation
        }
    )

    fun start(prompt: String, platformIds: List<String>): Int {
        val steps = platformIds.map { PipelineStep(platformId = it) }
        return coordinator.start(prompt, steps, onSnapshot)
    }

    fun startWithSteps(prompt: String, steps: List<PipelineStep>): Int =
        coordinator.start(prompt, steps, onSnapshot)

    fun cancel() {
        activeAutomation.getAndSet(null)?.cancel()
        coordinator.cancel()
    }

    fun snapshot(): PipelineSnapshot = coordinator.currentSnapshot()
}
