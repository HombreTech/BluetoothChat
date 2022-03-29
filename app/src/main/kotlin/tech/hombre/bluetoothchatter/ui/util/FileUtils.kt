package tech.hombre.bluetoothchatter.ui.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

fun Context.getPath(
    uri: Uri?
): String {

    if (uri == null)
        return ""

    try {

        val mFolder = File("$cacheDir/FilePicker")
        if (!mFolder.exists()) {
            mFolder.mkdirs()
        }

        val cR: ContentResolver = contentResolver
        val mime = MimeTypeMap.getSingleton()
        val extension = mime.getExtensionFromMimeType(cR.getType(uri))
        var filename = ""
        val cursor = cR.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            filename = cursor.getString(nameIndex)
        }

        val tmpFile = File(
            mFolder.absolutePath,
            if (filename.isNotEmpty()) filename else "${getTimestampString()}.${extension}"
        )

        val fos: FileOutputStream?
        try {
            fos = FileOutputStream(tmpFile)
            fos.copyInputStreamToFile(cR.openInputStream(uri))
            fos.flush()
            fos.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tmpFile.path
    } catch (e: Throwable) {
        e.printStackTrace()
    }

    return ""

}

fun OutputStream.copyInputStreamToFile(inputStream: InputStream?) {
    this.use { fileOut ->
        inputStream?.copyTo(fileOut)
    }
}

fun getTimestampString(): String {
    val date = Calendar.getInstance()
    return SimpleDateFormat("yyyy MM dd hh mm ss", Locale.US).format(date.time).replace(" ", "")
}

fun String.getFileType(): PayloadType {
    val imageTypes = listOf("jpeg", "jpg", "png", "gif", "bmp")
    val audioTypes = listOf("mp3", "aac", "wav", "m4a")
    return when (substringAfterLast(".")) {
        in imageTypes -> PayloadType.IMAGE
        in audioTypes -> PayloadType.AUDIO
        else -> PayloadType.FILE
    }
}