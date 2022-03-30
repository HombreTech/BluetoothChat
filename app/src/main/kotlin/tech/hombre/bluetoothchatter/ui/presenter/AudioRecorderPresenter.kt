package tech.hombre.bluetoothchatter.ui.presenter

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import tech.hombre.bluetoothchatter.data.recorder.RecorderController
import tech.hombre.bluetoothchatter.data.recorder.RecorderControllerInterface
import tech.hombre.bluetoothchatter.ui.view.AudioRecorderView

class AudioRecorderPresenter(
    private val view: AudioRecorderView,
    private val recorderController: RecorderController,
    private val uiContext: CoroutineDispatcher = Dispatchers.Main,
    private val bgContext: CoroutineDispatcher = Dispatchers.IO
) : BasePresenter(uiContext), RecorderControllerInterface {

    init {
        recorderController.init(
            this
        )
    }

    fun startRecording() {
        recorderController.startRecording()
    }

    fun stopRecording() {
        recorderController.stopRecording(isCanceled = false)
    }

    fun cancelRecording() {
        recorderController.stopRecording(isCanceled = true)
    }

    override fun onRecordingStarted() {
        view.onRecordingStarted()
    }

    override fun onRecordingStopped(filepath: String) {
        view.onRecordingStopped(filepath)
    }

    override fun onRecordingError(error: String) {
        view.onRecordingError(error)
    }

    override fun onRecordingDurationUpdate(duration: Long) {
        view.onRecordingDurationUpdate(duration)
    }

}
