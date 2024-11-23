package com.ileping.fill_light

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
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
        private fun showToast(message: String) {
            activity.runOnUiThread { Toast.makeText(activity, message, Toast.LENGTH_SHORT).show() }
        }

        private fun checkAndRequestPermission(): Boolean {
            if (!Settings.System.canWrite(activity)) {
                showToast("需要授权才能调节亮度")
                requestBrightnessPermission()
                return false
            }
            return true
        }

        @android.webkit.JavascriptInterface
        fun checkBrightnessPermission(): Boolean {
            return Settings.System.canWrite(activity)
        }

        @android.webkit.JavascriptInterface
        fun requestBrightnessPermission() {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:" + activity.packageName)
                activity.startActivity(intent)
            } catch (e: Exception) {
                Log.e("MainActivity", "请求权限失败: ${e.message}")
                showToast("打开设置页面失败，请手动授权")
            }
        }

        @android.webkit.JavascriptInterface
        fun setScreenBrightness(brightness: Int): Boolean {
            if (!checkAndRequestPermission()) {
                return false
            }

            return try {
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
                true
            } catch (e: Exception) {
                Log.e("MainActivity", "设置亮度失败: ${e.message}")
                showToast("设置亮度失败")
                false
            }
        }

        @android.webkit.JavascriptInterface
        fun setAutoBrightness(enabled: Boolean): Boolean {
            if (!checkAndRequestPermission()) {
                return false
            }

            return try {
                Settings.System.putInt(
                        activity.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS_MODE,
                        if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                        else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                true
            } catch (e: Exception) {
                Log.e("MainActivity", "设置自动亮度失败: ${e.message}")
                showToast("设置自动亮度失败")
                false
            }
        }

        @android.webkit.JavascriptInterface
        fun getScreenBrightness(): Int {
            try {
                return Settings.System.getInt(
                        activity.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "获取亮度失败: ${e.message}")
                e.printStackTrace()
            }
            return -1
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
        fun setKeepScreenOn(enabled: Boolean): Boolean {
            return try {
                activity.runOnUiThread {
                    if (enabled) {
                        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    } else {
                        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                }
                true
            } catch (e: Exception) {
                Log.e("MainActivity", "设置屏幕常亮失败: ${e.message}")
                showToast("设置屏幕常亮失败")
                false
            }
        }

        @android.webkit.JavascriptInterface
        fun isKeepScreenOn(): Boolean {
            return (activity.window.attributes.flags and
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) != 0
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
