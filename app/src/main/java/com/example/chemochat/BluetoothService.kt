package com.example.chemochat

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Service to manage Bluetooth Classic connections (RFCOMM).
 */
class BluetoothService(
    private val adapter: BluetoothAdapter?,
    private val onConnectionStatusChanged: (Status) -> Unit,
    private val onMessageReceived: (String) -> Unit
) {
    private var connectThread: ConnectThread? = null
    private var acceptThread: AcceptThread? = null
    private var connectedThread: ConnectedThread? = null

    enum class Status { DISCONNECTED, CONNECTING, CONNECTED }

    companion object {
        private const val TAG = "BluetoothService"
        private const val NAME = "ChemoChat"
        private val MY_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    }

    @SuppressLint("MissingPermission")
    fun startHost() {
        stop()
        acceptThread = AcceptThread().apply { start() }
        onConnectionStatusChanged(Status.CONNECTING)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice) {
        stop()
        connectThread = ConnectThread(device).apply { start() }
        onConnectionStatusChanged(Status.CONNECTING)
    }

    fun write(data: String) {
        connectedThread?.write(data.toByteArray())
    }

    fun stop() {
        connectThread?.cancel()
        connectThread = null
        acceptThread?.cancel()
        acceptThread = null
        connectedThread?.cancel()
        connectedThread = null
        onConnectionStatusChanged(Status.DISCONNECTED)
    }

    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            adapter?.listenUsingInsecureRfcommWithServiceRecord(NAME, MY_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }

    private inner class ConnectThread(device: BluetoothDevice) : Thread() {
        private val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
            device.createInsecureRfcommSocketToServiceRecord(MY_UUID)
        }

        @SuppressLint("MissingPermission")
        override fun run() {
            adapter?.cancelDiscovery()
            try {
                mmSocket?.let { socket ->
                    socket.connect()
                    manageConnectedSocket(socket)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Could not connect to client socket", e)
                onConnectionStatusChanged(Status.DISCONNECTED)
                cancel()
            }
        }

        fun cancel() {
            try {
                mmSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the client socket", e)
            }
        }
    }

    private fun manageConnectedSocket(socket: BluetoothSocket) {
        connectedThread = ConnectedThread(socket).apply { start() }
        onConnectionStatusChanged(Status.CONNECTED)
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024 * 1024) // 1MB buffer

        override fun run() {
            while (true) {
                try {
                    val bytes = mmInStream.read(mmBuffer)
                    val incomingMessage = String(mmBuffer, 0, bytes)
                    onMessageReceived(incomingMessage)
                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    onConnectionStatusChanged(Status.DISCONNECTED)
                    break
                }
            }
        }

        fun write(bytes: ByteArray) {
            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)
            }
        }

        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}
