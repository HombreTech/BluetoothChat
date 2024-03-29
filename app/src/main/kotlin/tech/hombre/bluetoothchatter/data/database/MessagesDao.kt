package tech.hombre.bluetoothchatter.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import androidx.room.Update
import tech.hombre.bluetoothchatter.data.entity.MessageFile

@Dao
interface MessagesDao {

    @Query("SELECT * FROM message WHERE deviceAddress = :address ORDER BY date DESC ")
    fun getMessagesByDevice(address: String): List<ChatMessage>

    @Query("SELECT * FROM message WHERE deviceAddress = :address AND uid = :replyMessageUid ORDER BY date DESC ")
    fun getMessageByDevice(address: String, replyMessageUid: Long): ChatMessage

    @Query("SELECT uid, filePath, own, messageType FROM message WHERE deviceAddress = :address AND (messageType = 1 OR messageType = 2 OR messageType = 3) AND own = 0 AND filePath IS NOT NULL ORDER BY date DESC")
    fun getFileMessagesByDevice(address: String): List<MessageFile>

    @Query("SELECT uid, filePath, own, messageType FROM message WHERE uid = :uid")
    fun getFileMessageById(uid: Long): MessageFile?

    @Query("SELECT uid, filePath, own, messageType FROM message WHERE (messageType = 1 OR messageType = 2 OR messageType = 3) AND own = 0 AND filePath IS NOT NULL ORDER BY date DESC")
    fun getAllFilesMessages(): List<MessageFile>

    @Insert
    fun insert(message: ChatMessage)

    @Update
    fun updateMessages(messages: List<ChatMessage>)

    @Update
    fun updateMessage(message: ChatMessage)

    @Delete
    fun delete(message: ChatMessage)

    @Query("DELETE FROM message WHERE deviceAddress = :address")
    fun deleteAllByDeviceAddress(address: String)

    @Query("DELETE FROM message WHERE deviceAddress = :address AND uid in (:messagesId)")
    fun deleteByDeviceAddressAndId(address: String, messagesId: List<Long>)

    @Query("UPDATE message SET fileInfo = null, filePath = null WHERE uid = :messageId")
    fun removeFileInfo(messageId: Long)

    @Query("UPDATE message SET delivered = 1 WHERE uid = :messageId")
    fun setMessageAsDelivered(messageId: Long)

    @Query("UPDATE message SET delivered = 0 WHERE uid = :messageId")
    fun setMessageAsNotDelivered(messageId: Long)

    @Query("UPDATE message SET seenThere = 1 WHERE uid = :messageId")
    fun setMessageAsSeenThere(messageId: Long)

    @Query("UPDATE message SET seenHere = 1 WHERE uid = :messageId")
    fun setMessageAsSeenHere(messageId: Long)
}
