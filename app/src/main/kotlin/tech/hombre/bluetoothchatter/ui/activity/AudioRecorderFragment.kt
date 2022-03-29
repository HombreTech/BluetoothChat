package tech.hombre.bluetoothchatter.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentAudioRecorderBinding

class AudioRecorderFragment(
) : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentAudioRecorderBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            FragmentAudioRecorderBinding.bind(
                inflater.inflate(
                    R.layout.fragment_audio_recorder,
                    container
                )
            )
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        with(binding) {

        }
    }

    companion object {
        const val EXTRA_FILE_PATH = "extra.file_path"
    }

}