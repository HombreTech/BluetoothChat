package tech.hombre.bluetoothchatter.data.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import java.util.*

@Entity(tableName = "message")
data class ChatMessage(
        @PrimaryKey(autoGenerate = true)
        var uid: Long = 0,
        var deviceAddress: String,
        var date: Date,
        var own: Boolean,
        var text: String,
        var replyMessageUid: Long? = null
) {

    var seenHere: Boolean = false
    var seenThere: Boolean = false
    var delivered: Boolean = false
    var edited: Boolean = false

    var messageType: PayloadType? = null
    var filePath: String? = null
    var fileInfo: String? = null

    @Ignore
    var fileExists: Boolean = false

    @Ignore
    var replyMessage: ChatMessage? = null
}
