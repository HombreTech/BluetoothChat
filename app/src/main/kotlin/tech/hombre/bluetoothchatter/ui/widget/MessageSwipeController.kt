package tech.hombre.bluetoothchatter.ui.widget


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.*
import androidx.recyclerview.widget.RecyclerView
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.ui.adapter.ChatAdapter
import tech.hombre.bluetoothchatter.utils.toPx
import kotlin.math.abs

class MessageSwipeController(
    private val context: Context,
    private val swipeControllerInterface: SwipeControllerInterface
) :
    ItemTouchHelper.Callback() {

    private lateinit var imageDrawable: Drawable

    private var currentItemViewHolder: RecyclerView.ViewHolder? = null
    private lateinit var mView: View
    private var dX = 0f

    private var replyButtonProgress: Float = 0.toFloat()
    private var lastReplyButtonAnimationTime: Long = 0
    private var swipeBack = false
    private var startTracking = false
    private var hasReplyFeedback = false

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val selectionModeEnabled =
            (recyclerView.adapter as ChatAdapter).getSelectedItemPositions().isNotEmpty()
        mView = viewHolder.itemView
        imageDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_reply_message)!!
        return if (!selectionModeEnabled) makeMovementFlags(
            ACTION_STATE_IDLE,
            LEFT
        )
        else makeMovementFlags(0, 0)
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        return false
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

    override fun convertToAbsoluteDirection(flags: Int, layoutDirection: Int): Int {
        if (swipeBack) {
            swipeBack = false
            return 0
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        if (actionState == ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder)
        }

        if (abs(mView.translationX) < convertTodp(64) || abs(dX) < this.dX) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            this.dX = abs(dX)
            startTracking = true
        }
        currentItemViewHolder = viewHolder
        drawReplyButton(c)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setTouchListener(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        recyclerView.setOnTouchListener { _, event ->
            swipeBack =
                event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_UP
            if (swipeBack) {
                hasReplyFeedback = false
                if (abs(mView.translationX) >= this@MessageSwipeController.convertTodp(64)) {
                    swipeControllerInterface.onReplyMessage(viewHolder.absoluteAdapterPosition)
                }
            }
            false
        }
    }

    @Suppress("LongMethod")
    private fun drawReplyButton(canvas: Canvas) {
        if (currentItemViewHolder == null) {
            return
        }
        val translationX = abs(mView.translationX)
        val newTime = System.currentTimeMillis()
        val dt = Math.min(17, newTime - lastReplyButtonAnimationTime)
        lastReplyButtonAnimationTime = newTime
        val showing = translationX >= convertTodp(30)
        println("showing $showing")
        if (showing) {
            if (replyButtonProgress < 1.0f) {
                replyButtonProgress += dt / 180.0f
                if (replyButtonProgress > 1.0f) {
                    replyButtonProgress = 1.0f
                } else {
                    mView.invalidate()
                }
            }
        } else if (translationX <= 0.0f) {
            replyButtonProgress = 0f
            startTracking = false
        } else {
            if (replyButtonProgress > 0.0f) {
                replyButtonProgress -= dt / 180.0f
                if (replyButtonProgress < 0.1f) {
                    replyButtonProgress = 0f
                } else {
                    mView.invalidate()
                }
            }
        }
        if (replyButtonProgress == 1f && !hasReplyFeedback) {
            hasReplyFeedback = true
            mView.performHapticFeedback(
                HapticFeedbackConstants.VIRTUAL_KEY,
                HapticFeedbackConstants.FLAG_IGNORE_VIEW_SETTING
            )
        } else if (replyButtonProgress == 0f) {
            hasReplyFeedback = false
        }
        val alpha: Int
        val scale: Float
        if (showing) {
            scale = if (replyButtonProgress <= 0.8f) {
                1.2f * (replyButtonProgress / 0.8f)
            } else {
                1.2f - 0.2f * ((replyButtonProgress - 0.8f) / 0.2f)
            }
            alpha = Math.min(255f, 255 * (replyButtonProgress / 0.8f)).toInt()
        } else {
            scale = replyButtonProgress
            alpha = Math.min(255f, 255 * replyButtonProgress).toInt()
        }

        imageDrawable.alpha = alpha

        val x: Int = if (translationX > convertTodp(64)) {
            mView.width - convertTodp(64) / 2
        } else {
            mView.width - (translationX / 2).toInt()
        }

        val y = (mView.top + mView.measuredHeight / 2).toFloat()

        imageDrawable.setBounds(
            (x - convertTodp(12) * scale).toInt(),
            (y - convertTodp(12) * scale).toInt(),
            (x + convertTodp(12) * scale).toInt(),
            (y + convertTodp(12) * scale).toInt()
        )
        imageDrawable.draw(canvas)
        imageDrawable.alpha = 255
    }

    private fun convertTodp(pixel: Int): Int {
        return pixel.toPx()
    }
}
