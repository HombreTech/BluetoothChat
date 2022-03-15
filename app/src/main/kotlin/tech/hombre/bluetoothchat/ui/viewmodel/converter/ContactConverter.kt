package tech.hombre.bluetoothchat.ui.viewmodel.converter

import com.amulyakhare.textdrawable.TextDrawable
import tech.hombre.bluetoothchat.data.entity.Conversation
import tech.hombre.bluetoothchat.ui.viewmodel.ContactViewModel
import tech.hombre.bluetoothchat.utils.getFirstLetter

class ContactConverter {

    fun transform(conversation: Conversation): ContactViewModel {
        return ContactViewModel(
                conversation.deviceAddress,
                "${conversation.displayName} (${conversation.deviceName})",
                TextDrawable.builder()
                        .buildRound(conversation.displayName.getFirstLetter(), conversation.color)
        )
    }

    fun transform(conversationCollection: Collection<Conversation>): List<ContactViewModel> {
        return conversationCollection.map {
            transform(it)
        }
    }
}
