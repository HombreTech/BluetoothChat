package tech.hombre.bluetoothchatter.ui.presenter

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.OnLifecycleEvent
import tech.hombre.bluetoothchatter.data.model.ConversationsStorage
import tech.hombre.bluetoothchatter.ui.view.ContactChooserView
import tech.hombre.bluetoothchatter.ui.viewmodel.converter.ContactConverter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactChooserPresenter(private val view: ContactChooserView,
                              private val model: ConversationsStorage,
                              private val converter: ContactConverter,
                              private val uiContext: CoroutineDispatcher = Dispatchers.Main,
                              private val bgContext: CoroutineDispatcher = Dispatchers.IO): BasePresenter(uiContext) {

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun loadContacts() = launch {

        val contacts = withContext(bgContext) { model.getContacts() }

        if (contacts.isNotEmpty()) {
            val viewModels = converter.transform(contacts)
            view.showContacts(viewModels)
        } else {
            view.showNoContacts()
        }
    }
}
