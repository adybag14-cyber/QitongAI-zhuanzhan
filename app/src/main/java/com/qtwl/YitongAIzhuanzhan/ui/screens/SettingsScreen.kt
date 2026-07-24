package com.qtwl.YitongAIzhuanzhan.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.qtwl.YitongAIzhuanzhan.R
import com.qtwl.YitongAIzhuanzhan.ui.components.GlassCard
import com.qtwl.YitongAIzhuanzhan.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == GlassBackgroundDark
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var gatewayEnabled by remember { mutableStateOf(GatewayPrefs.isEnabled(context)) }
    var gatewayHost by remember { mutableStateOf(GatewayPrefs.getHost(context)) }
    var gatewayPort by remember { mutableStateOf(GatewayPrefs.getPort(context)) }
    var gatewayApiKey by remember { mutableStateOf(GatewayPrefs.getApiKey(context)) }
    var showApiKey by remember { mutableStateOf(false) }
    var customUa by remember { mutableStateOf(GatewayPrefs.getUserAgent(context)) }
    var textZoom by remember { mutableIntStateOf(GatewayPrefs.getTextZoom(context)) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) GlassBackgroundDark else GlassBackground,
                    titleContentColor = if (isDark) AppleLabelDark else AppleLabel,
                    navigationIconContentColor = if (isDark) AppleBlueLight else AppleBlue
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDark) GlassBackgroundDark else GlassBackground)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // 网关开关
            SectionTitle(stringResource(R.string.gateway_switch), Icons.Outlined.PowerSettingsNew, isDark)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.gateway_switch),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) AppleLabelDark else AppleLabel
                        )
                        Text(
                            text = if (gatewayEnabled) stringResource(R.string.gateway_on) else stringResource(R.string.gateway_off),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (gatewayEnabled) AppleGreen else (if (isDark) AppleGray2 else AppleGray),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    Switch(
                        checked = gatewayEnabled,
                        onCheckedChange = {
                            gatewayEnabled = it
                            GatewayPrefs.setEnabled(context, it)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AppleBlue,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = if (isDark) AppleGray.copy(alpha = 0.4f) else AppleGray2.copy(alpha = 0.5f)
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 网关配置
            SectionTitle(stringResource(R.string.gateway_config), Icons.Outlined.Settings, isDark)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ConfigField(
                        label = stringResource(R.string.gateway_host),
                        value = gatewayHost,
                        placeholder = "127.0.0.1",
                        onValueChange = {
                            gatewayHost = it
                            GatewayPrefs.setHost(context, it)
                        },
                        isDark = isDark,
                        icon = Icons.Outlined.Dns
                    )

                    Spacer(Modifier.height(12.dp))

                    ConfigField(
                        label = stringResource(R.string.gateway_port),
                        value = gatewayPort,
                        placeholder = "8080",
                        onValueChange = {
                            gatewayPort = it
                            GatewayPrefs.setPort(context, it)
                        },
                        isDark = isDark,
                        icon = Icons.Outlined.Router,
                        keyboardType = KeyboardType.Number
                    )

                    Spacer(Modifier.height(12.dp))

                    // API Key
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Key,
                            contentDescription = null,
                            tint = if (isDark) AppleBlueLight else AppleBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gateway_api_key),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = if (isDark) AppleLabelDark else AppleLabel
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = { showApiKey = !showApiKey },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (showApiKey) stringResource(R.string.hide) else stringResource(R.string.show),
                                tint = if (isDark) AppleGray2 else AppleGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    OutlinedTextField(
                        value = gatewayApiKey,
                        onValueChange = {
                            gatewayApiKey = it
                            GatewayPrefs.setApiKey(context, it)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        placeholder = { Text("sk-xxx...", style = MaterialTheme.typography.bodySmall) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AppleBlue.copy(alpha = 0.5f),
                            unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorder,
                            cursorColor = AppleBlue,
                            focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
                            unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 浏览器设置
            SectionTitle("浏览器设置", Icons.Outlined.Smartphone, isDark)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // 字体缩放
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, tint = if (isDark) AppleBlueLight else AppleBlue, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("字体缩放", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = if (isDark) AppleLabelDark else AppleLabel)
                        Spacer(Modifier.weight(1f))
                        Text("${textZoom}%", style = MaterialTheme.typography.bodySmall, color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel)
                    }
                    Slider(
                        value = textZoom.toFloat(),
                        onValueChange = { textZoom = it.toInt(); GatewayPrefs.setTextZoom(context, it.toInt()) },
                        valueRange = 50f..200f,
                        steps = 14,
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = AppleBlue,
                            activeTrackColor = AppleBlue.copy(alpha = 0.6f),
                            inactiveTrackColor = if (isDark) AppleGray.copy(alpha = 0.3f) else AppleGray2.copy(alpha = 0.3f)
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    // UA
                    ConfigField(
                        label = "User-Agent",
                        value = customUa,
                        placeholder = "默认UA",
                        onValueChange = {
                            customUa = it
                            GatewayPrefs.setUserAgent(context, it)
                        },
                        isDark = isDark,
                        icon = Icons.Outlined.Code
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // 网关状态
            SectionTitle(stringResource(R.string.gateway_status), Icons.Outlined.Monitor, isDark)
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                elevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    StatusRow(
                        label = stringResource(R.string.running_status),
                        value = if (gatewayEnabled) stringResource(R.string.gateway_running) else stringResource(R.string.gateway_stopped),
                        valueColor = if (gatewayEnabled) AppleGreen else (if (isDark) AppleGray2 else AppleGray),
                        isDark = isDark
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusRow(
                        label = stringResource(R.string.gateway_address),
                        value = "http://${gatewayHost}:${gatewayPort}",
                        isDark = isDark
                    )
                    Spacer(Modifier.height(8.dp))
                    StatusRow(
                        label = stringResource(R.string.gateway_api_key),
                        value = if (gatewayApiKey.isNotEmpty()) "${gatewayApiKey.take(8)}..." else stringResource(R.string.not_set),
                        isDark = isDark
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector, isDark: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) AppleBlueLight else AppleBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) AppleLabelDark else AppleLabel
        )
    }
}

@Composable
private fun ConfigField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    isDark: Boolean,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) AppleBlueLight else AppleBlue,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isDark) AppleLabelDark else AppleLabel
        )
    }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        textStyle = MaterialTheme.typography.bodySmall,
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppleBlue.copy(alpha = 0.5f),
            unfocusedBorderColor = if (isDark) GlassBorderDark else GlassBorder,
            cursorColor = AppleBlue,
            focusedTextColor = if (isDark) AppleLabelDark else AppleLabel,
            unfocusedTextColor = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
        ),
        shape = RoundedCornerShape(10.dp)
    )
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
    isDark: Boolean,
    valueColor: Color? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isDark) AppleSecondaryLabelDark else AppleSecondaryLabel
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = valueColor ?: (if (isDark) AppleLabelDark else AppleLabel)
        )
    }
}

