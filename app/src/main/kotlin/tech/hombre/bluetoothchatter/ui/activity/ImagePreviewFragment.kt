package tech.hombre.bluetoothchatter.ui.activity

import android.app.AlertDialog
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.util.ArrayMap
import android.view.*
import androidx.core.view.ViewCompat
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import com.squareup.picasso.Callback
import com.squareup.picasso.Picasso
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.databinding.FragmentImagePreviewBinding
import tech.hombre.bluetoothchatter.ui.presenter.ImagePreviewPresenter
import tech.hombre.bluetoothchatter.ui.view.ImagePreviewView
import java.io.File
import java.lang.ref.WeakReference

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
        setHasOptionsMenu(true)
        (requireActivity() as MainActivity).setSupportActionBar(binding.appBar.tbToolbar)
        binding.appBar.tbToolbar.setTitleTextAppearance(
            requireContext(),
            R.style.ActionBar_TitleTextStyle
        )
        binding.appBar.tbToolbar.setSubtitleTextAppearance(
            requireContext(),
            R.style.ActionBar_SubTitleTextStyle
        )

        binding.imageView.minimumScale = .75f
        binding.imageView.maximumScale = 2f

        presenter.loadImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        postponeEnterTransition()
    }

    override fun displayImage(fileUrl: String) {

        val callback = object : Callback {

            override fun onSuccess() {
                startPostponedEnterTransition()
                //supportStartPostponedEnterTransition()
            }

            override fun onError(e: Exception?) {
                startPostponedEnterTransition()
               // supportStartPostponedEnterTransition()
            }
        }

        Picasso.get()
            .load(fileUrl)
            .config(Bitmap.Config.RGB_565)
            .noFade()
            .into(binding.imageView, callback)
    }

    override fun showFileInfo(name: String, readableSize: String) {
        binding.appBar.tbToolbar.title = name
        binding.appBar.tbToolbar.subtitle = readableSize
    }

    override fun close() {
        findNavController().navigateUp()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        if (!own) {
            inflater.inflate(R.menu.menu_image_preview, menu)
        }
        return super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.action_remove -> {
                confirmFileRemoval()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
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
