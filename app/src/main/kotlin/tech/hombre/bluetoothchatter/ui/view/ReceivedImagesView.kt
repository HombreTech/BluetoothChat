package tech.hombre.bluetoothchatter.ui.view

import tech.hombre.bluetoothchatter.data.entity.MessageFile

interface ReceivedImagesView {
    fun displayFiles(messages: List<MessageFile>)
    fun showNoFiles()
}
