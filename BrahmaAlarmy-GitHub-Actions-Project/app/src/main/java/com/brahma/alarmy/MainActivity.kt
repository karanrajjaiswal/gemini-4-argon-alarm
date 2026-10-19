package com.brahma.alarmy

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFFFB74D),
                    secondary = Color(0xFF9575CD),
                    background = Color(0xFF0D0B18),
                    surface = Color(0xFF1A162B)
                )
            ) {
                BrahmaAlarmyDashboard()
            }
        }
    }
}

@Composable
fun BrahmaAlarmyDashboard() {
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    var coords by remember { mutableStateOf(AlarmPrefs.getCoordinates(context)) }
    var brahmaEnabled by remember { mutableStateOf(AlarmPrefs.isBrahmaEnabled(context)) }
    var brahmaMission by remember { mutableStateOf(AlarmPrefs.getBrahmaMission(context)) }
    var customAlarm by remember { mutableStateOf(AlarmPrefs.getCustomAlarm(context)) }

    val brahmaWindow = remember(coords) {
        BrahmaMuhurtaCalculator.getNextBrahmaWindow(coords.first, coords.second)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            fetchDeviceGps(context)?.let { newCoords ->
                coords = newCoords
                AlarmPrefs.saveBrahmaState(context, brahmaEnabled, brahmaMission, newCoords.first, newCoords.second)
                if (brahmaEnabled) AlarmScheduler.scheduleBrahmaAlarm(context)
            }
        }
    }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(perms.toTypedArray())
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0B18))
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Brahma Alarmy",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = "Solar-Synced Brahma Muhurta & Mission Wake-Up",
            color = Color(0xFFB39DDB),
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // BRAHMA MUHURTA HERO CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF281B4E), Color(0xFF4A2545), Color(0xFF8D3B32))
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.WbTwilight,
                                contentDescription = null,
                                tint = Color(0xFFFFB74D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BRAHMA MUHURTA",
                                color = Color(0xFFFFB74D),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                letterSpacing = 1.2.sp
                            )
                        }
                        Switch(
                            checked = brahmaEnabled,
                            onCheckedChange = { checked ->
                                checkExactAlarmPermission(context)
                                brahmaEnabled = checked
                                AlarmPrefs.saveBrahmaState(
                                    context, checked, brahmaMission, coords.first, coords.second
                                )
                                if (checked) {
                                    val win = AlarmScheduler.scheduleBrahmaAlarm(context)
                                    Toast.makeText(
                                        context,
                                        "Brahma Muhurta set for ${win.brahmaStart.format(timeFormatter)}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    AlarmScheduler.cancelAlarm(context, AlarmScheduler.BRAHMA_ALARM_ID)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = brahmaWindow.brahmaStart.format(timeFormatter),
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Sacred Window: ${brahmaWindow.brahmaStart.format(timeFormatter)} – ${brahmaWindow.brahmaEnd.format(timeFormatter)} • Sunrise: ${brahmaWindow.sunriseTime.format(timeFormatter)}",
                        color = Color(0xFFE0E0E0),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFFFCC80),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "GPS: ${"%.4f".format(coords.first)}°, ${"%.4f".format(coords.second)}°",
                                color = Color(0xFFFFCC80),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = {
                            val updated = fetchDeviceGps(context)
                            if (updated != null) {
                                coords = updated
                                AlarmPrefs.saveBrahmaState(context, brahmaEnabled, brahmaMission, updated.first, updated.second)
                                if (brahmaEnabled) AlarmScheduler.scheduleBrahmaAlarm(context)
                                Toast.makeText(context, "GPS Updated!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Using saved coordinates (Enable GPS for live fix)", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Text("Sync GPS", color = Color(0xFFFFB74D), fontSize = 12.sp)
                        }
                    }

                    HorizontalDivider(color = Color(0x33FFFFFF), modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "Wake-Up Mission (Required to Stop Alarm):",
                        color = Color(0xFFB39DDB),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MissionSelectorRow(
                        selected = brahmaMission,
                        onSelect = { newMission ->
                            brahmaMission = newMission
                            AlarmPrefs.saveBrahmaState(context, brahmaEnabled, newMission, coords.first, coords.second)
                            if (brahmaEnabled) AlarmScheduler.scheduleBrahmaAlarm(context)
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CUSTOM MISSION ALARM CARD
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A162B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = Color(0xFF9575CD))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CUSTOM MISSION ALARM",
                            color = Color(0xFF9575CD),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Switch(
                        checked = customAlarm.enabled,
                        onCheckedChange = { enabled ->
                            checkExactAlarmPermission(context)
                            val updated = customAlarm.copy(enabled = enabled)
                            customAlarm = updated
                            AlarmPrefs.saveCustomAlarm(context, updated.hour, updated.minute, enabled, updated.mission)
                            if (enabled) {
                                AlarmScheduler.scheduleCustomAlarm(context, updated)
                            } else {
                                AlarmScheduler.cancelAlarm(context, AlarmScheduler.CUSTOM_ALARM_ID)
                            }
                        }
                    )
                }

                TextButton(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, h, m ->
                                val updated = customAlarm.copy(hour = h, minute = m, enabled = true)
                                customAlarm = updated
                                AlarmPrefs.saveCustomAlarm(context, h, m, true, updated.mission)
                                AlarmScheduler.scheduleCustomAlarm(context, updated)
                            },
                            customAlarm.hour,
                            customAlarm.minute,
                            false
                        ).show()
                    }
                ) {
                    Text(
                        text = "%02d:%02d".format(customAlarm.hour, customAlarm.minute),
                        color = Color.White,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                MissionSelectorRow(
                    selected = customAlarm.mission,
                    onSelect = { m ->
                        val updated = customAlarm.copy(mission = m)
                        customAlarm = updated
                        AlarmPrefs.saveCustomAlarm(context, updated.hour, updated.minute, updated.enabled, m)
                        if (updated.enabled) AlarmScheduler.scheduleCustomAlarm(context, updated)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // INSTANT MISSION TESTER BUTTON
        Button(
            onClick = {
                val intent = Intent(context, AlarmService::class.java).apply {
                    putExtra("LABEL", "Brahma Muhurta Live Test")
                    putExtra("MISSION", brahmaMission.name)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Test Loud Alarm & ${brahmaMission.title} Now",
                color = Color.Black,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MissionSelectorRow(selected: MissionType, onSelect: (MissionType) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MissionType.entries.forEach { mission ->
            FilterChip(
                selected = selected == mission,
                onClick = { onSelect(mission) },
                label = { Text(mission.name, fontSize = 12.sp) }
            )
        }
    }
}

@SuppressLint("MissingPermission")
fun fetchDeviceGps(context: Context): Pair<Double, Double>? {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        return null
    }
    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = lm.getProviders(true)
    for (provider in providers.reversed()) {
        val loc = lm.getLastKnownLocation(provider)
        if (loc != null) {
            return Pair(loc.latitude, loc.longitude)
        }
    }
    return null
}

fun checkExactAlarmPermission(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!am.canScheduleExactAlarms()) {
            context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
        }
    }
}
