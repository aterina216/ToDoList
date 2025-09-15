package com.example.todolist.view

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.todolist.R



class ReminderReceiver : BroadcastReceiver() {
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("task_title") ?: "Напоминание"
        val description = intent.getStringExtra("task_description") ?: ""

        // Создаем канал уведомлений
        createNotificationChannel(context)

        // Создаем уведомление
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(description)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000)) // Вибрация: пауза, вибрация, пауза, вибрация
            .build()

        // Показываем уведомление
        NotificationManagerCompat.from(context).notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "reminder_channel",
                "Напоминания о задачах",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний о задачах"
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000)
            }

            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}