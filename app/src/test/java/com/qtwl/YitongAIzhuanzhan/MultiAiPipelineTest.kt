package com.qtwl.YitongAIzhuanzhan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class MultiAiPipelineTest {

    @Test
    fun repliesAreForwardedToEveryStepInOrder() {
        val receivedPrompts = mutableListOf<Pair<String, String>>()
        var finalSnapshot = PipelineSnapshot.Idle
        val prefixes = mapOf("doubao" to "D:", "yuanbao" to "Y:", "deepseek" to "S:")
        val coordinator = PipelineCoordinator(
            PipelineStepExecutor { step, prompt, callback ->
                receivedPrompts += step.platformId to prompt
                callback(
                    PipelineExecutionResult(
                        success = true,
                        output = prefixes.getValue(step.platformId) + prompt,
                        detail = "fixture complete"
                    )
                )
                PipelineCancellation.None
            }
        )

        coordinator.start(
            prompt = "hello",
            pipelineSteps = listOf(
                PipelineStep("doubao"),
                PipelineStep("yuanbao"),
                PipelineStep("deepseek")
            )
        ) { finalSnapshot = it }

        assertEquals(
            listOf(
                "doubao" to "hello",
                "yuanbao" to "D:hello",
                "deepseek" to "Y:D:hello"
            ),
            receivedPrompts
        )
        assertEquals(PipelineRunState.SUCCEEDED, finalSnapshot.state)
        assertEquals("S:Y:D:hello", finalSnapshot.finalOutput)
        assertTrue(finalSnapshot.steps.all { it.state == PipelineStepState.SUCCEEDED })
    }

    @Test
    fun promptTemplateReceivesPreviousAndOriginalText() {
        val prompts = mutableListOf<String>()
        val coordinator = PipelineCoordinator(
            PipelineStepExecutor { _, prompt, callback ->
                prompts += prompt
                callback(PipelineExecutionResult(success = true, output = "answer-${prompts.size}"))
                PipelineCancellation.None
            }
        )

        coordinator.start(
            prompt = "original",
            pipelineSteps = listOf(
                PipelineStep("doubao"),
                PipelineStep(
                    platformId = "yuanbao",
                    promptTemplate = "step={{step}} previous={{input}} original={{original}}"
                )
            )
        ) { }

        assertEquals("original", prompts[0])
        assertEquals("step=2 previous=answer-1 original=original", prompts[1])
    }

    @Test
    fun failedStepIsRetriedThenPipelineContinues() {
        var attempts = 0
        var finalSnapshot = PipelineSnapshot.Idle
        val coordinator = PipelineCoordinator(
            PipelineStepExecutor { _, prompt, callback ->
                attempts++
                if (attempts == 1) {
                    callback(PipelineExecutionResult(success = false, detail = "temporary failure"))
                } else {
                    callback(PipelineExecutionResult(success = true, output = "ok:$prompt"))
                }
                PipelineCancellation.None
            }
        )

        coordinator.start(
            prompt = "retry me",
            pipelineSteps = listOf(PipelineStep("doubao", maxRetries = 1))
        ) { finalSnapshot = it }

        assertEquals(2, attempts)
        assertEquals(PipelineRunState.SUCCEEDED, finalSnapshot.state)
        assertEquals("ok:retry me", finalSnapshot.finalOutput)
        assertEquals(2, finalSnapshot.steps.single().attempt)
    }

    @Test
    fun cancellationStopsCurrentWorkAndRejectsLateCallback() {
        lateinit var lateCallback: (PipelineExecutionResult) -> Unit
        val workCancelled = AtomicBoolean(false)
        var finalSnapshot = PipelineSnapshot.Idle
        val coordinator = PipelineCoordinator(
            PipelineStepExecutor { _, _, callback ->
                lateCallback = callback
                PipelineCancellation { workCancelled.set(true) }
            }
        )

        coordinator.start(
            prompt = "cancel",
            pipelineSteps = listOf(PipelineStep("doubao"))
        ) { finalSnapshot = it }
        coordinator.cancel()
        lateCallback(PipelineExecutionResult(success = true, output = "too late"))

        assertTrue(workCancelled.get())
        assertEquals(PipelineRunState.CANCELLED, finalSnapshot.state)
        assertTrue(finalSnapshot.finalOutput.isEmpty())
    }
}
