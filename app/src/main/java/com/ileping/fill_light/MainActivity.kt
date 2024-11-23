package com.ileping.fill_light

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private var _exitTime = 0L

    inner class WebAppInterface(private val activity: Activity) {
        @android.webkit.JavascriptInterface
        fun checkBrightnessPermission(): Boolean {
            return Settings.System.canWrite(activity)
        }

        @android.webkit.JavascriptInterface
        fun requestBrightnessPermission() {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:" + activity.packageName)
            activity.startActivity(intent)
        }

        @android.webkit.JavascriptInterface
        fun getScreenBrightness(): Int {
            try {
                val systemBrightness = Settings.System.getInt(
                    activity.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
                return systemBrightness
            } catch (e: Exception) {
                Log.e("MainActivity", "获取亮度失败: ${e.message}")
                e.printStackTrace()
            }
            return -1
        }

        @android.webkit.JavascriptInterface
        fun setScreenBrightness(brightness: Int) {
            try {
                if (Settings.System.canWrite(activity)) {
                    val validBrightness = brightness.coerceIn(0, 255)

                    Settings.System.putInt(
                            activity.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            validBrightness
                    )

                    activity.runOnUiThread {
                        activity.window.attributes =
                                activity.window.attributes.apply {
                                    screenBrightness = validBrightness / 255f
                                }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "设置亮度失败: ${e.message}")
                e.printStackTrace()
            }
        }

        @android.webkit.JavascriptInterface
        fun setAutoBrightness(enabled: Boolean) {
            try {
                if (Settings.System.canWrite(activity)) {
                    Settings.System.putInt(
                            activity.contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE,
                            if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                            else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                    )
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "设置自动亮度失败: ${e.message}")
                e.printStackTrace()
            }
        }

        @android.webkit.JavascriptInterface
        fun isAutoBrightnessEnabled(): Boolean {
            return try {
                Settings.System.getInt(
                        activity.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE
                ) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            } catch (e: Exception) {
                Log.e("MainActivity", "获取自动亮度状态失败: ${e.message}")
                false
            }
        }

        @android.webkit.JavascriptInterface
        fun setKeepScreenOn(enabled: Boolean) {
            activity.runOnUiThread {
                if (enabled) {
                    activity.window.addFlags(
                            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                } else {
                    activity.window.clearFlags(
                            android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    )
                }
            }
        }

        @android.webkit.JavascriptInterface
        fun isKeepScreenOn(): Boolean {
            return (activity.window.attributes.flags and
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查是否有修改系统设置的权限
        if (!Settings.System.canWrite(this)) {
            Toast.makeText(this, "需要授权才能调节亮度", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        }

        webView =
                WebView(this).apply {
                    settings.apply {
                        domStorageEnabled = true
                        javaScriptEnabled = true
                        blockNetworkImage = false
                    }

                    // 更新 JavaScript 接口
                    addJavascriptInterface(WebAppInterface(this@MainActivity), "Android")

                    webViewClient = object : WebViewClient() {}

                    webChromeClient = WebChromeClient()

                    loadUrl("file:///android_asset/main.html")
                }

        findViewById<LinearLayout>(R.id.main_container)
                .addView(
                        webView,
                        LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                        )
                )
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (System.currentTimeMillis() - _exitTime > 2000) {
            Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show()
            _exitTime = System.currentTimeMillis()
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }
}
