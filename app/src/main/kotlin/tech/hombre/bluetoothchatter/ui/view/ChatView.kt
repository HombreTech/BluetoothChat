package tech.hombre.bluetoothchatter.ui.view

import androidx.annotation.ColorInt
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.ui.viewmodel.ChatMessageViewModel

interface ChatView {

    fun showMessagesHistory(messages: List<ChatMessageViewModel>)
    fun showReceivedMessage(message: ChatMessageViewModel)
    fun showSentMessage(message: ChatMessageViewModel)
    fun showSendingMessageFailure()
    fun showServiceDestroyed()
    fun showRejectedConnection()
    fun showConnectionRequest(displayName: String, deviceName: String)
    fun showFailedConnection()
    fun showLostConnection()
    fun showDisconnected()
    fun hideLostConnection()
    fun hideDisconnected()
    fun showNotConnectedToAnyDevice()
    fun showNotConnectedToThisDevice(currentDevice: String)
    fun showNotValidMessage()
    fun showNotConnectedToSend()
    fun showReceiverUnableToReceiveFiles()
    fun showDeviceIsNotAvailable()
    fun showWainingForOpponent()
    fun hideActions()
    fun afterMessageSent()
    fun showStatusConnected()
    fun showStatusNotConnected()
    fun showStatusPending()
    fun showPartnerName(name: String, device: String)
    fun showBluetoothDisabled()
    fun showBluetoothEnablingFailed()
    fun requestBluetoothEnabling()
    fun dismissMessageNotification()
    fun setBackgroundColor(@ColorInt color: Int)

    fun openImagePicker()
    fun openFilePicker()
    fun showPresharingImage(path: String)
    fun showPresharingFile()
    fun showFileTooBig(maxSize: Long)
    fun showImageNotExist()
    fun showFileTransferLayout(
        fileAddress: String?,
        fileSize: Long,
        transferType: FileTransferType,
        type: PayloadType
    )
    fun updateFileTransferProgress(transferredBytes: Long, totalBytes: Long)
    fun hideFileTransferLayout()
    fun showFileTransferCanceled()
    fun showFileTransferFailure()

    enum class FileTransferType {
        SENDING,
        RECEIVING
    }
}
