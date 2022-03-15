package tech.hombre.bluetoothchatter.ui.view

import tech.hombre.bluetoothchatter.data.entity.MessageFile

interface ReceivedImagesView {
    fun displayImages(imageMessages: List<MessageFile>)
    fun showNoImages()
}
