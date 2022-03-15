package tech.hombre.bluetoothchat.ui.view

import tech.hombre.bluetoothchat.ui.viewmodel.ContactViewModel

interface ContactChooserView {
    fun showContacts(contacts: List<ContactViewModel>)
    fun showNoContacts()
}
