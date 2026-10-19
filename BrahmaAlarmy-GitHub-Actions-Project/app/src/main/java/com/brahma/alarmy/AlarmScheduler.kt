package com.brahma.alarmy

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

object AlarmScheduler {
    const val BRAHMA_ALARM_ID = 1001
    const val CUSTOM_ALARM_ID = 2002

    fun scheduleBrahmaAlarm(context: Context): BrahmaWindow {
        val (lat, lon) = AlarmPrefs.getCoordinates(context)
        val mission = AlarmPrefs.getBrahmaMission(context)
        val window = BrahmaMuhurtaCalculator.getNextBrahmaWindow(lat, lon)

        scheduleExact(
            context = context,
            alarmId = BRAHMA_ALARM_ID,
            triggerAtMillis = window.triggerEpochMillis,
            label = "Brahma Muhurta (${window.brahmaStart})",
            mission = mission,
            isBrahma = true
        )
        return window
    }

    fun scheduleCustomAlarm(context: Context, item: AlarmItem) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, item.hour)
            set(Calendar.MINUTE, item.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        scheduleExact(
            context = context,
            alarmId = CUSTOM_ALARM_ID,
            triggerAtMillis = cal.timeInMillis,
            label = item.label,
            mission = item.mission,
            isBrahma = false
        )
    }

    private fun scheduleExact(
        context: Context,
        alarmId: Int,
        triggerAtMillis: Long,
        label: String,
        mission: MissionType,
        isBrahma: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.brahma.alarmy.ACTION_TRIGGER_ALARM"
            putExtra("ALARM_ID", alarmId)
            putExtra("LABEL", label)
            putExtra("MISSION", mission.name)
            putExtra("IS_BRAHMA", isBrahma)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val showIntent = Intent(context, MainActivity::class.java)
        val showPending = PendingIntent.getActivity(
            context,
            alarmId,
            showIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, showPending)
        alarmManager.setAlarmClock(clockInfo, pendingIntent)
    }

    fun cancelAlarm(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.brahma.alarmy.ACTION_TRIGGER_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
