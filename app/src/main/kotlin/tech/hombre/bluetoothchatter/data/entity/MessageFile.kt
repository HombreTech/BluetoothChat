package tech.hombre.bluetoothchatter.data.entity

data class MessageFile(
    val uid: Long,
    val filePath: String?,
    val own: Boolean,
    val messageType: Int
)
