package tech.hombre.bluetoothchat.presenter

import tech.hombre.bluetoothchat.data.entity.Conversation
import tech.hombre.bluetoothchat.data.model.ConversationsStorage
import tech.hombre.bluetoothchat.ui.presenter.ContactChooserPresenter
import tech.hombre.bluetoothchat.ui.view.ContactChooserView
import tech.hombre.bluetoothchat.ui.viewmodel.ContactViewModel
import tech.hombre.bluetoothchat.ui.viewmodel.converter.ContactConverter
import io.mockk.*
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test

class ContactChooserPresenterUnitTest {

    @RelaxedMockK
    private lateinit var storage: ConversationsStorage
    @RelaxedMockK
    private lateinit var view: ContactChooserView
    @RelaxedMockK
    private lateinit var converter: ContactConverter

    private lateinit var presenter: ContactChooserPresenter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        presenter = ContactChooserPresenter(view, storage, converter, Dispatchers.Unconfined, Dispatchers.Unconfined)
    }

    @Test
    fun loading_empty() {
        coEvery { storage.getContacts() } returns ArrayList()
        presenter.loadContacts()
        verify { view.showNoContacts() }
    }

    @Test
    fun loading_notEmpty() {
        val list = arrayListOf<Conversation>(mockk())
        val viewModels = arrayListOf<ContactViewModel>(mockk())
        coEvery { storage.getContacts() } returns list
        every { converter.transform(list) } returns viewModels
        presenter.loadContacts()
        verify { view.showContacts(viewModels) }
    }
}
