package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.*

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        
        // Handle Action complete or Action decline directly from active notification buttons
        if (action == "com.example.me_va_be.ACTION_COMPLETE" || action == "com.example.me_va_be.ACTION_DECLINE") {
            val notificationId = intent.getIntExtra("EXTRA_ID", -1)
            val itemId = intent.getStringExtra("EXTRA_ITEM_ID")
            val itemType = intent.getStringExtra("EXTRA_TYPE") ?: ""

            // Dismiss the notification first
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (notificationId != -1) {
                notificationManager.cancel(notificationId)
            }

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getDatabase(context)
                    if (action == "com.example.me_va_be.ACTION_COMPLETE") {
                        if (itemType == "APPOINTMENT" && itemId != null) {
                            db.appointmentDao().getAppointmentById(itemId)?.let { appt ->
                                db.appointmentDao().insertOrUpdate(appt.copy(status = "COMPLETED"))
                            }
                        } else if (itemType == "REMINDER" && itemId != null) {
                            db.medicationReminderDao().getReminderById(itemId)?.let { rem ->
                                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                db.medicationReminderDao().insertOrUpdate(rem.copy(lastTakenDate = todayStr))
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "🎉 Đã hoàn thành nhiệm vụ thai kỳ!", Toast.LENGTH_SHORT).show()
                        }
                    } else if (action == "com.example.me_va_be.ACTION_DECLINE") {
                        if (itemType == "APPOINTMENT" && itemId != null) {
                            db.appointmentDao().getAppointmentById(itemId)?.let { appt ->
                                db.appointmentDao().insertOrUpdate(appt.copy(status = "CANCELLED"))
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Đã từ chối nhắc nhở thai kỳ.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Standard notification trigger
        val title = intent.getStringExtra("EXTRA_TITLE") ?: "Nhắc nhở thai kỳ Mẹ & Bé"
        val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "Mẹ bầu ơi, đến giờ kiểm tra thông tin rồi!"
        val notificationId = intent.getIntExtra("EXTRA_ID", 1001)
        val itemId = intent.getStringExtra("EXTRA_ITEM_ID")
        val itemType = intent.getStringExtra("EXTRA_TYPE") ?: ""
        val targetTab = intent.getIntExtra("EXTRA_TARGET_TAB", -1)

        // Reschedule recurring exact alerts
        if (itemType == "REMINDER") {
            val timeStr = intent.getStringExtra("EXTRA_TIME_STR")
            if (!itemId.isNullOrBlank() && !timeStr.isNullOrBlank()) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val reminder = db.medicationReminderDao().getReminderById(itemId)
                        if (reminder != null && reminder.isActive) {
                            withContext(Dispatchers.Main) {
                                com.example.ui.PregnancyNotifier.scheduleReminderAlert(
                                    context = context,
                                    id = reminder.id,
                                    title = title,
                                    message = message,
                                    timeStr = timeStr
                                )
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } else if (itemType == "WEIGHT") {
            com.example.ui.PregnancyNotifier.scheduleWeeklyWeightAlert(context)
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "me_va_be_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở thai kỳ Mẹ & Bé",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh phát thông báo lịch khám, uống thuốc và cân nặng mẹ bầu"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Complete action button intent
        val completeIntent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = "com.example.me_va_be.ACTION_COMPLETE"
            putExtra("EXTRA_ID", notificationId)
            putExtra("EXTRA_ITEM_ID", itemId)
            putExtra("EXTRA_TYPE", itemType)
        }
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 100000,
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action button intent
        val declineIntent = Intent(context, NotificationReceiver::class.java).apply {
            this.action = "com.example.me_va_be.ACTION_DECLINE"
            putExtra("EXTRA_ID", notificationId)
            putExtra("EXTRA_ITEM_ID", itemId)
            putExtra("EXTRA_TYPE", itemType)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 200000,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Main content click intent deep-linking to specific tab
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_TARGET_TAB", targetTab)
            putExtra("EXTRA_ITEM_ID", itemId)
            putExtra("EXTRA_TYPE", itemType)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add action buttons if item details are attached
        if (!itemId.isNullOrBlank()) {
            notificationBuilder.addAction(0, "Từ chối", declinePendingIntent)
            notificationBuilder.addAction(0, "Hoàn thành", completePendingIntent)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}
