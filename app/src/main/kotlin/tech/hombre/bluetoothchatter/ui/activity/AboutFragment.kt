package tech.hombre.bluetoothchatter.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentAboutBinding

class AboutFragment : BaseFragment<FragmentAboutBinding>(R.layout.fragment_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.tvVersion.text =
            "v${tech.hombre.bluetoothchatter.BuildConfig.VERSION_NAME} / ${tech.hombre.bluetoothchatter.BuildConfig.VERSION_CODE}"
        binding.tvAppCredentials.text =
            "${getString(R.string.about__app_name, getString(R.string.bl_app_name))}\n" +
                    "${getString(R.string.about__app_uuid, getString(R.string.bl_app_uuid))}"

        binding.btnGithub.setOnClickListener {
            openLink("https://github.com/HombreTech/BluetoothChat")
        }
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AboutFragment::class.java))
        }
    }
}
