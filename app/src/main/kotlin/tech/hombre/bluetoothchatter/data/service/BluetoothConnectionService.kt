package tech.hombre.bluetoothchatter.data.service

import android.app.Service
import android.app.TaskStackBuilder
import android.bluetooth.BluetoothDevice
import android.content.*
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.RemoteInput
import androidx.core.net.toUri
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.model.OnConnectionListener
import tech.hombre.bluetoothchatter.data.model.OnFileListener
import tech.hombre.bluetoothchatter.data.model.OnMessageListener
import tech.hombre.bluetoothchatter.data.service.connection.ConnectionController
import tech.hombre.bluetoothchatter.data.service.connection.ConnectionSubject
import tech.hombre.bluetoothchatter.data.service.message.Contract
import tech.hombre.bluetoothchatter.data.service.message.Message
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.data.service.message.TransferringFile
import tech.hombre.bluetoothchatter.ui.activity.MainActivity
import tech.hombre.bluetoothchatter.ui.view.NotificationView
import java.io.File

class BluetoothConnectionService : Service(), ConnectionSubject {

    private val binder = ConnectionBinder()

    private var connectionListener: OnConnectionListener? = null
    private var messageListener: OnMessageListener? = null
    private var fileListener: OnFileListener? = null

    private val controller: ConnectionController by inject {
        parametersOf(application as tech.hombre.bluetoothchatter.ChatApplication, this)
    }

