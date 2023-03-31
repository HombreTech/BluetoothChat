package tech.hombre.bluetoothchatter.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.Conversation

@Database(entities = [(ChatMessage::class), (Conversation::class)], version = 4)
@TypeConverters(Converter::class)
abstract class ChatDatabase: RoomDatabase() {
    abstract fun conversationsDao(): ConversationsDao
    abstract fun messagesDao(): MessagesDao
}
