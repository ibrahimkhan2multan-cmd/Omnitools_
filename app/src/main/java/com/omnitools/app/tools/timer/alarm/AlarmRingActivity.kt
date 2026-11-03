package com.omnitools.app.tools.timer.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnitools.app.tools.timer.TimerController
import com.omnitools.app.tools.timer.TimerStore
import com.omnitools.app.ui.theme.OmniToolsTheme
import com.omnitools.app.util.TimeFormat

/** Full-screen "Time's up" screen. Shows over the lock screen and turns the display on while the alarm rings. */
class AlarmRingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        TimerController.init(applicationContext)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            OmniToolsTheme(themeMode = "dark") {
                RingScreen(
                    onStop = { TimerController.stopRinging(applicationContext) },
                    onClose = { finish() },
                )
            }
        }
    }
}

@Composable
private fun RingScreen(onStop: () -> Unit, onClose: () -> Unit) {
    val ringing = TimerController.ringing
    // Close as soon as the alarm is stopped, whether from this screen or from the notification.
    LaunchedEffect(ringing) { if (!ringing) onClose() }

    val pulse = rememberInfiniteTransition(label = "ring pulse")
    val wave by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "wave",
    )
    val bell by pulse.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bell",
    )
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) {
                    val maxR = size.minDimension / 2f
                    for (i in 0..2) {
                        val t = (wave + i / 3f) % 1f
                        drawCircle(
                            color = (if (i % 2 == 0) primary else secondary).copy(alpha = (1f - t) * 0.55f),
                            radius = maxR * (0.35f + 0.65f * t),
                            style = Stroke(width = 6.dp.toPx()),
                        )
                    }
                }
                Icon(
                    Icons.Filled.Alarm,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(84.dp).scale(bell),
                )
            }
            Text("Time's up!", fontSize = 36.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Text(
                "Set for " + TimeFormat.countdownPrecise(TimerStore.timer.totalMs),
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 40.dp),
            )
            Button(
                onClick = onStop,
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth().height(64.dp),
            ) { Text("Stop", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = Color.White) }
        }
    }
}
