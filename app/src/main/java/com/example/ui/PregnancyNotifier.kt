package com.example.ui

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import com.example.NotificationReceiver
import java.text.SimpleDateFormat
import java.util.*

object PregnancyNotifier {

    // Helper to send test notification immediately
    fun sendNotificationImmediately(
        context: Context, 
        title: String, 
        message: String,
        itemId: String? = null,
        type: String? = null,
        targetTab: Int = -1
    ) {
        try {
            val notificationId = Random().nextInt(10000)
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_ID", notificationId)
                putExtra("EXTRA_ITEM_ID", itemId)
                putExtra("EXTRA_TYPE", type)
                putExtra("EXTRA_TARGET_TAB", targetTab)
            }
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schedule an appointment alert on its date at 08:00 AM
    fun scheduleAppointmentAlert(context: Context, id: String, title: String, message: String, dateStr: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            
            // Schedule the alert for the given date at 08:00 AM
            val fullDateStr = "${dateStr.trim()} 08:00"
            val targetDate = sdf.parse(fullDateStr) ?: return
            val calendar = Calendar.getInstance().apply {
                time = targetDate
            }

            // If scheduled date is in the past, don't trigger
            if (calendar.timeInMillis < System.currentTimeMillis()) {
                return
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_ID", id.hashCode())
                putExtra("EXTRA_ITEM_ID", id)
                putExtra("EXTRA_TYPE", "APPOINTMENT")
                putExtra("EXTRA_TARGET_TAB", 1)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use exact alarms where permitted, with fallback to preserve stability
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Cancel scheduled appointment/reminder
    fun cancelScheduledAlert(context: Context, id: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, NotificationReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schedule a daily repeating medicine/diet reminder alert at HH:mm
    fun scheduleReminderAlert(context: Context, id: String, title: String, message: String, timeStr: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val parts = timeStr.trim().split(":")
            if (parts.size != 2) return
            val hour = parts[0].trim().toIntOrNull() ?: 8
            val minute = parts[1].trim().toIntOrNull() ?: 0

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                
                // If the time has already passed today, schedule for tomorrow
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", title)
                putExtra("EXTRA_MESSAGE", message)
                putExtra("EXTRA_ID", id.hashCode())
                putExtra("EXTRA_ITEM_ID", id)
                putExtra("EXTRA_TYPE", "REMINDER")
                putExtra("EXTRA_TARGET_TAB", 3)
                putExtra("EXTRA_TIME_STR", timeStr.trim())
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // For exact timing matching user setup, use exact-allow-while-idle with self-reschedule on trigger
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Schedule a weekly reminder to update pregnancy weight
    fun scheduleWeeklyWeightAlert(context: Context) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val calendar = Calendar.getInstance().apply {
                // Schedule alert for Sunday morning at 09:00 AM
                set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // If Sunday 9:00 AM has already passed this week, schedule for next Sunday
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.WEEK_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("EXTRA_TITLE", "📅 Cập nhật cân nặng mẹ bầu")
                putExtra("EXTRA_MESSAGE", "Mẹ bầu ơi, đã tròn 1 tuần rồi! Hãy dành 1 phút cập nhật cân nặng mới nhất để phòng tránh rủi ro thai kỳ nhé.")
                putExtra("EXTRA_ID", 9999)
                putExtra("EXTRA_ITEM_ID", "WEIGHT_TRACKER")
                putExtra("EXTRA_TYPE", "WEIGHT")
                putExtra("EXTRA_TARGET_TAB", 4)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                9999,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Use exact one-shot alert for precise weekly timing, with self-reschedule on trigger
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
