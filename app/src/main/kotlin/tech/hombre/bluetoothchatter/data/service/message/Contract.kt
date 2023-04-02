package tech.hombre.bluetoothchatter.data.service.message

import androidx.annotation.ColorInt
import java.io.File

class Contract {

    var partnerVersion: Int = MESSAGE_CONTRACT_VERSION

    infix fun setupWith(version: Int) {
        partnerVersion = version
    }

    fun reset() {
        partnerVersion = MESSAGE_CONTRACT_VERSION
    }

    fun createChatMessage(message: String, replyMessageUid: Long?) = Message(generateUniqueId(), message, replyMessageUid, MessageType.MESSAGE)

    fun createConnectMessage(name: String, @ColorInt color: Int) =
            Message(0, "$name$DIVIDER$color$DIVIDER$MESSAGE_CONTRACT_VERSION", null,true, MessageType.CONNECTION_REQUEST)

    fun createDisconnectMessage() = Message(false, MessageType.CONNECTION_REQUEST)

    fun createAcceptConnectionMessage(name: String, @ColorInt color: Int) =
            Message("$name$DIVIDER$color$DIVIDER$MESSAGE_CONTRACT_VERSION", true, MessageType.CONNECTION_RESPONSE)


    fun createRejectConnectionMessage(name: String, @ColorInt color: Int) =
            Message("$name$DIVIDER$color$DIVIDER$MESSAGE_CONTRACT_VERSION", false, MessageType.CONNECTION_RESPONSE)

    fun createSuccessfulDeliveryMessage(id: Long) = Message(id, true, MessageType.DELIVERY)

    fun createSeenMessage(id: Long) = Message(id, true, MessageType.SEEING)

    fun createFileStartMessage(file: File, replyMessageUid: Long?, type: PayloadType): Message {
        val uid = generateUniqueId()
        return Message(uid, "${file.name.replace(DIVIDER, "")}$DIVIDER${file.length()}$DIVIDER${type.value}", replyMessageUid, false, MessageType.FILE_START)
    }

    fun createFileEndMessage() = Message(false, MessageType.FILE_END)

    fun isFeatureAvailable(feature: Feature) = when (feature) {
        Feature.IMAGE_SHARING -> partnerVersion >= 1
        Feature.FILE_SHARING -> partnerVersion >= 1
        Feature.VOICE_RECORDING -> partnerVersion >= 1
        Feature.REPLY_TO_MESSAGE -> partnerVersion >= 2
    }

    enum class MessageType(val value: Int) {

        UNEXPECTED(-1),
        MESSAGE(0),
        DELIVERY(1),
        CONNECTION_RESPONSE(2),
        CONNECTION_REQUEST(3),
        SEEING(4),
        EDITING(5),
        FILE_START(6),
        FILE_END(7),
        FILE_CANCELED(8);

        companion object {
            fun from(findValue: Int) = values().first { it.value == findValue }
        }
    }

    enum class Feature {
        IMAGE_SHARING,
        FILE_SHARING,
        VOICE_RECORDING,
        REPLY_TO_MESSAGE;
    }

    companion object {

        const val DIVIDER = "¯"
        const val MESSAGE_CONTRACT_VERSION = 2

        fun generateUniqueId() = System.nanoTime()
    }
}
