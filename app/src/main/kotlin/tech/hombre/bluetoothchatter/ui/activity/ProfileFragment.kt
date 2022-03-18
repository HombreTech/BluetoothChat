package tech.hombre.bluetoothchatter.ui.activity

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.amulyakhare.textdrawable.TextDrawable
import me.priyesh.chroma.ChromaDialog
import me.priyesh.chroma.ColorMode
import me.priyesh.chroma.ColorSelectListener
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentProfileBinding
import tech.hombre.bluetoothchatter.ui.presenter.ProfilePresenter
import tech.hombre.bluetoothchatter.ui.util.SimpleTextWatcher
import tech.hombre.bluetoothchatter.ui.view.ProfileView
import tech.hombre.bluetoothchatter.utils.getFirstLetter

class ProfileFragment : BaseFragment<FragmentProfileBinding>(R.layout.fragment_profile),
    ProfileView {

    private val args: ProfileFragmentArgs by navArgs()

    private val editMode: Boolean by lazy { args.editMode }

    private val presenter: ProfilePresenter by inject { parametersOf(this) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycle.addObserver(presenter)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayHomeAsUpEnabled(editMode)
        (requireActivity() as MainActivity).supportActionBar?.setDisplayShowHomeEnabled(editMode)

        if (editMode) {
            binding.appBar.tbToolbar.title = getString(R.string.profile__profile)
            requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)
        }

        binding.vColor.setOnClickListener {
            presenter.prepareColorPicker()
        }

        binding.btnSave.setOnClickListener {
            presenter.saveUser()
        }

        binding.tvDeviceName.setOnClickListener {
            val bluetoothSettings = Intent().apply {
                action = Settings.ACTION_BLUETOOTH_SETTINGS
            }
            try {
                startActivity(bluetoothSettings)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.profile__no_bluetooth_settings_activity),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.etName.addTextChangedListener(textWatcher)
    }

    override fun onStop() {
        super.onStop()
        binding.etName.removeTextChangedListener(textWatcher)
        hideKeyboard()
    }

    override fun showDeviceName(name: String?) {
        binding.tvDeviceName.text = name ?: getString(R.string.profile__no_name)
    }

    override fun showUserData(name: String, color: Int) {
        binding.tvName.text = if (name.isEmpty()) getString(R.string.profile__your_name) else name
        binding.tvName.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (name.isEmpty()) R.color.text_light else R.color.text_dark
            )
        )
        val drawable = TextDrawable.builder().buildRound(name.getFirstLetter(), color)
        binding.ivAvatar.setImageDrawable(drawable)
        binding.vColor.setBackgroundColor(color)
    }

    override fun showColorPicker(@ColorInt color: Int) {

        ChromaDialog.Builder()
            .initialColor(color)
            .colorMode(ColorMode.RGB)
            .onColorSelected(colorSelectListener)
            .create()
            .show(childFragmentManager, "ChromaDialog")
    }

    override fun redirectToConversations() {
         if (!editMode) {
             findNavController().navigate(R.id.conversationsFragment, arguments)
         } else {
             findNavController().navigateUp()
         }
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

    override fun prefillUsername(name: String) {
        binding.etName.setText(name)
    }

    override fun showNotValidNameError(divider: String) {
        binding.etName.error = getString(R.string.profile__validation_error, divider)
    }

    private val textWatcher = object : SimpleTextWatcher() {
        override fun afterTextChanged(text: String) {
            binding.etName.error = null
            presenter.onNameChanged(text)
        }
    }

    private val colorSelectListener = object : ColorSelectListener {
        override fun onColorSelected(color: Int) {
            presenter.onColorPicked(color)
        }
    }

}
