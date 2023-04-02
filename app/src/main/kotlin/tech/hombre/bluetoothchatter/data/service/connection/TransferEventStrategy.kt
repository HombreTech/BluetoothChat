package tech.hombre.bluetoothchatter.data.service.connection

import tech.hombre.bluetoothchatter.data.service.message.Contract
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.utils.isNumber

class TransferEventStrategy : DataTransferThread.EventsStrategy {

    private val divider = Contract.DIVIDER
    private val generalMessageRegex = Regex("\\d+$divider\\d+$divider\\d+$divider*")
    private val fileStartRegex = Regex("6$divider\\d+${divider}0$divider*")

    override fun isMessage(message: String?) =
        message != null && generalMessageRegex.containsMatchIn(message)

    override fun isFileStart(message: String?): DataTransferThread.FileInfo? =
        if (message != null && fileStartRegex.containsMatchIn(message)) {
            val dividersCount = message.count { it == divider.first() }
            val messageBody = "6$divider" + message.substringAfter("6$divider")

            val info = fileStartRegex.replace(messageBody, "")
            val uid = messageBody.substring(2).substringBefore(divider)

            if (info.isEmpty() || !uid.isNumber()) {
                null
            } else {
                val size = info.substringAfter(divider).substringBefore(divider)
                val typeValue: Int = if (dividersCount >= 6) {
                    info.substringBeforeLast(divider).substringAfterLast(divider).take(1).toInt()
                } else {
                    messageBody.substringAfterLast(divider).take(1).toInt()
                }
                val type = PayloadType.from(typeValue)
                if (size.isNumber()) {
                    DataTransferThread.FileInfo(
                        uid.toLong(),
                        info.substringBefore(divider),
                        size.toLong(),
                        type
                    )
                } else {
                    null
                }
            }
        } else {
            null
        }

    override fun isFileCanceled(message: String?) =

        if (message != null && (message.contains("8${divider}0${divider}0$divider") || message.contains(
                "8${divider}0${divider}1$divider"
            ))
        ) {
            val byPartner = message
                .substringAfter("8${divider}0$divider")
                .replace("8${divider}0$divider", "")
                .substringBefore(divider)
            DataTransferThread.CancelInfo(byPartner == "1")
        } else {
            null
        }

    override fun isFileFinish(message: String?) =
        message != null && message.contains("7${divider}0${divider}0$divider")

    override fun getCancellationMessage(byPartner: Boolean) =
        "8${divider}0$divider${if (byPartner) "1" else "0"}$divider"
}
