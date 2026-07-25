package com.qtwl.YitongAIzhuanzhan

import android.os.Looper
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class PipelineWebViewInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val webViews = mutableListOf<WebView>()

    @After
    fun cleanUp() {
        onMain {
            webViews.forEach { it.destroy() }
            webViews.clear()
        }
    }

    @Test
    fun threeWebViewsCaptureAndForwardRepliesEndToEnd() {
        val fixtures = mapOf(
            "doubao" to createFixture(
                baseUrl = "https://www.doubao.com/chat/",
                inputHtml = "<textarea data-testid='chat_input_input'></textarea>",
                buttonHtml = "<button id='flow-end-msg-send'>Send</button>",
                responseHtml = "<div data-testid='message_text_content' id='response'></div>",
                loadingHtml = "<div class='loading-spinner' id='loading' style='display:none'>loading</div>",
                prefix = "D:"
            ),
            "yuanbao" to createFixture(
                baseUrl = "https://yuanbao.tencent.com/",
                inputHtml = "<div data-slate-editor='true' contenteditable='true' id='editor'></div>",
                buttonHtml = "<button data-testid='send-button'>Send</button>",
                responseHtml = "<div data-role='assistant' id='response'></div>",
                loadingHtml = "<div data-testid='loading-indicator' id='loading' style='display:none'>loading</div>",
                prefix = "Y:"
            ),
            "deepseek" to createFixture(
                baseUrl = "https://chat.deepseek.com/",
                inputHtml = "<textarea id='editor'></textarea>",
                buttonHtml = "<button aria-label='Send'>Send</button>",
                responseHtml = "<div data-message-author-role='assistant' id='response'></div>",
                loadingHtml = "<div aria-busy='false' id='loading' style='display:none'>loading</div>",
                prefix = "S:"
            )
        )

        val terminal = CountDownLatch(1)
        val finalSnapshot = AtomicReference(PipelineSnapshot.Idle)
        val coordinator = PipelineCoordinator(
            PipelineStepExecutor { step, prompt, callback ->
                val handleRef = AtomicReference<AutomationHandle?>()
                onMain {
                    handleRef.set(
                        JsInjector.sendAndAwaitReply(
                            platformId = step.platformId,
                            webView = fixtures.getValue(step.platformId),
                            message = prompt,
                            timeoutMs = 15_000L
                        ) { result ->
                            callback(
                                PipelineExecutionResult(
                                    success = result.success,
                                    output = result.response,
                                    detail = result.detail
                                )
                            )
                        }
                    )
                }
                PipelineCancellation {
                    onMain { handleRef.get()?.cancel() }
                }
            }
        )

        coordinator.start(
            prompt = "hello",
            pipelineSteps = listOf(
                PipelineStep("doubao", maxRetries = 0),
                PipelineStep("yuanbao", maxRetries = 0),
                PipelineStep("deepseek", maxRetries = 0)
            )
        ) { snapshot ->
            finalSnapshot.set(snapshot)
            if (snapshot.state in setOf(
                    PipelineRunState.SUCCEEDED,
                    PipelineRunState.FAILED,
                    PipelineRunState.CANCELLED
                )
            ) {
                terminal.countDown()
            }
        }

        assertTrue("Pipeline did not finish in time", terminal.await(45, TimeUnit.SECONDS))
        val result = finalSnapshot.get()
        assertEquals(result.detail, PipelineRunState.SUCCEEDED, result.state)
        assertEquals("S:Y:D:hello", result.finalOutput)
        assertEquals(
            listOf("D:hello", "Y:D:hello", "S:Y:D:hello"),
            result.steps.map { it.output }
        )
    }

    @Test
    fun echoedUserPromptIsNotAcceptedAsAssistantReply() {
        val fixture = createFixture(
            baseUrl = "https://chat.deepseek.com/",
            inputHtml = "<textarea id='editor'></textarea>",
            buttonHtml = "<button aria-label='Send'>Send</button>",
            responseHtml = "<div data-message-author-role='assistant' id='response'></div>",
            loadingHtml = "<div aria-busy='false' id='loading' style='display:none'>loading</div>",
            prefix = ""
        )
        val terminal = CountDownLatch(1)
        val captured = AtomicReference<WebAutomationResult>()

        onMain {
            JsInjector.sendAndAwaitReply(
                platformId = "deepseek",
                webView = fixture,
                message = "echo-me",
                timeoutMs = 4_000L
            ) { result ->
                captured.set(result)
                terminal.countDown()
            }
        }

        assertTrue("Echo rejection test did not finish", terminal.await(10, TimeUnit.SECONDS))
        assertEquals(false, captured.get().success)
        assertEquals("reply", captured.get().stage)
    }

    private fun createFixture(
        baseUrl: String,
        inputHtml: String,
        buttonHtml: String,
        responseHtml: String,
        loadingHtml: String,
        prefix: String
    ): WebView {
        val loaded = CountDownLatch(1)
        val holder = AtomicReference<WebView>()
        val html = """
            <!doctype html>
            <html>
              <head><meta name="viewport" content="width=device-width,initial-scale=1"></head>
              <body>
                $inputHtml
                $buttonHtml
                $loadingHtml
                $responseHtml
                <script>
                  (function(){
                    var input = document.querySelector('textarea, [contenteditable="true"]');
                    var button = document.querySelector('button');
                    var loading = document.getElementById('loading');
                    var response = document.getElementById('response');
                    button.addEventListener('click', function(){
                      var value = input.value !== undefined ? input.value : (input.innerText || input.textContent || '');
                      loading.style.display = 'block';
                      loading.setAttribute('aria-busy', 'true');
                      setTimeout(function(){
                        response.textContent = '${prefix}' + value;
                        loading.style.display = 'none';
                        loading.setAttribute('aria-busy', 'false');
                      }, 250);
                    });
                  })();
                </script>
              </body>
            </html>
        """.trimIndent()

        onMain {
            val webView = WebView(instrumentation.targetContext).apply {
                settings.javaScriptEnabled = true
                layoutParams = android.view.ViewGroup.LayoutParams(1080, 1920)
                measure(
                    View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY)
                )
                layout(0, 0, 1080, 1920)
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        loaded.countDown()
                    }
                }
                loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
            }
            holder.set(webView)
            webViews += webView
        }
        assertTrue("Fixture failed to load: $baseUrl", loaded.await(10, TimeUnit.SECONDS))
        return holder.get()
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            instrumentation.runOnMainSync(block)
        }
    }
}
