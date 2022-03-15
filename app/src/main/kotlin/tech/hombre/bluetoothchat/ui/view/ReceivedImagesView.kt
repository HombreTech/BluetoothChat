package tech.hombre.bluetoothchat.ui.view

import tech.hombre.bluetoothchat.data.entity.MessageFile

interface ReceivedImagesView {
    fun displayImages(imageMessages: List<MessageFile>)
    fun showNoImages()
}
