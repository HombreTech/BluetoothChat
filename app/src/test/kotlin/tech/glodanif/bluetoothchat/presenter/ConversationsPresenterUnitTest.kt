package tech.hombre.bluetoothchat.presenter

import tech.hombre.bluetoothchat.data.entity.ConversationWithMessages
import tech.hombre.bluetoothchat.data.model.BluetoothConnector
import tech.hombre.bluetoothchat.data.model.ConversationsStorage
import tech.hombre.bluetoothchat.data.model.MessagesStorage
import tech.hombre.bluetoothchat.data.model.ProfileManager
import tech.hombre.bluetoothchat.ui.presenter.ConversationsPresenter
import tech.hombre.bluetoothchat.ui.view.ConversationsView
import tech.hombre.bluetoothchat.ui.viewmodel.ConversationViewModel
import tech.hombre.bluetoothchat.ui.viewmodel.converter.ConversationConverter
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class ConversationsPresenterUnitTest {

    @RelaxedMockK
    private lateinit var profile: ProfileManager
    @RelaxedMockK
    private lateinit var conversationsStorage: ConversationsStorage
    @RelaxedMockK
    private lateinit var messageStorage: MessagesStorage
    @RelaxedMockK
    private lateinit var connector: BluetoothConnector
    @RelaxedMockK
    private lateinit var view: ConversationsView
    @RelaxedMockK
    private lateinit var converter: ConversationConverter

    private lateinit var presenter: ConversationsPresenter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        presenter = ConversationsPresenter(view, connector, conversationsStorage,
                messageStorage, profile, converter, Dispatchers.Unconfined, Dispatchers.Unconfined)
    }

    @Test
    fun loadConversations_empty() = runBlocking {
        coEvery { conversationsStorage.getConversations() } returns ArrayList()
        presenter.loadConversations()
        verify { view.showNoConversations() }
    }

    @Test
    fun loadConversations_notEmpty_notConnected() {
        val list = listOf<ConversationWithMessages>(mockk())
        val viewModels = arrayListOf<ConversationViewModel>(mockk())
        coEvery { conversationsStorage.getConversations() } returns list
        every { converter.transform(list) } returns viewModels
        every { connector.isConnected() } returns false
        presenter.loadConversations()
        verify { view.showConversations(viewModels, null) }
    }

    @Test
    fun loadConversations_notEmpty_connected() {
        val address = "00:00:00:00"
        val list = listOf<ConversationWithMessages>(mockk())
        val viewModels = arrayListOf<ConversationViewModel>(mockk())
        coEvery { conversationsStorage.getConversations() } returns list
        every { converter.transform(list) } returns viewModels
        every { connector.isConnected() } returns true
        every { connector.getCurrentConversation()?.deviceAddress } returns address
        presenter.loadConversations()
        verify { view.showConversations(viewModels, address) }
    }

    @Test
    fun startChat() {
        val conversation = mockk<ConversationViewModel>()
        presenter.startChat(conversation)
        verify { view.hideActions() }
        verify { view.redirectToChat(conversation) }
    }

    @Test
    fun connection_reject() {
        presenter.rejectConnection()
        verify { view.hideActions() }
    }

    @Test
    fun connection_prepare() {
        presenter.prepareConnection()
        verify { view.dismissConversationNotification() }
    }

    @Test
    fun user_load() {
        presenter.loadUserProfile()
        verify { view.showUserProfile(any(), 0) }
    }
}
