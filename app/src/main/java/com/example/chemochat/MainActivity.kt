package com.example.chemochat

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
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
import java.util.*

class MainActivity : ComponentActivity() {

    private var bluetoothService: BluetoothService? = null
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChemoChatApp(bluetoothAdapter)
        }
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

        // Initialize Bluetooth Service
        LaunchedEffect(Unit) {
            bluetoothService = BluetoothService(
                adapter = adapter,
                onConnectionStatusChanged = { status -> connectionStatus = status },
                onMessageReceived = { encryptedMsg ->
                    try {
                        val decrypted = EncryptionUtils.decryptText(encryptedMsg, password)
                        messages.add(Message(sender = "Other", content = encryptedMsg, decryptedContent = decrypted, isFromMe = false))
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

        LaunchedEffect(Unit) {
            val requiredPermissions = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requiredPermissions.add(Manifest.permission.BLUETOOTH_SCAN)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_CONNECT)
                requiredPermissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            }
            permissionLauncher.launch(requiredPermissions.toTypedArray())
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
                        onBack = { currentScreen = "start" },
                        onConnected = { currentScreen = "chat" }
                    )
                    "join" -> JoinScreen(
                        onScanQR = { barcodeLauncher.launch(ScanOptions()) },
                        onBack = { currentScreen = "start" }
                    )
                    "chat" -> ChatScreen(
                        messages = messages,
                        connectionStatus = connectionStatus,
                        password = password,
                        onSend = { text ->
                            val encrypted = EncryptionUtils.encryptText(text, password)
                            bluetoothService?.write(encrypted)
                            messages.add(Message(sender = "You", content = encrypted, decryptedContent = text, isFromMe = true))
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
            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
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
    fun HostScreen(adapter: BluetoothAdapter?, password: String, onBack: () -> Unit, onConnected: () -> Unit) {
        val macAddress = adapter?.address ?: "Unknown"
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
            
            Button(onClick = { bluetoothService?.startHost() }) {
                Text("Start Listening")
            }
        }
    }

    @Composable
    fun JoinScreen(onScanQR: () -> Unit, onBack: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Join a Chat", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Scan the host's QR code to connect securely.", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Color.Gray)
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(onClick = onScanQR, modifier = Modifier.size(120.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = onBack) { Text("Cancel") }
        }
    }

    @Composable
    fun ChatScreen(
        messages: List<Message>,
        connectionStatus: BluetoothService.Status,
        password: String,
        onSend: (String) -> Unit,
        onBack: () -> Unit
    ) {
        var inputText by remember { mutableStateOf("") }

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
                    IconButton(onClick = { /* Record Audio */ }) { Icon(Icons.Default.Mic, contentDescription = null) }
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
        val textColor = if (message.isFromMe) Color.White else Color.White

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
                Text(
                    text = message.decryptedContent ?: "Encrypted Message",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = textColor
                )
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
