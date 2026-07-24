package com.qtwl.YitongAIzhuanzhan

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.webkit.*
import android.view.ViewGroup
import androidx.compose.runtime.*
import java.io.*
import com.qtwl.YitongAIzhuanzhan.ui.screens.GatewayPrefs

// 桌面UA，骗过移动端限制
const val USER_AGENT_DESKTOP =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
    "(KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"

/**
 * WebView 多实例管理器
 * 每个 Tab 拥有独立的 WebView 实例，独立 Cookie 存储
 */
data class WebViewTab(
    val id: Int,
    var url: String,
    var title: String,
    var canGoBack: Boolean = false,
    var canGoForward: Boolean = false,
    var isLoading: Boolean = false,
    var progress: Int = 0,
    var webView: WebView? = null
)

object WebViewManager {
    private val tabs = mutableListOf<WebViewTab>()
    private var tabCounter = 0
    private var currentTabIndex = 0

    fun createTab(context: Context, url: String = "https://www.doubao.com"): WebViewTab {
        tabCounter++
        val tab = WebViewTab(
            id = tabCounter,
            url = url,
            title = ""
        )
        tabs.add(tab)
        currentTabIndex = tabs.size - 1
        return tab
    }

    fun initWebView(context: Context, tabId: Int, onStateChange: () -> Unit) {
        val tab = tabs.find { it.id == tabId } ?: return
        if (tab.webView != null) return

        @SuppressLint("SetJavaScriptEnabled")
        val wv = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.builtInZoomControls = true
            settings.displayZoomControls = false
            settings.setSupportZoom(true)
            settings.allowFileAccess = false
            settings.setSupportMultipleWindows(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.userAgentString = USER_AGENT_DESKTOP
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            settings.cacheMode = WebSettings.LOAD_DEFAULT
            settings.databaseEnabled = true
            settings.allowContentAccess = true
            settings.textZoom = GatewayPrefs.getTextZoom(context)

            // Cookie 持久化
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            // 恢复保存的 Cookie
            PersistentCookieJar(context).restore()

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    tab.isLoading = true
                    url?.let { tab.url = it }
                    onStateChange()
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    tab.isLoading = false
                    tab.title = view?.title ?: ""
                    tab.canGoBack = view?.canGoBack() ?: false
                    tab.canGoForward = view?.canGoForward() ?: false
                    // 保存 Cookie
                    CookieManager.getInstance().flush()
                    PersistentCookieJar(context).save()
                    onStateChange()
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false
                }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    tab.progress = newProgress
                    if (newProgress == 100) tab.isLoading = false
                    onStateChange()
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    super.onReceivedTitle(view, title)
                    tab.title = title ?: ""
                    onStateChange()
                }
            }

            loadUrl(tab.url)
        }
        tab.webView = wv
    }

    fun getCurrentTab(): WebViewTab? = tabs.getOrNull(currentTabIndex)
    fun getTab(index: Int): WebViewTab? = tabs.getOrNull(index)
    fun getTabCount(): Int = tabs.size
    fun getCurrentIndex(): Int = currentTabIndex

    fun switchTab(index: Int): Boolean {
        if (index < 0 || index >= tabs.size) return false
        currentTabIndex = index
        return true
    }

    fun addTab(context: Context, url: String = "https://www.google.com"): WebViewTab {
        return createTab(context, url)
    }

    fun closeTab(index: Int): Boolean {
        if (tabs.size <= 1) return false
        if (index < 0 || index >= tabs.size) return false
        tabs[index].webView?.destroy()
        tabs.removeAt(index)
        // 修复：关闭前面的标签页时，当前索引要跟着减1
        if (index < currentTabIndex) {
            currentTabIndex--
        } else if (currentTabIndex >= tabs.size) {
            currentTabIndex = tabs.size - 1
        }
        return true
    }

    fun getTabs(): List<WebViewTab> = tabs.toList()

    fun destroyAll() {
        tabs.forEach { it.webView?.destroy() }
        tabs.clear()
        tabCounter = 0
        currentTabIndex = 0
    }
}