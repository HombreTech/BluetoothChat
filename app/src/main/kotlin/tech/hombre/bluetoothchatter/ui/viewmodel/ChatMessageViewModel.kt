package tech.hombre.bluetoothchatter.ui.viewmodel

import androidx.annotation.StringRes
import tech.hombre.bluetoothchatter.data.entity.ChatMessage
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.utils.FileSize
import tech.hombre.bluetoothchatter.utils.Size

data class ChatMessageViewModel(
        val uid: Long,
        val dayOfYear: String,
        val dayOfYearRaw: Long,
        val time: String,
        val text: String?,
        val own: Boolean,
        var seenThere: Boolean,
        var seenHere: Boolean,
        var delivered: Boolean,
        val type: PayloadType?,
        val isImageAvailable: Boolean,
        @StringRes
        val imageProblemText: Int,
        val imageSize: Size,
        val fileSize: FileSize,
        val filePath: String?,
        val fileUri: String?,
        val replyMessage: ChatMessage?
)
