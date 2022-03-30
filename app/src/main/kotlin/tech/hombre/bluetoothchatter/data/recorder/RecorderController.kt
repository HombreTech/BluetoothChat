package tech.hombre.bluetoothchatter.data.recorder

interface RecorderController {
    fun init(listener: RecorderControllerInterface)
    fun startRecording()
    fun stopRecording(isCanceled: Boolean)
}