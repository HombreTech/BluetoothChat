package tech.hombre.bluetoothchatter.ui.widget

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewAnimationUtils
import android.view.WindowManager
import android.widget.PopupWindow
import tech.hombre.bluetoothchatter.R
import tech.hombre.bluetoothchatter.ui.util.EmptyAnimatorListener
import tech.hombre.bluetoothchatter.utils.getLayoutInflater

class SendFilePopup(context: Context) : PopupWindow() {

    enum class Option {
        IMAGES;
    }

    private val appearingAnimationDuration = 200L

    private var clickListener: ((Option) -> (Unit))? = null

    private var rootView: View
    private var container: View

    private var isDismissing: Boolean = false

    fun setOnOptionClickListener(clickListener: (Option) -> (Unit)) {
        this.clickListener = clickListener
    }

    init {

        @SuppressLint("InflateParams")
        rootView = context.getLayoutInflater().inflate(R.layout.popup_send_files, null)
        container = rootView.findViewById(R.id.fl_container)

        rootView.findViewById<View>(R.id.ll_images_button).setOnClickListener {
            dismiss()
            clickListener?.invoke(Option.IMAGES)
        }

        contentView = rootView
    }

    fun show(anchor: View) {

        prepare()

        populateUi()

        val xPosition: Int
        val yPosition: Int

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)

        val anchorRect = Rect(
            location[0], location[1],
            location[0] + anchor.width, location[1] + anchor.height
        )

        xPosition = anchorRect.right + rootView.measuredWidth
        yPosition = (anchorRect.top * 0.97f).toInt()

        showAtLocation(anchor, Gravity.NO_GRAVITY, xPosition, yPosition)

        container.post {
            if (container.isAttachedToWindow) {
                val animator = ViewAnimationUtils.createCircularReveal(
                    container,
                    container.width, 0, 0f, container.measuredWidth.toFloat()
                )
                container.visibility = View.VISIBLE
                animator.duration = appearingAnimationDuration
                animator.start()
            }
        }
    }

    override fun dismiss() {

        val animator = ViewAnimationUtils.createCircularReveal(
            container,
            container.width, 0, container.measuredWidth.toFloat(), 0f
        )
        container.visibility = View.VISIBLE
        animator.addListener(object : EmptyAnimatorListener() {

            override fun onAnimationStart(animation: Animator?) {
                isDismissing = true
            }

            override fun onAnimationEnd(animation: Animator?) {
                container.visibility = View.INVISIBLE
                actualDismiss()
            }
        })
        animator.duration = appearingAnimationDuration
        animator.start()
    }

    private fun actualDismiss() {
        isDismissing = false
        super.dismiss()
    }

    private fun prepare() {
        setBackgroundDrawable(ColorDrawable())
        width = WindowManager.LayoutParams.WRAP_CONTENT
        height = WindowManager.LayoutParams.WRAP_CONTENT
        isTouchable = true
        isFocusable = true
        isOutsideTouchable = true
    }

    private fun populateUi() {

    }
}
