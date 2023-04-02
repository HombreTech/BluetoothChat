package tech.hombre.bluetoothchatter.data.model

import android.bluetooth.BluetoothDevice
import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.data.service.message.TransferringFile
import tech.hombre.bluetoothchatter.data.service.message.Contract
import java.io.File

interface BluetoothConnector {

    fun prepare()
    fun release()
    fun restartIfNeeded()
    fun stop()
    fun disconnect()
    fun addOnConnectListener(listener: OnConnectionListener)
    fun addOnPrepareListener(listener: OnPrepareListener)
    fun addOnMessageListener(listener: OnMessageListener)
    fun addOnFileListener(listener: OnFileListener)
    fun removeOnConnectListener(listener: OnConnectionListener)
    fun removeOnPrepareListener(listener: OnPrepareListener)
    fun removeOnMessageListener(listener: OnMessageListener)
    fun removeOnFileListener(listener: OnFileListener)
    fun connect(device: BluetoothDevice)
    fun sendMessage(messageText: String, replyMessageUid: Long?)
    fun sendFile(file: File, type: PayloadType, replyMessageUid: Long?)
    fun cancelFileTransfer()
    fun isConnected(): Boolean
    fun isConnectedOrPending(): Boolean
    fun isPending(): Boolean
    fun isConnectionPrepared(): Boolean
    fun getCurrentConversation(): Conversation?
    fun getTransferringFile(): TransferringFile?
    fun acceptConnection()
    fun rejectConnection()
    fun sendDisconnectRequest()
    fun isFeatureAvailable(feature: Contract.Feature): Boolean
    fun setMessageAsSeen(id: Long)
}