    private val connectionActionReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null) {

                if (intent.getBooleanExtra(NotificationView.EXTRA_APPROVED, false)) {

                    controller.approveConnection()

                    val address = intent.getStringExtra(NotificationView.EXTRA_ADDRESS)
                    val chatIntent = Intent(
                        Intent.ACTION_VIEW,
                        "bluetoothchatter://conversations/$address".toUri(),
                        context,
                        MainActivity::class.java
                    )
                    val conversationsIntent = Intent(
                        Intent.ACTION_VIEW,
                        "bluetoothchatter://conversations".toUri(),
                        context,
                        MainActivity::class.java
                    )

                    TaskStackBuilder.create(context)
                        .addNextIntentWithParentStack(conversationsIntent)
                        .addNextIntentWithParentStack(chatIntent)
                        .startActivities()

                } else {
                    controller.rejectConnection()
                }
            }
        }
    }

    private val replyActionReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {

            if (intent != null) {
                RemoteInput.getResultsFromIntent(intent)?.let {
                    val replyText = it.getCharSequence(NotificationView.EXTRA_TEXT_REPLY)
                    controller.replyFromNotification("$replyText")
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = binder

    inner class ConnectionBinder : Binder() {
        fun getService() = this@BluetoothConnectionService
    }

    override fun onCreate() {
        super.onCreate()

        controller.onNewForegroundMessage = { showNotification(it) }

        registerReceiver(connectionActionReceiver, IntentFilter(NotificationView.ACTION_CONNECTION))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(replyActionReceiver, IntentFilter(NotificationView.ACTION_REPLY))
        }

        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent?.action == ACTION_STOP) {

            isRunning = false
            controller.stop()
            connectionListener?.onConnectionDestroyed()

            stopSelf()
            return START_NOT_STICKY
        }

        controller.prepareForAccept()
        showNotification(getString(R.string.notification__ready_to_connect))
        return Service.START_STICKY
    }

    private fun showNotification(message: String) {
        val notification = controller.createForegroundNotification(message)
        startForeground(FOREGROUND_SERVICE, notification)
    }

    fun connect(device: BluetoothDevice) {
        controller.connect(device)
    }

    fun stop() {
        controller.stop()
    }

    fun disconnect() {
        controller.disconnect()
    }

    fun isConnected() = controller.isConnected()

    fun isConnectedOrPending() = controller.isConnectedOrPending()

    fun isPending() = controller.isPending()

    fun getCurrentConversation(): Conversation? = controller.getCurrentConversation()

    fun getTransferringFile(): TransferringFile? = controller.getTransferringFile()

    fun getCurrentContract(): Contract = controller.getCurrentContract()

    fun sendMessage(message: Message) {
        controller.sendMessage(message)
    }

    fun sendFile(file: File, type: PayloadType) {
        controller.sendFile(file, type)
    }

    fun approveConnection() {
        controller.approveConnection()
    }

    fun rejectConnection() {
        controller.rejectConnection()
    }

    fun cancelFileTransfer() {
        controller.cancelFileTransfer()
    }

    override fun isRunning() = isRunning

    override fun handleConnectedOut(conversation: Conversation) {
        connectionListener?.onConnectedOut(conversation)
    }

    override fun handleConnectedIn(conversation: Conversation) {
        connectionListener?.onConnectedIn(conversation)
    }

    override fun handleConnectionAccepted() {
        connectionListener?.onConnectionAccepted()
    }

    override fun handleConnected(device: BluetoothDevice) {
        connectionListener?.onConnected(device)
    }

    override fun handleConnectingInProgress() {
        connectionListener?.onConnecting()
    }

    override fun handleDisconnected() {
        connectionListener?.onDisconnected()
    }

    override fun handleConnectionRejected() {
        connectionListener?.onConnectionRejected()
    }

    override fun handleConnectionFailed() {
        connectionListener?.onConnectionFailed()
    }

    override fun handleConnectionLost() {
        connectionListener?.onConnectionLost()
    }

    override fun handleConnectionWithdrawn() {
        connectionListener?.onConnectionWithdrawn()
    }

    override fun handleFileSendingStarted(fileAddress: String?, fileSize: Long, type: PayloadType) {
        fileListener?.onFileSendingStarted(fileAddress, fileSize, type)
    }

    override fun handleFileSendingProgress(sentBytes: Long, totalBytes: Long) {
        fileListener?.onFileSendingProgress(sentBytes, totalBytes)
    }

    override fun handleFileSendingFinished() {
        fileListener?.onFileSendingFinished()
    }

    override fun handleFileSendingFailed() {
        fileListener?.onFileSendingFailed()
    }

    override fun handleFileReceivingStarted(fileSize: Long, type: PayloadType) {
        fileListener?.onFileReceivingStarted(fileSize, type)
    }

    override fun handleFileReceivingProgress(sentBytes: Long, totalBytes: Long) {
        fileListener?.onFileReceivingProgress(sentBytes, totalBytes)
    }

    override fun handleFileReceivingFinished() {
        fileListener?.onFileReceivingFinished()
    }

    override fun handleFileReceivingFailed() {
        fileListener?.onFileReceivingFailed()
    }

    override fun handleFileTransferCanceled(byPartner: Boolean) {
        fileListener?.onFileTransferCanceled(byPartner)
    }

    override fun handleMessageReceived(message: ChatMessage) {
        messageListener?.onMessageReceived(message)
    }

    override fun handleMessageSent(message: ChatMessage) {
        messageListener?.onMessageSent(message)
    }

    override fun handleMessageSendingFailed() {
        messageListener?.onMessageSendingFailed()
    }

    override fun handleMessageDelivered(uid: Long) {
        messageListener?.onMessageDelivered(uid)
    }

    override fun handleMessageNotDelivered(uid: Long) {
        messageListener?.onMessageNotDelivered(uid)
    }

    override fun handleMessageSeen(uid: Long) {
        messageListener?.onMessageSeen(uid)
    }

    override fun isAnybodyListeningForMessages() = messageListener != null

    fun setConnectionListener(listener: OnConnectionListener?) {
        this.connectionListener = listener
    }

    fun setMessageListener(listener: OnMessageListener?) {
        this.messageListener = listener
    }

    fun setFileListener(listener: OnFileListener?) {
        this.fileListener = listener
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(connectionActionReceiver)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            unregisterReceiver(replyActionReceiver)
        }

        isRunning = false
        controller.stop()
    }

    companion object {

        var isRunning = false

        private const val FOREGROUND_SERVICE = 101
        const val ACTION_STOP = "action.stop"

        fun start(context: Context) {
            val intent = Intent(context, BluetoothConnectionService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun bind(context: Context, connection: ServiceConnection) {
            val intent = Intent(context, BluetoothConnectionService::class.java)
            context.bindService(intent, connection, AppCompatActivity.BIND_ABOVE_CLIENT)
        }
    }
}
