package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class MainActivity : ComponentActivity() {

    private lateinit var configManager: GameConfigurationManager
    private lateinit var prefsManager: GamePreferencesManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results quietly
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        configManager = GameConfigurationManager(this)
        prefsManager = GamePreferencesManager(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameScreen(
                        configManager = configManager,
                        prefsManager = prefsManager,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

data class DailyChallenge(val level: Int, val speedMultiplier: Float)

fun getDailyChallenge(): DailyChallenge {
    val calendar = Calendar.getInstance()
    val seed = calendar.get(Calendar.YEAR) * 10000 + (calendar.get(Calendar.MONTH) + 1) * 100 + calendar.get(Calendar.DAY_OF_MONTH)
    val random = Random(seed)
    return DailyChallenge(
        level = random.nextInt(47) + 1,
        speedMultiplier = 1.0f + random.nextFloat()
    )
}

@Composable
fun GameScreen(configManager: GameConfigurationManager, prefsManager: GamePreferencesManager, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var batterySaverMode by remember { mutableStateOf(prefsManager.batterySaverMode) }
    var accelerometerSteering by remember { mutableStateOf(prefsManager.accelerometerSteering) }
    var currentPalette by remember { mutableStateOf(prefsManager.snakeColorPalette) }
    val dailyChallenge = remember { getDailyChallenge() }
    
    // 3D Entry Animation states
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1200)
    )
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.8f,
        animationSpec = tween(durationMillis = 1200)
    )

    if (isPlaying) {
        val context = LocalContext.current
        DisposableEffect(accelerometerSteering) {
            var listener: SensorEventListener? = null
            var sensorManager: SensorManager? = null
            if (accelerometerSteering) {
                sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        // event?.values -> Handle tilt steering
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            }
            onDispose {
                listener?.let { sensorManager?.unregisterListener(it) }
            }
        }
        
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setRenderer(SnakeRenderer(batterySaverMode))
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // HUD
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { isPaused = true }) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (isPaused) {
                PauseOverlay(
                    onResume = { isPaused = false },
                    onRestart = { isPaused = false },
                    onQuit = { 
                        isPaused = false
                        isPlaying = false 
                    }
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp)
                .alpha(alpha)
                .scale(scale)
        ) {
            // App Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text(
                    text = configManager.gameTitleDisplay,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-1).sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "V${configManager.gameVersionName} • ${configManager.developerCreditString}".uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }

            // Daily Challenge
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text("DAILY CHALLENGE", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Level: ${dailyChallenge.level}", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Text("Speed: ${"%.1f".format(dailyChallenge.speedMultiplier)}x", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Main Space
            Spacer(modifier = Modifier.weight(1f))

            // Preferences
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Color Palette
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Snake Color", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val palettes = listOf(Color.Green, Color.Cyan, Color.Magenta)
                        palettes.forEachIndexed { index, color ->
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(color, CircleShape)
                                    .border(if (currentPalette == index) 2.dp else 0.dp, Color.White, CircleShape)
                                    .clickable {
                                        currentPalette = index
                                        prefsManager.snakeColorPalette = index
                                    }
                            )
                        }
                    }
                }
                // Battery Saver
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Battery Saver (30 FPS)", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = batterySaverMode,
                        onCheckedChange = { 
                            batterySaverMode = it
                            prefsManager.batterySaverMode = it
                        }
                    )
                }
                // Accelerometer Steering
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Tilt Steering", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = accelerometerSteering,
                        onCheckedChange = { 
                            accelerometerSteering = it
                            prefsManager.accelerometerSteering = it
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // Bottom Action Bar
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = { isPlaying = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "START GAME",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
                
                Button(
                    onClick = { /* Integrate BluetoothMultiplayerManager */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Text(
                        text = "BLUETOOTH MULTIPLAYER",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun PauseOverlay(onResume: () -> Unit, onRestart: () -> Unit, onQuit: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "PAUSED",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onResume,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("RESUME", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("RESTART LEVEL", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onQuit,
                modifier = Modifier.fillMaxWidth(0.6f),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Text("MAIN MENU", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}
