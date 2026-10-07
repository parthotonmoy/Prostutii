package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val phase = intent.getStringExtra("phase") ?: "focus"
        val soundOn = intent.getBooleanExtra("soundOn", true)
        val vibrateOn = intent.getBooleanExtra("vibrateOn", true)
        val title = intent.getStringExtra("title") ?: if (phase == "focus") "Focus complete" else "Break over"
        val message = intent.getStringExtra("message") ?: if (phase == "focus") "25 minutes done. Stand up for 5." else "Break's over. Ready for the next block."

        val channelId = "prostuti_timer"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Focus Timer",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when pomodoro focus or break completes"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(1001, notification)

        // Soft beep via ToneGenerator
        if (soundOn) {
            try {
                val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 70)
                toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 300)
            } catch (e: Exception) {
                // Ignore tone error
            }
        }

        // Vibration
        if (vibrateOn) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(400)
                }
            } catch (e: Exception) {
                // Ignore vibration error
            }
        }
    }
}
