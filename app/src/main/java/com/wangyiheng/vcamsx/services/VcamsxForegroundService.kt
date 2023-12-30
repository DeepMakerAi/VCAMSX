package com.wangyiheng.vcamsx.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.wangyiheng.vcamsx.MainActivity
import com.wangyiheng.vcamsx.R

class VcamsxForegroundService: Service()  {
    private val NOTIFICATION_ID = 1
    private val CHANEL_ID: String = VcamsxForegroundService::class.java.getName() + ".foreground"

    fun start(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(
                Intent(
                    context,
                    VcamsxForegroundService::class.java
                )
            )
        } else {
            context.startService(Intent(context, VcamsxForegroundService::class.java))
        }
    }

    fun stop(context: Context) {
        context.stopService(Intent(context, VcamsxForegroundService::class.java))
    }

    override fun onCreate() {
        super.onCreate()
        startForeground()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun startForeground() {
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
        //        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, MainActivity_.intent(this).get(), 0);
        val flags = PendingIntent.FLAG_IMMUTABLE
        val contentIntent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flags)
        return NotificationCompat.Builder(this, CHANEL_ID)
            .setContentTitle(getString(R.string.foreground_notification_title))
            .setContentText(getString(R.string.foreground_notification_text))
            .setOngoing(true)
            .setSmallIcon(R.drawable.logo)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(contentIntent)
            .setChannelId(CHANEL_ID)
            .setVibrate(LongArray(0))
            .build()
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val manager = (getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager)
        val name: CharSequence = getString(R.string.foreground_notification_channel_name)
        val description = getString(R.string.foreground_notification_channel_name)
        val channel = NotificationChannel(CHANEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = description
        channel.enableLights(false)
        manager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        stopForeground(true)
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(
                    Intent(
                        context,
                        VcamsxForegroundService::class.java
                    )
                )
            } else {
                context.startService(Intent(context, VcamsxForegroundService::class.java))
            }
        }
        fun stop(context: Context) {
            context.stopService(Intent(context, VcamsxForegroundService::class.java))
        }
    }
}