package tech.hombre.bluetoothchatter.ui.activity

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentAudioRecorderBinding
import tech.hombre.bluetoothchatter.ui.presenter.AudioRecorderPresenter
import tech.hombre.bluetoothchatter.ui.view.AudioRecorderView
import tech.hombre.bluetoothchatter.utils.convertMsToHMmSs
import tech.hombre.bluetoothchatter.utils.setResultOk


class AudioRecorderFragment : BottomSheetDialogFragment(), AudioRecorderView {

    private lateinit var binding: FragmentAudioRecorderBinding

    private val presenter: AudioRecorderPresenter by inject { parametersOf(this) }

    private var isRecordingEnded = false

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            buttonSend.setOnClickListener {
                isRecordingEnded = true
                presenter.stopRecording()
            }
        }
        presenter.startRecording()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!isRecordingEnded) {
            presenter.cancelRecording()
        }
    }

    override fun onRecordingStarted() {
        with(binding) {
            buttonSend.animate().rotation(360f).setDuration(250).start()
            ObjectAnimator.ofPropertyValuesHolder(
                aivAudio,
                PropertyValuesHolder.ofFloat("scaleX", 0.85f),
                PropertyValuesHolder.ofFloat("scaleY", 0.85f)
            ).apply {
                duration = 500
                repeatCount = ObjectAnimator.INFINITE
                repeatMode = ObjectAnimator.REVERSE
                start()
            }
        }
    }

    override fun onRecordingStopped(filepath: String) {
        setResultOk(
            EXTRA_FILE_PATH to filepath
        )
        dismiss()
    }

    override fun onRecordingError(error: String) {
        Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
        binding.atvRecordingComment.text = error
    }

    override fun onRecordingDurationUpdate(duration: Long) {
        binding.tvDuration.text = duration.convertMsToHMmSs()
    }

    companion object {
        const val EXTRA_FILE_PATH = "extra.file_path"
    }

}