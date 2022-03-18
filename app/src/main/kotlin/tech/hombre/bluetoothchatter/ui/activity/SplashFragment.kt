package tech.hombre.bluetoothchatter.ui.activity

import android.os.Bundle
import android.os.Handler
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.navArgument
import org.koin.android.ext.android.get
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.model.ProfileManager
import tech.hombre.bluetoothchatter.databinding.FragmentSplashBinding

class SplashFragment : BaseFragment<FragmentSplashBinding>(R.layout.fragment_splash) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Handler().postDelayed({

            val profileManager = get<ProfileManager>()
            val nextScreen = if (!profileManager.isInitialized()) {
                R.id.profileFragment
            } else {
                R.id.conversationsFragment
            }
            findNavController().navigate(nextScreen, arguments)
        }, 500)
    }
}
