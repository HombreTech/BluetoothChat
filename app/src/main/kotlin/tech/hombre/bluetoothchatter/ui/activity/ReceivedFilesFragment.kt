package tech.hombre.bluetoothchatter.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.core.content.FileProvider
import androidx.core.view.doOnPreDraw
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.entity.MessageFile
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.databinding.FragmentReceivedFilesBinding
import tech.hombre.bluetoothchatter.ui.adapter.FilesAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ReceivedImagesPresenter
import tech.hombre.bluetoothchatter.ui.view.ReceivedImagesView
import java.io.File


class ReceivedFilesFragment :
    BaseFragment<FragmentReceivedFilesBinding>(R.layout.fragment_received_files),
    ReceivedImagesView {

    private val args: ReceivedFilesFragmentArgs by navArgs()

    private val address by lazy { args.address ?: "" }

    private val presenter: ReceivedImagesPresenter by inject {
        parametersOf(address, this)
    }

    private var filesAdapter = FilesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()

        binding.appBar.tbToolbar.title = getString(R.string.chat__received_files)
        binding.rvFiles.layoutManager = GridLayoutManager(requireContext(), calculateNoOfColumns())
        binding.rvFiles.adapter = filesAdapter

        binding.rvFiles.doOnPreDraw {
            binding.rvFiles.postDelayed({
                startPostponedEnterTransition()
            }, 100)
        }

        filesAdapter.clickListener = { view, file ->
            if (file.messageType == PayloadType.IMAGE.value) {
                val extras = FragmentNavigatorExtras(
                    view!! to file.uid.toString()
                )
                findNavController().navigate(
                    ReceivedFilesFragmentDirections.actionReceivedImagesFragmentToImagePreviewFragment(
                        messageId = file.uid,
                        imagePath = file.filePath,
                        own = file.own
                    ), navigatorExtras = extras
                )
            } else {
                with(Intent(Intent.ACTION_VIEW)) {
                    val uri = FileProvider.getUriForFile(context!!, requireContext().packageName + ".provider", File(file.filePath))
                    data = uri
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    startActivity(Intent.createChooser(this, getString(R.string.files__open_chooser)))
                }
            }
        }

        presenter.loadImages()
    }

    override fun onBackPressed() {
        findNavController().popBackStack()
    }

    override fun displayFiles(messages: List<MessageFile>) {
        filesAdapter.files = ArrayList(messages)
        filesAdapter.notifyDataSetChanged()
    }

    override fun showNoFiles() {
        binding.rvFiles.visibility = View.GONE
        binding.tvNoFiles.visibility = View.VISIBLE
    }

    private fun calculateNoOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val no =
            displayMetrics.widthPixels / resources.getDimensionPixelSize(R.dimen.thumbnail_width)
        return if (no == 0) 1 else no
    }

}
