package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

object UrlUtils {
    const val TAG = "UrlUtils"
    const val SANGUOSHA_OFFICIAL_URL = "https://www.sanguosha.com/"

    /**
     * 判断给定 URL 是否属于三国杀官方域名体系
     * 包括：sanguosha.com, www.sanguosha.com, web.sanguosha.com, 及所有 *.sanguosha.com 子域名
     */
    fun isSanGuoShaDomain(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: return false
            host == "sanguosha.com" || host.endsWith(".sanguosha.com")
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing host from url: $url", e)
            false
        }
    }

    /**
     * 安全处理外部 URL 跳转或特殊 Scheme (如 tel:, mailto:, intent:)
     * 避免因未安装对应应用或格式异常导致崩溃
     */
    fun handleExternalUri(context: Context, url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase() ?: return false

            when (scheme) {
                "http", "https" -> {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                "tel", "mailto", "sms", "geo" -> {
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
                "intent" -> {
                    try {
                        val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                        true
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse or launch intent scheme: $url", e)
                        false
                    }
                }
                else -> {
                    // 其他自定义 scheme 尝试安全拉起
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    true
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not open external url safely: $url", e)
            false
        }
    }
}
