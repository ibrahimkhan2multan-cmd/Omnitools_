package com.omnitools.app.tools.timer.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/** What the phone must allow so the timer can ring while the app is closed. */
object AlarmPermissions {

    /** Needed to show the ringing notification with its Stop button (a runtime permission on Android 13+). */
    fun notificationsAllowed(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** Needed to open the full-screen "Time's up" screen over the lock screen (a switch in Settings on Android 14+). */
    fun fullScreenAllowed(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 34) {
            context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() ?: true
        } else {
            true
        }

    fun notificationSettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun fullScreenSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= 34) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            notificationSettingsIntent(context)
        }
}
