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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersAdapter
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.data.service.message.PayloadType
import tech.hombre.bluetoothchatter.ui.util.ClickableMovementMethod
import tech.hombre.bluetoothchatter.ui.view.AudioPlayerView
import tech.hombre.bluetoothchatter.ui.viewmodel.ChatMessageViewModel
import tech.hombre.bluetoothchatter.ui.widget.voiceplayerview.VoicePlayerView
import tech.hombre.bluetoothchatter.utils.setViewBackgroundWithoutResettingPadding
import java.util.*

class ChatAdapter(private val nickname: String, private val isAlwaysSelectable: Boolean = false) :
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

    var replyClickListener: ((uid: Long) -> Unit)? =
        null

    private var isSelectableMode = isAlwaysSelectable

    private val selectedItemPositions = mutableSetOf<Int>()

    private var audioPlayerListener: AudioPlayerView? = null

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is ImageMessageViewHolder -> {

                holder.messageView.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (message.replyMessage != null) {
                    holder.replyNickname.text = nickname
                    holder.replyText.text = holder.itemView.context.getString(R.string.chat__image_message, "\uD83D\uDDBC")
                    holder.replyLayout.setOnClickListener {
                        replyClickListener?.invoke(message.replyMessage.uid)
                    }
                    holder.replyLayout.isVisible = true
                } else {
                    holder.replyLayout.setOnClickListener(null)
                    holder.replyLayout.isVisible = false
                }

                if (!message.isImageAvailable) {

                    holder.image.visibility = View.GONE
                    holder.missingLabel.visibility = View.VISIBLE
                    holder.missingLabel.setText(message.imageProblemText)

                } else {

                    holder.image.visibility = View.VISIBLE
                    holder.missingLabel.visibility = View.GONE

                    ViewCompat.setTransitionName(holder.image, message.uid.toString())

                    val size = message.imageSize
                    //holder.image.layoutParams = FrameLayout.LayoutParams(size.width, size.height)
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
                        .into(holder.image)
                }

                holder.date.text = message.time

                if (message.own) {
                    holder.state.setImageResource(
                        getMessageStateDrawableId(
                            message.delivered,
                            message.seenThere
                        )
                    )
                }

            }
            is FileMessageViewHolder -> {

                holder.messageView.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (message.replyMessage != null) {
                    holder.replyNickname.text = nickname
                    holder.replyText.text = holder.itemView.context.getString(R.string.chat__image_file, "\uD83D\uDCCE")
                    holder.replyLayout.setOnClickListener {
                        replyClickListener?.invoke(message.replyMessage.uid)
                    }
                    holder.replyLayout.isVisible = true
                } else {
                    holder.replyLayout.setOnClickListener(null)
                    holder.replyLayout.isVisible = false
                }

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

                if (message.own) {
                    holder.state.setImageResource(
                        getMessageStateDrawableId(
                            message.delivered,
                            message.seenThere
                        )
                    )
                }
            }
            is AudioMessageViewHolder -> {

                holder.messageView.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (message.replyMessage != null) {
                    holder.replyNickname.text = nickname
                    holder.replyText.text = holder.itemView.context.getString(R.string.chat__image_audio, "\uD83C\uDFA7")
                    holder.replyLayout.setOnClickListener {
                        replyClickListener?.invoke(message.replyMessage.uid)
                    }
                    holder.replyLayout.isVisible = true
                } else {
                    holder.replyLayout.setOnClickListener(null)
                    holder.replyLayout.isVisible = false
                }

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
                        holder.playerView.onPlayClick = {
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
                    holder.playerView.visibility = View.VISIBLE
                    holder.missingLabel.visibility = View.GONE
                    holder.date.text = message.time

                    holder.itemView.setOnClickListener {
                        if (!isSelectableMode && !isAlwaysSelectable) {
                            return@setOnClickListener
                        } else {
                            if (isSelectedItem(position)) removeSelectedItem(position)
                            else addSelectedItem(position)

                            onBindViewHolder(holder, position)
                        }
                    }
                }

                if (message.own) {
                    holder.state.setImageResource(
                        getMessageStateDrawableId(
                            message.delivered,
                            message.seenThere
                        )
                    )
                }
            }
            is TextMessageViewHolder -> {

                holder.messageView.setViewBackgroundWithoutResettingPadding(
                    getBackground(
                        message.own,
                        position
                    )
                )

                if (message.replyMessage != null) {
                    holder.replyNickname.text = nickname
                    val text = when (message.replyMessage.messageType) {
                        PayloadType.FILE -> {
                            holder.itemView.context.getString(R.string.chat__image_file, "\uD83D\uDCCE")
                        }
                        PayloadType.AUDIO -> {
                            holder.itemView.context.getString(R.string.chat__image_audio, "\uD83C\uDFA7")
                        }
                        PayloadType.IMAGE -> {
                            holder.itemView.context.getString(R.string.chat__image_message, "\uD83D\uDDBC")
                        }
                        else -> {
                            message.replyMessage.text
                        }
                    }
                    holder.replyText.text = text
                    holder.replyLayout.setOnClickListener {
                        replyClickListener?.invoke(message.replyMessage.uid)
                    }
                    holder.replyLayout.isVisible = true
                } else {
                    holder.replyLayout.setOnClickListener(null)
                    holder.replyLayout.isVisible = false
                }

                val spannableMessage = SpannableString(message.text)
                LinkifyCompat.addLinks(
                    spannableMessage,
                    Linkify.WEB_URLS or Linkify.PHONE_NUMBERS or Linkify.EMAIL_ADDRESSES
                )

                holder.text.movementMethod = ClickableMovementMethod
                holder.text.setText(spannableMessage, TextView.BufferType.SPANNABLE)
                holder.date.text = message.time

                holder.itemView.setOnClickListener {
                    if (!isSelectableMode && !isAlwaysSelectable) {
                        return@setOnClickListener
                    } else {
                        if (isSelectedItem(position)) removeSelectedItem(position)
                        else addSelectedItem(position)

                        onBindViewHolder(holder, position)
                    }
                }

                if (message.own) {
                    holder.state.setImageResource(
                        getMessageStateDrawableId(
                            message.delivered,
                            message.seenThere
                        )
                    )
                }
            }
        }

        holder.itemView.setOnLongClickListener {
            val pos = holder.bindingAdapterPosition
            if (isSelectedItem(pos)) removeSelectedItem(pos)
            else addSelectedItem(pos)

            onBindViewHolder(holder, pos)
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

    private fun getMessageStateDrawableId(isDelivered: Boolean, isSeen: Boolean): Int {
        return when {
            isSeen -> R.drawable.ic_check_double
            !isSeen && isDelivered -> R.drawable.ic_check
            isDelivered -> R.drawable.ic_check
            else -> R.drawable.ic_error
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
        audioPlayerListener?.stopAudio()
    }

    fun pauseAudio() {
        audioPlayerListener?.pauseAudio()
    }

    fun setMessageAsDelivered(id: Long) {
        val position = messages.indexOfFirst { it.uid == id }
        messages.lastOrNull { it.uid == id }?.delivered = true
        notifyItemChanged(position, 0)
    }

    fun setMessageAsNotDelivered(id: Long) {
        val position = messages.indexOfFirst { it.uid == id }
        messages.lastOrNull { it.uid == id }?.delivered = false
        notifyItemChanged(position, 0)
    }

    fun setMessageAsSeen(id: Long) {
        val position = messages.indexOfFirst { it.uid == id }
        messages.lastOrNull { it.uid == id }?.seenThere = true
        notifyItemChanged(position, 0)
    }

    fun getMessagePositionById(uid: Long): Int {
        val message = messages.find { it.uid == uid }
        return if (message == null) {
            -1
        } else {
            messages.indexOf(message)
        }
    }

    class DateDividerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
    }

    class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val text: TextView = itemView.findViewById(R.id.tv_text)
        val messageView: View = itemView.findViewById(R.id.messageView)
        val state: ImageView = itemView.findViewById(R.id.state)
        val replyLayout: View = itemView.findViewById(R.id.reply_layout)
        val replyNickname: TextView = itemView.findViewById(R.id.reply_nickname)
        val replyText: TextView = itemView.findViewById(R.id.reply_text)
    }

    class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val image: ImageView = itemView.findViewById(R.id.iv_image)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val container: FrameLayout = itemView.findViewById(R.id.container)
        val messageView: View = itemView.findViewById(R.id.messageView)
        val state: ImageView = itemView.findViewById(R.id.state)
        val replyLayout: View = itemView.findViewById(R.id.reply_layout)
        val replyNickname: TextView = itemView.findViewById(R.id.reply_nickname)
        val replyText: TextView = itemView.findViewById(R.id.reply_text)
    }

    class FileMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val image: ImageView = itemView.findViewById(R.id.iv_image)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val label: TextView = itemView.findViewById(R.id.tv_label_file)
        val container: FrameLayout = itemView.findViewById(R.id.container)
        val messageView: View = itemView.findViewById(R.id.messageView)
        val state: ImageView = itemView.findViewById(R.id.state)
        val replyLayout: View = itemView.findViewById(R.id.reply_layout)
        val replyNickname: TextView = itemView.findViewById(R.id.reply_nickname)
        val replyText: TextView = itemView.findViewById(R.id.reply_text)
    }

    class AudioMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val date: TextView = itemView.findViewById(R.id.tv_date)
        val missingLabel: TextView = itemView.findViewById(R.id.tv_missing_file)
        val playerView: VoicePlayerView = itemView.findViewById(R.id.audioPlayerView)
        val container: FrameLayout = itemView.findViewById(R.id.container)
        val messageView: View = itemView.findViewById(R.id.messageView)
        val state: ImageView = itemView.findViewById(R.id.state)
        val replyLayout: View = itemView.findViewById(R.id.reply_layout)
        val replyNickname: TextView = itemView.findViewById(R.id.reply_nickname)
        val replyText: TextView = itemView.findViewById(R.id.reply_text)
    }
}
