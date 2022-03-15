package tech.hombre.bluetoothchat.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import tech.hombre.bluetoothchat.R
import tech.hombre.bluetoothchat.data.entity.MessageFile
import tech.hombre.bluetoothchat.ui.adapter.ImagesAdapter
import tech.hombre.bluetoothchat.ui.presenter.ReceivedImagesPresenter
import tech.hombre.bluetoothchat.ui.view.ReceivedImagesView
import tech.hombre.bluetoothchat.utils.argument
import tech.hombre.bluetoothchat.utils.bind
import org.koin.android.ext.android.inject
import org.koin.core.parameter.parametersOf

class ReceivedImagesActivity : SkeletonActivity(), ReceivedImagesView {

    private val address: String? by argument(EXTRA_ADDRESS)

    private val presenter: ReceivedImagesPresenter by inject {
        parametersOf(address ?: "", this)
    }

    private val imagesGrid: RecyclerView by bind(R.id.rv_images)
    private val noImagesLabel: TextView by bind(R.id.tv_no_images)

    private var imagesAdapter = ImagesAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_received_images, ActivityType.CHILD_ACTIVITY)

        imagesGrid.layoutManager = GridLayoutManager(this, calculateNoOfColumns())
        imagesGrid.adapter = imagesAdapter

        imagesAdapter.clickListener = { view, message ->
            ImagePreviewActivity.start(this, view, message)
        }

        presenter.loadImages()
    }

    override fun displayImages(imageMessages: List<MessageFile>) {
        imagesAdapter.images = ArrayList(imageMessages)
        imagesAdapter.notifyDataSetChanged()
    }

    override fun showNoImages() {
        imagesGrid.visibility = View.GONE
        noImagesLabel.visibility = View.VISIBLE
    }

    private fun calculateNoOfColumns(): Int {
        val displayMetrics = resources.displayMetrics
        val no = displayMetrics.widthPixels / resources.getDimensionPixelSize(R.dimen.thumbnail_width)
        return if (no == 0) 1 else no
    }

    companion object {

        private const val EXTRA_ADDRESS = "extra.address"

        fun start(context: Context, address: String?) {
            val intent = Intent(context, ReceivedImagesActivity::class.java)
                    .putExtra(EXTRA_ADDRESS, address ?: "")
            context.startActivity(intent)
        }
    }
}
