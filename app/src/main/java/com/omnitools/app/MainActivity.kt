package com.omnitools.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // Restore a timer that was running when the app was closed.
        com.omnitools.app.tools.timer.TimerController.init(applicationContext)
        // Temporary copies and shared images from earlier runs (for example a Wi-Fi QR code) should not linger on disk.
        val cache = cacheDir
        Thread {
            try {
                java.io.File(cache, "shared").deleteRecursively()
                cache.listFiles()?.forEach { f -> if (f.isFile && (f.name == "zip_open.zip" || f.name.endsWith(".pdf") || f.name.startsWith("pdf"))) f.delete() }
            } catch (_: Throwable) {}
        }.start()
        val prefs = com.omnitools.app.data.Prefs(applicationContext)
        setContent { OmniApp(prefs) }
    }
}
