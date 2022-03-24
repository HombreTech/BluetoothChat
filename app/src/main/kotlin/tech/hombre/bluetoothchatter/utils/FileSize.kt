package tech.hombre.bluetoothchatter.utils

class FileSize(val size: Long) {
    fun getFileSize(): String {
        return size.toReadableFileSize()
    }
}
