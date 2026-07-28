package com.example

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.UUID

/**
 * P2P Bluetooth Multiplayer Lobby using BluetoothAdapter and RfcommSocket APIs.
 * Skips account creation and uses local device names for instantaneous connectivity.
 */
@SuppressLint("MissingPermission") // Permissions handled at runtime in MainActivity
class BluetoothMultiplayerManager(context: Context) {
    private val bluetoothManager: BluetoothManager? = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    
    // Standard SPP UUID for simple stream communication
    private val APP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val APP_NAME = "Snake3D_Hex"

    private var serverThread: AcceptThread? = null

    val isBluetoothSupported: Boolean
        get() = bluetoothAdapter != null

    val localDeviceName: String
        get() = bluetoothAdapter?.name ?: "Unknown Device"

    fun startServer() {
        if (serverThread == null) {
            serverThread = AcceptThread()
            serverThread?.start()
        }
    }

    fun stopServer() {
        serverThread?.cancel()
        serverThread = null
    }

    private inner class AcceptThread : Thread() {
        private val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
            bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
        }

        override fun run() {
            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    shouldLoop = false
                    null
                }
                socket?.also {
                    manageMyConnectedSocket(it)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        fun cancel() {
            try {
                mmServerSocket?.close()
            } catch (e: IOException) { }
        }
    }

    private fun manageMyConnectedSocket(socket: BluetoothSocket) {
        // Hand off socket to a dedicated thread for fast, low-latency state syncing
        // between devices. Zero cloud dependency.
    }
}
