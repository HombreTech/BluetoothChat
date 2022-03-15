package tech.hombre.bluetoothchatter.ui.util

import androidx.appcompat.app.AppCompatDelegate.*

interface ThemeHolder {
    fun setNightMode(@NightMode nightMode: Int)
    @NightMode
    fun getNightMode(): Int
}
