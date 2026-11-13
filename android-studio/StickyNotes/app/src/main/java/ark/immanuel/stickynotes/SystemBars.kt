package ark.immanuel.stickynotes

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Apps targeting API 35+ are always drawn edge to edge: content sits behind the status bar,
 * the navigation bar and the keyboard, and android:statusBarColor is ignored.
 *
 * Call after super.onCreate(), before setContentView(). Bars are transparent with light icons, so the root view's
 * background (colorPrimaryVariant, as the old status bar colour) shows through them.
 */
fun ComponentActivity.drawBehindSystemBars() {
    enableEdgeToEdge(
        statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
    )
}

/** Pads [root] so its content clears the system bars, display cutout and on-screen keyboard. */
fun padForSystemBars(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
        val bars = insets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        )
        view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        WindowInsetsCompat.CONSUMED
    }
}
