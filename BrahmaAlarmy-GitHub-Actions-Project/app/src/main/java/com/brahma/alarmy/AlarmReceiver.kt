package com.brahma.alarmy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (AlarmPrefs.isBrahmaEnabled(context)) {
                    AlarmScheduler.scheduleBrahmaAlarm(context)
                }
                val custom = AlarmPrefs.getCustomAlarm(context)
                if (custom.enabled) {
                    AlarmScheduler.scheduleCustomAlarm(context, custom)
                }
            }
            "com.brahma.alarmy.ACTION_TRIGGER_ALARM" -> {
                val label = intent.getStringExtra("LABEL") ?: "Brahma Muhurta Alarm"
                val mission = intent.getStringExtra("MISSION") ?: MissionType.MATH.name
                val isBrahma = intent.getBooleanExtra("IS_BRAHMA", false)

                if (isBrahma && AlarmPrefs.isBrahmaEnabled(context)) {
                    AlarmScheduler.scheduleBrahmaAlarm(context)
                }

                val serviceIntent = Intent(context, AlarmService::class.java).apply {
                    putExtra("LABEL", label)
                    putExtra("MISSION", mission)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
