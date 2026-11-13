package ark.immanuel.stickynotes

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.app.ProgressDialog
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.*
import android.util.Log
import android.webkit.*
import android.webkit.WebView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout








class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout

    // Back steps through the WebView's history. onBackPressed() is no longer called for apps
    // targeting API 36 (predictive back), so this goes through the OnBackPressedDispatcher. It is
    // enabled only while there is a page to go back to; otherwise Back leaves the app as usual.
    private val webViewBack = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            webView.goBack()
        }
    }

    // The page records audio with getUserMedia(). WebView turns that into onPermissionRequest(),
    // and the page's promise stays pending until the request is granted or denied. When the app
    // does not hold RECORD_AUDIO yet, the request waits here while Android asks the user.
    private var pendingMicRequest: PermissionRequest? = null

    private val micPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val request = pendingMicRequest ?: return@registerForActivityResult
            pendingMicRequest = null
            if (granted) {
                request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
            } else {
                request.deny()
                // After a denial or two Android stops showing its dialog, so point at Settings.
                // Keep it short: Android 12+ cuts toasts off after two lines.
                Toast.makeText(
                    this,
                    "Microphone blocked. Allow it in Settings > Apps > Sticky Notes",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        drawBehindSystemBars()
        setContentView(R.layout.activity_main)
        padForSystemBars(findViewById(R.id.root))
        onBackPressedDispatcher.addCallback(this, webViewBack)
        val progressDialog: ProgressDialog = ProgressDialog(this)
        webView = findViewById(R.id.webView)
        swipeRefreshLayout = findViewById(R.id.swiperefresh);

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.setRefreshing(false)
            webView.reload()
        }

        swipeRefreshLayout.setColorSchemeColors(
            getResources().getColor(android.R.color.holo_blue_bright),
            getResources().getColor(android.R.color.holo_orange_dark),
            getResources().getColor(android.R.color.holo_green_dark),
            getResources().getColor(android.R.color.holo_red_dark)
        );

        webView.setDownloadListener(DownloadListener {
                url,
                userAgent,
                contentDescription,
                mimetype,
                contentLength ->

            Toast.makeText(this, "Download Listen 1...." + mimetype, Toast.LENGTH_SHORT).show()
            // Initialize download request
            val request = DownloadManager.Request(Uri.parse(url))

            // Get the cookie
            val cookies = CookieManager.getInstance().getCookie(url)

            // Add the download request header
            request.addRequestHeader("Cookie",cookies)
            request.addRequestHeader("User-Agent",userAgent)

            // Set download request description
            request.setDescription("Downloading requested file....")

            // Set download request mime tytpe
            request.setMimeType(mimetype)

            // Allow scanning
            request.allowScanningByMediaScanner()

            // Download request notification setting
            request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

            // Guess the file name
            var fileName = URLUtil.guessFileName(url, contentDescription, mimetype)
            fileName = fileName.replace(".bin", ".csv", true)
            Toast.makeText(this, "Download Listen 2...." + url, Toast.LENGTH_SHORT).show()
            // Set a destination storage for downloaded file
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)

            // Set request title
            //request.setTitle(URLUtil.guessFileName(url, contentDescription, mimetype));
            request.setTitle(fileName)


            // DownloadManager request more settings
            request.setAllowedOverMetered(true)
            request.setAllowedOverRoaming(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                request.setRequiresCharging(false)
                request.setRequiresDeviceIdle(false)
            }
            request.setVisibleInDownloadsUi(true)


            // Get the system download service
            val dManager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            // Finally, request the download to system download service
            dManager.enqueue(request)
        })

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, urlNewString: String?): Boolean {
//                if (!loadingFinished) {
//                    redirect = true
//                }
//                loadingFinished = false
                webView.loadUrl(urlNewString!!)
                return true
            }
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                println("Load Started: " + url)
                progressDialog.setTitle("Please Wait")
                progressDialog.setMessage("Loading ...")
                progressDialog.setCancelable(false) // blocks UI interaction
                progressDialog.show()
            }
            override fun onPageFinished(view: WebView, url: String) {
                println("Load Finished: " + url)
                progressDialog.hide()
            }

            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                webViewBack.isEnabled = webView.canGoBack()
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                println("Load Errored: ")
                progressDialog.hide()
                super.onReceivedError(view, request, error)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                Log.i("Permissions", "onPermissionRequest ${request.origin} ${request.resources.joinToString()}")

                // Only the mic, and only for the Sticky Notes site: other pages opened in this
                // WebView (links in notes load here too) get nothing.
                if (request.origin.host != APP_HOST ||
                    PermissionRequest.RESOURCE_AUDIO_CAPTURE !in request.resources
                ) {
                    request.deny()
                    return
                }
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
                ) {
                    request.grant(arrayOf(PermissionRequest.RESOURCE_AUDIO_CAPTURE))
                } else {
                    pendingMicRequest?.deny()
                    pendingMicRequest = request
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }

            override fun onPermissionRequestCanceled(request: PermissionRequest) {
                if (pendingMicRequest == request) pendingMicRequest = null
            }
        }
        // this will enable the javascript settings, it can also allow xss vulnerabilities
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(MyJavascriptInterface(this), "MyJavascriptInterface")
        if (checkForInternet(this)){
            webView.loadUrl("https://sticky-notes.immanuel.co/record")
        } else {
            webView.loadUrl("file:///android_asset/no_internet.html")
            fun hh() {
                Handler(Looper.getMainLooper()).postDelayed({
                    if (checkForInternet(this)){
                        webView.loadUrl("https://sticky-notes.immanuel.co/record")
                    } else {
                        hh();
                        Toast.makeText(this, "Checking Internet Connection....", Toast.LENGTH_SHORT).show()
                    }
                }, 15000)
            }
            hh()
        }
        // if you want to enable zoom feature
        //webView.settings.setSupportZoom(true)
        //myWebView.loadUrl("https://www.immanuel.co")
        //myWebView.loadUrl("https://sticky-notes.immanuel.co/record")

    }

    companion object {
        private const val APP_HOST = "sticky-notes.immanuel.co"
    }

    class MyJavascriptInterface(private val context: Context) {
        @JavascriptInterface
        fun showToast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    @SuppressLint("ServiceCast")
    private fun checkForInternet(context: Context): Boolean {

        // register activity with the connectivity manager service
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // if the android version is equal to M
        // or greater we need to use the
        // NetworkCapabilities to check what type of
        // network has the internet connection
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            // Returns a Network object corresponding to
            // the currently active default data network.
            val network = connectivityManager.activeNetwork ?: return false

            // Representation of the capabilities of an active network.
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                // Indicates this network uses a Wi-Fi transport,
                // or WiFi has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true

                // Indicates this network uses a Cellular transport. or
                // Cellular has network connectivity
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true

                // else return false
                else -> false
            }
        } else {
            // if the android version is below M
            @Suppress("DEPRECATION") val networkInfo =
                connectivityManager.activeNetworkInfo ?: return false
            @Suppress("DEPRECATION")
            return networkInfo.isConnected
        }
    }
}