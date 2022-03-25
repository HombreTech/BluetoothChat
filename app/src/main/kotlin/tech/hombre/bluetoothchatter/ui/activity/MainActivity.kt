package tech.hombre.bluetoothchatter.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.commitNow
import androidx.navigation.fragment.NavHostFragment
import org.koin.android.ext.android.get
import tech.hombre.bluetoothchatter.ChatApplication
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.model.ProfileManager
import tech.hombre.bluetoothchatter.ui.util.StartStopFragmentLifecycleCallbacks
import tech.hombre.bluetoothchatter.ui.util.ThemeHolder
import tech.hombre.bluetoothchatter.utils.findNavControllerFor
import tech.hombre.bluetoothchatter.utils.getAsNavHostFragmentFor

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nightMode = (application as ThemeHolder).getNightMode()
        AppCompatDelegate.setDefaultNightMode(nightMode)

        installSplashScreen()

        setContentView(R.layout.activity_main)

        val args = Bundle()
        if (intent.action == Intent.ACTION_SEND) {
            val type = intent.type
            val text = intent.getStringExtra(Intent.EXTRA_TEXT)
            val stream = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            args.apply {
                putString(Intent.EXTRA_MIME_TYPES, type)
                putString(Intent.EXTRA_TEXT, text)
                putParcelable(Intent.EXTRA_STREAM, stream)
            }
        }

        getAsNavHostFragmentFor(R.id.nav_host_fragment)
            .parentFragmentManager
            .registerFragmentLifecycleCallbacks(
                StartStopFragmentLifecycleCallbacks(application as ChatApplication),
                true
            )

        supportFragmentManager.commitNow {
            add(R.id.nav_host_fragment, NavHostFragment.create(R.navigation.nav_graph, args))
        }

        val profileManager = get<ProfileManager>()
        if (!profileManager.isInitialized()) {
            findNavControllerFor(R.id.nav_host_fragment).navigate(
                R.id.profileFragment, args
            )
        }

    }

}
