package tech.hombre.bluetoothchat.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import tech.hombre.bluetoothchat.BuildConfig
import tech.hombre.bluetoothchat.R
import tech.hombre.bluetoothchat.utils.bind

class AboutActivity : SkeletonActivity() {

    private val infoLabel: TextView by bind(R.id.tv_app_credentials)
    private val versionLabel: TextView by bind(R.id.tv_version)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about, ActivityType.CHILD_ACTIVITY)

        versionLabel.text = "v${tech.hombre.bluetoothchat.BuildConfig.VERSION_NAME} / ${tech.hombre.bluetoothchat.BuildConfig.VERSION_CODE}"
        infoLabel.text = "${getString(R.string.about__app_name, getString(R.string.bl_app_name))}\n" +
                "${getString(R.string.about__app_uuid, getString(R.string.bl_app_uuid))}"

        findViewById<Button>(R.id.btn_github).setOnClickListener {
            openLink("https://github.com/glodanif/BluetoothChat")
        }

        findViewById<Button>(R.id.btn_crowdin).setOnClickListener {
            openLink("https://crowdin.com/project/bluetoothchat")
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}
