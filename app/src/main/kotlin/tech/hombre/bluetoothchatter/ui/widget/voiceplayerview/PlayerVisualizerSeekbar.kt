package tech.hombre.bluetoothchatter.ui.widget.voiceplayerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatSeekBar
import androidx.core.content.ContextCompat
import tech.hombre.bluetoothchatter.R
import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or


class PlayerVisualizerSeekbar : AppCompatSeekBar {
    /**
     * bytes array converted from file.
     */
    private var bytes: ByteArray? = null

    /**
     * Percentage of audio sample scale
     * Should updated dynamically while audioPlayer is played
     */
    private var denseness = 0f

    /**
     * Canvas painting for sample scale, filling played part of audio sample
     */
    private val playedStatePainting = Paint()

    /**
     * Canvas painting for sample scale, filling not played part of audio sample
     */
    private val notPlayedStatePainting = Paint()
    private var mWidth = 0
    private var mHeight = 0

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    fun setColors(playedColor: Int, notPlayedColor: Int) {
        playedStatePainting.color = notPlayedColor
        notPlayedStatePainting.color = playedColor
    }

    private fun init() {
        bytes = null
        playedStatePainting.strokeWidth = 1f
        playedStatePainting.isAntiAlias = true
        playedStatePainting.color = ContextCompat.getColor(getContext(), R.color.gray)
        notPlayedStatePainting.strokeWidth = 1f
        notPlayedStatePainting.isAntiAlias = true
        notPlayedStatePainting.color = ContextCompat.getColor(getContext(), R.color.lightPink)
    }

    /**
     * update and redraw Visualizer view
     */
    fun updateVisualizer(file: File) {
        FileUtils.updateVisualizer(getContext(), file, this)
    }

    fun setBytes(bytes: ByteArray?) {
        this.bytes = bytes
    }

    /**
     * Update player percent. 0 - file not played, 1 - full played
     *
     * @param percent
     */
    fun updatePlayerPercent(percent: Float) {
        denseness = Math.ceil((mWidth * percent).toDouble()).toFloat()
        if (denseness < 0) {
            denseness = 0f
        } else if (denseness > mWidth) {
            denseness = mWidth.toFloat()
        }
        invalidate()
    }

    protected override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int
    ) {
        super.onLayout(changed, left, top, right, bottom)
        mWidth = getMeasuredWidth()
        mHeight = getMeasuredHeight()
    }

    protected override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (bytes == null || mWidth == 0) {
            return
        }
        val totalBarsCount = (mWidth / dp(3f)).toFloat()
        if (totalBarsCount <= 0.1f) {
            return
        }
        var value: Byte
        val samplesCount = bytes!!.size * 8 / 5
        val samplesPerBar = samplesCount / totalBarsCount
        var barCounter = 0f
        var nextBarNum = 0
        val y = mHeight - dp(VISUALIZER_HEIGHT.toFloat())
        var barNum = 0
        var lastBarNum: Int
        var drawBarCount: Int
        for (a in 0 until samplesCount) {
            if (a != nextBarNum) {
                continue
            }
            drawBarCount = 0
            lastBarNum = nextBarNum
            while (lastBarNum == nextBarNum) {
                barCounter += samplesPerBar
                nextBarNum = barCounter.toInt()
                drawBarCount++
            }
            val bitPointer = a * 5
            val byteNum = bitPointer / java.lang.Byte.SIZE
            val byteBitOffset = bitPointer - byteNum * java.lang.Byte.SIZE
            val currentByteCount = java.lang.Byte.SIZE - byteBitOffset
            val nextByteRest = 5 - currentByteCount

            value = ((bytes!![byteNum].toInt() shr byteBitOffset and (2 shl Math.min(
                5,
                currentByteCount
            ) - 1) - 1).toByte())
            if (nextByteRest > 0) {
                value = (value.toInt() shl nextByteRest).toByte()
                value = value or (bytes!![byteNum + 1] and ((2 shl nextByteRest - 1) - 1).toByte())
            }
            for (b in 0 until drawBarCount) {
                val x = barNum * dp(3f)
                val left = x.toFloat()
                val top = (y + dp(
                    VISUALIZER_HEIGHT - Math.max(
                        1f,
                        VISUALIZER_HEIGHT * value / 31.0f
                    )
                )).toFloat()
                val right = (x + dp(2f)).toFloat()
                val bottom = (y + dp(VISUALIZER_HEIGHT.toFloat())).toFloat()
                if (x < denseness && x + dp(2f) < denseness) {
                    canvas.drawRect(left, top, right, bottom, notPlayedStatePainting)
                } else {
                    canvas.drawRect(left, top, right, bottom, playedStatePainting)
                    if (x < denseness) {
                        canvas.drawRect(left, top, right, bottom, notPlayedStatePainting)
                    }
                }
                barNum++
            }
        }
    }

    fun dp(value: Float): Int {
        return if (value == 0f) {
            0
        } else Math.ceil(
            (getContext().getResources().getDisplayMetrics().density * value).toDouble()
        ).toInt()
    }

    companion object {
        /**
         * constant value for Height of the bar
         */
        const val VISUALIZER_HEIGHT = 28
    }
}