package tech.hombre.bluetoothchatter.data.model

import tech.hombre.bluetoothchatter.data.database.ChatDatabase
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.MessageFile
import java.io.File

class MessagesStorageImpl(db: ChatDatabase) : MessagesStorage {

    private val dao = db.messagesDao()

    override suspend fun insertMessage(message: ChatMessage) {
        dao.insert(message)
    }

    override suspend fun getMessagesByDevice(address: String): List<ChatMessage> {
        val messages = dao.getMessagesByDevice(address)
        messages.forEach {
            if (it.filePath != null) {
                it.fileExists = File(it.filePath).exists()
            }
        }
        return messages
    }

    override suspend fun getFileMessageById(uid: Long): MessageFile? {
        return dao.getFileMessageById(uid)
    }

    override suspend fun getFileMessagesByDevice(address: String): List<MessageFile> {
        return (if (address.isNotEmpty())
            dao.getFileMessagesByDevice(address) else dao.getAllFilesMessages())
                .filter { File(it.filePath).exists() }
    }

    override suspend fun updateMessage(message: ChatMessage) {
        dao.updateMessage(message)
    }

    override suspend fun updateMessages(messages: List<ChatMessage>) {
        dao.updateMessages(messages)
    }

    override suspend fun removeFileInfo(messageId: Long) {
        dao.removeFileInfo(messageId)
    }

    override suspend fun removeMessagesByAddress(address: String) {
        dao.deleteAllByDeviceAddress(address)
    }

    override suspend fun removeMessagesByAddressAndId(address: String, messagesId: List<Long>) {
        dao.deleteByDeviceAddressAndId(address, messagesId)
    }

    override suspend fun setMessageAsDelivered(messageId: Long) {
        dao.setMessageAsDelivered(messageId)
    }

    override suspend fun setMessageAsNotDelivered(messageId: Long) {
        dao.setMessageAsNotDelivered(messageId)
    }

    override suspend fun setMessageAsSeenThere(messageId: Long) {
        dao.setMessageAsSeenThere(messageId)
    }

    override suspend fun setMessageAsSeenHere(messageId: Long) {
        dao.setMessageAsSeenHere(messageId)
    }
}
