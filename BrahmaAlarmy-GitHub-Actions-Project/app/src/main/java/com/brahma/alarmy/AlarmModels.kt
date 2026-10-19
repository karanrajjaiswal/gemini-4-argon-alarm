package com.brahma.alarmy

import android.content.Context

enum class MissionType(val title: String, val description: String) {
    MATH("Math Brain-Wake", "Solve 3 arithmetic problems to stop the alarm"),
    SHAKE("Physical Shake", "Shake your phone vigorously 30 times"),
    MANTRA("Mindful Typing", "Type a morning focus affirmation accurately")
}

data class AlarmItem(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val enabled: Boolean,
    val mission: MissionType,
    val isBrahmaMuhurta: Boolean = false
)

object AlarmPrefs {
    private const val PREFS_NAME = "brahma_alarmy_prefs"

    fun saveBrahmaState(
        context: Context,
        enabled: Boolean,
        mission: MissionType,
        lat: Double,
        lon: Double
    ) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putBoolean("brahma_enabled", enabled)
            putString("brahma_mission", mission.name)
            putString("lat", lat.toString())
            putString("lon", lon.toString())
            apply()
        }
    }

    fun isBrahmaEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean("brahma_enabled", false)

    fun getBrahmaMission(context: Context): MissionType {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("brahma_mission", MissionType.MATH.name) ?: MissionType.MATH.name
        return MissionType.valueOf(name)
    }

    // Defaults to Hyderabad (17.3850, 78.4867) if GPS is not yet acquired
    fun getCoordinates(context: Context): Pair<Double, Double> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getString("lat", "17.3850")?.toDoubleOrNull() ?: 17.3850
        val lon = prefs.getString("lon", "78.4867")?.toDoubleOrNull() ?: 78.4867
        return Pair(lat, lon)
    }

    fun saveCustomAlarm(context: Context, hour: Int, minute: Int, enabled: Boolean, mission: MissionType) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
            putInt("custom_hour", hour)
            putInt("custom_min", minute)
            putBoolean("custom_enabled", enabled)
            putString("custom_mission", mission.name)
            apply()
        }
    }

    fun getCustomAlarm(context: Context): AlarmItem {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hour = prefs.getInt("custom_hour", 6)
        val min = prefs.getInt("custom_min", 30)
        val enabled = prefs.getBoolean("custom_enabled", false)
        val missionName = prefs.getString("custom_mission", MissionType.SHAKE.name) ?: MissionType.SHAKE.name
        return AlarmItem(
            id = 2002,
            hour = hour,
            minute = min,
            label = "Daily Wake-Up Mission",
            enabled = enabled,
            mission = MissionType.valueOf(missionName),
            isBrahmaMuhurta = false
        )
    }
}
