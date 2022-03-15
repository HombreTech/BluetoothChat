package tech.hombre.bluetoothchat.presenter

import tech.hombre.bluetoothchat.data.model.MessagesStorage
import tech.hombre.bluetoothchat.ui.presenter.ImagePreviewPresenter
import tech.hombre.bluetoothchat.ui.view.ImagePreviewView
import tech.hombre.bluetoothchat.utils.toReadableFileSize
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test
import java.io.File

class ImagesPreviewPresenterUnitTest {

    @RelaxedMockK
    private lateinit var storage: MessagesStorage
    @RelaxedMockK
    private lateinit var view: ImagePreviewView
    @RelaxedMockK
    private lateinit var file: File

    private lateinit var presenter: ImagePreviewPresenter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        presenter = ImagePreviewPresenter(0, file, view, storage,
                Dispatchers.Unconfined, Dispatchers.Unconfined)
    }

    @Test
    fun file_removing() {
        presenter.removeFile()
        verify { view.close() }
    }

    @Test
    fun file_display() {
        val fileName = "file.png"
        val fileSize = 1024.toLong().toReadableFileSize()
        val fileUri = "file://$fileName"
        every { file.name } returns fileName
        every { file.length() } returns 1024
        every { file.absolutePath } returns fileName
        presenter.loadImage()
        verify { view.showFileInfo(fileName, fileSize) }
        verify { view.displayImage(fileUri) }
    }
}
