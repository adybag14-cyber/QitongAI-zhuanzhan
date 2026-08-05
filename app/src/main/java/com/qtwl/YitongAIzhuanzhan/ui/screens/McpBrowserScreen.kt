package com.qtwl.YitongAIzhuanzhan.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.qtwl.YitongAIzhuanzhan.BrowserBrain
import com.qtwl.YitongAIzhuanzhan.BrowserBrainConfig
import com.qtwl.YitongAIzhuanzhan.R
import com.qtwl.YitongAIzhuanzhan.ui.components.GlassCard
import com.qtwl.YitongAIzhuanzhan.ui.theme.*
import kotlinx.coroutines.launch

/**
 * 独立的MCP浏览器界面 — 专给MCP对接用的隔离浏览器，与主浏览器分开
 */
@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun McpBrowserScreen(onBack: () -> Unit) {
    val isDark = MaterialTheme.colorScheme.background == GlassBackgroundDark
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var webView by remember { mutableStateOf<WebView?>(null) }
    var urlInput by remember { mutableStateOf("https://www.doubao.com") }
    var currentUrl by remember { mutableStateOf("") }
    var currentTitle by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var brainTask by remember { mutableStateOf("") }
    var brainResult by remember { mutableStateOf("") }
    var showBrainDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                color = if (isDark) GlassBackgroundDark else GlassBackground,
                tonalElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back), tint = if (isDark) AppleBlueLight else AppleBlue)
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = currentTitle.ifEmpty { "MCP浏览器" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) AppleLabelDark else AppleLabel,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        IconButton(
                            onClick = { showBrainDialog = true },
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isDark) AppleBlue.copy(alpha = 0.15f) else AppleBlue.copy(alpha = 0.1f))
                        ) {
                            Icon(Icons.Outlined.Psychology, contentDescription = "大脑", tint = AppleBlue, modifier = Modifier.size(20.dp))
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.weight(1f).height(44.dp),
                            textStyle = MaterialTheme.typography.bodySmall,
                            singleLine = true,
                            placeholder = { Text("输入URL") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppleBlue.copy(alpha = 0.5f),
                                unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorder,
                                cursorColor = AppleBlue,
                                focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                                unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val finalUrl = if (urlInput.startsWith("http://") || urlInput.startsWith("https://")) urlInput
                                else "https://$urlInput"
                                webView?.loadUrl(finalUrl)
                            },
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                                .background(AppleBlue)
                        ) {
                            Icon(Icons.Filled.ArrowForward, contentDescription = "GO", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            color = AppleBlue,
                            trackColor = Color.Transparent
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding)
                .background(if (isDark) GlassBackgroundDark else GlassBackground)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true
                        settings.builtInZoomControls = true
                        settings.displayZoomControls = false
                        settings.setSupportZoom(true)
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                currentUrl = url ?: ""
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView, url: String?) {
                                super.onPageFinished(view, url)
                                currentUrl = url ?: ""
                                currentTitle = view.title ?: ""
                                isLoading = false
                                urlInput = url ?: ""
                            }
                        }
                        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }.also { webView = it }
                },
                update = { },
                modifier = Modifier.fillMaxSize()
            )
        }
    }

    // 大脑对话框
    if (showBrainDialog) {
        AlertDialog(
            onDismissRequest = { showBrainDialog = false },
            containerColor = if (isDark) GlassBackgroundDark else GlassBackground,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Psychology, contentDescription = null, tint = AppleBlue, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("浏览器大脑", fontWeight = FontWeight.SemiBold)
                }
            },
            text = {
                Column {
                    Text("描述你要在浏览器上做什么，大脑会自动生成脚本并执行。", style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = brainTask,
                        onValueChange = { brainTask = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall,
                        placeholder = { Text("例如：打开百度搜索\"綦桐网络\"") },
                        minLines = 2, maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorder,
                            cursorColor = AppleBlue,
                            focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                            unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                    if (brainResult.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        GlassCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), elevation = 1.dp) {
                            Text(brainResult, style = MaterialTheme.typography.bodySmall,
                                color = if (isDark) AppleLabelDark else AppleLabel,
                                modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val wv = webView ?: return@Button
                        scope.launch {
                            brainResult = "思考中..."
                            val result = if (BrowserBrainConfig.isEnabled(context)) {
                                BrowserBrain.generateAndExecute(context, wv, brainTask)
                            } else {
                                BrowserBrain.BrainResult(false, error = "大脑未启用，请在关于页开启")
                            }
                            brainResult = if (result.success) "✅ ${result.explanation}\n脚本已注入"
                            else "❌ ${result.error}"
                        }
                    },
                    enabled = brainTask.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("执行") }
            },
            dismissButton = {
                TextButton(onClick = { showBrainDialog = false; brainTask = ""; brainResult = "" }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}