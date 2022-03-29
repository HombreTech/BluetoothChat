package tech.hombre.bluetoothchatter.ui.adapter

import android.graphics.Bitmap
import android.text.SpannableString
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.util.LinkifyCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import me.jagar.chatvoiceplayerlibrary.VoicePlayerView
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.ui.util.ClickableMovementMethod
import tech.hombre.bluetoothchatter.ui.view.AudioPlayerView
import tech.hombre.bluetoothchatter.ui.viewmodel.ChatMessageViewModel
import tech.hombre.bluetoothchatter.utils.setViewBackgroundWithoutResettingPadding
import java.util.*

class ChatAdapter(private val isAlwaysSelectable: Boolean = false) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>(),
    StickyRecyclerHeadersAdapter<RecyclerView.ViewHolder> {

    companion object {
        private const val OWN_TEXT_MESSAGE = 0
        private const val OWN_IMAGE_MESSAGE = 1
        private const val FOREIGN_TEXT_MESSAGE = 2
        private const val FOREIGN_IMAGE_MESSAGE = 3
        private const val OWN_FILE_MESSAGE = 4
        private const val FOREIGN_FILE_MESSAGE = 5
        private const val FOREIGN_AUDIO_MESSAGE = 6
        private const val OWN_AUDIO_MESSAGE = 7
    }

    val picassoTag = Object()

    var messages = LinkedList<ChatMessageViewModel>()

    var imageClickListener: ((view: ImageView, message: ChatMessageViewModel) -> Unit)? = null

    var messageSelectionListener: ((selectedItemPositions: Set<Int>, isSelectableMode: Boolean) -> Unit)? =
        null

    private var isSelectableMode = isAlwaysSelectable

    private val selectedItemPositions = mutableSetOf<Int>()

    private lateinit var audioPlayerListener: AudioPlayerView

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        val message = messages[position]

        when (holder) {
            is ImageMessageViewHolder -> {

                holder.container.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (!message.isImageAvailable) {

                    holder.image.visibility = View.GONE
                    holder.missingLabel.visibility = View.VISIBLE
                    holder.missingLabel.setText(message.imageProblemText)

                } else {

                    holder.image.visibility = View.VISIBLE
                    holder.missingLabel.visibility = View.GONE

                    ViewCompat.setTransitionName(holder.image, message.uid.toString())

                    val size = message.imageSize
                    holder.image.layoutParams = FrameLayout.LayoutParams(size.width, size.height)
                    holder.itemView.setOnClickListener {
                        if (!isSelectableMode && !isAlwaysSelectable) {
                            return@setOnClickListener
                        } else {
                            if (isSelectedItem(position)) removeSelectedItem(position)
                            else addSelectedItem(position)

                            onBindViewHolder(holder, position)
                        }
                    }
                    holder.image.setOnClickListener {
                        if (!isSelectableMode && !isAlwaysSelectable) {
                            imageClickListener?.invoke(holder.image, message)
                        } else {
                            if (isSelectedItem(position)) removeSelectedItem(position)
                            else addSelectedItem(position)

                            onBindViewHolder(holder, position)
                        }
                    }
                    holder.image.setOnLongClickListener {
                        holder.itemView.performLongClick()
                    }
                    Picasso.get()
                        .load(message.fileUri)
                        .config(Bitmap.Config.RGB_565)
                        .error(R.color.background_image)
                        .placeholder(R.color.background_image)
                        .tag(picassoTag)
                        .resize(size.width, size.height)
                        .into(holder.image)
                }

                holder.date.text = message.time

            }
            is FileMessageViewHolder -> {

                holder.container.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (!message.isImageAvailable) {
                    holder.image.visibility = View.GONE
                    holder.missingLabel.visibility = View.VISIBLE
                    holder.missingLabel.setText(message.imageProblemText)
                } else {
                    holder.image.visibility = View.VISIBLE
                    holder.missingLabel.visibility = View.GONE
                    holder.label.text = StringBuilder()
                        .append(message.filePath?.substringAfterLast("/"))
                        .appendLine()
                        .append(message.fileSize.getFileSize())

                    holder.date.text = message.time
                    holder.itemView.setOnClickListener {
                        if (!isSelectableMode && !isAlwaysSelectable) {
                            imageClickListener?.invoke(holder.image, message)
                        } else {
                            if (isSelectedItem(position)) removeSelectedItem(position)
                            else addSelectedItem(position)

                            onBindViewHolder(holder, position)
                        }
                    }
                }
            }
            is AudioMessageViewHolder -> {

                holder.container.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (!message.isImageAvailable) {
                    holder.playerView.visibility = View.GONE
                    holder.missingLabel.visibility = View.VISIBLE
                    holder.missingLabel.setText(message.imageProblemText)
                } else {
                    if (holder.playerView.mediaPlayer != null &&
                        holder.playerView.progressBar.progress > 0
                    ) {
                        holder.playerView.imgPlay.performClick()
                    } else {
                        holder.playerView.setAudio(message.filePath)
                    }
                    holder.playerView.visibility = View.VISIBLE
                    holder.missingLabel.visibility = View.GONE

                    holder.date.text = message.time
                    holder.itemView.setOnClickListener {

                    }

                    audioPlayerListener = object : AudioPlayerView {
                        override fun pauseAudio() {
                            holder.playerView.onPause()
                        }

                        override fun stopAudio() {
                            holder.playerView.onStop()
                        }
                    }
                }
            }
            is TextMessageViewHolder -> {

                holder.text.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                val spannableMessage = SpannableString(message.text)
                LinkifyCompat.addLinks(
                    spannableMessage,
                    Linkify.WEB_URLS or Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES
                )

                holder.text.movementMethod = ClickableMovementMethod
                holder.text.setText(spannableMessage, TextView.BufferType.SPANNABLE)
                holder.date.text = message.time

                holder.text.setOnClickListener {
                    if (!isSelectableMode && !isAlwaysSelectable) {
                        return@setOnClickListener
                    } else {
                        if (isSelectedItem(position)) removeSelectedItem(position)
                        else addSelectedItem(position)

                        onBindViewHolder(holder, position)
                    }
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            if (isSelectedItem(position)) removeSelectedItem(position)
            else addSelectedItem(position)

            onBindViewHolder(holder, position)
            true
        }
    }

    private fun getBackground(own: Boolean, position: Int): Int {
        return when {
            own -> {
                if (isSelectedItem(position))
                    R.drawable.out_message_checked
                else
                    R.drawable.out_message
            }
            else -> {
                if (isSelectedItem(position))
                    R.drawable.inner_message_checked
                else
                    R.drawable.inner_message
            }
        }
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (messages[position].own) {
            when (message.type) {
                PayloadType.IMAGE -> OWN_IMAGE_MESSAGE
                PayloadType.FILE -> OWN_FILE_MESSAGE
                PayloadType.AUDIO -> OWN_AUDIO_MESSAGE
                else -> OWN_TEXT_MESSAGE
            }
        } else {
            when (message.type) {
                PayloadType.IMAGE -> FOREIGN_IMAGE_MESSAGE
                PayloadType.FILE -> FOREIGN_FILE_MESSAGE
                PayloadType.AUDIO -> FOREIGN_AUDIO_MESSAGE
                else -> FOREIGN_TEXT_MESSAGE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val layoutId = when (viewType) {
            OWN_TEXT_MESSAGE -> R.layout.item_message_text_own
            OWN_IMAGE_MESSAGE -> R.layout.item_message_image_own
            FOREIGN_TEXT_MESSAGE -> R.layout.item_message_text_foreign
            FOREIGN_IMAGE_MESSAGE -> R.layout.item_message_image_foreign
            OWN_FILE_MESSAGE -> R.layout.item_message_file_own
            FOREIGN_FILE_MESSAGE -> R.layout.item_message_file_foreign
            FOREIGN_AUDIO_MESSAGE -> R.layout.item_message_audio_foreign
            OWN_AUDIO_MESSAGE -> R.layout.item_message_audio_own
            else -> 0
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)

        return when (viewType) {
            OWN_IMAGE_MESSAGE, FOREIGN_IMAGE_MESSAGE -> ImageMessageViewHolder(view)
            OWN_FILE_MESSAGE, FOREIGN_FILE_MESSAGE -> FileMessageViewHolder(view)
            FOREIGN_AUDIO_MESSAGE, OWN_AUDIO_MESSAGE -> AudioMessageViewHolder(view)
            else -> TextMessageViewHolder(view)
        }
    }

    override fun getHeaderId(position: Int) = messages[position].dayOfYearRaw

    override fun onCreateHeaderViewHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_date_divider, parent, false)
        return DateDividerViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as DateDividerViewHolder).date.text = messages[position].dayOfYear
    }

    fun getSelectedItemPositions() = selectedItemPositions.toSet()

    private fun isSelectedItem(position: Int): Boolean = (selectedItemPositions.contains(position))

    private fun addSelectedItem(position: Int) {
        if (selectedItemPositions.isEmpty() && !isAlwaysSelectable) {
            isSelectableMode = true
        }
        selectedItemPositions.add(position)
        messageSelectionListener?.invoke(getSelectedItemPositions(), isSelectableMode)
    }

    private fun removeSelectedItem(position: Int) {
        selectedItemPositions.remove(position)
        if (selectedItemPositions.isEmpty() && !isAlwaysSelectable) {
            isSelectableMode = false
        }
        messageSelectionListener?.invoke(getSelectedItemPositions(), isSelectableMode)
    }

    fun resetSelections() {
        val oldSelections = selectedItemPositions.toList()
        selectedItemPositions.clear()
        isSelectableMode = false
        oldSelections.forEach {
            notifyItemChanged(it)
        }
        messageSelectionListener?.invoke(emptySet(), isSelectableMode)
    }

    fun stopAudio() {
        audioPlayerListener.stopAudio()
    }

    fun pauseAudio() {
        audioPlayerListener.pauseAudio()
    }

    class DateDividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
    }

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val text: TextView = itemView.findViewById(R.id.tv_text)
    }

    class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val image: ImageView = itemView.findViewById(R.id.iv_image)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val container: FrameLayout = itemView.findViewById(R.id.container)
    }

    class FileMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val image: ImageView = itemView.findViewById(R.id.iv_image)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val label: TextView = itemView.findViewById(R.id.tv_label_file)
        val container: FrameLayout = itemView.findViewById(R.id.container)
    }

    class AudioMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val playerView: VoicePlayerView = itemView.findViewById(R.id.audioPlayerView)
        val container: FrameLayout = itemView.findViewById(R.id.container)
    }
}
