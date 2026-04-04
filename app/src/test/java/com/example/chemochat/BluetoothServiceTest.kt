package com.example.chemochat

import android.bluetooth.BluetoothAdapter
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

class BluetoothServiceTest {

    @Test
    fun testStartHostInvokesCallbacksAndStartsThread() {
        // Arrange
        val adapter = mockk<BluetoothAdapter>(relaxed = true)
        val onConnectionStatusChanged = mockk<(BluetoothService.Status) -> Unit>(relaxed = true)
        val onMessageReceived = mockk<(String) -> Unit>(relaxed = true)

        val service = spyk(BluetoothService(adapter, onConnectionStatusChanged, onMessageReceived))

        // Mock startAcceptThread to do nothing to avoid starting a real thread
        every { service.startAcceptThread() } returns Unit

        // Act
        service.startHost()

        // Assert
        verify { service.stop() }
        verify { service.startAcceptThread() }

        // Verify status changes: DISCONNECTED (from stop()) and then CONNECTING (from startHost())
        verify { onConnectionStatusChanged(BluetoothService.Status.DISCONNECTED) }
        verify { onConnectionStatusChanged(BluetoothService.Status.CONNECTING) }
    }
}
