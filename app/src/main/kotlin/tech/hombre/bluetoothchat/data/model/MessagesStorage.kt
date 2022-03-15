package tech.hombre.bluetoothchat.data.model

import tech.hombre.bluetoothchat.data.entity.ChatMessage
import tech.hombre.bluetoothchat.data.entity.MessageFile

interface MessagesStorage {
    suspend fun getMessagesByDevice(address: String): List<ChatMessage>
    suspend fun getFileMessageById(uid: Long): MessageFile?
    suspend fun getFileMessagesByDevice(address: String): List<MessageFile>
    suspend fun insertMessage(message: ChatMessage)
    suspend fun updateMessage(message: ChatMessage)
    suspend fun updateMessages(messages: List<ChatMessage>)
    suspend fun removeFileInfo(messageId: Long)
    suspend fun removeMessagesByAddress(address: String)
    suspend fun setMessageAsDelivered(messageId: Long)
    suspend fun setMessageAsSeenThere(messageId: Long)
}
