package tech.hombre.bluetoothchat.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.material.floatingactionbutton.FloatingActionButton
import androidx.core.view.ViewCompat
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.TextView
import tech.hombre.bluetoothchat.R
import tech.hombre.bluetoothchat.utils.getLayoutInflater
import tech.hombre.bluetoothchat.utils.isNumber
import tech.hombre.bluetoothchat.utils.toPx

class GoDownButton : FrameLayout {

    private var goDownButton: FloatingActionButton
    private var newMessagesCount: TextView

    private var onClickListener: (() -> Unit)? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {

        @SuppressLint("InflateParams")
        val root = context.getLayoutInflater().inflate(R.layout.view_go_down_button, null)

        goDownButton = root.findViewById(R.id.fab_go_down)
        newMessagesCount = root.findViewById(R.id.tv_new_messages)

        ViewCompat.setElevation(newMessagesCount, 6.toPx().toFloat())
        goDownButton.setOnClickListener { onClickListener?.invoke() }

        addView(root)
    }

    fun setOnClickListener(listener: () -> Unit) {
        onClickListener = listener
    }

    fun setUnreadMessageNumber(number: Int) {
        newMessagesCount.text = number.toString()
        newMessagesCount.visibility = if (number > 0) VISIBLE else GONE
    }

    fun getUnreadMessageNumber(): Int {
        val text = newMessagesCount.text.toString()
        return if (text.isEmpty() || !text.isNumber()) 0 else text.toInt()
    }
}
