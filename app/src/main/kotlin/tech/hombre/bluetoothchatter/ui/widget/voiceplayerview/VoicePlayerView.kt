package tech.hombre.bluetoothchatter.ui.widget.voiceplayerview

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View.OnClickListener
import android.widget.*
import android.widget.SeekBar.OnSeekBarChangeListener
import tech.hombre.bluetoothchatter.R
import java.io.File
import java.io.IOException
import java.net.URLConnection


class VoicePlayerView : LinearLayout {
    private var playPaueseBackgroundColor = 0
    private var shareBackgroundColor = 0
    private var viewBackgroundColor = 0
    private var seekBarProgressColor = 0
    private var seekBarThumbColor = 0
    private var progressTimeColor = 0
    private var timingBackgroundColor = 0
    private var visualizationPlayedColor = 0
    private var visualizationNotPlayedColor = 0
    private var playProgressbarColor = 0
    private var viewCornerRadius = 0f
    private var playPauseCornerRadius = 0f
    private var shareCornerRadius = 0f
    private var isShowShareButton = false
    private var isShowTiming = false
    private var isEnableVirtualizer = false
    private var playPauseShape: GradientDrawable? = null
    private var shareShape: GradientDrawable? = null
    private var viewShape: GradientDrawable? = null
    private var path: String? = null
    private var shareTitle: String? = "Share Voice"
    private lateinit var main_layout: LinearLayout
    private lateinit var padded_layout: LinearLayout
    private lateinit var container_layout: LinearLayout
    lateinit var imgPlay: ImageView
    private lateinit var imgPause: ImageView
    private lateinit var imgShare: ImageView
    private lateinit var seekBar: SeekBar
    lateinit var progressBar: ProgressBar
    private lateinit var txtProcess: TextView
    var mediaPlayer: MediaPlayer? = null
    private lateinit var playProgressbar: ProgressBar
    private lateinit var seekbarV: PlayerVisualizerSeekbar
    private var contentUri: Uri? = null
    var onPlayClick = {}
    var onPauseClick = {}

    constructor(context: Context) : super(context) {
        LayoutInflater.from(context).inflate(R.layout.voice_player_layout, this)
       // this.context = context
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initViews(context, attrs)
     //   this.context = context
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initViews(context, attrs)
     //   this.context = context
    }

