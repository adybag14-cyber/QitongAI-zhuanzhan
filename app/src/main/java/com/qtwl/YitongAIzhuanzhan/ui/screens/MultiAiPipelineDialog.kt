package com.qtwl.YitongAIzhuanzhan.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qtwl.YitongAIzhuanzhan.AiPlatformRegistry
import com.qtwl.YitongAIzhuanzhan.PipelineRunState
import com.qtwl.YitongAIzhuanzhan.PipelineSnapshot
import com.qtwl.YitongAIzhuanzhan.PipelineStepState
import com.qtwl.YitongAIzhuanzhan.R
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleBlue
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleGray
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleGray2
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleGreen
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleLabel
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleLabelDark
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleRed
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleSecondaryLabel
import com.qtwl.YitongAIzhuanzhan.ui.theme.AppleSecondaryLabelDark
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassBackground
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassBackgroundDark
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassBorder
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassBorderDark
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassSurfaceDark
import com.qtwl.YitongAIzhuanzhan.ui.theme.GlassSurfaceDarkMode2

@Composable
fun MultiAiPipelineDialog(
    prompt: String,
    onPromptChange: (String) -> Unit,
    orderedPlatformIds: List<String>,
    selectedPlatformIds: Set<String>,
    onTogglePlatform: (String) -> Unit,
    onMovePlatform: (fromIndex: Int, toIndex: Int) -> Unit,
    snapshot: PipelineSnapshot,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean
) {
    val running = snapshot.state == PipelineRunState.RUNNING
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = { if (!running) onDismiss() },
        containerColor = if (isDark) GlassBackgroundDark else GlassBackground,
        titleContentColor = if (isDark) AppleLabelDark else AppleLabel,
        textContentColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.AccountTree,
                    contentDescription = null,
                    tint = AppleBlue,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.pipeline_title), fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 560.dp)
                    .verticalScroll(scrollState)
            ) {
                Text(
                    stringResource(R.string.pipeline_description),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = prompt,
                    onValueChange = onPromptChange,
                    enabled = !running,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.pipeline_initial_prompt)) },
                    placeholder = { Text(stringResource(R.string.pipeline_prompt_hint)) },
                    minLines = 3,
                    maxLines = 7,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppleBlue.copy(alpha = 0.55f),
                        unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorder,
                        cursorColor = AppleBlue,
                        focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                        unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.pipeline_platform_order),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) AppleLabelDark else AppleLabel
                )
                Text(
                    stringResource(R.string.pipeline_platform_order_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                )
                Spacer(Modifier.height(6.dp))

                orderedPlatformIds.forEachIndexed { index, platformId ->
                    val platform = AiPlatformRegistry.require(platformId)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .background(
                                if (isDark) GlassSurfaceDarkMode2 else GlassSurfaceDark,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = platformId in selectedPlatformIds,
                            enabled = !running,
                            onCheckedChange = { onTogglePlatform(platformId) }
                        )
                        Text(
                            platform.displayName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) AppleLabelDark else AppleLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(
                            onClick = { onMovePlatform(index, index - 1) },
                            enabled = !running && index > 0,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowUpward,
                                contentDescription = stringResource(R.string.pipeline_move_up),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        IconButton(
                            onClick = { onMovePlatform(index, index + 1) },
                            enabled = !running && index < orderedPlatformIds.lastIndex,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                Icons.Filled.ArrowDownward,
                                contentDescription = stringResource(R.string.pipeline_move_down),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                if (snapshot.state != PipelineRunState.IDLE) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        pipelineRunLabel(snapshot.state),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = when (snapshot.state) {
                            PipelineRunState.SUCCEEDED -> AppleGreen
                            PipelineRunState.FAILED -> AppleRed
                            PipelineRunState.CANCELLED -> AppleGray
                            else -> AppleBlue
                        }
                    )
                    if (snapshot.detail.isNotBlank()) {
                        Text(
                            snapshot.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                        )
                    }
                    Spacer(Modifier.height(6.dp))

                    snapshot.steps.forEach { step ->
                        val platform = AiPlatformRegistry.require(step.platformId)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = when (step.state) {
                                    PipelineStepState.PENDING -> Icons.Filled.RadioButtonUnchecked
                                    PipelineStepState.RUNNING -> Icons.Filled.HourglassTop
                                    PipelineStepState.SUCCEEDED -> Icons.Filled.CheckCircle
                                    PipelineStepState.FAILED -> Icons.Filled.Error
                                    PipelineStepState.CANCELLED -> Icons.Filled.Cancel
                                },
                                contentDescription = null,
                                tint = when (step.state) {
                                    PipelineStepState.SUCCEEDED -> AppleGreen
                                    PipelineStepState.FAILED -> AppleRed
                                    PipelineStepState.CANCELLED -> AppleGray
                                    PipelineStepState.RUNNING -> AppleBlue
                                    PipelineStepState.PENDING -> if (isDark) AppleGray2 else AppleGray
                                },
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${step.index + 1}. ${platform.displayName}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isDark) AppleLabelDark else AppleLabel
                                )
                                if (step.detail.isNotBlank()) {
                                    Text(
                                        step.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (step.state == PipelineStepState.FAILED) AppleRed
                                        else if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                                    )
                                }
                                if (step.output.isNotBlank()) {
                                    Text(
                                        step.output,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                                    )
                                }
                            }
                        }
                    }

                    if (snapshot.finalOutput.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.pipeline_final_output),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                                .background(
                                    if (isDark) GlassSurfaceDarkMode2 else GlassSurfaceDark,
                                    RoundedCornerShape(10.dp)
                                )
                                .padding(10.dp)
                        ) {
                            Text(
                                snapshot.finalOutput,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) AppleLabelDark else AppleLabel
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (running) {
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = AppleRed),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.Cancel, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.size(5.dp))
                    Text(stringResource(R.string.pipeline_cancel))
                }
            } else {
                Button(
                    onClick = onStart,
                    enabled = prompt.isNotBlank() && selectedPlatformIds.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text(stringResource(R.string.pipeline_start))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !running) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun pipelineRunLabel(state: PipelineRunState): String = when (state) {
    PipelineRunState.IDLE -> stringResource(R.string.pipeline_ready)
    PipelineRunState.RUNNING -> stringResource(R.string.pipeline_running)
    PipelineRunState.SUCCEEDED -> stringResource(R.string.pipeline_completed)
    PipelineRunState.FAILED -> stringResource(R.string.pipeline_failed)
    PipelineRunState.CANCELLED -> stringResource(R.string.pipeline_cancelled)
}
