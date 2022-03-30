package tech.hombre.bluetoothchatter.ui.view

import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate.NightMode

interface AudioRecorderView {
    fun onRecordingStarted()
    fun onRecordingStopped(filepath: String)
    fun onRecordingError(error: String)
    fun onRecordingDurationUpdate(duration: Long)
}
