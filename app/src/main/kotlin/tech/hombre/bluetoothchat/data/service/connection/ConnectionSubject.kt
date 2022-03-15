package tech.hombre.bluetoothchat.data.service.connection

import android.bluetooth.BluetoothDevice
import tech.hombre.bluetoothchat.data.entity.ChatMessage
import tech.hombre.bluetoothchat.data.entity.Conversation

interface ConnectionSubject {

    fun handleConnectedOut(conversation: Conversation)
    fun handleConnectedIn(conversation: Conversation)
    fun handleConnectionAccepted()
    fun handleConnected(device: BluetoothDevice)
    fun handleConnectingInProgress()
    fun handleDisconnected()
    fun handleConnectionRejected()
    fun handleConnectionFailed()
    fun handleConnectionLost()
    fun handleConnectionWithdrawn()

    fun handleFileSendingStarted(fileAddress: String?, fileSize: Long)
    fun handleFileSendingProgress(sentBytes: Long, totalBytes: Long)
    fun handleFileSendingFinished()
    fun handleFileSendingFailed()
    fun handleFileReceivingStarted(fileSize: Long)
    fun handleFileReceivingProgress(sentBytes: Long, totalBytes: Long)
    fun handleFileReceivingFinished()
    fun handleFileReceivingFailed()
    fun handleFileTransferCanceled(byPartner: Boolean)

    fun handleMessageReceived(message: ChatMessage)
    fun handleMessageSent(message: ChatMessage)
    fun handleMessageSendingFailed()
    fun handleMessageSeen(uid: Long)
    fun handleMessageDelivered(uid: Long)
    fun handleMessageNotDelivered(uid: Long)
    fun isAnybodyListeningForMessages(): Boolean

    fun isRunning(): Boolean
}
