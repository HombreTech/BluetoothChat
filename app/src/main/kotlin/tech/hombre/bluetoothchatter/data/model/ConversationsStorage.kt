package tech.hombre.bluetoothchatter.data.model

import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.entity.ConversationWithMessages

interface ConversationsStorage {
    suspend fun getContacts(): List<Conversation>
    suspend fun getConversations(): List<ConversationWithMessages>
    suspend fun getConversationByAddress(address: String): Conversation?
    suspend fun insertConversation(conversation: Conversation)
    suspend fun removeConversationByAddress(address: String)
}
