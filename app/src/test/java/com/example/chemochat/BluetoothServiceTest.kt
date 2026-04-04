package com.example.chemochat

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BluetoothServiceTest {

    private lateinit var bluetoothService: BluetoothService

    @Mock
    private lateinit var mockAdapter: BluetoothAdapter

    @Mock
    private lateinit var mockDevice: BluetoothDevice

    private lateinit var onConnectionStatusChanged: (BluetoothService.Status) -> Unit
    private lateinit var onMessageReceived: (String) -> Unit

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        onConnectionStatusChanged = mock()
        onMessageReceived = mock()

        bluetoothService = BluetoothService(
            adapter = mockAdapter,
            onConnectionStatusChanged = onConnectionStatusChanged,
            onMessageReceived = onMessageReceived
        )
    }

    @Test
    fun testStartHostUpdatesStatus() {
        bluetoothService.startHost()
        verify(onConnectionStatusChanged).invoke(BluetoothService.Status.CONNECTING)
    }

    @Test
    fun testConnectToDeviceUpdatesStatus() {
        bluetoothService.connectToDevice(mockDevice)
        verify(onConnectionStatusChanged).invoke(BluetoothService.Status.CONNECTING)
    }

    @Test
    fun testStopUpdatesStatus() {
        bluetoothService.stop()
        verify(onConnectionStatusChanged).invoke(BluetoothService.Status.DISCONNECTED)
    }

    @Test
    fun testStartHostWithNullAdapterDoesNotCrash() {
        val serviceWithNullAdapter = BluetoothService(
            adapter = null,
            onConnectionStatusChanged = onConnectionStatusChanged,
            onMessageReceived = onMessageReceived
        )
        serviceWithNullAdapter.startHost()
        verify(onConnectionStatusChanged).invoke(BluetoothService.Status.CONNECTING)
    }
}
