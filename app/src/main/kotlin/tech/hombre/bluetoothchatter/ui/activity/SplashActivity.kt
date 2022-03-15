package tech.hombre.bluetoothchatter.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.model.ProfileManager
import org.koin.android.ext.android.get

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setContentView(R.layout.activity_splash)

        Handler().postDelayed({

            val profileManager = get<ProfileManager>()
            val nextScreen = if (!profileManager.isInitialized())
                ProfileActivity::class.java else ConversationsActivity::class.java
            val newIntent = Intent(this, nextScreen)

            if (intent.action == Intent.ACTION_SEND) {
                newIntent.action = Intent.ACTION_SEND
                newIntent.type = intent.type
                newIntent.putExtra(Intent.EXTRA_TEXT, intent.getStringExtra(Intent.EXTRA_TEXT))
                newIntent.putExtra(Intent.EXTRA_STREAM, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
                newIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(newIntent)
            finish()

        }, 500)
    }
}
