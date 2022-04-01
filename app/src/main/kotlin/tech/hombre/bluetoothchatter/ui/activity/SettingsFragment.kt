package tech.hombre.bluetoothchatter.ui.activity

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.*
import androidx.navigation.fragment.findNavController
import me.priyesh.chroma.ChromaDialog
import me.priyesh.chroma.ColorMode
import me.priyesh.chroma.ColorSelectListener
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentSettingsBinding
import tech.hombre.bluetoothchatter.ui.presenter.SettingsPresenter
import tech.hombre.bluetoothchatter.ui.util.ThemeHolder
import tech.hombre.bluetoothchatter.ui.view.SettingsView

class SettingsFragment : BaseFragment<FragmentSettingsBinding>(R.layout.fragment_settings),
    SettingsView {

    private val presenter: SettingsPresenter by inject {
        parametersOf(this, requireActivity().application as ThemeHolder)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as MainActivity).setSupportActionBar(binding.appBar.tbToolbar)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(true)
        binding.appBar.tbToolbar.title = getString(R.string.settings__title)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            binding.tvNotificationsHeader.visibility = View.GONE
            binding.spSound.visibility = View.GONE
        } else {
            binding.spSound.listener = { presenter.onNewSoundPreference(it) }
        }

        binding.spClassFilter.listener = { presenter.onNewClassificationPreference(it) }

        binding.spPausePlayerOnHide.listener = { presenter.onNewPausePlayerPreference(it) }

        binding.rlChatBgColorButton.setOnClickListener {
            presenter.prepareColorPicker(requireContext())
        }

        binding.llNightModeButton.setOnClickListener {
            presenter.prepareNightModePicker()
        }

        presenter.loadPreferences(requireContext())
    }

    override fun displayNotificationSetting(sound: Boolean) {
        binding.spSound.setChecked(sound)
    }

    override fun displayBgColorSettings(@ColorInt color: Int) {
        binding.vColor.setBackgroundColor(color)
    }

    override fun displayNightModeSettings(@NightMode nightMode: Int) {
        val modeLabelText = when (nightMode) {
            MODE_NIGHT_YES -> R.string.settings__night_mode_on
            MODE_NIGHT_NO -> R.string.settings__night_mode_off
            MODE_NIGHT_FOLLOW_SYSTEM -> R.string.settings__night_mode_system
            else -> R.string.settings__night_mode_off
        }
        binding.tvNightMode.setText(modeLabelText)
    }

    override fun displayDiscoverySetting(classification: Boolean) {
        binding.spClassFilter.setChecked(classification)
    }

    override fun displayColorPicker(@ColorInt color: Int) {

        ChromaDialog.Builder()
            .initialColor(color)
            .colorMode(ColorMode.RGB)
            .onColorSelected(colorSelectListener)
            .create()
            .show(childFragmentManager, "ChromaDialog")
    }

    override fun displayNightModePicker(nightMode: Int) {

        val items = arrayOf<CharSequence>(
            getString(R.string.settings__night_mode_on),
            getString(R.string.settings__night_mode_off),
            getString(R.string.settings__night_mode_system)
        )

        val modes = arrayOf(MODE_NIGHT_YES, MODE_NIGHT_NO, MODE_NIGHT_FOLLOW_SYSTEM)

        AlertDialog.Builder(requireContext()).apply {
            setSingleChoiceItems(items, modes.indexOf(nightMode)) { dialog, which ->
                presenter.onNewNightModePreference(modes[which])
                dialog.dismiss()
            }
            setNegativeButton(R.string.general__cancel, null)
            setTitle(R.string.settings__night_mode)
        }.show()
    }

    override fun displayPausePlayerOnHide(paused: Boolean) {
        binding.spPausePlayerOnHide.setChecked(paused)
    }

    private val colorSelectListener = object : ColorSelectListener {
        override fun onColorSelected(color: Int) {
            presenter.onNewColorPicked(color)
        }
    }

    override fun onBackPressed() {
        if (presenter.isNightModeChanged()) {
            val nightMode = (requireActivity().application as ThemeHolder).getNightMode()
            setDefaultNightMode(nightMode)
        }
        findNavController().navigateUp()
    }

}
