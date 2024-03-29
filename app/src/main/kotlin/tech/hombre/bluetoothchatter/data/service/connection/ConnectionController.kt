package tech.hombre.bluetoothchatter.data.service.connection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import kotlinx.coroutines.*
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.model.ConversationsStorage
import tech.hombre.bluetoothchatter.data.model.MessagesStorage
import tech.hombre.bluetoothchatter.data.model.ProfileManager
import tech.hombre.bluetoothchatter.data.model.UserPreferences
import tech.hombre.bluetoothchatter.data.service.message.Contract
import tech.hombre.bluetoothchatter.data.service.message.Message
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.data.service.message.TransferringFile
import tech.hombre.bluetoothchatter.ui.view.NotificationView
import tech.hombre.bluetoothchatter.ui.widget.ShortcutManager
import tech.hombre.bluetoothchatter.utils.LimitedQueue
import tech.hombre.bluetoothchatter.utils.Size
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.coroutines.CoroutineContext

class ConnectionController(
    private val application: tech.hombre.bluetoothchatter.ChatApplication,
    private val subject: ConnectionSubject,
    private val view: NotificationView,
    private val conversationStorage: ConversationsStorage,
    private val messagesStorage: MessagesStorage,
    private val preferences: UserPreferences,
    private val profileManager: ProfileManager,
    private val shortcutManager: ShortcutManager,
    private val uiContext: CoroutineDispatcher = Dispatchers.Main,
    private val bgContext: CoroutineDispatcher = Dispatchers.IO
) : CoroutineScope {

    private val blAppName = application.getString(R.string.bl_app_name)
    private val blAppUUID = UUID.fromString(application.getString(R.string.bl_app_uuid))

    private var acceptThread: AcceptJob? = null
    private var connectThread: ConnectJob? = null
    private var dataTransferThread: DataTransferThread? = null

    @Volatile
    private var connectionState: ConnectionState = ConnectionState.NOT_CONNECTED

    @Volatile
    private var connectionType: ConnectionType? = null

    private var currentSocket: BluetoothSocket? = null
    private var currentConversation: Conversation? = null
    private var contract = Contract()

    private var justRepliedFromNotification = false
    private val me: Person by lazy {
        Person.Builder().setName(application.getString(R.string.notification__me)).build()
    }
    private val shallowHistory =
        LimitedQueue<Pair<Long, NotificationCompat.MessagingStyle.Message>>(4)

    var onNewForegroundMessage: ((String) -> Unit)? = null

    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + uiContext

    fun createForegroundNotification(message: String) = view.getForegroundNotification(message)

    @Synchronized
    fun prepareForAccept() {

        cancelConnections()

        if (subject.isRunning()) {
            acceptThread = AcceptJob()
            acceptThread?.start()
            onNewForegroundMessage?.invoke(application.getString(R.string.notification__ready_to_connect))
        }
    }

    @Synchronized
    fun disconnect() {
        dataTransferThread?.cancel(true)
        dataTransferThread = null
        prepareForAccept()
    }

    @Synchronized
    fun connect(device: BluetoothDevice) {

        if (connectionState == ConnectionState.CONNECTING) {
            connectThread?.cancel()
            connectThread = null
        }

        dataTransferThread?.cancel(true)
        acceptThread?.cancel()
        dataTransferThread = null
        acceptThread = null
        currentSocket = null
        currentConversation = null
        contract.reset()
        connectionType = null

        connectThread = ConnectJob(device)
        connectThread?.start()

        launch { subject.handleConnectingInProgress() }
    }

    private fun cancelConnections() {
        connectThread?.cancel()
        connectThread = null
        dataTransferThread?.cancel()
        dataTransferThread = null
        currentSocket = null
        currentConversation = null
        contract.reset()
        connectionType = null
        justRepliedFromNotification = false
    }

    private fun cancelAccept() {
        acceptThread?.cancel()
        acceptThread = null
    }

    private fun connectionFailed() {
        currentSocket = null
        currentConversation = null
        contract.reset()
        launch { subject.handleConnectionFailed() }
        connectionState = ConnectionState.NOT_CONNECTED
        prepareForAccept()
    }

    @Synchronized
    fun stop() {
        cancelConnections()
        cancelAccept()
        connectionState = ConnectionState.NOT_CONNECTED
        job.cancel()
    }

    @Synchronized
    fun connected(socket: BluetoothSocket, type: ConnectionType) {

        cancelConnections()

        connectionType = type
        currentSocket = socket

        cancelAccept()

        val transferEventsListener = object : DataTransferThread.TransferEventsListener {

            override fun onMessageReceived(message: String) {
                launch {
                    this@ConnectionController.onMessageReceived(message)
                }
            }

            override fun onMessageSent(message: String) {
                launch {
                    this@ConnectionController.onMessageSent(message)
                }
            }

            override fun onMessageSendingFailed() {
                launch {
                    this@ConnectionController.onMessageSendingFailed()
                }
            }

            override fun onConnectionPrepared(type: ConnectionType) {

                onNewForegroundMessage?.invoke(
                    application.getString(
                        R.string.notification__connected_to, socket.remoteDevice.name
                            ?: "?"
                    )
                )
                connectionState = ConnectionState.PENDING

                if (type == ConnectionType.OUTCOMING) {
                    contract.createConnectMessage(
                        profileManager.getUserName(),
                        profileManager.getUserColor()
                    ).let { message ->
                        dataTransferThread?.write(message.getDecodedMessage())
                    }
                }
            }

            override fun onConnectionCanceled() {
                currentSocket = null
                currentConversation = null
                contract.reset()
            }

            override fun onConnectionLost() {
                connectionLost()
            }
        }

        val fileEventsListener = object : DataTransferThread.OnFileListener {

            override fun onFileSendingStarted(file: TransferringFile, type: PayloadType) {

                subject.handleFileSendingStarted(file.name, file.size, type)

                currentConversation?.let {

                    val silently = application.currentChat != null && currentSocket != null &&
                            application.currentChat.equals(currentSocket?.remoteDevice?.address)

                    view.showFileTransferNotification(
                        it.displayName, it.deviceName,
                        it.deviceAddress, file, 0, silently
                    )
                }
            }

            override fun onFileSendingProgress(file: TransferringFile, sentBytes: Long) {

                launch {

                    subject.handleFileSendingProgress(sentBytes, file.size)

                    if (currentConversation != null) {
                        view.updateFileTransferNotification(sentBytes, file.size)
                    }
                }
            }

            override fun onFileSendingFinished(uid: Long, replyMessageUid: Long?, path: String, type: PayloadType) {

                contract.createFileEndMessage().let { message ->
                    dataTransferThread?.write(message.getDecodedMessage())
                }

                currentSocket?.let { socket ->

                    val message =
                        ChatMessage(uid, socket.remoteDevice.address, Date(), true, "", replyMessageUid).apply {
                            seenHere = true
                            messageType = type
                            filePath = path
                        }

                    launch(bgContext) {

                        val size = getImageSize(path)
                        message.fileInfo = "${size.width}x${size.height}"
                        message.fileExists = true

                        messagesStorage.insertMessage(message)

                        launch(uiContext) {

                            subject.handleFileSendingFinished()
                            subject.handleMessageSent(message)

                            view.dismissFileTransferNotification()
                            currentConversation?.let {
                                shortcutManager.addConversationShortcut(
                                    message.deviceAddress,
                                    it.displayName,
                                    it.color
                                )
                            }
                        }
                    }
                }
            }

            override fun onFileSendingFailed() {

                launch {
                    subject.handleFileSendingFailed()
                    view.dismissFileTransferNotification()
                }
            }

            override fun onFileReceivingStarted(file: TransferringFile, type: PayloadType) {

                launch {
println("onFileReceivingStarted $type")
                    subject.handleFileReceivingStarted(file.size, type)

                    currentConversation?.let {

                        val silently = application.currentChat != null && currentSocket != null &&
                                application.currentChat.equals(currentSocket?.remoteDevice?.address)

                        view.showFileTransferNotification(
                            it.displayName, it.deviceName,
                            it.deviceAddress, file, 0, silently
                        )
                    }
                }
            }

            override fun onFileReceivingProgress(file: TransferringFile, receivedBytes: Long) {

                launch {

                    subject.handleFileReceivingProgress(receivedBytes, file.size)

                    if (currentConversation != null) {
                        view.updateFileTransferNotification(receivedBytes, file.size)
                    }
                }
            }

            override fun onFileReceivingFinished(uid: Long, replyMessageUid: Long?, path: String, type: PayloadType) {

                currentSocket?.remoteDevice?.let { device ->

                    val address = device.address
                    val message = ChatMessage(uid, address, Date(), false, "", replyMessageUid).apply {
                        messageType = type
                        filePath = path
                    }

                    val partner = Person.Builder().setName(
                        currentConversation?.displayName
                            ?: "?"
                    ).build()
                    shallowHistory.add(
                        Pair(
                            message.uid,
                            NotificationCompat.MessagingStyle.Message(
                                getTextByType(
                                    message.messageType
                                ), message.date.time, partner
                            )
                        )
                    )
                    if (!subject.isAnybodyListeningForMessages() || application.currentChat == null || !application.currentChat.equals(
                            address
                        )
                    ) {
                        //FIXME: Fixes not appearing notification
                        view.dismissMessageNotification()
                        view.showNewMessageNotification(
                            getTextByType(message.messageType),
                            currentConversation?.displayName,
                            device.name,
                            address,
                            shallowHistory.map { it.second },
                            preferences.isSoundEnabled()
                        )
                    } else {
                        message.seenHere = true
                        val seenReport = contract.createSeenMessage(uid)
                        sendMessage(seenReport)
                        shallowHistory.removeAll { uid == it.first }
                    }

                    launch(bgContext) {

                        val size = getImageSize(path)
                        message.fileInfo = "${size.width}x${size.height}"
                        message.fileExists = true

                        messagesStorage.insertMessage(message)

                        launch(uiContext) {
                            subject.handleFileReceivingFinished()
                            subject.handleMessageReceived(message)

                            view.dismissFileTransferNotification()

                            val deliveryReport = contract.createSuccessfulDeliveryMessage(uid)
                            sendMessage(deliveryReport)

                            currentConversation?.let {
                                shortcutManager.addConversationShortcut(
                                    address,
                                    it.displayName,
                                    it.color
                                )
                            }
                        }
                    }
                }
            }

            override fun onFileReceivingFailed() {
                launch {
                    subject.handleFileReceivingFailed()
                    view.dismissFileTransferNotification()
                }
            }

            override fun onFileTransferCanceled(byPartner: Boolean) {
                launch {
                    subject.handleFileTransferCanceled(byPartner)
                    view.dismissFileTransferNotification()
                }
            }
        }

        val eventsStrategy = TransferEventStrategy()
        val filesDirectory =
            application.applicationContext.getExternalFilesDir(application.getString(R.string.app_name))
                ?: application.filesDir

        dataTransferThread =
            object : DataTransferThread(
                socket,
                type,
                transferEventsListener,
                filesDirectory,
                fileEventsListener,
                eventsStrategy
            ) {
                override fun shouldRun(): Boolean {
                    return isConnectedOrPending()
                }
            }
        dataTransferThread?.prepare()
        dataTransferThread?.start()

        launch { subject.handleConnected(socket.remoteDevice) }
    }

    fun getCurrentConversation() = currentConversation

    fun getCurrentContract() = contract

    fun isConnected() = connectionState == ConnectionState.CONNECTED

    fun isPending() = connectionState == ConnectionState.PENDING

    fun isConnectedOrPending() = isConnected() || isPending()

    fun replyFromNotification(text: String) {
        contract.createChatMessage(text, null).let { message ->
            justRepliedFromNotification = true
            sendMessage(message)
        }
    }

    fun sendMessage(message: Message) {

        if (isConnectedOrPending()) {

            val disconnect =
                message.type == Contract.MessageType.CONNECTION_REQUEST && !message.flag

            dataTransferThread?.write(message.getDecodedMessage(), disconnect)

            if (disconnect) {
                dataTransferThread?.cancel(disconnect)
                dataTransferThread = null
                prepareForAccept()
            }
        }

        if (message.type == Contract.MessageType.SEEING) {
            shallowHistory.removeAll { message.uid == it.first }
        }

        if (message.type == Contract.MessageType.CONNECTION_RESPONSE) {
            if (message.flag) {
                connectionState = ConnectionState.CONNECTED
            } else {
                disconnect()
            }
            view.dismissConnectionNotification()
        }
    }

    fun sendFile(file: File, type: PayloadType, replyMessageUid: Long?) {

        if (isConnected()) {
            contract.createFileStartMessage(file, replyMessageUid, type).let { message ->
                dataTransferThread?.write(message.getDecodedMessage())
                dataTransferThread?.writeFile(message.uid, message.replyMessageUid, file, type)
            }
        }
    }

    fun approveConnection() {
        sendMessage(
            contract.createAcceptConnectionMessage(
                profileManager.getUserName(), profileManager.getUserColor()
            )
        )
    }

    fun rejectConnection() {
        sendMessage(
            contract.createRejectConnectionMessage(
                profileManager.getUserName(), profileManager.getUserColor()
            )
        )
    }

    fun getTransferringFile(): TransferringFile? {
        return dataTransferThread?.getTransferringFile()
    }

    fun cancelFileTransfer() {
        dataTransferThread?.cancelFileTransfer()
        view.dismissFileTransferNotification()
    }

    private fun onMessageSent(messageBody: String) = currentSocket?.let { socket ->

        val device = socket.remoteDevice
        val message = Message(messageBody)
        val sentMessage =
            ChatMessage(message.uid, socket.remoteDevice.address, Date(), true, message.body)

        if (message.type == Contract.MessageType.MESSAGE) {

            sentMessage.seenHere = true

            launch(bgContext) {

                messagesStorage.insertMessage(sentMessage)

                if ((!subject.isAnybodyListeningForMessages() || application.currentChat == null || !application.currentChat.equals(
                        device.address
                    )) && justRepliedFromNotification
                ) {
                    view.showNewMessageNotification(
                        message.body,
                        currentConversation?.displayName,
                        device.name,
                        device.address,
                        shallowHistory.map { it.second },
                        preferences.isSoundEnabled()
                    )
                    justRepliedFromNotification = false
                }

                launch(uiContext) { subject.handleMessageSent(sentMessage) }
                currentConversation?.let {
                    shortcutManager.addConversationShortcut(
                        sentMessage.deviceAddress,
                        it.displayName,
                        it.color
                    )
                }
            }
        }
    }

    private fun onMessageReceived(messageBody: String) {

        val message = Message(messageBody)

        if (message.type == Contract.MessageType.MESSAGE && currentSocket != null) {

            handleReceivedMessage(message.uid, message.body, message.replyMessageUid)

        } else if (message.type == Contract.MessageType.DELIVERY) {

            if (message.flag) {
                subject.handleMessageDelivered(message.uid)
                launch(bgContext) {
                    messagesStorage.setMessageAsDelivered(message.uid)
                }
            } else {
                subject.handleMessageNotDelivered(message.uid)
            }
        } else if (message.type == Contract.MessageType.SEEING) {

            if (message.flag) {
                subject.handleMessageSeen(message.uid)
                launch(bgContext) {
                    messagesStorage.setMessageAsSeenThere(message.uid)
                }
            }
        } else if (message.type == Contract.MessageType.CONNECTION_RESPONSE) {

            if (message.flag) {
                handleConnectionApproval(message)
            } else {
                connectionState = ConnectionState.REJECTED
                prepareForAccept()
                subject.handleConnectionRejected()
            }
        } else if (message.type == Contract.MessageType.CONNECTION_REQUEST && currentSocket != null) {

            if (message.flag) {
                handleConnectionRequest(message)
            } else {
                disconnect()
                subject.handleDisconnected()
            }
        } else if (message.type == Contract.MessageType.FILE_CANCELED) {
            dataTransferThread?.cancelFileTransfer()
        }
    }

    private fun onMessageSendingFailed() {
        subject.handleMessageSendingFailed()
    }

    private fun handleReceivedMessage(uid: Long, text: String, replyMessageUid: Long?) = currentSocket?.let { socket ->

        val device: BluetoothDevice = socket.remoteDevice

        val receivedMessage = ChatMessage(uid, device.address, Date(), false, text, replyMessageUid)

        val partner = Person.Builder().setName(currentConversation?.displayName ?: "?").build()
        shallowHistory.add(
            Pair(
                receivedMessage.uid,
                NotificationCompat.MessagingStyle.Message(
                    receivedMessage.text, receivedMessage.date.time, partner
                )
            )
        )
        if (!subject.isAnybodyListeningForMessages() || application.currentChat == null || !application.currentChat.equals(
                device.address
            )
        ) {
            view.showNewMessageNotification(
                text,
                currentConversation?.displayName,
                device.name,
                device.address,
                shallowHistory.map { it.second },
                preferences.isSoundEnabled()
            )
        } else {
            receivedMessage.seenHere = true
            val seenReport = contract.createSeenMessage(uid)
            sendMessage(seenReport)
            shallowHistory.removeAll { uid == it.first }
        }

        launch(bgContext) {
            if (receivedMessage.replyMessageUid != null) {
                val replyMessage = messagesStorage.getMessageByDevice(
                    address = receivedMessage.deviceAddress,
                    uid = receivedMessage.replyMessageUid!!
                )
                receivedMessage.replyMessage = replyMessage
            }
            messagesStorage.insertMessage(receivedMessage)
            launch(uiContext) {
                subject.handleMessageReceived(receivedMessage)
                val deliveryReport = contract.createSuccessfulDeliveryMessage(uid)
                sendMessage(deliveryReport)
            }
            currentConversation?.let {
                shortcutManager.addConversationShortcut(device.address, it.displayName, it.color)
            }
        }
    }

    private fun handleConnectionRequest(message: Message) = currentSocket?.let { socket ->

        val device: BluetoothDevice = socket.remoteDevice

        val parts = message.body.split(Contract.DIVIDER)
        val conversation = Conversation(
            device.address, device.name
                ?: "?", parts[0], parts[1].toInt()
        )

        launch(bgContext) { conversationStorage.insertConversation(conversation) }

        currentConversation = conversation
        contract setupWith if (parts.size >= 3) parts[2].trim().toInt() else 0

        subject.handleConnectedIn(conversation)

        if (!application.isConversationsOpened && !(application.currentChat != null && application.currentChat.equals(
                device.address
            ))
        ) {
            view.showConnectionRequestNotification(
                "${conversation.displayName} (${conversation.deviceName})",
                conversation.deviceAddress,
                conversation.displayName,
                preferences.isSoundEnabled()
            )
        }
    }

    private fun handleConnectionApproval(message: Message) = currentSocket?.let { socket ->

        val device: BluetoothDevice = socket.remoteDevice

        val parts = message.body.split(Contract.DIVIDER)
        val conversation = Conversation(
            device.address, device.name
                ?: "?", parts[0], parts[1].toInt()
        )

        launch(bgContext) { conversationStorage.insertConversation(conversation) }

        currentConversation = conversation
        contract setupWith if (parts.size >= 3) parts[2].trim().toInt() else 0

        connectionState = ConnectionState.CONNECTED
        subject.handleConnectionAccepted()
        subject.handleConnectedOut(conversation)
    }

    private fun connectionLost() {

        currentSocket = null
        currentConversation = null
        contract.reset()
        if (isConnectedOrPending()) {
            launch {
                if (isPending() && connectionType == ConnectionType.INCOMING) {
                    connectionState = ConnectionState.NOT_CONNECTED
                    subject.handleConnectionWithdrawn()
                } else {
                    connectionState = ConnectionState.NOT_CONNECTED
                    subject.handleConnectionLost()
                }
                prepareForAccept()
            }
        } else {
            prepareForAccept()
        }
    }

    private fun getImageSize(path: String): Size {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        return Size(options.outWidth, options.outHeight)
    }

    private fun getTextByType(messageType: PayloadType?): String {
        return when (messageType) {
            PayloadType.IMAGE ->
                application.getString(R.string.chat__image_message, "\uD83D\uDDBC")
            PayloadType.FILE ->
                application.getString(R.string.chat__image_file, "\uD83D\uDCCE")
            PayloadType.AUDIO ->
                application.getString(R.string.chat__image_audio, "\uD83C\uDFA7")
            else -> {
                application.getString(R.string.chat__image_file, "\uD83D\uDCCE")
            }
        }
    }

    private inner class AcceptJob : Thread() {

        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket = BluetoothAdapter.getDefaultAdapter()
                    ?.listenUsingRfcommWithServiceRecord(blAppName, blAppUUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }

            connectionState = ConnectionState.LISTENING
        }

        override fun run() {

            while (!isConnectedOrPending()) {

                try {
                    serverSocket?.accept()?.let { socket ->
                        when (connectionState) {
                            ConnectionState.LISTENING, ConnectionState.CONNECTING -> {
                                connected(socket, ConnectionType.INCOMING)
                            }
                            ConnectionState.NOT_CONNECTED, ConnectionState.CONNECTED, ConnectionState.PENDING, ConnectionState.REJECTED -> try {
                                socket.close()
                            } catch (e: IOException) {
                                e.printStackTrace()
                            }
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        fun cancel() {
            try {
                serverSocket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class ConnectJob(private val bluetoothDevice: BluetoothDevice) : Thread() {

        private var socket: BluetoothSocket? = null

        override fun run() {

            try {
                socket = bluetoothDevice.createRfcommSocketToServiceRecord(blAppUUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
            connectionState = ConnectionState.CONNECTING

            try {
                socket?.connect()
            } catch (connectException: IOException) {
                connectException.printStackTrace()
                try {
                    socket?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                connectionFailed()
                return
            }

            synchronized(this@ConnectionController) {
                connectThread = null
            }

            socket?.let {
                connected(it, ConnectionType.OUTCOMING)
            }
        }

        fun cancel() {
            try {
                socket?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}

