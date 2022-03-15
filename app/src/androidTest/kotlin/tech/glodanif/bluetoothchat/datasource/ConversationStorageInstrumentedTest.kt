package tech.hombre.bluetoothchatter.datasource

import android.graphics.Color
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import tech.hombre.bluetoothchatter.data.database.Database
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.entity.Conversation
import tech.hombre.bluetoothchatter.data.entity.ConversationWithMessages
import tech.hombre.bluetoothchatter.data.entity.SimpleChatMessage
import tech.hombre.bluetoothchatter.data.model.ConversationsStorage
import tech.hombre.bluetoothchatter.data.model.ConversationsStorageImpl
import tech.hombre.bluetoothchatter.data.model.MessagesStorage
import tech.hombre.bluetoothchatter.data.model.MessagesStorageImpl
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ConversationStorageInstrumentedTest {

    private lateinit var storage: ConversationsStorage
    private lateinit var messagesStorage: MessagesStorage

    private val address1 = "00:00:00:00:00:01"
    private val conversation1 = Conversation(address1, "deviceName1", "displayName1", Color.BLACK)
    private val editedConversation1 = Conversation(address1, "deviceName1_1", "displayName1_1", Color.GREEN)

    private val address2 = "00:00:00:00:00:02"
    private val conversation2 = Conversation(address2, "deviceName2", "displayName2", Color.WHITE)

    private val address3 = "00:00:00:00:00:03"
    private val conversation3 = Conversation(address3, "deviceName3", "displayName3", Color.GRAY)
    private val message1 = ChatMessage(0, address3, Date(1533585790), true, "text1")

    private val address4 = "00:00:00:00:00:04"
    private val conversation4 = Conversation(address4, "deviceName4", "displayName4", Color.TRANSPARENT)

    @Before
    fun prepare() = runBlocking {
        val context = InstrumentationRegistry.getTargetContext()
        val db = Database.getInstance(context)
        storage = ConversationsStorageImpl(db).apply {
            insertConversation(conversation1)
            insertConversation(conversation2)
            insertConversation(conversation3)
        }
        messagesStorage = MessagesStorageImpl(db).apply {
            insertMessage(message1)
        }
    }

    @After
    fun release() = runBlocking {
        storage.removeConversationByAddress(address1)
        storage.removeConversationByAddress(address2)
        storage.removeConversationByAddress(address3)
        storage.removeConversationByAddress(address4)
        messagesStorage.removeMessagesByAddress(address3)
    }

    @Test
    fun insertConversation() = runBlocking {
        storage.insertConversation(conversation4)
        val dbConversation = storage.getConversationByAddress(address4)
        assertNotNull(dbConversation)
        assertTrue(equal(conversation4, dbConversation))
    }

    @Test
    fun updateConversation() = runBlocking {
        storage.insertConversation(editedConversation1)
        val dbConversation = storage.getConversationByAddress(address1)
        assertNotNull(dbConversation)
        assertTrue(equal(editedConversation1, dbConversation))
    }

    @Test
    fun removeConversation() = runBlocking {
        storage.removeConversationByAddress(address2)
        val dbConversation = storage.getConversationByAddress(address2)
        assertNull(dbConversation)
    }

    @Test
    fun getConversation_existingAddress() = runBlocking {
        val dbConversation = storage.getConversationByAddress(address1)
        assertNotNull(dbConversation)
        assertTrue(equal(conversation1, dbConversation))
    }

    @Test
    fun getConversation_nonExistingAddress() = runBlocking {
        val dbConversation = storage.getConversationByAddress("abcd")
        assertNull(dbConversation)
    }

    @Test
    fun getContacts() = runBlocking {
        val dbContacts = storage.getContacts()
        assertTrue(dbContacts.contains(conversation3))
    }

    @Test
    fun getConversations() = runBlocking {
        val dbConversations = storage.getConversations()
        val conversation = dbConversations.lastOrNull { it.address == address3 }
        assertNotNull(conversation)
        assertTrue(equal(conversation3, conversation))
    }

    @Test
    fun getConversationWithTheMessages() = runBlocking {
        val conversations = storage.getConversations()
        val conversation = conversations.lastOrNull { it.address == address3 }
        val message = conversation?.messages?.lastOrNull { it.text == "text1" }
        assertNotNull(message)
        assertTrue(equal(message1, message))
    }

    private fun equal(c1: Conversation?, c2: Conversation?) = c1 != null && c2 != null &&
            c1.deviceAddress == c2.deviceAddress && c1.deviceName == c2.deviceName &&
            c1.displayName == c2.displayName && c1.color == c2.color

    private fun equal(c1: Conversation?, c2: ConversationWithMessages?) = c1 != null && c2 != null &&
            c1.deviceAddress == c2.address && c1.deviceName == c2.deviceName &&
            c1.displayName == c2.displayName && c1.color == c2.color

    private fun equal(m1: ChatMessage?, m2: SimpleChatMessage?) = m1 != null && m2 != null &&
             m1.date == m2.date && m1.deviceAddress == m2.deviceAddress && m1.text == m2.text
}
