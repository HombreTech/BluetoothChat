package tech.hombre.bluetoothchatter.ui.activity

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.entity.MessageFile
import tech.hombre.bluetoothchatter.databinding.FragmentReceivedImagesBinding
import tech.hombre.bluetoothchatter.ui.adapter.ImagesAdapter
import tech.hombre.bluetoothchatter.ui.presenter.ReceivedImagesPresenter
import tech.hombre.bluetoothchatter.ui.view.ReceivedImagesView

class ReceivedImagesFragment :
    BaseFragment<FragmentReceivedImagesBinding>(R.layout.fragment_received_images),
    ReceivedImagesView {

    private val args: ReceivedImagesFragmentArgs by navArgs()

    private val address by lazy { args.address ?: "" }

    private val presenter: ReceivedImagesPresenter by inject {
        parametersOf(address, this)
    }

    private var imagesAdapter = ImagesAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBar.tbToolbar.title = getString(R.string.images__received_images)
        binding.rvImages.layoutManager = GridLayoutManager(requireContext(), calculateNoOfColumns())
        binding.rvImages.adapter = imagesAdapter

        imagesAdapter.clickListener = { view, message ->
            findNavController().navigate(ReceivedImagesFragmentDirections.actionReceivedImagesFragmentToImagePreviewFragment(
                messageId = message.uid,
                imagePath = message.filePath,
                own = message.own
            ))
        }

        presenter.loadImages()
    }

    override fun onBackPressed() {
        findNavController().navigateUp()
    }

    override fun displayImages(imageMessages: List<MessageFile>) {
        imagesAdapter.images = ArrayList(imageMessages)
        imagesAdapter.notifyDataSetChanged()
    }

    override fun showNoImages() {
        binding.rvImages.visibility = View.GONE
        binding.tvNoImages.visibility = View.VISIBLE
    }

    private fun calculateNoOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val no =
            displayMetrics.widthPixels / resources.getDimensionPixelSize(R.dimen.thumbnail_width)
        return if (no == 0) 1 else no
    }

}
