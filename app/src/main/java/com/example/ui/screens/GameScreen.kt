package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.ErrorOverlay
import com.example.util.UrlUtils

private const val TAG = "GameScreen"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GameScreen(
    onBackPressWhenRoot: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // 处理返回手势
    BackHandler {
        val webView = webViewRef
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        } else {
            onBackPressWhenRoot()
        }
    }

    // 监听生命周期，同步 WebView 暂停/恢复及 Cookie 持久化
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_PAUSE -> {
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                    CookieManager.getInstance().flush()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.let { wv ->
                        wv.stopLoading()
                        wv.clearHistory()
                        wv.loadUrl("about:blank")
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        wv.destroy()
                    }
                    webViewRef = null
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // WebView 核心组件
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                createConfiguredWebView(
                    context = ctx,
                    onErrorOccurred = { error ->
                        hasError = true
                        errorMessage = error
                    },
                    onPageFinish = {
                        hasError = false
                        CookieManager.getInstance().flush()
                    }
                ).also { wv ->
                    webViewRef = wv
                    wv.loadUrl(UrlUtils.SANGUOSHA_OFFICIAL_URL)
                }
            },
            update = { wv ->
                webViewRef = wv
            }
        )

        // 错误提示浮层
        ErrorOverlay(
            visible = hasError,
            errorMessage = errorMessage,
            onReload = {
                hasError = false
                val wv = webViewRef
                if (wv != null) {
                    val currentUrl = wv.url
                    if (currentUrl.isNullOrBlank() || currentUrl == "about:blank") {
                        wv.loadUrl(UrlUtils.SANGUOSHA_OFFICIAL_URL)
                    } else {
                        wv.reload()
                    }
                }
            }
        )
    }
}

/**
 * 创建并配置专用于 HTML5 网页游戏的 Android WebView
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    onErrorOccurred: (String?) -> Unit,
    onPageFinish: () -> Unit
): WebView {
    return WebView(context).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        // 开启硬件加速渲染与平滑缩放
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
        setBackgroundColor(android.graphics.Color.BLACK)

        // 配置 WebSettings，针对 HTML5 网页游戏优化
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true

            // 允许无需用户交互直接自动播放音效/背景音乐
            mediaPlaybackRequiresUserGesture = false

            // 视口与布局适配
            useWideViewPort = true
            loadWithOverviewMode = true

            // 禁用缩放控件，保持游戏界面固定比例
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 100

            // 安全性配置：禁止本地文件越权访问
            allowFileAccess = false
            allowContentAccess = false

            // 安全性配置：严格禁止非安全混合内容
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // 缓存策略：默认加载缓存，提升二次启动与资源加载速度
            cacheMode = WebSettings.LOAD_DEFAULT

            // 支持 window.open / target="_blank" 弹出窗口
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        // 配置 Cookie 管理器：保留登录态，支持第三方 Cookie
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(this, true)

        // 配置 WebViewClient 页面跳转与错误拦截
        webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                Log.d(TAG, "shouldOverrideUrlLoading: $url")

                // 如果属于三国杀官方域名体系，继续在当前 WebView 内加载
                if (UrlUtils.isSanGuoShaDomain(url)) {
                    return false
                }

                // 其他外部第三方链接或特殊 Scheme，交由系统安全拉起
                UrlUtils.handleExternalUri(context, url)
                return true
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                Log.d(TAG, "onPageStarted: $url")
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.d(TAG, "onPageFinished: $url")
                onPageFinish()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                // 仅对主框架页面加载失败展示错误遮罩
                if (request?.isForMainFrame == true) {
                    val desc = error?.description?.toString()
                    Log.e(TAG, "onReceivedError for main frame: $desc, code=${error?.errorCode}")
                    onErrorOccurred(desc)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                // 安全规范：严禁忽略或绕过 SSL 证书错误
                Log.e(TAG, "onReceivedSslError: $error")
                handler?.cancel()
                onErrorOccurred("安全证书校验失败，已终止连接")
            }
        }

        // 配置 WebChromeClient 多窗口支持
        webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                // 安全处理 window.open / target="_blank"，避免白屏
                val newWebView = WebView(context).apply {
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            v: WebView?,
                            req: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = req?.url?.toString()
                            if (!targetUrl.isNullOrBlank()) {
                                if (UrlUtils.isSanGuoShaDomain(targetUrl)) {
                                    view?.loadUrl(targetUrl)
                                } else {
                                    UrlUtils.handleExternalUri(context, targetUrl)
                                }
                            }
                            return true
                        }
                    }
                }
                val transport = resultMsg?.obj as? WebView.WebViewTransport
                transport?.webView = newWebView
                resultMsg?.sendToTarget()
                return true
            }
        }
    }
}
