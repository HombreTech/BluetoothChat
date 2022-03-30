package tech.hombre.bluetoothchatter.data.recorder

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.*
import tech.hombre.bluetoothchatter.ui.util.getRecorderFilePath
import java.io.IOException

class RecorderControllerImpl(private val context: Context):  RecorderController {

    private val scope = CoroutineScope(Job())

    private var recorder: MediaRecorder? = null

    private val filepath: String = context.getRecorderFilePath()

    private var listener: RecorderControllerInterface? = null

    fun init(listener: RecorderControllerInterface) {
        this.listener = listener
    }

    override fun startRecording() {
        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        recorder?.apply {
            try {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(filepath)
                setAudioSamplingRate(48000)
                setAudioEncodingBitRate(48000)
                setOnErrorListener { _, what, extra ->
                    listener?.onRecordingError("$what: $extra")
                    stopDurationTimer()
                }
                prepare()
                start()
                startDurationTimer()
                listener?.onRecordingStarted()

            } catch (e: IOException) {
                e.printStackTrace()
                listener?.onRecordingError(e.message ?: "Start recording failed")
                stopDurationTimer()
            }
        }
    }

    override fun stopRecording() {
        listener?.onRecordingStopped(filepath)
        stopDurationTimer()
    }

    private val timerJob = scope.launch(Dispatchers.IO) {
        var duration = 0L
        while (isActive) {
            scope.launch {
                duration += TIMER_PERIOD_DURATION
                listener?.onRecordingDurationUpdate(duration)
            }
            delay(TIMER_PERIOD_DURATION)
        }
    }

    private fun startDurationTimer() {
        timerJob.start()
    }

    private fun stopDurationTimer() {
        timerJob.cancel()
    }

    companion object {
        const val TIMER_PERIOD_DURATION = 1000L
    }

}