    private fun initViews(context: Context, attrs: AttributeSet?) {
        val typedArray = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.VoicePlayerView, 0, 0
        )
        viewShape = GradientDrawable()
        playPauseShape = GradientDrawable()
        shareShape = GradientDrawable()
        try {
            isShowShareButton =
                typedArray.getBoolean(R.styleable.VoicePlayerView_showShareButton, true)
            isShowTiming = typedArray.getBoolean(R.styleable.VoicePlayerView_showTiming, true)
            viewCornerRadius = typedArray.getFloat(R.styleable.VoicePlayerView_viewCornerRadius, 0f)
            playPauseCornerRadius =
                typedArray.getFloat(R.styleable.VoicePlayerView_playPauseCornerRadius, 0f)
            shareCornerRadius =
                typedArray.getFloat(R.styleable.VoicePlayerView_shareCornerRadius, 0f)
            playPaueseBackgroundColor = typedArray.getColor(
                R.styleable.VoicePlayerView_playPauseBackgroundColor,
                resources.getColor(R.color.pink)
            )
            shareBackgroundColor = typedArray.getColor(
                R.styleable.VoicePlayerView_shareBackgroundColor,
                resources.getColor(R.color.pink)
            )
            viewBackgroundColor = typedArray.getColor(
                R.styleable.VoicePlayerView_viewBackground,
                resources.getColor(R.color.white)
            )
            seekBarProgressColor = typedArray.getColor(
                R.styleable.VoicePlayerView_seekBarProgressColor,
                resources.getColor(R.color.pink)
            )
            seekBarThumbColor = typedArray.getColor(
                R.styleable.VoicePlayerView_seekBarThumbColor,
                resources.getColor(R.color.pink)
            )
            progressTimeColor =
                typedArray.getColor(R.styleable.VoicePlayerView_progressTimeColor, Color.GRAY)
            shareTitle = typedArray.getString(R.styleable.VoicePlayerView_shareText)
            isEnableVirtualizer =
                typedArray.getBoolean(R.styleable.VoicePlayerView_enableVisualizer, false)
            timingBackgroundColor = typedArray.getColor(
                R.styleable.VoicePlayerView_timingBackgroundColor, resources.getColor(
                    R.color.transparent
                )
            )
            visualizationNotPlayedColor = typedArray.getColor(
                R.styleable.VoicePlayerView_visualizationNotPlayedColor,
                resources.getColor(R.color.gray)
            )
            visualizationPlayedColor = typedArray.getColor(
                R.styleable.VoicePlayerView_visualizationPlayedColor,
                resources.getColor(R.color.pink)
            )
            playProgressbarColor = typedArray.getColor(
                R.styleable.VoicePlayerView_playProgressbarColor,
                resources.getColor(R.color.pink)
            )
        } finally {
            typedArray.recycle()
        }
        LayoutInflater.from(context).inflate(R.layout.voice_player_layout, this)
        main_layout = findViewById(R.id.collectorLinearLayout)
        padded_layout = findViewById(R.id.paddedLinearLayout)
        container_layout = findViewById(R.id.containerLinearLayout)
        imgPlay = findViewById(R.id.imgPlay)
        imgPause = findViewById(R.id.imgPause)
        imgShare = findViewById(R.id.imgShare)
        seekBar = findViewById(R.id.seekBar)
        progressBar = findViewById(R.id.progressBar)
        txtProcess = findViewById(R.id.txtTime)
        seekbarV = findViewById(R.id.seekBarV)
        playProgressbar = findViewById(R.id.pb_play)
        viewShape!!.setColor(viewBackgroundColor)
        viewShape!!.cornerRadius = viewCornerRadius
        playPauseShape!!.setColor(playPaueseBackgroundColor)
        playPauseShape!!.cornerRadius = playPauseCornerRadius
        shareShape!!.setColor(shareBackgroundColor)
        shareShape!!.cornerRadius = shareCornerRadius
        imgPlay.setBackground(playPauseShape)
        imgPause.setBackground(playPauseShape)
        imgShare.setBackground(shareShape)
        main_layout.setBackground(viewShape)
        seekBar.getProgressDrawable().setColorFilter(seekBarProgressColor, PorterDuff.Mode.SRC_IN)
        seekBar.getThumb().setColorFilter(seekBarThumbColor, PorterDuff.Mode.SRC_IN)
        val timingBackground = GradientDrawable()
        timingBackground.setColor(timingBackgroundColor)
        timingBackground.cornerRadius = 25f
        txtProcess.setBackground(timingBackground)
        txtProcess.setPadding(16, 8, 16, 8)
        txtProcess.setTextColor(progressTimeColor)
        playProgressbar.getIndeterminateDrawable().setColorFilter(
            playProgressbarColor,
            PorterDuff.Mode.SRC_IN
        )
        if (!isShowShareButton) imgShare.setVisibility(GONE)
        if (!isShowTiming) txtProcess.setVisibility(INVISIBLE)
        if (isEnableVirtualizer) {
            seekbarV.setVisibility(VISIBLE)
            seekBar.setVisibility(GONE)
            seekbarV.getProgressDrawable()
                .setColorFilter(resources.getColor(R.color.transparent), PorterDuff.Mode.SRC_IN)
            seekbarV.getThumb()
                .setColorFilter(resources.getColor(R.color.transparent), PorterDuff.Mode.SRC_IN)
            seekbarV.setColors(visualizationPlayedColor, visualizationNotPlayedColor)
        }
    }

    //Set the audio source and prepare mediaplayer
    fun setAudio(audioPath: String?) {
        if (path == audioPath) return
        path = audioPath
        mediaPlayer = MediaPlayer()
        if (path != null) {
            try {
                mediaPlayer!!.setDataSource(path)
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer!!.prepare()
                mediaPlayer!!.setVolume(10f, 10f)
                //START and PAUSE are in other listeners
                mediaPlayer!!.setOnPreparedListener { mp ->
                    seekBar!!.max = mp.duration
                    if (seekbarV!!.visibility == VISIBLE) {
                        seekbarV!!.max = mp.duration
                    }
                    txtProcess!!.text =
                        "00:00:00/" + convertSecondsToHMmSs((mp.duration / 1000).toLong())
                }
                mediaPlayer!!.setOnCompletionListener {
                    imgPause!!.visibility = GONE
                    imgPlay!!.visibility = VISIBLE
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        seekBar!!.setOnSeekBarChangeListener(seekBarListener)
        imgPlay!!.setOnClickListener(imgPlayClickListener)
        imgPause!!.setOnClickListener(imgPauseClickListener)
        imgShare!!.setOnClickListener(imgShareClickListener)
        if (seekbarV!!.visibility == VISIBLE) {
            seekbarV!!.updateVisualizer(File(path))
        }
        seekbarV!!.setOnSeekBarChangeListener(seekBarListener)
        seekbarV!!.updateVisualizer(File(path))
    }

    //Components' listeners
    var imgPlayClickListener = OnClickListener {
        (context as Activity).runOnUiThread {
            imgPause!!.visibility = VISIBLE
            imgPlay!!.visibility = GONE
        }
        try {
            if (mediaPlayer != null) {
                mediaPlayer!!.start()
            }
            update(mediaPlayer, txtProcess, seekBar, context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onPlayClick()
    }

    var seekBarListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) {
                (context as Activity).runOnUiThread {
                    mediaPlayer!!.seekTo(progress)
                    update(mediaPlayer, txtProcess, seekBar, context)
                    if (seekbarV!!.visibility == VISIBLE) {
                        seekbarV!!.updatePlayerPercent(mediaPlayer!!.currentPosition.toFloat() / mediaPlayer!!.duration)
                    }
                }
            }
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            (context as Activity).runOnUiThread {
                imgPause!!.visibility = GONE
                imgPlay!!.visibility = VISIBLE
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            (context as Activity).runOnUiThread {
                imgPlay!!.visibility = GONE
                imgPause!!.visibility = VISIBLE
                try {
                    mediaPlayer!!.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
    var imgPauseClickListener = OnClickListener {
        (context as Activity).runOnUiThread {
            imgPause!!.visibility = GONE
            imgPlay!!.visibility = VISIBLE
            try {
                mediaPlayer!!.pause()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onPauseClick()
    }
    var imgShareClickListener = OnClickListener {
        (context as Activity).runOnUiThread {
            imgShare!!.visibility = GONE
            progressBar!!.visibility = VISIBLE
        }
        if (contentUri == null) {
            val file = File(path)
            if (file.exists()) {
                val intentShareFile = Intent(Intent.ACTION_SEND)
                intentShareFile.type = URLConnection.guessContentTypeFromName(file.name)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val builder = VmPolicy.Builder()
                    StrictMode.setVmPolicy(builder.build())
                }
                intentShareFile.putExtra(
                    Intent.EXTRA_STREAM,
                    Uri.parse("file://" + file.absolutePath)
                )
                context.startActivity(Intent.createChooser(intentShareFile, shareTitle))
            }
        } else {
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // temp permission for receiving app to read this file
            shareIntent.setDataAndType(
                contentUri, context.contentResolver.getType(
                    contentUri!!
                )
            )
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
            (context as Activity).startActivity(Intent.createChooser(shareIntent, shareTitle))
        }
        val handler = Handler()
        handler.postDelayed({
            (context as Activity).runOnUiThread {
                progressBar!!.visibility = GONE
                imgShare!!.visibility = VISIBLE
            }
        }, 500)
    }

    //Updating seekBar in realtime
    private fun update(
        mediaPlayer: MediaPlayer?,
        time: TextView?,
        seekBar: SeekBar?,
        context: Context
    ) {
        (context as Activity).runOnUiThread {
            seekBar!!.progress = mediaPlayer!!.currentPosition
            if (seekbarV!!.visibility == VISIBLE) {
                seekbarV!!.progress = mediaPlayer.currentPosition
                seekbarV!!.updatePlayerPercent(mediaPlayer.currentPosition.toFloat() / mediaPlayer.duration)
            }
            if (mediaPlayer.duration - mediaPlayer.currentPosition > 100) {
                time!!.text =
                    convertSecondsToHMmSs((mediaPlayer.currentPosition / 1000).toLong()) + " / " + convertSecondsToHMmSs(
                        (mediaPlayer.duration / 1000).toLong()
                    )
            } else {
                time!!.text =
                    convertSecondsToHMmSs((mediaPlayer.duration / 1000).toLong())
                seekBar.progress = 0
                if (seekbarV!!.visibility == VISIBLE) {
                    seekbarV!!.updatePlayerPercent(0f)
                    seekbarV!!.progress = 0
                }
            }
            val handler = Handler()
            try {
                val runnable = Runnable {
                    try {
                        if (mediaPlayer.currentPosition > -1) {
                            try {
                                update(mediaPlayer, time, seekBar, context)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                handler.postDelayed(runnable, 500)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    //These both functions to avoid mediaplayer errors
    fun onStop() {
        try {
            mediaPlayer!!.stop()
            mediaPlayer!!.reset()
            mediaPlayer!!.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun onPause() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer!!.isPlaying) mediaPlayer!!.pause()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        (context as Activity).runOnUiThread {
            imgPause!!.visibility = GONE
            imgPlay!!.visibility = VISIBLE
        }
    }

    // Programmatically functions
    fun setViewBackgroundShape(color: Int, radius: Float) {
        val shape = GradientDrawable()
        shape.setColor(resources.getColor(color))
        shape.cornerRadius = radius
        main_layout!!.background = shape
    }

    fun setShareBackgroundShape(color: Int, radius: Float) {
        val shape = GradientDrawable()
        shape.setColor(resources.getColor(color))
        shape.cornerRadius = radius
        imgShare!!.background = shape
    }

    fun setPlayPaueseBackgroundShape(color: Int, radius: Float) {
        val shape = GradientDrawable()
        shape.setColor(resources.getColor(color))
        shape.cornerRadius = radius
        imgPause!!.background = shape
        imgPlay!!.background = shape
    }

    fun setSeekBarStyle(progressColor: Int, thumbColor: Int) {
        seekBar!!.progressDrawable.setColorFilter(
            resources.getColor(progressColor),
            PorterDuff.Mode.SRC_IN
        )
        seekBar!!.thumb.setColorFilter(resources.getColor(thumbColor), PorterDuff.Mode.SRC_IN)
    }

    fun setTimingVisibility(visibility: Boolean) {
        if (!visibility) txtProcess!!.visibility = INVISIBLE else txtProcess!!.visibility =
            VISIBLE
    }

    fun setShareButtonVisibility(visibility: Boolean) {
        if (!visibility) imgShare!!.visibility = GONE else imgShare!!.visibility =
            VISIBLE
    }

    fun setShareText(shareText: String?) {
        shareTitle = shareText
    }

    fun showPlayProgressbar() {
        imgPlay!!.visibility = GONE
        playProgressbar!!.visibility = VISIBLE
    }

    fun hidePlayProgresbar() {
        playProgressbar!!.visibility = GONE
        imgPlay!!.visibility = VISIBLE
    }

    fun refreshPlayer(audioPath: String?) {
        path = audioPath
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
        mediaPlayer = MediaPlayer()
        if (path != null) {
            try {
                mediaPlayer!!.setDataSource(path)
                mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                mediaPlayer!!.prepare()
                mediaPlayer!!.setVolume(10f, 10f)
                //START and PAUSE are in other listeners
                mediaPlayer!!.setOnPreparedListener { mp ->
                    (context as Activity).runOnUiThread {
                        seekBar!!.max = mp.duration
                        seekBar!!.progress = 0
                        if (seekbarV!!.visibility == VISIBLE) {
                            seekbarV!!.max = mp.duration
                            seekbarV!!.progress = 0
                        }
                        if (imgPause!!.visibility == VISIBLE) {
                            imgPause!!.visibility = GONE
                            imgPlay!!.visibility = VISIBLE
                        }
                        txtProcess!!.text =
                            "00:00:00/" + convertSecondsToHMmSs((mp.duration / 1000).toLong())
                    }
                }
                mediaPlayer!!.setOnCompletionListener {
                    (context as Activity).runOnUiThread {
                        imgPause!!.visibility = GONE
                        imgPlay!!.visibility = VISIBLE
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        (context as Activity).runOnUiThread {
            seekBar!!.setOnSeekBarChangeListener(seekBarListener)
            imgPlay!!.setOnClickListener(imgPlayClickListener)
            imgPause!!.setOnClickListener(imgPauseClickListener)
            imgShare!!.setOnClickListener(imgShareClickListener)
            if (seekbarV!!.visibility == VISIBLE) {
                seekbarV!!.updateVisualizer(File(path))
                seekbarV!!.setOnSeekBarChangeListener(seekBarListener)
                seekbarV!!.updateVisualizer(File(path))
            }
        }
        seekBar!!.invalidate()
        seekbarV!!.invalidate()
        this.invalidate()
    }

    fun refreshVisualizer() {
        if (seekbarV!!.visibility == VISIBLE) {
            seekbarV!!.updateVisualizer(File(path))
        }
    }

  /*  fun setContext(context: Context) {
        this.context = context
    }*/

    companion object {
        //Convert long milli seconds to a formatted String to display it
        private fun convertSecondsToHMmSs(seconds: Long): String {
            val s = seconds % 60
            val m = seconds / 60 % 60
            val h = seconds / (60 * 60) % 24
            return String.format("%02d:%02d:%02d", h, m, s)
        }
    }
}