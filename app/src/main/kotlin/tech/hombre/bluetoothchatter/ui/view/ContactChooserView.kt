package tech.hombre.bluetoothchatter.ui.view

import tech.hombre.bluetoothchatter.ui.viewmodel.ContactViewModel

interface ContactChooserView {
    fun showContacts(contacts: List<ContactViewModel>)
    fun showNoContacts()
}
