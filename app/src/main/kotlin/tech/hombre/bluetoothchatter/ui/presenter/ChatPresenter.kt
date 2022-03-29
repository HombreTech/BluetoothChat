package tech.hombre.bluetoothchatter.ui.presenter

import android.bluetooth.BluetoothDevice
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.*
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.model.*
import tech.hombre.bluetoothchatter.data.service.message.Contract
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.data.service.message.TransferringFile
import tech.hombre.bluetoothchatter.ui.view.ChatView
import tech.hombre.bluetoothchatter.ui.viewmodel.ChatMessageViewModel
import tech.hombre.bluetoothchatter.ui.viewmodel.converter.ChatMessageConverter
import java.io.File
import java.util.*

class ChatPresenter(
    private val deviceAddress: String,
    private val view: ChatView,
    private val conversationsStorage: ConversationsStorage,
    private val messagesStorage: MessagesStorage,
    private val scanModel: BluetoothScanner,
    private val connectionModel: BluetoothConnector,
    private val preferences: UserPreferences,
    private val converter: ChatMessageConverter,
    private val uiContext: CoroutineDispatcher = Dispatchers.Main,
    private val bgContext: CoroutineDispatcher = Dispatchers.IO
) : BasePresenter(uiContext) {

    private val maxFileSize = 10_485_760

    private var fileToSend: File? = null
    private var filePresharing: File? = null
    private var type = PayloadType.TEXT

    private val prepareListener = object : OnPrepareListener {

        override fun onPrepared() {

            with(connectionModel) {
                addOnConnectListener(connectionListener)
                addOnMessageListener(messageListener)
                addOnFileListener(fileListener)
            }
            updateState()
            dismissNotification()

            if (!connectionModel.isConnected()) {
                fileToSend?.let {
                    filePresharing = fileToSend
                    if (type == PayloadType.IMAGE) {
                        view.showPresharingImage(it.absolutePath)
                    } else view.showPresharingFile()
                }
            } else {

                if (filePresharing != null) {
                    return
                }

                fileToSend?.let { file ->

                    if (file.length() > maxFileSize) {
                        view.showFileTooBig(maxFileSize.toLong())
                    } else {
                        connectionModel.sendFile(file, type)
                    }
                    fileToSend = null
                    filePresharing = null
                }
            }
        }

        override fun onError() {
            releaseConnection()
        }
    }

    private val connectionListener = object : OnConnectionListener {

        override fun onConnected(device: BluetoothDevice) {

        }

        override fun onConnectionWithdrawn() {
            updateState()
        }

        override fun onConnectionDestroyed() {
            view.showServiceDestroyed()
        }

        override fun onConnectionAccepted() {

            view.showStatusConnected()
            view.hideActions()

            launch {
                val conversation = withContext(bgContext) {
                    conversationsStorage.getConversationByAddress(deviceAddress)
                }
                if (conversation != null) {
                    view.showPartnerName(conversation.displayName, conversation.deviceName)
                }
            }
        }

        override fun onConnectionRejected() {
            view.showRejectedConnection()
            updateState()
        }

        override fun onConnectedIn(conversation: Conversation) {
            val currentConversation: Conversation? = connectionModel.getCurrentConversation()
            if (currentConversation?.deviceAddress == deviceAddress) {
                view.showStatusPending()
                view.hideDisconnected()
                view.hideLostConnection()
                view.showConnectionRequest(conversation.displayName, conversation.deviceName)
                view.showPartnerName(conversation.displayName, conversation.deviceName)
            }
        }

        override fun onConnectedOut(conversation: Conversation) {
        }

        override fun onConnecting() {
        }

        override fun onConnectionLost() {
            view.showLostConnection()
            updateState()
        }

        override fun onConnectionFailed() {
            view.showFailedConnection()
            updateState()
        }

        override fun onDisconnected() {
            view.showDisconnected()
            updateState()
        }
    }

    private val messageListener = object : OnMessageListener {

        override fun onMessageReceived(message: ChatMessage) {
            view.showReceivedMessage(converter.transform(message))
        }

        override fun onMessageSent(message: ChatMessage) {
            view.showSentMessage(converter.transform(message))
        }

        override fun onMessageSendingFailed() {
            view.showSendingMessageFailure()
        }

        override fun onMessageDelivered(id: Long) {
        }

        override fun onMessageNotDelivered(id: Long) {
        }

        override fun onMessageSeen(id: Long) {
        }
    }

    private val fileListener = object : OnFileListener {

        override fun onFileSendingStarted(fileAddress: String?, fileSize: Long, type: PayloadType) {
            view.showFileTransferLayout(
                fileAddress,
                fileSize,
                ChatView.FileTransferType.SENDING,
                type
            )
        }

        override fun onFileSendingProgress(sentBytes: Long, totalBytes: Long) {
            view.updateFileTransferProgress(sentBytes, totalBytes)
        }

        override fun onFileSendingFinished() {
            view.hideFileTransferLayout()
        }

        override fun onFileSendingFailed() {
            view.hideFileTransferLayout()
            view.showFileTransferFailure()
        }

        override fun onFileReceivingStarted(fileSize: Long, type: PayloadType) {
            view.showFileTransferLayout(
                null,
                fileSize,
                ChatView.FileTransferType.RECEIVING,
                type
            )
        }

        override fun onFileReceivingProgress(sentBytes: Long, totalBytes: Long) {
            view.updateFileTransferProgress(sentBytes, totalBytes)
        }

        override fun onFileReceivingFinished() {
            view.hideFileTransferLayout()
        }

        override fun onFileReceivingFailed() {
            view.hideFileTransferLayout()
            view.showFileTransferFailure()
        }

        override fun onFileTransferCanceled(byPartner: Boolean) {
            view.hideFileTransferLayout()
            if (byPartner) {
                view.showFileTransferCanceled()
            }
        }
    }

    private fun dismissNotification() {

        connectionModel.getCurrentConversation()?.let {
            if (connectionModel.isConnectedOrPending() && it.deviceAddress == deviceAddress) {
                view.dismissMessageNotification()
            }
        }
    }

    fun onViewCreated(context: Context) = launch {
        val color = withContext(bgContext) { preferences.getChatBackgroundColor(context) }
        view.setBackgroundColor(color)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun prepareConnection() {

        if (!scanModel.isBluetoothEnabled()) {
            view.showBluetoothDisabled()
        } else {

            connectionModel.addOnPrepareListener(prepareListener)

            if (connectionModel.isConnectionPrepared()) {
                with(connectionModel) {
                    addOnConnectListener(connectionListener)
                    addOnMessageListener(messageListener)
                    addOnFileListener(fileListener)
                }
                updateState()
                dismissNotification()
                sendFileIfPrepared()
            } else {
                connectionModel.prepare()
            }
        }

        launch {
            val messagesDef =
                async(bgContext) { messagesStorage.getMessagesByDevice(deviceAddress) }
            val conversationDef =
                async(bgContext) { conversationsStorage.getConversationByAddress(deviceAddress) }
            displayInfo(messagesDef.await(), conversationDef.await())
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun releaseConnection() {
        with(connectionModel) {
            removeOnPrepareListener(prepareListener)
            removeOnConnectListener(connectionListener)
            removeOnMessageListener(messageListener)
            removeOnFileListener(fileListener)
        }
    }

    private fun sendFileIfPrepared() = fileToSend?.let { file ->

        if (connectionModel.isConnected()) {
            if (file.length() > maxFileSize) {
                view.showFileTooBig(maxFileSize.toLong())
            } else {
                connectionModel.sendFile(file, type)
            }
            fileToSend = null
            filePresharing = null
        } else {
            filePresharing = fileToSend
            if (type == PayloadType.IMAGE) {
                view.showPresharingImage(file.absolutePath)
            } else {
                view.showPresharingFile()
            }
        }
    }

    private fun displayInfo(messages: List<ChatMessage>, partner: Conversation?) {

        messages.forEach { it.seenHere = true }
        view.showMessagesHistory(converter.transform(messages))
        if (partner != null) {
            view.showPartnerName(partner.displayName, partner.deviceName)
        }

        launch(bgContext) {
            messagesStorage.updateMessages(messages)
        }
    }

    fun resetConnection() {
        connectionModel.disconnect()
        view.showStatusNotConnected()
        view.showNotConnectedToAnyDevice()
    }

    fun disconnect() {
        connectionModel.sendDisconnectRequest()
        view.showStatusNotConnected()
        view.showNotConnectedToAnyDevice()
    }

    fun connectToDevice() {

        scanModel.getDeviceByAddress(deviceAddress).let { device ->

            if (device != null) {
                view.showStatusPending()
                view.showWainingForOpponent()
                connectionModel.connect(device)
            } else {
                view.showStatusNotConnected()
                view.showDeviceIsNotAvailable()
            }
        }
    }

    fun sendMessage(message: String) {

        if (!connectionModel.isConnected()) {
            view.showNotConnectedToSend()
            return
        }

        if (message.isEmpty()) {
            view.showNotValidMessage()
        } else {
            connectionModel.sendMessage(message)
            view.afterMessageSent()
        }
    }

    fun performImagePicking() {

        if (connectionModel.isFeatureAvailable(Contract.Feature.IMAGE_SHARING)) {
            view.openImagePicker()
        } else {
            view.showReceiverUnableToReceiveFiles()
        }
    }

    fun performFilePicking() {

        if (connectionModel.isFeatureAvailable(Contract.Feature.FILE_SHARING)) {
            view.openFilePicker()
        } else {
            view.showReceiverUnableToReceiveFiles()
        }
    }

    fun sendFile(file: File, type: PayloadType) {
        this.type = type
        if (!file.exists()) {
            view.showImageNotExist()
        } else if (!connectionModel.isConnectionPrepared()) {
            fileToSend = file
            connectionModel.addOnPrepareListener(prepareListener)
            connectionModel.prepare()
        } else if (!connectionModel.isConnected()) {
            if (type == PayloadType.IMAGE) {
                view.showPresharingImage(file.absolutePath)
            } else view.showPresharingFile()
            filePresharing = file
        } else {
            fileToSend = file
            if (connectionModel.isConnectedOrPending()) {
                sendFileIfPrepared()
            }
        }
    }

    fun cancelPresharing() {
        fileToSend = null
        filePresharing = null
    }

    fun proceedPresharing() {

        filePresharing?.let {

            if (!connectionModel.isConnected()) {
                if (type == PayloadType.IMAGE) {
                    view.showPresharingImage(it.absolutePath)
                } else view.showPresharingFile()
            } else if (!connectionModel.isFeatureAvailable(Contract.Feature.IMAGE_SHARING)) {
                view.showReceiverUnableToReceiveFiles()
            } else if (it.length() > maxFileSize) {
                view.showFileTooBig(maxFileSize.toLong())
            } else {
                connectionModel.sendFile(it, type)
                fileToSend = null
                filePresharing = null
            }
        }
    }

    fun cancelFileTransfer() {
        connectionModel.cancelFileTransfer()
        view.hideFileTransferLayout()
    }

    fun reconnect() {

        if (scanModel.isBluetoothEnabled()) {
            connectToDevice()
            view.showStatusPending()
            view.showWainingForOpponent()
        } else {
            view.showBluetoothDisabled()
        }
    }

    fun acceptConnection() {
        view.hideActions()
        view.showStatusConnected()
        connectionModel.acceptConnection()
    }

    fun rejectConnection() {
        view.hideActions()
        view.showStatusNotConnected()
        connectionModel.rejectConnection()
        updateState()
    }

    fun onBluetoothEnabled() {
        prepareConnection()
    }

    fun onBluetoothEnablingFailed() {
        view.showBluetoothEnablingFailed()
    }

    fun enableBluetooth() {
        view.requestBluetoothEnabling()
    }

    private fun updateState() {

        connectionModel.getTransferringFile().let { file ->

            if (file != null) {
                val type = if (file.transferType == TransferringFile.TransferType.RECEIVING)
                    ChatView.FileTransferType.RECEIVING else ChatView.FileTransferType.SENDING
                view.showFileTransferLayout(file.name, file.size, type, this.type)
            } else {
                view.hideFileTransferLayout()
            }
        }

        connectionModel.getCurrentConversation().let { conversation ->

            if (conversation == null) {
                if (connectionModel.isPending()) {
                    view.showStatusPending()
                    view.showWainingForOpponent()
                } else {
                    view.showStatusNotConnected()
                    view.showNotConnectedToAnyDevice()
                }
            } else if (conversation.deviceAddress != deviceAddress) {
                view.showStatusNotConnected()
                view.showNotConnectedToThisDevice("${conversation.displayName} (${conversation.deviceName})")
            } else if (connectionModel.isPending() && conversation.deviceAddress == deviceAddress) {
                view.hideDisconnected()
                view.hideLostConnection()
                view.showStatusPending()
                view.showConnectionRequest(conversation.displayName, conversation.deviceName)
            } else {
                view.showStatusConnected()
            }
        }
    }

    fun removeMessages(
        messages: LinkedList<ChatMessageViewModel>,
        selectedItems: Set<Int>,
        deviceAddress: String
    ) {
        launch(bgContext) {
            val messagesToRemove = messages.filterIndexed { index, _ -> index in selectedItems }
            messagesStorage.removeMessagesByAddressAndId(
                deviceAddress,
                messagesToRemove.map { it.uid }.toList()
            )
            withContext(uiContext) {
                view.updateHistoryRemoved(selectedItems)
            }
        }
    }
}
