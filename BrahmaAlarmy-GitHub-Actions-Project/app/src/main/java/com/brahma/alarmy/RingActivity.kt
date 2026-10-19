package com.brahma.alarmy

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt
import kotlin.random.Random

class RingActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var shakeCountState = mutableIntStateOf(0)
    private var lastShakeTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        turnScreenOnAndUnlock()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* Blocked until mission is complete */ }
        })

        val label = intent.getStringExtra("LABEL") ?: "Brahma Muhurta"
        val missionName = intent.getStringExtra("MISSION") ?: MissionType.MATH.name
        val mission = MissionType.valueOf(missionName)

        if (mission == MissionType.SHAKE) {
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                MissionRingScreen(
                    label = label,
                    mission = mission,
                    shakeCount = shakeCountState.intValue,
                    onMissionCompleted = { dismissAlarmAndFinish() }
                )
            }
        }
    }

    private fun dismissAlarmAndFinish() {
        sensorManager?.unregisterListener(this)
        stopService(Intent(this, AlarmService::class.java))
        finish()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
            if (gForce > 2.2) {
                val now = System.currentTimeMillis()
                if (now - lastShakeTimestamp > 250) {
                    lastShakeTimestamp = now
                    shakeCountState.intValue += 1
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun turnScreenOnAndUnlock() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }
}

@Composable
fun MissionRingScreen(
    label: String,
    mission: MissionType,
    shakeCount: Int,
    onMissionCompleted: () -> Unit
) {
    val bgGradient = Brush.verticalGradient(
        listOf(Color(0xFF120D31), Color(0xFF301934), Color(0xFFFF6F00))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE61A162B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ALARM RINGING",
                    color = Color(0xFFFFB74D),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                when (mission) {
                    MissionType.MATH -> MathMissionView(onMissionCompleted)
                    MissionType.SHAKE -> {
                        if (shakeCount >= 30) {
                            LaunchedEffect(Unit) { onMissionCompleted() }
                        }
                        ShakeMissionView(shakeCount = shakeCount, target = 30)
                    }
                    MissionType.MANTRA -> MantraMissionView(onMissionCompleted)
                }
            }
        }
    }
}

@Composable
fun MathMissionView(onComplete: () -> Unit) {
    var solvedCount by remember { mutableIntStateOf(0) }
    var a by remember { mutableIntStateOf(Random.nextInt(12, 49)) }
    var b by remember { mutableIntStateOf(Random.nextInt(11, 39)) }
    var c by remember { mutableIntStateOf(Random.nextInt(5, 19)) }
    var answerInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf(false) }

    val expected = a + b - c

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Problem ${solvedCount + 1} of 3",
            color = Color(0xFFB39DDB),
            fontSize = 15.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "$a + $b - $c = ?",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = answerInput,
            onValueChange = {
                answerInput = it
                errorMsg = false
            },
            label = { Text("Enter Result") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (errorMsg) {
            Text("Incorrect answer, try again!", color = Color(0xFFFF5252), fontSize = 13.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (answerInput.trim().toIntOrNull() == expected) {
                    if (solvedCount + 1 >= 3) {
                        onComplete()
                    } else {
                        solvedCount += 1
                        a = Random.nextInt(15, 59)
                        b = Random.nextInt(12, 45)
                        c = Random.nextInt(6, 25)
                        answerInput = ""
                    }
                } else {
                    errorMsg = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text("Verify Answer", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ShakeMissionView(shakeCount: Int, target: Int) {
    val progress = (shakeCount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Shake your phone vigorously!",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "$shakeCount / $target",
            color = Color(0xFFFFB74D),
            fontSize = 48.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(16.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp),
            color = Color(0xFFFF9800)
        )
    }
}

@Composable
fun MantraMissionView(onComplete: () -> Unit) {
    val targetPhrase = "I awake in Brahma Muhurta with clarity and purpose"
    var typed by remember { mutableStateOf("") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Type the exact phrase below:",
            color = Color(0xFFB39DDB),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = ""$targetPhrase"",
            color = Color(0xFFFFCC80),
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it },
            label = { Text("Type affirmation") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (typed.trim().equals(targetPhrase, ignoreCase = true)) {
                    onComplete()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800))
        ) {
            Text("Complete Mission", color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}
