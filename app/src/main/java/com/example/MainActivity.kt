package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.GameScreen
import com.example.ui.theme.SanGuoShaTheme
import com.example.util.ImmersiveUtils

class MainActivity : ComponentActivity() {

    private var lastBackPressTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 应用横屏沉浸式全屏与常亮设置
        ImmersiveUtils.applyImmersiveFullScreen(this)

        setContent {
            SanGuoShaTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    GameScreen(
                        onBackPressWhenRoot = { handleRootBackPress() }
                    )
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 当窗口重新获取焦点时（如收起通知栏/系统栏后），再次进入沉浸式模式
        if (hasFocus) {
            ImmersiveUtils.applyImmersiveFullScreen(this)
        }
    }

    override fun onResume() {
        super.onResume()
        ImmersiveUtils.applyImmersiveFullScreen(this)
    }

    /**
     * 处理处于网页首页/根路径时的返回按键逻辑：
     * 第一次提示“再次返回退出”，2秒内二次返回才真正退出应用，防止误触导致游戏中断
     */
    private fun handleRootBackPress() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastBackPressTime < 2000) {
            finish()
        } else {
            lastBackPressTime = currentTime
            Toast.makeText(this, getString(R.string.back_again_to_exit), Toast.LENGTH_SHORT).show()
        }
    }
}
