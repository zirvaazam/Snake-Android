package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var configManager: GameConfigurationManager
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Handle permission results quietly
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        configManager = GameConfigurationManager(this)

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
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun GameScreen(configManager: GameConfigurationManager, modifier: Modifier = Modifier) {
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var showDevOverlay by remember { mutableStateOf(false) }

    if (isPlaying) {
        Box(modifier = modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setRenderer(SnakeRenderer())
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (showDevOverlay) {
                DeveloperHexOverlay(modifier = Modifier.fillMaxSize())
            }

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
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "DEV MODE", 
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Switch(
                        checked = showDevOverlay, 
                        onCheckedChange = { showDevOverlay = it },
                        modifier = Modifier.padding(start = 8.dp)
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

            // Main Space
            Spacer(modifier = Modifier.weight(1f))

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
fun DeveloperHexOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val hexRadius = 50f
        val hexWidth = sqrt(3f) * hexRadius
        val hexHeight = 2f * hexRadius
        
        for (q in -5..5) {
            for (r in -5..5) {
                val x = (size.width / 2) + hexRadius * (1.5f * q)
                val y = (size.height / 2) + hexRadius * (sqrt(3f) / 2f * q + sqrt(3f) * r)
                
                drawHexagon(x, y, hexRadius, color = Color.Green.copy(alpha = 0.5f))
            }
        }
    }
}

fun DrawScope.drawHexagon(centerX: Float, centerY: Float, radius: Float, color: Color) {
    val path = Path()
    for (i in 0..6) {
        val angle = Math.PI / 3.0 * i
        val x = centerX + radius * cos(angle).toFloat()
        val y = centerY + radius * sin(angle).toFloat()
        if (i == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2f)
    )
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
