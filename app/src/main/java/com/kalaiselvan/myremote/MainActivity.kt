package com.kalaiselvan.myremote

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var irManager: IrManager
    private lateinit var repository: IrCodeRepository
    private lateinit var prefs: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        irManager = IrManager(this)
        repository = IrCodeRepository(this)
        prefs = AppPreferences(this)

        if (!irManager.hasIrEmitter()) {
            Toast.makeText(this, "No IR Emitter found on this device!", Toast.LENGTH_LONG).show()
        }

        setContent {
            val darkTheme by prefs.darkTheme.collectAsState()
            val animationsEnabled by prefs.animationsEnabled.collectAsState()

            // Dynamic Theme selection
            val colorScheme = if (darkTheme) {
                androidx.compose.material3.darkColorScheme(
                    background = androidx.compose.ui.graphics.Color.Black,
                    surface = androidx.compose.ui.graphics.Color.Black,
                    onBackground = androidx.compose.ui.graphics.Color.White,
                    onSurface = androidx.compose.ui.graphics.Color.White
                )
            } else {
                androidx.compose.material3.lightColorScheme(
                    background = androidx.compose.ui.graphics.Color.White,
                    surface = androidx.compose.ui.graphics.Color.White,
                    onBackground = androidx.compose.ui.graphics.Color.Black,
                    onSurface = androidx.compose.ui.graphics.Color.Black
                )
            }

            MaterialTheme(colorScheme = colorScheme) {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "remote") {
                    composable("remote") {
                         RemoteUI(
                             irManager = irManager,
                             repository = repository,
                             animationsEnabled = animationsEnabled, // Pass toggle
                             onSettingsClick = { navController.navigate("settings") }
                         )
                    }
                    composable("settings") {
                        SettingsScreen(
                            repository = repository,
                            prefs = prefs,
                            irManager = irManager,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }

    // State for rate limiting and double click detection
    private var lastTransmitTime = 0L
    private var lastVolUpTime = 0L
    private var lastVolDownTime = 0L
    private var pendingVolumeJob: kotlinx.coroutines.Job? = null
    private val DOUBLE_CLICK_DELAY = 300L
    private val REPEAT_DELAY = 200L

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent?): Boolean {
        // Check preference first
        // We use value directly from state flow if collected, or getting it from prefs object if exposed.
        // Since we are in Activity, we can access the stateflow value synchronously.
        if (!prefs.physicalButtonsEnabled.value) {
            return super.onKeyDown(keyCode, event)
        }

        val currentTime = System.currentTimeMillis()
        
        return when (keyCode) {
            android.view.KeyEvent.KEYCODE_VOLUME_UP -> {
                if (event?.repeatCount == 0) {
                    // Initial Press
                    if (currentTime - lastVolUpTime < DOUBLE_CLICK_DELAY) {
                        // Double Click Detected -> Cancel pending volume, Transmit Next
                        pendingVolumeJob?.cancel()
                        pendingVolumeJob = null
                        
                        val code = repository.getCode("Next")
                        if (code.isNotBlank()) irManager.transmit(code)
                    } else {
                        // Single Click -> Wait 300ms before sending Vol+
                        pendingVolumeJob?.cancel() // Cancel any existing
                        pendingVolumeJob = lifecycleScope.launch {
                            delay(DOUBLE_CLICK_DELAY)
                            val code = repository.getCode("Vol+")
                            if (code.isNotBlank()) irManager.transmit(code)
                        }
                    }
                    lastVolUpTime = currentTime
                    lastTransmitTime = currentTime
                } else {
                    // Holding (Repeat) -> Rate Limit
                    // If holding, we cancel the pending job immediately and start repeating
                    pendingVolumeJob?.cancel()
                    pendingVolumeJob = null
                    
                    if (currentTime - lastTransmitTime > REPEAT_DELAY) {
                        val code = repository.getCode("Vol+")
                        if (code.isNotBlank()) irManager.transmit(code)
                        lastTransmitTime = currentTime
                    }
                }
                true // Consume event
            }
            android.view.KeyEvent.KEYCODE_VOLUME_DOWN -> {
                 if (event?.repeatCount == 0) {
                    // Initial Press
                    if (currentTime - lastVolDownTime < DOUBLE_CLICK_DELAY) {
                        // Double Click Detected -> Cancel pending volume, Transmit Prev
                        pendingVolumeJob?.cancel()
                        pendingVolumeJob = null

                        val code = repository.getCode("Prev")
                        if (code.isNotBlank()) irManager.transmit(code)
                    } else {
                        // Single Click -> Wait 300ms before sending Vol-
                        pendingVolumeJob?.cancel()
                        pendingVolumeJob = lifecycleScope.launch {
                            delay(DOUBLE_CLICK_DELAY)
                            val code = repository.getCode("Vol-")
                            if (code.isNotBlank()) irManager.transmit(code)
                        }
                    }
                    lastVolDownTime = currentTime
                    lastTransmitTime = currentTime
                } else {
                    // Holding (Repeat) -> Rate Limit
                    pendingVolumeJob?.cancel()
                    pendingVolumeJob = null

                    if (currentTime - lastTransmitTime > REPEAT_DELAY) {
                        val code = repository.getCode("Vol-")
                        if (code.isNotBlank()) irManager.transmit(code)
                        lastTransmitTime = currentTime
                    }
                }
                true // Consume event
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }
}
