package com.kalaiselvan.myremote

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.os.Build
import android.os.Vibrator
import android.os.VibrationEffect
import android.content.Context

@Composable
fun RemoteUI(
    irManager: IrManager, 
    repository: IrCodeRepository, 
    animationsEnabled: Boolean,
    onSettingsClick: () -> Unit
) {
    val hasIrEmitter = remember { irManager.hasIrEmitter() }
    val context = androidx.compose.ui.platform.LocalContext.current

    if (!hasIrEmitter) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            android.widget.Toast.makeText(context, "No IR Emitter found on this device", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    // Dynamic background container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // Theme aware
        contentAlignment = Alignment.Center
    ) {
        // Signal Bubble Animation State
        var signalTrigger by remember { mutableStateOf(0L) }
        
        // Render animate behind (Only if enabled)
        if (animationsEnabled) {
            SignalRippleEffect(trigger = signalTrigger)
        }

        // Settings Button
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground.copy(alpha=0.5f))
        }

        RemoteBody(
            modifier = Modifier, // no scaling
            irManager = irManager, 
            repository = repository, 
            isEnabled = hasIrEmitter,
            onTransmit = { 
                if (animationsEnabled) {
                    signalTrigger = System.currentTimeMillis()
                }
            }
        )
    }
}

@Composable
fun SignalRippleEffect(trigger: Long) {
    // 1. Expansion Animation (Spring physics for "bounce")
    val expansion = remember { Animatable(0f) }
    // 2. Opacity Animation (Fade out)
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(trigger) {
        if (trigger > 0) {
            // Pulse logic:
            // If already expanded (animating), kick it back slightly to create a "beat"
            val currentExp = expansion.value
            val targetReset = if (currentExp > 0f) (currentExp - 0.2f).coerceAtLeast(0f) else 0f
            
            expansion.snapTo(targetReset)
            alpha.snapTo(1f) // Flash bright again
            
            // Parallel Animation: Expand with bounce, then fade
            launch {
                expansion.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            launch {
                // Delay fade slightly so the "pop" is visible
                delay(200)
                alpha.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 800, easing = LinearEasing)
                )
            }
        }
    }

    Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {}) {
        if (alpha.value > 0f) {
            val centerX = size.width / 2
            val originY = size.height // Bottom edge
            val maxRadius = size.height * 1.5f // Increase radius to cover screen from bottom
            
            val currentRadius = maxRadius * expansion.value
            val currentAlpha = alpha.value

            // Guard against crash (RadialGradient requires radius > 0)
            if (currentRadius > 0f) {
                // 1. Outer "Shockwave" Glow (Soft Blue)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00E5FF).copy(alpha = 0.3f * currentAlpha), // Bright Cyan
                            Color.Transparent
                        ),
                        center = Offset(centerX, originY),
                        radius = currentRadius
                    ),
                    center = Offset(centerX, originY),
                    radius = currentRadius
                )

                // 2. Middle "Energy" Body (Electric Blue)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2979FF).copy(alpha = 0.6f * currentAlpha), // Electric Blue
                            Color.Transparent
                        ),
                        center = Offset(centerX, originY),
                        radius = currentRadius * 0.7f
                    ),
                    center = Offset(centerX, originY),
                    radius = currentRadius * 0.7f
                )

                // 3. Inner "Hot" Core (White/Cyan)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.9f * currentAlpha),
                            Color(0xFF00E5FF).copy(alpha = 0.5f * currentAlpha),
                            Color.Transparent
                        ),
                        center = Offset(centerX, originY),
                        radius = currentRadius * 0.3f
                    ),
                    center = Offset(centerX, originY),
                    radius = currentRadius * 0.3f
                )
            }
        }
    }
}