object GatewayPrefs {
    private const val PREFS_NAME = "gateway_prefs"
    private const val KEY_ENABLED = "gateway_enabled"
    private const val KEY_HOST = "gateway_host"
    private const val KEY_PORT = "gateway_port"
    private const val KEY_API_KEY = "gateway_api_key"
    private const val KEY_UA = "custom_ua"
    private const val KEY_TEXT_ZOOM = "text_zoom"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isEnabled(context: Context): Boolean = prefs(context).getBoolean(KEY_ENABLED, false)
    fun setEnabled(context: Context, enabled: Boolean) =
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()

    fun getHost(context: Context): String = prefs(context).getString(KEY_HOST, "127.0.0.1") ?: "127.0.0.1"
    fun setHost(context: Context, host: String) =
        prefs(context).edit().putString(KEY_HOST, host).apply()

    fun getPort(context: Context): String = prefs(context).getString(KEY_PORT, "8080") ?: "8080"
    fun setPort(context: Context, port: String) =
        prefs(context).edit().putString(KEY_PORT, port).apply()

    fun getApiKey(context: Context): String = prefs(context).getString(KEY_API_KEY, "") ?: ""
    fun setApiKey(context: Context, apiKey: String) =
        prefs(context).edit().putString(KEY_API_KEY, apiKey).apply()

    fun getUserAgent(context: Context): String = prefs(context).getString(KEY_UA,
        "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    ) ?: "Mozilla/5.0 (Linux; Android 15; Pixel 9) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"
    fun setUserAgent(context: Context, ua: String) =
        prefs(context).edit().putString(KEY_UA, ua).apply()

    fun getTextZoom(context: Context): Int = prefs(context).getInt(KEY_TEXT_ZOOM, 100)
    fun setTextZoom(context: Context, zoom: Int) =
        prefs(context).edit().putInt(KEY_TEXT_ZOOM, zoom).apply()
}