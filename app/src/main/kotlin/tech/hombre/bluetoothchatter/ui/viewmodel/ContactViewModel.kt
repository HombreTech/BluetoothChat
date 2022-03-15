package tech.hombre.bluetoothchatter.ui.viewmodel

import com.amulyakhare.textdrawable.TextDrawable

data class ContactViewModel(
        val address: String,
        val name: String,
        val avatar: TextDrawable
)
