package tech.hombre.bluetoothchatter.data.model

import tech.hombre.bluetoothchatter.data.service.message.PayloadType

interface OnFileListener {
    fun onFileSendingStarted(fileAddress: String?, fileSize: Long, type: PayloadType)
    fun onFileSendingProgress(sentBytes: Long, totalBytes: Long)
    fun onFileSendingFinished()
    fun onFileSendingFailed()
    fun onFileReceivingStarted(fileSize: Long, type: PayloadType)
    fun onFileReceivingProgress(sentBytes: Long, totalBytes: Long)
    fun onFileReceivingFinished()
    fun onFileReceivingFailed()
    fun onFileTransferCanceled(byPartner: Boolean)
}
