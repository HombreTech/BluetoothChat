package tech.hombre.bluetoothchatter.data.service.message

import tech.hombre.bluetoothchatter.utils.isNumber

class Message() {

    var type: Contract.MessageType = Contract.MessageType.MESSAGE
    var uid: Long = 0
    var replyMessageUid: Long? = null
    var flag: Boolean = false
    var body: String = ""

    constructor(message: String) : this() {

        var messageText = message

        val parsedType = messageText.substring(0, messageText.indexOf(Contract.DIVIDER))
        if (!parsedType.isNumber()) {
            type = Contract.MessageType.UNEXPECTED
            return
        }

        type = Contract.MessageType.from(parsedType.toInt())
        messageText = messageText.substring(messageText.indexOf(Contract.DIVIDER) + 1, messageText.length)

        val parsedUid = messageText.substring(0, messageText.indexOf(Contract.DIVIDER))
        uid = if (parsedType.isNumber()) parsedUid.toLong() else 0
        messageText = messageText.substring(messageText.indexOf(Contract.DIVIDER) + 1, messageText.length)

        flag = messageText.substring(0, messageText.indexOf(Contract.DIVIDER)).toInt() == 1
        messageText = messageText.substring(messageText.indexOf(Contract.DIVIDER) + 1, messageText.length)


        if (type == Contract.MessageType.MESSAGE && messageText.contains(Contract.DIVIDER)) {
            println("messageText $messageText")
            body = messageText.substring(0, messageText.indexOf(Contract.DIVIDER))
            messageText = messageText.substring(messageText.indexOf(Contract.DIVIDER) + 1, messageText.length)
            replyMessageUid = messageText.toLongOrNull()
        } else {
            body = messageText
        }
    }

    constructor(uid: Long, body: String, replyMessageUid: Long?, flag: Boolean, type: Contract.MessageType) : this() {
        this.uid = uid
        this.replyMessageUid = replyMessageUid
        this.body = body
        this.type = type
        this.flag = flag
    }

    constructor(body: String, flag: Boolean, type: Contract.MessageType) : this() {
        this.body = body
        this.type = type
        this.flag = flag
    }

    constructor(uid: Long, body: String, replyMessageUid: Long?, type: Contract.MessageType) : this() {
        this.uid = uid
        this.replyMessageUid = replyMessageUid
        this.body = body
        this.type = type
    }

    constructor(uid: Long, flag: Boolean, type: Contract.MessageType) : this() {
        this.uid = uid
        this.flag = flag
        this.type = type
    }

    constructor(flag: Boolean, type: Contract.MessageType) : this() {
        this.flag = flag
        this.type = type
    }

    fun getDecodedMessage(): String {
        val flag = if (this.flag) 1 else 0
        var message = "${type.value}${Contract.DIVIDER}$uid${Contract.DIVIDER}$flag${Contract.DIVIDER}$body"
        if (replyMessageUid != null) message += "${Contract.DIVIDER}$replyMessageUid"
        return message
    }
}
