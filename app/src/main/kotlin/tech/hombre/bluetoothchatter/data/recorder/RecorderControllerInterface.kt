package tech.hombre.bluetoothchatter.data.recorder

interface RecorderControllerInterface {
    fun onRecordingStarted()
    fun onRecordingStopped(filepath: String)
    fun onRecordingError(error: String)
    fun onRecordingDurationUpdate(duration: Long)
}