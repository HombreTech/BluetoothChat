package tech.hombre.bluetoothchat.data.model

import android.bluetooth.BluetoothDevice
import tech.hombre.bluetoothchat.data.entity.Conversation

interface OnConnectionListener {
    fun onConnecting()
    fun onConnected(device: BluetoothDevice)
    fun onConnectedIn(conversation: Conversation)
    fun onConnectedOut(conversation: Conversation)
    fun onConnectionLost()
    fun onConnectionFailed()
    fun onConnectionDestroyed()
    fun onDisconnected()
    fun onConnectionAccepted()
    fun onConnectionRejected()
    fun onConnectionWithdrawn()
}
