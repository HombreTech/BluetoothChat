package tech.hombre.bluetoothchat.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.hombre.bluetoothchat.data.entity.ChatMessage
import tech.hombre.bluetoothchat.data.entity.Conversation

@Database(entities = [(ChatMessage::class), (Conversation::class)], version = 3)
@TypeConverters(Converter::class)
abstract class ChatDatabase: RoomDatabase() {
    abstract fun conversationsDao(): ConversationsDao
    abstract fun messagesDao(): MessagesDao
}
