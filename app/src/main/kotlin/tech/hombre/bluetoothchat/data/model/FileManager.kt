package tech.hombre.bluetoothchat.data.model

import android.net.Uri

interface FileManager {
    suspend fun extractApkFile(): Uri?
}
