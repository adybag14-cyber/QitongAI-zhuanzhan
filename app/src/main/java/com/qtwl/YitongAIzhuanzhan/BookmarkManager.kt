package com.qtwl.YitongAIzhuanzhan

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 收藏夹管理
 */
data class Bookmark(
    val name: String,
    val url: String,
    val icon: String = "web"  // 图标标识
)

object BookmarkManager {
    private const val PREFS_NAME = "bookmark_prefs"
    private const val KEY_BOOKMARKS = "bookmarks"

    // 预置AI平台收藏
    val defaultBookmarks = listOf(
        Bookmark("豆包", "https://www.doubao.com", "doubao"),
        Bookmark("元宝", "https://yuanbao.tencent.com", "yuanbao"),
        Bookmark("通义千问", "https://tongyi.aliyun.com", "tongyi"),
        Bookmark("DeepSeek", "https://chat.deepseek.com", "deepseek"),
        Bookmark("Kimi", "https://kimi.moonshot.cn", "kimi"),
        Bookmark("Google", "https://www.google.com", "google"),
        Bookmark("GitHub", "https://github.com", "github")
    )

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getBookmarks(context: Context): List<Bookmark> {
        val json = prefs(context).getString(KEY_BOOKMARKS, null) ?: return defaultBookmarks
        try {
            val arr = JSONArray(json)
            val list = mutableListOf<Bookmark>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(Bookmark(
                    name = obj.getString("name"),
                    url = obj.getString("url"),
                    icon = obj.optString("icon", "web")
                ))
            }
            return list
        } catch (e: Exception) {
            return defaultBookmarks
        }
    }

    fun saveBookmarks(context: Context, bookmarks: List<Bookmark>) {
        val arr = JSONArray()
        bookmarks.forEach { b ->
            val obj = JSONObject()
            obj.put("name", b.name)
            obj.put("url", b.url)
            obj.put("icon", b.icon)
            arr.put(obj)
        }
        prefs(context).edit().putString(KEY_BOOKMARKS, arr.toString()).apply()
    }

    fun addBookmark(context: Context, name: String, url: String) {
        val list = getBookmarks(context).toMutableList()
        // 去重
        list.removeAll { it.url == url }
        list.add(0, Bookmark(name, url))
        saveBookmarks(context, list)
    }

    fun removeBookmark(context: Context, url: String) {
        val list = getBookmarks(context).toMutableList()
        list.removeAll { it.url == url }
        saveBookmarks(context, list)
    }

    fun resetToDefault(context: Context) {
        prefs(context).edit().remove(KEY_BOOKMARKS).apply()
    }
}