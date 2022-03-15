package tech.hombre.bluetoothchatter.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.utils.bind

class AboutActivity : SkeletonActivity() {

    private val infoLabel: TextView by bind(R.id.tv_app_credentials)
    private val versionLabel: TextView by bind(R.id.tv_version)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about, ActivityType.CHILD_ACTIVITY)

        versionLabel.text = "v${tech.hombre.bluetoothchatter.BuildConfig.VERSION_NAME} / ${tech.hombre.bluetoothchatter.BuildConfig.VERSION_CODE}"
        infoLabel.text = "${getString(R.string.about__app_name, getString(R.string.bl_app_name))}\n" +
                "${getString(R.string.about__app_uuid, getString(R.string.bl_app_uuid))}"

        findViewById<Button>(R.id.btn_github).setOnClickListener {
            openLink("https://github.com/HombreTech/BluetoothChat")
        }
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutActivity::class.java))
        }
    }
}
