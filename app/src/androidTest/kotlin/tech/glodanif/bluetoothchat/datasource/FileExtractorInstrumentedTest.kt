package tech.hombre.bluetoothchatter.datasource

import android.os.Environment
import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.model.FileManager
import tech.hombre.bluetoothchatter.data.model.FileManagerImpl
import kotlinx.coroutines.experimental.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class FileExtractorInstrumentedTest {

    private lateinit var extractor: FileManager

    private lateinit var apkFile: File
    private lateinit var zipFile: File

    @Before
    fun prepare() {

        val context = InstrumentationRegistry.getTargetContext()
        extractor = FileManagerImpl(context)

        val directory = context.externalCacheDir
                ?: File(Environment.getExternalStorageDirectory(), context.getString(R.string.app_name))
        apkFile = File(directory, "BluetoothChat.apk")
        zipFile = File(directory, "BluetoothChat.zip")

        apkFile.delete()
        zipFile.delete()
    }

    @After
    fun release() {
        apkFile.delete()
        zipFile.delete()
    }

    @Test
    fun apkExtraction() = runBlocking {
        val uri = extractor.extractApkFile()
        assertNotNull(uri)
        assertTrue(uri?.lastPathSegment == "BluetoothChat.zip")
        assertTrue(apkFile.exists())
        assertTrue(zipFile.exists())
        assertTrue(apkFile.length() > 2_097_152)
        assertTrue(zipFile.length() > 2_097_152)
    }
}
