package com.example.chemochat

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.*

class MainActivity : ComponentActivity() {

    private var bluetoothService: DummyBluetoothService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChemoChatApp()
        }
    }

    @Composable
    fun ChemoChatApp() {
        var displayName by remember { mutableStateOf("User") }
        var chatColor by remember { mutableStateOf(AndroidColor.parseColor("#10b981")) }
        var password by remember { mutableStateOf("1234") }

        var connectionStatus by remember { mutableStateOf(DummyBluetoothService.Status.DISCONNECTED) }
        val messages = remember { mutableStateListOf<Message>() }
        var currentScreen by remember { mutableStateOf("start") }

        // Dummy Bluetooth Service
        LaunchedEffect(Unit) {
            bluetoothService = DummyBluetoothService(
                onConnectionStatusChanged = { status -> connectionStatus = status },
                onMessageReceived = { encryptedMsg ->
                    messages.add(Message(sender = "Other", content = encryptedMsg, decryptedContent = encryptedMsg, isFromMe = false))
                }
            )
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
                        password = password,
                        onBack = { currentScreen = "start" },
                        onConnected = { 
                            currentScreen = "chat"
                            bluetoothService?.connect()
                        }
                    )
                    "join" -> JoinScreen(
                        onScanQR = { 
                            currentScreen = "chat"
                            bluetoothService?.connect()
                        },
                        onBack = { currentScreen = "start" }
                    )
                    "chat" -> ChatScreen(
                        messages = messages,
                        connectionStatus = connectionStatus,
                        onSend = { text ->
                            messages.add(Message(sender = "You", content = text, decryptedContent = text, isFromMe = true))
                            bluetoothService?.sendMessage(text)
                        },
                        onBack = { 
                            bluetoothService?.disconnect()
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

            Button(onClick = onHost, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Host a Chat") }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onJoin, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Join a Chat") }
            Spacer(modifier = Modifier.height(32.dp))
            IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
        }
    }

    @Composable
    fun HostScreen(password: String, onBack: () -> Unit, onConnected: () -> Unit) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) }
                Text("Hosting Chat", fontSize = 20.sp, fontWeight = FontWeight.Medium)
            }
            Spacer(modifier = Modifier.height(40.dp))
            Text("Dummy QR Code: $password", color = Color.Gray)
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onConnected) { Text("Start Listening") }
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
            Button(onClick = onScanQR) { Text("Simulate Scan QR") }
            Spacer(modifier = Modifier.height(48.dp))
            TextButton(onClick = onBack) { Text("Cancel") }
        }
    }

    @Composable
    fun ChatScreen(messages: List<Message>, connectionStatus: DummyBluetoothService.Status, onSend: (String) -> Unit, onBack: () -> Unit) {
        var inputText by remember { mutableStateOf("") }
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Column { Text("ChemoChat", fontSize = 18.sp); Text(connectionStatus.name, fontSize = 12.sp, color = if (connectionStatus == DummyBluetoothService.Status.CONNECTED) Color.Green else Color.Red) } },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = null) } },
                actions = {}
            )

            LazyColumn(modifier = Modifier.weight(1f).padding(16.dp)) { items(messages) { ChatBubble(it) } }

            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                TextField(value = inputText, onValueChange = { inputText = it }, modifier = Modifier.weight(1f), placeholder = { Text("Type a message...") }, shape = RoundedCornerShape(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                FloatingActionButton(onClick = { if (inputText.isNotBlank()) { onSend(inputText); inputText = "" } }, containerColor = MaterialTheme.colorScheme.primary) {
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
            Surface(color = bgColor, shape = RoundedCornerShape(16.dp)) {
                Text(text = message.decryptedContent ?: message.content, modifier = Modifier.padding(16.dp), color = Color.White)
            }
            Text(text = message.sender, fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
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
            TextField(value = pass, onValueChange = { pass = it }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.weight(1f))
            Button(onClick = { onSave(name, initialColor, pass) }, modifier = Modifier.fillMaxWidth()) { Text("Save Settings") }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

// --- Dummy Bluetooth Service for Testing ---
class DummyBluetoothService(
    private val onConnectionStatusChanged: (MainActivity.DummyBluetoothService.Status) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    enum class Status { CONNECTED, DISCONNECTED }
    var status = Status.DISCONNECTED

    fun connect() {
        status = Status.CONNECTED
        onConnectionStatusChanged(status)
        simulateIncomingMessages()
    }

    fun sendMessage(msg: String) {
        simulateIncomingMessages(msg)
    }

    fun disconnect() {
        status = Status.DISCONNECTED
        onConnectionStatusChanged(status)
    }

    private fun simulateIncomingMessages(reply: String? = null) {
        // Simulate a delayed response
        val response = reply?.let { "Echo: $it" } ?: "Hello from host"
        onMessageReceived(response)
    }
}
