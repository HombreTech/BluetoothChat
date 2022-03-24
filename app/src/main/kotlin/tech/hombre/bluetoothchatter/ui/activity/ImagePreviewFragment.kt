package tech.hombre.bluetoothchatter.ui.activity

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentImagePreviewBinding
import tech.hombre.bluetoothchatter.ui.presenter.ImagePreviewPresenter
import tech.hombre.bluetoothchatter.ui.view.ImagePreviewView
import java.io.File

class ImagePreviewFragment :
    BaseFragment<FragmentImagePreviewBinding>(R.layout.fragment_image_preview), ImagePreviewView {

    private val args: ImagePreviewFragmentArgs by navArgs()

    val messageId: Long by lazy { args.messageId }
    private val imagePath: String? by lazy { args.imagePath }
    private val own: Boolean by lazy { args.own }

    private val presenter: ImagePreviewPresenter by inject {
        parametersOf(messageId, File(imagePath), this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setTransitionName(binding.imageView, messageId.toString())
        if (!own) {
            binding.btnDelete.setOnClickListener {
                confirmFileRemoval()
            }
            binding.btnDelete.isVisible = true
        }
        binding.imageView.minimumScale = .75f
        binding.imageView.maximumScale = 2f

        presenter.loadImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val set = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.change_image_transition)
        sharedElementEnterTransition = set
        postponeEnterTransition()
    }

    override fun displayImage(fileUrl: String) {

        val callback = object : Callback {

            override fun onSuccess() {
                startPostponedEnterTransition()
            }

            override fun onError(e: Exception?) {
                startPostponedEnterTransition()
            }
        }

        Picasso.get()
            .load(fileUrl)
            .config(Bitmap.Config.RGB_565)
            .noFade()
            .into(binding.imageView, callback)
    }

    override fun showFileInfo(name: String, readableSize: String) {
        binding.tvFilename.text = name
        binding.tvFileSize.text = readableSize
    }

    override fun close() {
        findNavController().popBackStack()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!own) {
            inflater.inflate(R.menu.menu_image_preview, menu)
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    private fun confirmFileRemoval() {
        AlertDialog.Builder(requireContext())
            .setMessage(getString(R.string.images__removal_confirmation))
            .setPositiveButton(getString(R.string.general__yes)) { _, _ ->
                presenter.removeFile()
            }
            .setNegativeButton(getString(R.string.general__no), null)
            .show()
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

}
