package tech.hombre.bluetoothchatter.ui.presenter

import tech.hombre.bluetoothchatter.data.model.MessagesStorage
import tech.hombre.bluetoothchatter.ui.view.ReceivedImagesView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReceivedImagesPresenter(private val address: String,
                              private val view: ReceivedImagesView,
                              private val model: MessagesStorage,
                              private val uiContext: CoroutineDispatcher = Dispatchers.Main,
                              private val bgContext: CoroutineDispatcher = Dispatchers.IO) : BasePresenter(uiContext) {

    fun loadImages() = launch {
        val messages = withContext(bgContext) { model.getFileMessagesByDevice(address) }
        if (messages.isNotEmpty()) {
            view.displayFiles(messages)
        } else {
            view.showNoFiles()
        }
    }
}
