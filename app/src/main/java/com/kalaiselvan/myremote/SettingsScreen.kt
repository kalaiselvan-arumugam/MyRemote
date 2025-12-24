package com.kalaiselvan.myremote

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// Helper component for sections
@Composable
fun SettingsSection(
    title: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!expanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    Divider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 16.dp))
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    repository: IrCodeRepository,
    prefs: AppPreferences,
    irManager: IrManager,
    onBack: () -> Unit
) {
    val darkTheme by prefs.darkTheme.collectAsState()
    val animationsEnabled by prefs.animationsEnabled.collectAsState()
    val physicalButtonsEnabled by prefs.physicalButtonsEnabled.collectAsState()
    var codes by remember { mutableStateOf(repository.getAllCodes()) }

    // Section States
    var appearanceExpanded by remember { mutableStateOf(true) }
    var controlsExpanded by remember { mutableStateOf(true) }
    var inputExpanded by remember { mutableStateOf(false) }
    var troubleshootExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Appearance Section
            item {
                SettingsSection(
                    title = "Appearance",
                    expanded = appearanceExpanded,
                    onExpandChange = { appearanceExpanded = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Dark Theme", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = darkTheme,
                            onCheckedChange = { prefs.setDarkTheme(it) }
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Animations", color = MaterialTheme.colorScheme.onSurface)
                        Switch(
                            checked = animationsEnabled,
                            onCheckedChange = { prefs.setAnimationsEnabled(it) }
                        )
                    }
                }
            }

            // 2. Controls Section
            item {
                SettingsSection(
                    title = "Controls",
                    expanded = controlsExpanded,
                    onExpandChange = { controlsExpanded = it }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Physical Buttons", color = MaterialTheme.colorScheme.onSurface)
                            Text(
                                "Use Volume keys to control app", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = physicalButtonsEnabled,
                            onCheckedChange = { prefs.setPhysicalButtonsEnabled(it) }
                        )
                    }
                }
            }

            // 3. Button Configuration
            item {
                SettingsSection(
                    title = "Button Configuration",
                    expanded = inputExpanded,
                    onExpandChange = { inputExpanded = it }
                ) {
                    codes.entries.toList().sortedBy { it.key }.forEach { (key, value) ->
                        var currentValue by remember(value) { mutableStateOf(value) }
                        
                        OutlinedTextField(
                            value = currentValue,
                            onValueChange = { 
                                currentValue = it
                                repository.setCode(key, it)
                            },
                            label = { Text(key) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            repository.resetToDefaults()
                            codes = repository.getAllCodes()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Reset to Defaults")
                    }
                }
            }

            // 3. Troubleshooter (Integrated)
            item {
                SettingsSection(
                    title = "Troubleshooter",
                    expanded = troubleshootExpanded,
                    onExpandChange = { troubleshootExpanded = it }
                ) {
                    var tabIndex by remember { mutableStateOf(0) }
                    TabRow(selectedTabIndex = tabIndex) {
                        Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("Freq Scan") })
                        Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("Code Scan") })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (tabIndex == 0) {
                        FreqScanner(irManager, repository)
                    } else {
                        CodeScanner(irManager)
                    }
                }
            }

            // 4. About Section
            item {
                var clickCount by remember { mutableStateOf(0) }
                var showDialog by remember { mutableStateOf(false) }
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Version 1.0.0",
                        color = Color.Gray,
                        modifier = Modifier.clickable {
                            clickCount++
                            if (clickCount >= 5) {
                                showDialog = true
                                clickCount = 0
                            }
                        }
                    )
                }

                if (showDialog) {
                    AlertDialog(
                        onDismissRequest = { showDialog = false },
                        title = { Text("About Developer") },
                        text = {
                            Column {
                                Text("Author : Kalaiselvan Arumugam")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "GitHub : https://github.com/kalaiselvan-arumugam/",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        uriHandler.openUri("https://github.com/kalaiselvan-arumugam/")
                                    }
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }
        }
    }
}

// --- Troubleshooter Components (Moved & Adapted) ---

@Composable
fun FreqScanner(irManager: IrManager, repository: IrCodeRepository) {
    val frequencies = listOf(36000, 37000, 38000, 38400, 39000, 40000)
    var isScanning by remember { mutableStateOf(false) }
    var currentIndex by remember { mutableStateOf(0) }
    var useLsb by remember { mutableStateOf(true) }
    
    val currentFreq = frequencies[currentIndex]
    val orderText = if (useLsb) "LSB" else "MSB"

    LaunchedEffect(isScanning) {
        if (isScanning) {
            while(true) {
                val powerCode = repository.getCode("Power")
                irManager.transmit(powerCode, currentFreq, useLsb)
                delay(1500)
                if (currentIndex < frequencies.size - 1) {
                    currentIndex++
                } else {
                    if (useLsb) {
                        useLsb = false
                        currentIndex = 0
                    } else {
                        isScanning = false
                        useLsb = true
                        currentIndex = 0
                    }
                }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${currentFreq/1000}.${(currentFreq%1000)/100} kHz - $orderText", fontSize = 24.sp, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { isScanning = !isScanning },
            colors = ButtonDefaults.buttonColors(containerColor = if(isScanning) Color.Red else MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "STOP SCAN" else "START FREQ SCAN")
        }
    }
}

@Composable
fun CodeScanner(irManager: IrManager) {
    var isScanning by remember { mutableStateOf(false) }
    var addressHex by remember { mutableStateOf("00") }
    var currentCommand by remember { mutableStateOf(0) }
    
    val currentHexCode = derivedHex(addressHex, currentCommand)

    LaunchedEffect(isScanning) {
        if (isScanning) {
            while(true) {
                irManager.transmit(currentHexCode, 38400, true)
                delay(400)
                if (currentCommand < 255) currentCommand++ else { isScanning = false; currentCommand = 0 }
            }
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = addressHex,
            onValueChange = { if(it.length <= 2) addressHex = it },
            label = { Text("Address (Hex)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("CMD: ${currentCommand.toString(16).uppercase().padStart(2, '0')}", fontSize = 20.sp, color = MaterialTheme.colorScheme.secondary)
        Text("Code: $currentHexCode", fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(16.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { if(currentCommand>0) currentCommand-- }) { Text("<") }
            Button(onClick = { irManager.transmit(currentHexCode, 38400, true) }) { Text("TEST") }
            Button(onClick = { if(currentCommand<255) currentCommand++ }) { Text(">") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { isScanning = !isScanning },
            colors = ButtonDefaults.buttonColors(containerColor = if(isScanning) Color.Red else MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isScanning) "STOP SCAN" else "START CODE SCAN")
        }
    }
}

fun derivedHex(addr: String, cmd: Int): String {
    val addrInt = addr.toIntOrNull(16) ?: 0
    val addrInv = addrInt.inv() and 0xFF
    val cmdInv = cmd.inv() and 0xFF
    val full = (addrInt.toLong() shl 24) or (addrInv.toLong() shl 16) or (cmd.toLong() shl 8) or cmdInv.toLong()
    return "0x${full.toString(16).uppercase().padStart(8, '0')}"
}
