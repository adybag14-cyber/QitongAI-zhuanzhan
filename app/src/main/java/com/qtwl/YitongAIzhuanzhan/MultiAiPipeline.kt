package com.qtwl.YitongAIzhuanzhan

/** A single ordered stage in a multi-AI pipeline. */
data class PipelineStep(
    val platformId: String,
    val promptTemplate: String = "{{input}}",
    val maxRetries: Int = 1
) {
    fun renderPrompt(input: String, originalPrompt: String, stepNumber: Int): String =
        promptTemplate
            .replace("{{input}}", input)
            .replace("{{original}}", originalPrompt)
            .replace("{{step}}", stepNumber.toString())
}

enum class PipelineStepState {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

enum class PipelineRunState {
    IDLE,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}

data class PipelineStepSnapshot(
    val index: Int,
    val platformId: String,
    val state: PipelineStepState,
    val attempt: Int = 0,
    val input: String = "",
    val output: String = "",
    val detail: String = ""
)

data class PipelineSnapshot(
    val runId: Int,
    val state: PipelineRunState,
    val originalPrompt: String,
    val steps: List<PipelineStepSnapshot>,
    val finalOutput: String = "",
    val detail: String = ""
) {
    companion object {
        val Idle = PipelineSnapshot(
            runId = 0,
            state = PipelineRunState.IDLE,
            originalPrompt = "",
            steps = emptyList()
        )
    }
}

data class PipelineExecutionResult(
    val success: Boolean,
    val output: String = "",
    val detail: String = ""
)

fun interface PipelineCancellation {
    fun cancel()

    companion object {
        val None = PipelineCancellation { }
    }
}

fun interface PipelineStepExecutor {
    fun execute(
        step: PipelineStep,
        renderedPrompt: String,
        callback: (PipelineExecutionResult) -> Unit
    ): PipelineCancellation
}

/**
 * Pure Kotlin pipeline state machine.
 *
 * It executes one stage at a time, forwards the successful output to the next
 * stage, retries failed stages, rejects stale callbacks after cancellation, and
 * publishes immutable snapshots suitable for Compose or unit tests.
 */
class PipelineCoordinator(
    private val executor: PipelineStepExecutor
) {
    private var nextRunId = 0
    private var activeRunId = 0
    private var activeCancellation: PipelineCancellation = PipelineCancellation.None
    private var activeExecutionToken = 0
    private var listener: ((PipelineSnapshot) -> Unit)? = null
    private var steps: List<PipelineStep> = emptyList()
    private var originalPrompt: String = ""
    private var mutableSteps: MutableList<PipelineStepSnapshot> = mutableListOf()
    private var runState: PipelineRunState = PipelineRunState.IDLE
    private var finalOutput: String = ""
    private var runDetail: String = ""

    @Synchronized
    fun start(
        prompt: String,
        pipelineSteps: List<PipelineStep>,
        onSnapshot: (PipelineSnapshot) -> Unit
    ): Int {
        require(prompt.isNotBlank()) { "Pipeline prompt must not be blank" }
        require(pipelineSteps.isNotEmpty()) { "Pipeline must contain at least one step" }
        pipelineSteps.forEach { AiPlatformRegistry.require(it.platformId) }

        cancelInternal(publish = false)
        nextRunId += 1
        activeRunId = nextRunId
        listener = onSnapshot
        steps = pipelineSteps.toList()
        originalPrompt = prompt
        runState = PipelineRunState.RUNNING
        finalOutput = ""
        runDetail = ""
        mutableSteps = pipelineSteps.mapIndexed { index, step ->
            PipelineStepSnapshot(
                index = index,
                platformId = step.platformId,
                state = PipelineStepState.PENDING
            )
        }.toMutableList()
        publish()
        executeStep(activeRunId, index = 0, input = prompt, attempt = 1)
        return activeRunId
    }

    @Synchronized
    fun cancel() {
        cancelInternal(publish = true)
    }

    @Synchronized
    fun currentSnapshot(): PipelineSnapshot = snapshot()

    private fun executeStep(runId: Int, index: Int, input: String, attempt: Int) {
        val step: PipelineStep
        val renderedPrompt: String
        val executionToken: Int
        synchronized(this) {
            if (runId != activeRunId || runState != PipelineRunState.RUNNING) return
            if (index >= steps.size) {
                runState = PipelineRunState.SUCCEEDED
                finalOutput = input
                runDetail = "All ${steps.size} pipeline steps completed"
                activeCancellation = PipelineCancellation.None
                publish()
                return
            }

            step = steps[index]
            renderedPrompt = step.renderPrompt(input, originalPrompt, index + 1)
            mutableSteps[index] = mutableSteps[index].copy(
                state = PipelineStepState.RUNNING,
                attempt = attempt,
                input = renderedPrompt,
                output = "",
                detail = if (attempt > 1) "Retry $attempt of ${step.maxRetries + 1}" else ""
            )
            activeExecutionToken += 1
            executionToken = activeExecutionToken
            publish()
        }

        val cancellation = executor.execute(step, renderedPrompt) { result ->
            onExecutionResult(runId, index, input, attempt, result)
        }

        synchronized(this) {
            if (
                runId == activeRunId &&
                runState == PipelineRunState.RUNNING &&
                executionToken == activeExecutionToken
            ) {
                activeCancellation = cancellation
            } else {
                cancellation.cancel()
            }
        }
    }

    private fun onExecutionResult(
        runId: Int,
        index: Int,
        previousInput: String,
        attempt: Int,
        result: PipelineExecutionResult
    ) {
        val nextAction: (() -> Unit)?
        synchronized(this) {
            if (runId != activeRunId || runState != PipelineRunState.RUNNING) return
            activeCancellation = PipelineCancellation.None
            val step = steps[index]

            if (result.success && result.output.isNotBlank()) {
                mutableSteps[index] = mutableSteps[index].copy(
                    state = PipelineStepState.SUCCEEDED,
                    output = result.output,
                    detail = result.detail
                )
                publish()
                nextAction = {
                    executeStep(runId, index + 1, result.output, attempt = 1)
                }
            } else if (attempt <= step.maxRetries) {
                mutableSteps[index] = mutableSteps[index].copy(
                    state = PipelineStepState.RUNNING,
                    detail = result.detail.ifBlank { "Step failed; retrying" }
                )
                publish()
                nextAction = {
                    executeStep(runId, index, previousInput, attempt + 1)
                }
            } else {
                mutableSteps[index] = mutableSteps[index].copy(
                    state = PipelineStepState.FAILED,
                    detail = result.detail.ifBlank { "No assistant response was returned" }
                )
                for (remaining in index + 1 until mutableSteps.size) {
                    mutableSteps[remaining] = mutableSteps[remaining].copy(
                        state = PipelineStepState.CANCELLED,
                        detail = "Not run because an earlier step failed"
                    )
                }
                runState = PipelineRunState.FAILED
                runDetail = result.detail.ifBlank {
                    "${AiPlatformRegistry.require(step.platformId).displayName} failed"
                }
                publish()
                nextAction = null
            }
        }
        nextAction?.invoke()
    }

    private fun cancelInternal(publish: Boolean) {
        activeExecutionToken += 1
        activeCancellation.cancel()
        activeCancellation = PipelineCancellation.None
        if (runState == PipelineRunState.RUNNING) {
            runState = PipelineRunState.CANCELLED
            runDetail = "Pipeline cancelled"
            mutableSteps.replaceAll { step ->
                if (step.state == PipelineStepState.RUNNING || step.state == PipelineStepState.PENDING) {
                    step.copy(state = PipelineStepState.CANCELLED)
                } else {
                    step
                }
            }
            if (publish) publish()
        }
        activeRunId = 0
    }

    private fun publish() {
        listener?.invoke(snapshot())
    }

    private fun snapshot(): PipelineSnapshot = PipelineSnapshot(
        runId = activeRunId,
        state = runState,
        originalPrompt = originalPrompt,
        steps = mutableSteps.toList(),
        finalOutput = finalOutput,
        detail = runDetail
    )
}
