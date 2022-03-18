package tech.hombre.bluetoothchatter.ui.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import tech.hombre.bluetoothchatter.ChatApplication
import tech.hombre.bluetoothchatter.ui.activity.ChatFragment
import tech.hombre.bluetoothchatter.ui.activity.ConversationsFragment

class StartStopFragmentLifecycleCallbacks(val application: ChatApplication) : FragmentManager.FragmentLifecycleCallbacks() {

    override fun onFragmentStopped(fm: FragmentManager, f: Fragment) {
        when(f) {
            is ConversationsFragment -> {
                application.isConversationsOpened = false
            }
            is ChatFragment -> {
                application.currentChat = null
            }
        }
    }

    override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
        when(f) {
            is ConversationsFragment -> {
                application.isConversationsOpened = true
            }
            is ChatFragment -> {
                val address = f.arguments?.getString("address") ?: return
                application.currentChat = address
            }
        }
    }

}