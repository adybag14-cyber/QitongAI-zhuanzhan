package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import android.webkit.CookieManager

/**
 * Cookie 持久化管理器
 * 将 Cookie 保存到 SharedPreferences，重启后恢复
 */
class PersistentCookieJar(private val context: Context) {
    private val prefs = context.getSharedPreferences("ai_cookies", Context.MODE_PRIVATE)
    
    // 需要持久化的域名列表
    private val domains = listOf(
        "https://www.doubao.com",
        "https://yuanbao.tencent.com",
        "https://www.coze.cn",
        "https://chat.deepseek.com",
        "https://kimi.moonshot.cn",
        "https://www.google.com",
        "https://github.com"
    )
    
    /**
     * 保存所有域名的 Cookie
     */
    fun save() {
        val cm = CookieManager.getInstance()
        val editor = prefs.edit()
        
        domains.forEach { domain ->
            val cookies = cm.getCookie(domain)
            if (!cookies.isNullOrEmpty()) {
                editor.putString(domain, cookies)
            }
        }
        
        editor.apply()
    }
    
    /**
     * 恢复所有域名的 Cookie
     */
    fun restore() {
        val cm = CookieManager.getInstance()
        
        domains.forEach { domain ->
            val cookies = prefs.getString(domain, null)
            if (!cookies.isNullOrEmpty()) {
                cookies.split("; ").forEach { cookie ->
                    if (cookie.isNotEmpty()) {
                        cm.setCookie(domain, cookie)
                    }
                }
            }
        }
        
        cm.flush()
    }
    
    /**
     * 清除所有保存的 Cookie
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    /**
     * 获取指定域名的 Cookie
     */
    fun getCookies(domain: String): String? {
        return prefs.getString(domain, null)
    }
    
    /**
     * 保存指定域名的 Cookie
     */
    fun saveDomain(domain: String) {
        val cm = CookieManager.getInstance()
        val cookies = cm.getCookie(domain)
        if (!cookies.isNullOrEmpty()) {
            prefs.edit().putString(domain, cookies).apply()
        }
    }
}