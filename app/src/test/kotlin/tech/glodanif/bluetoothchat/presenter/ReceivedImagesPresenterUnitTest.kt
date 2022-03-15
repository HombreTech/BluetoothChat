package tech.hombre.bluetoothchat.presenter

import tech.hombre.bluetoothchat.data.entity.MessageFile
import tech.hombre.bluetoothchat.data.model.MessagesStorage
import tech.hombre.bluetoothchat.ui.presenter.ReceivedImagesPresenter
import tech.hombre.bluetoothchat.ui.view.ReceivedImagesView
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import org.junit.Before
import org.junit.Test

class ReceivedImagesPresenterUnitTest {

    @RelaxedMockK
    private lateinit var storage: MessagesStorage
    @RelaxedMockK
    private lateinit var view: ReceivedImagesView

    private lateinit var presenter: ReceivedImagesPresenter

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        presenter = ReceivedImagesPresenter("", view, storage,
                Dispatchers.Unconfined, Dispatchers.Unconfined)
    }

    @Test
    fun loading_empty() {
        coEvery { storage.getFileMessagesByDevice("") } returns ArrayList()
        presenter.loadImages()
        verify { view.showNoImages() }
    }

    @Test
    fun loading_notEmpty() {
        val list = arrayListOf<MessageFile>(mockk())
        coEvery { storage.getFileMessagesByDevice("") } returns list
        presenter.loadImages()
        verify { view.displayImages(list) }
    }
}
