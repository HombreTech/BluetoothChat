package tech.hombre.bluetoothchat.ui.presenter

import tech.hombre.bluetoothchat.data.model.MessagesStorage
import tech.hombre.bluetoothchat.ui.view.ImagePreviewView
import tech.hombre.bluetoothchat.utils.toReadableFileSize
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ImagePreviewPresenter(private val messageId: Long,
                            private val image: File,
                            private val view: ImagePreviewView,
                            private val storage: MessagesStorage,
                            private val uiContext: CoroutineDispatcher = Dispatchers.Main,
                            private val bgContext: CoroutineDispatcher = Dispatchers.IO): BasePresenter(uiContext) {

    fun loadImage() {
        view.showFileInfo(image.name, image.length().toReadableFileSize())
        view.displayImage("file://${image.absolutePath}")
    }

    fun removeFile() {

        launch(bgContext) {
            image.delete()
            storage.removeFileInfo(messageId)
        }

        view.close()
    }
}
