package ark.immanuel.stickynotes

import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient

import android.webkit.WebSettings



class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawBehindSystemBars()
        setContentView(R.layout.activity_splash)
        padForSystemBars(findViewById(R.id.root))
        val myWebView: WebView = findViewById(R.id.webViewSplash)
        val webSetting: WebSettings = myWebView.getSettings()
        //webSetting.builtInZoomControls = true
        myWebView.setWebViewClient(WebViewClient())
        //myWebView.loadData(wedData,mimeType,utfType)
        myWebView.loadUrl("file:///android_asset/splash.html")

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}