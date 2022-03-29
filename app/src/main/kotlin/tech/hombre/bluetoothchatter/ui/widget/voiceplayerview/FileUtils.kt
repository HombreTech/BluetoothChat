package tech.hombre.bluetoothchatter.ui.widget.voiceplayerview

import android.app.Activity
import android.content.Context
import android.os.AsyncTask
import android.util.Log
import java.io.*


object FileUtils {
    fun updateVisualizer(
        context: Context,
        file: File,
        playerVisualizerSeekbar: PlayerVisualizerSeekbar
    ) {
        Log.e(" BYTES", "CALLED")
        object : AsyncTask<Void?, Void?, ByteArray>() {
            protected override fun doInBackground(vararg p0: Void?): ByteArray? {
                return fileToBytes(file)
            }

            override fun onPostExecute(bytes: ByteArray) {
                super.onPostExecute(bytes)
                Log.e("BYTES", bytes.size.toString())
                (context as Activity).runOnUiThread {
                    playerVisualizerSeekbar.setBytes(bytes)
                    playerVisualizerSeekbar.invalidate()
                }
            }
        }.execute()
    }

    fun fileToBytes(file: File): ByteArray {
        val size = file.length().toInt()
        val bytes = ByteArray(size)
        try {
            val buf = BufferedInputStream(FileInputStream(file))
            buf.read(bytes, 0, bytes.size)
            buf.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return bytes
    }
}