package tech.hombre.bluetoothchatter.data.entity

import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import java.util.*

data class SimpleChatMessage(
        var deviceAddress: String,
        var date: Date,
        var text: String,
        var seenHere: Boolean,
        var messageType: PayloadType?
)
