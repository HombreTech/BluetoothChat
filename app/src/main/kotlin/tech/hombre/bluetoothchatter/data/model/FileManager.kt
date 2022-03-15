package tech.hombre.bluetoothchatter.data.model

import android.net.Uri

interface FileManager {
    suspend fun extractApkFile(): Uri?
}
