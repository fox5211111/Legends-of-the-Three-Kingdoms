package com.example.util

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

object ImmersiveUtils {

    /**
     * 进入横屏全屏沉浸模式
     * 隐藏系统状态栏和底部导航栏，并启用边缘滑动呼出后自动隐藏
     */
    fun applyImmersiveFullScreen(activity: Activity) {
        val window = activity.window

        // 保持屏幕常亮，防止游玩过程中息屏
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 适配异形屏、刘海屏、挖孔屏 (Android 9.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        // 设置内容延伸到系统栏下方
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        // 允许用户通过边缘轻扫唤出系统栏，稍后自动隐藏
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 隐藏状态栏与导航栏
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
