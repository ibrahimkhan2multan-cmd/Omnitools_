package com.omnitools.app.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** One set of sizes and timings for the whole app, so screens stop drifting apart. */
object Dim {
    val ScreenPad = 16.dp
    val CardRadius = 18.dp
    val ButtonRadius = 14.dp
    val IconBadge = 44
    val IconBadgeSmall = 40
    val MinTouch = 48.dp
}

/** Animation timings (milliseconds). Short, so nothing makes the user wait. */
object Dur {
    const val PRESS = 110
    const val FAST = 160
    const val MEDIUM = 240
    const val SCREEN = 260
}

/** True when the user has switched animations off (or to zero) in the phone's developer/accessibility settings. */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Throwable) { false }
    }
}