@Composable
fun RemoteBody(
    modifier: Modifier = Modifier,
    irManager: IrManager, 
    repository: IrCodeRepository, 
    isEnabled: Boolean,
    onTransmit: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = modifier
            .width(300.dp), // Slightly wider for fullscreen feel
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Row 1
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = null,
                icon = Icons.Filled.PowerSettingsNew,
                color = Color(0xFFD32F2F), // Red
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "Power")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "Mode",
                color = Color.DarkGray, // Darker buttons for dark mode
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "Mode")
                    onTransmit()
                }
            )
            RemoteButton(
                text = null,
                icon = Icons.Filled.VolumeOff,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "Mute")
                    onTransmit()
                }
            )
        }

        // Row 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = null,
                icon = Icons.Filled.PlayArrow,
                color = Color(0xFF1976D2), // Blue
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "Play/Pause")
                    onTransmit()
                }
            )
            RemoteButton(
                text = null,
                icon = Icons.Filled.SkipPrevious,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                repeating = true, // Enable repeating for Prev
                onClick = { 
                    transmit(context, irManager, repository, "Prev")
                    onTransmit()
                }
            )
            RemoteButton(
                text = null,
                icon = Icons.Filled.SkipNext,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                repeating = true, // Enable repeating for Next
                onClick = { 
                    transmit(context, irManager, repository, "Next")
                    onTransmit()
                }
            )
        }

        // Row 3
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = "EQ",
                color = Color(0xFF9C27B0), // Purple
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "EQ")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "VOL-",
                fontSize = 12.sp,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                repeating = true, // Enable repeating for Vol-
                onClick = { 
                    transmit(context, irManager, repository, "Vol-")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "VOL+",
                fontSize = 12.sp,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                repeating = true, // Enable repeating for Vol+
                onClick = { 
                    transmit(context, irManager, repository, "Vol+")
                    onTransmit()
                }
            )
        }

        // Row 4
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = "0",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "0")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "RPT",
                fontSize = 12.sp,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "RPT")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "U/SD",
                fontSize = 12.sp,
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "U/SD")
                    onTransmit()
                }
            )
        }

        // Row 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = "1",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "1")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "2",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "2")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "3",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "3")
                    onTransmit()
                }
            )
        }

        // Row 6
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = "4",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "4")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "5",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "5")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "6",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "6")
                    onTransmit()
                }
            )
        }

        // Row 7
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RemoteButton(
                text = "7",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "7")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "8",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "8")
                    onTransmit()
                }
            )
            RemoteButton(
                text = "9",
                color = Color.DarkGray,
                contentColor = Color.White,
                enabled = isEnabled,
                onClick = { 
                    transmit(context, irManager, repository, "9")
                    onTransmit()
                }
            )
        }
    }
}

fun transmit(context: android.content.Context, irManager: IrManager, repository: IrCodeRepository, key: String) {
    val code = repository.getCode(key)
    if (code.isBlank()) {
        android.widget.Toast.makeText(context, "$key: Not Configured", android.widget.Toast.LENGTH_SHORT).show()
        return
    }
    irManager.transmit(code)
}

@Composable
fun RemoteButton(
    text: String? = null,
    icon: ImageVector? = null,
    color: Color = Color.DarkGray,
    contentColor: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    enabled: Boolean = true,
    repeating: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val vibrator = remember { context.getSystemService(Vibrator::class.java) }
    
    val backgroundColor = if (!enabled) color.copy(alpha = 0.3f) else if (isPressed) color.copy(alpha = 0.8f) else color
    val contentColorFinal = if (!enabled) contentColor.copy(alpha = 0.3f) else contentColor

    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope() // Scope for event emission

    // Trigger haptic
    val performHaptic = {
        if (enabled) {
             if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(40, 255)) 
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(40)
            }
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed && enabled && repeating) {
            // Initial Click (on Down)
            performHaptic()
            currentOnClick()
            
            // Wait before repeating
            delay(500)
            
            // Repeat loop
            while (isPressed) {
                currentOnClick() 
                delay(200)
            }
        }
    }

    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            // If repeating, we handle clicks in LaunchedEffect(isPressed)
            // If NOT repeating, we use standard clickable
            .then(
                if (repeating) {
                    Modifier.pointerInput(interactionSource) {
                        awaitPointerEventScope {
                            while (true) {
                                val press = awaitFirstDown(requireUnconsumed = false)
                                // We manualy emit press interaction to drive the ripple and isPressed state
                                val pressInteraction = androidx.compose.foundation.interaction.PressInteraction.Press(press.position)
                                scope.launch { interactionSource.emit(pressInteraction) }
                                
                                val upOrCancel = waitForUpOrCancellation()
                                
                                val releaseInteraction = if (upOrCancel == null) {
                                    androidx.compose.foundation.interaction.PressInteraction.Cancel(pressInteraction)
                                } else {
                                    androidx.compose.foundation.interaction.PressInteraction.Release(pressInteraction)
                                }
                                scope.launch { interactionSource.emit(releaseInteraction) }
                            }
                        }
                    }
                } else {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = rememberRipple(),
                        enabled = enabled,
                        onClick = {
                            performHaptic()
                            onClick()
                        }
                    )
                }
            ),
        shape = CircleShape,
        color = backgroundColor,
        shadowElevation = 0.dp
    ) {

        Box(
            contentAlignment = Alignment.Center,
             modifier = if (repeating) Modifier.indication(interactionSource, rememberRipple()) else Modifier
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColorFinal,
                    modifier = Modifier.size(28.dp)
                )
            } else if (text != null) {
                Text(
                    text = text,
                    color = contentColorFinal,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
