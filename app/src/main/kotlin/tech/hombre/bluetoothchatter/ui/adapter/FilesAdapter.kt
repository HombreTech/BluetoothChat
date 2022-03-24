package tech.hombre.bluetoothchatter.ui.adapter

import android.graphics.Bitmap
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.entity.MessageFile
import com.squareup.picasso.Picasso

class FilesAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var clickListener: ((ImageView?, MessageFile) -> Unit)? = null
    var files: ArrayList<MessageFile> = ArrayList()

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val file = files[position]
        when(holder) {
            is ImageViewHolder -> {
                ViewCompat.setTransitionName(holder.thumbnail, file.uid.toString())
                holder.itemView.setOnClickListener { clickListener?.invoke(holder.thumbnail, file) }
                Picasso.get()
                    .load("file://${file.filePath}")
                    .config(Bitmap.Config.RGB_565)
                    .error(R.color.background_image)
                    .placeholder(R.color.background_image)
                    .centerCrop()
                    .fit()
                    .into(holder.thumbnail)
            }
            is FileViewHolder -> {
                holder.itemView.setOnClickListener { clickListener?.invoke(null, file) }
                holder.filename.text = file.filePath?.substringAfterLast("/")
            }
        }

    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = files[position]
        return when (message.messageType) {
            1 -> VIEW_TYPE_IMAGE
            2 -> VIEW_TYPE_FILE
            else -> VIEW_TYPE_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutId = when(viewType) {
            VIEW_TYPE_IMAGE -> R.layout.item_image_grid
            VIEW_TYPE_FILE-> R.layout.item_file_grid
            else -> 0
        }
        val view = LayoutInflater.from(parent.context)
                .inflate(layoutId, parent, false)
        return when(viewType){
            VIEW_TYPE_IMAGE -> ImageViewHolder(view)
            VIEW_TYPE_FILE -> FileViewHolder(view)
            else -> ImageViewHolder(view)
        }
    }

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)  {
        val thumbnail: ImageView = itemView.findViewById(R.id.iv_thumbnail)
    }

    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val filename: TextView = itemView.findViewById(R.id.tv_filename)
    }

    companion object {
        private const val VIEW_TYPE_IMAGE = 1
        private const val VIEW_TYPE_FILE = 2
    }
}
