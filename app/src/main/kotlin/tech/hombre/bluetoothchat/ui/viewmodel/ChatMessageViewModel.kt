package tech.hombre.bluetoothchat.ui.viewmodel

import androidx.annotation.StringRes
import tech.hombre.bluetoothchat.data.service.message.PayloadType
import tech.hombre.bluetoothchat.utils.Size

data class ChatMessageViewModel(
        val uid: Long,
        val dayOfYear: String,
        val dayOfYearRaw: Long,
        val time: String,
        val text: String?,
        val own: Boolean,
        val type: PayloadType?,
        val isImageAvailable: Boolean,
        @StringRes
        val imageProblemText: Int,
        val imageSize: Size,
        val imagePath: String?,
        val imageUri: String?
)
