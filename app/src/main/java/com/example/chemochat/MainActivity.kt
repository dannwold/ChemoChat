package com.example.chemochat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import java.io.File
import android.net.Uri
import androidx.compose.material.icons.filled.Stop
import java.util.*

class MainActivity : ComponentActivity() {

    private val discoveredDevices = mutableStateListOf<BluetoothDevice>()

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    device?.let {
                        if (!discoveredDevices.contains(it)) {
                            discoveredDevices.add(it)
                        }
                    }
                }
            }
        }
    }

    private var bluetoothService: BluetoothService? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
        setContent {
            ChemoChatApp(bluetoothAdapter)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }

    @Composable
    fun ChemoChatApp(adapter: BluetoothAdapter?) {
        val context = LocalContext.current
        val sharedPrefs = remember { context.getSharedPreferences("ChemoChatPrefs", Context.MODE_PRIVATE) }
        
        var displayName by remember { mutableStateOf(sharedPrefs.getString("displayName", "User") ?: "User") }
        var chatColor by remember { mutableStateOf(sharedPrefs.getInt("chatColor", AndroidColor.parseColor("#10b981"))) }
        var password by remember { mutableStateOf(sharedPrefs.getString("password", "") ?: "") }
        
        var connectionStatus by remember { mutableStateOf(BluetoothService.Status.DISCONNECTED) }
        val messages = remember { mutableStateListOf<Message>() }
        var currentScreen by remember { mutableStateOf("start") } // start, host, join, chat, settings

        // Current password reference to be used in callback
        val currentPassword = rememberUpdatedState(password)

        // Initialize Bluetooth Service
        LaunchedEffect(Unit) {
            bluetoothService = BluetoothService(
                adapter = adapter,
                onConnectionStatusChanged = { status -> connectionStatus = status },
                onMessageReceived = { encryptedWithPrefix ->
                    try {
                        val typeStr = encryptedWithPrefix.take(2)
                        val encrypted = encryptedWithPrefix.drop(2)
                        val decrypted = EncryptionUtils.decrypt(encrypted, currentPassword.value)

                        if (typeStr == "T:") {
                            val text = String(decrypted, Charsets.UTF_8)
                            messages.add(Message(sender = "Other", content = encrypted, type = MessageType.TEXT, decryptedContent = text, isFromMe = false))
                        } else if (typeStr == "A:") {
                            val audioFile = File(context.cacheDir, "received_audio_${System.currentTimeMillis()}.aac")
                            audioFile.writeBytes(decrypted)
                            messages.add(Message(sender = "Other", content = encrypted, type = MessageType.AUDIO, localUri = Uri.fromFile(audioFile), isFromMe = false))
                        }
                    } catch (e: Exception) {
                        messages.add(Message(sender = "System", content = "Error decrypting message", isFromMe = false))
                    }
                }
            )
        }

        // QR Scanner Launcher
        val barcodeLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
            if (result.contents != null) {
                val parts = result.contents.split("|")
                if (parts.size == 2) {
                    val mac = parts[0]
                    val pass = parts[1]
                    password = pass
                    sharedPrefs.edit().putString("password", pass).apply()
                    
                    val device = adapter?.getRemoteDevice(mac)
                    if (device != null) {
                        bluetoothService?.connectToDevice(device)
                        currentScreen = "chat"
                    }
                }
            }
        }

        // Permission Launcher
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (!allGranted) {
                Toast.makeText(context, "Permissions required for Bluetooth and multimedia", Toast.LENGTH_LONG).show()
            }
        }

        fun checkPermissions(): Boolean {
            val requiredPermissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }

            val allGranted = requiredPermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }

            if (!allGranted) {
                permissionLauncher.launch(requiredPermissions.toTypedArray())
                return false
            }
            return true
        }

        LaunchedEffect(Unit) {
            checkPermissions()
        }

        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(chatColor),
                background = Color(0xFF0A0A0A),
                surface = Color(0xFF1A1A1A)
            )
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (currentScreen) {
                    "start" -> StartScreen(
                        onHost = { currentScreen = "host" },
                        onJoin = { currentScreen = "join" },
                        onSettings = { currentScreen = "settings" }
                    )
                    "host" -> HostScreen(
                        adapter = adapter,
                        password = password,
                        checkPermissions = { checkPermissions() },
                        onBack = { currentScreen = "start" },
                        onConnected = { currentScreen = "chat" }
                    )
                    "join" -> JoinScreen(
                        adapter = adapter,
                        initialPassword = password,
                        checkPermissions = { checkPermissions() },
                        onPasswordChange = {
                            password = it
                            sharedPrefs.edit().putString("password", it).apply()
                        },
                        onScanQR = { barcodeLauncher.launch(ScanOptions()) },
                        onConnect = { device ->
                            bluetoothService?.connectToDevice(device)
                            currentScreen = "chat"
                        },
                        onBack = { currentScreen = "start" }
                    )
                    "chat" -> ChatScreen(
                        messages = messages,
                        connectionStatus = connectionStatus,
                        password = password,
                        onSend = { text ->
                            val encrypted = EncryptionUtils.encryptText(text, password)
                            bluetoothService?.write("T:$encrypted")
                            messages.add(Message(sender = "You", content = encrypted, type = MessageType.TEXT, decryptedContent = text, isFromMe = true))
                        },
                        onSendAudio = { audioFile ->
                            val encrypted = EncryptionUtils.encrypt(audioFile.readBytes(), password)
                            bluetoothService?.write("A:$encrypted")
                            messages.add(Message(sender = "You", content = encrypted, type = MessageType.AUDIO, localUri = Uri.fromFile(audioFile), isFromMe = true))
                        },
                        onBack = { 
                            bluetoothService?.stop()
                            currentScreen = "start" 
                        }
                    )
                    "settings" -> SettingsScreen(
                        initialName = displayName,
                        initialColor = chatColor,
                        initialPassword = password,
                        onSave = { name, color, pass ->
                            displayName = name
                            chatColor = color
                            password = pass
                            sharedPrefs.edit().apply {
                                putString("displayName", name)
                                putInt("chatColor", color)
                                putString("password", pass)
                                apply()
                            }
                            currentScreen = "start"
                        },
                        onBack = { currentScreen = "start" }
                    )
                }
            }
        }
    }

    @Composable
    fun StartScreen(onHost: () -> Unit, onJoin: () -> Unit, onSettings: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("ChemoChat", fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Secure P2P Bluetooth Chat", color = Color.Gray)
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(onClick = onHost, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Host a Chat")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Join a Chat")
            }
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(onClick = onSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Settings")
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Composable
    fun HostScreen(
        adapter: BluetoothAdapter?,
        password: String,
        checkPermissions: () -> Boolean,
        onBack: () -> Unit,
        onConnected: () -> Unit
    ) {
        val context = LocalContext.current
        val macAddress = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) adapter?.address ?: "Unknown" else "Scan QR Code"
        val qrContent = "$macAddress|$password"
        val qrBitmap = remember { generateQRCode(qrContent) }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                Text("Hosting Chat", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("Scan this QR to join", color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            
            qrBitmap?.let {
                androidx.compose.foundation.Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(250.dp).background(Color.White).padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            Text("Waiting for connection...", fontWeight = FontWeight.Light)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 16.dp))
            
            Row {
                Button(onClick = {
                    if (checkPermissions()) {
                        val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                            putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                        }
                        context.startActivity(discoverableIntent)
                    }
                }) {
                    Text("Make Discoverable")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { bluetoothService?.startHost() }) {
                    Text("Start Listening")
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @SuppressLint("MissingPermission")
    @Composable
    fun JoinScreen(
        adapter: BluetoothAdapter?,
        initialPassword: String,
        checkPermissions: () -> Boolean,
        onPasswordChange: (String) -> Unit,
        onScanQR: () -> Unit,
        onConnect: (BluetoothDevice) -> Unit,
        onBack: () -> Unit
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Join a Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scan host QR or select from discovered devices.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)

            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = initialPassword,
                onValueChange = onPasswordChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Encryption Passphrase") },
                label = { Text("Passphrase") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onScanQR, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Scan QR Code")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                if (checkPermissions()) {
                    discoveredDevices.clear()
                    adapter?.startDiscovery()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Refresh Devices")
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(discoveredDevices) { device ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        onClick = { onConnect(device) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(device.name ?: "Unknown Device", fontWeight = FontWeight.Bold)
                            Text(device.address, fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = {
                adapter?.cancelDiscovery()
                onBack()
            }) { Text("Cancel") }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ChatScreen(
        messages: List<Message>,
        connectionStatus: BluetoothService.Status,
        password: String,
        onSend: (String) -> Unit,
        onSendAudio: (File) -> Unit,
        onBack: () -> Unit
    ) {
        var inputText by remember { mutableStateOf("") }
        val context = LocalContext.current
        var isRecording by remember { mutableStateOf(false) }
        var recordingFile by remember { mutableStateOf<File?>(null) }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            TopAppBar(
                title = { 
                    Column {
                        Text("ChemoChat", fontSize = 18.sp)
                        Text(connectionStatus.name, fontSize = 12.sp, color = if (connectionStatus == BluetoothService.Status.CONNECTED) Color.Green else Color.Red)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                },
                actions = {
                    IconButton(onClick = { /* Attach Image */ }) { Icon(Icons.Default.Image, contentDescription = null) }
                    IconButton(onClick = {
                        if (isRecording) {
                            AudioHandler.stopRecording()
                            isRecording = false
                            recordingFile?.let { onSendAudio(it) }
                        } else {
                            val file = File(context.cacheDir, "sent_audio_${System.currentTimeMillis()}.aac")
                            recordingFile = file
                            AudioHandler.startRecording(context, file)
                            isRecording = true
                        }
                    }) {
                        Icon(
                            if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Stop Recording" else "Record Audio",
                            tint = if (isRecording) Color.Red else LocalContentColor.current
                        )
                    }
                }
            )

            // Messages
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }

            // Input
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Type a message...") },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSend(inputText)
                            inputText = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null)
                }
            }
        }
    }

    @Composable
    fun ChatBubble(message: Message) {
        val alignment = if (message.isFromMe) Alignment.End else Alignment.Start
        val bgColor = if (message.isFromMe) MaterialTheme.colorScheme.primary else Color(0xFF333333)

        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalAlignment = alignment) {
            Surface(
                color = bgColor,
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (message.isFromMe) 16.dp else 4.dp,
                    bottomEnd = if (message.isFromMe) 4.dp else 16.dp
                )
            ) {
                if (message.type == MessageType.AUDIO) {
                    AudioPlayerBubble(message.localUri)
                } else {
                    Text(
                        text = message.decryptedContent ?: "Encrypted Message",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White
                    )
                }
            }
            Text(
                text = message.sender,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }

    @Composable
    fun AudioPlayerBubble(uri: Uri?) {
        var isPlaying by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                if (isPlaying) {
                    AudioHandler.stopPlayback()
                    isPlaying = false
                } else {
                    uri?.path?.let { path ->
                        AudioHandler.startPlayback(File(path)) {
                            isPlaying = false
                        }
                        isPlaying = true
                    }
                }
            }) {
                Icon(
                    if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White
                )
            }
            Text("Voice Message", color = Color.White, fontSize = 14.sp)
        }
    }

    @Composable
    fun SettingsScreen(initialName: String, initialColor: Int, initialPassword: String, onSave: (String, Int, String) -> Unit, onBack: () -> Unit) {
        var name by remember { mutableStateOf(initialName) }
        var pass by remember { mutableStateOf(initialPassword) }
        
        Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Text("Settings", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            
            Text("Display Name", fontSize = 14.sp, color = Color.Gray)
            TextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth())
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("Encryption Password", fontSize = 14.sp, color = Color.Gray)
            TextField(value = pass, onValueChange = { pass = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("Shared secret") })
            
            Spacer(modifier = Modifier.weight(1f))
            
            Button(onClick = { onSave(name, initialColor, pass) }, modifier = Modifier.fillMaxWidth()) {
                Text("Save Settings")
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("Cancel")
            }
        }
    }

    private fun generateQRCode(content: String): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
                }
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(title: @Composable () -> Unit, navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF1A1A1A)
        )
    )
}
