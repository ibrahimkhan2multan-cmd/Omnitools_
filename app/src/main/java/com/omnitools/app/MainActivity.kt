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
        val prefs = com.omnitools.app.data.Prefs(applicationContext)
        setContent { OmniApp(prefs) }
    }
}
