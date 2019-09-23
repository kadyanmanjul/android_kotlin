package com.joshtalks.joshskills.core.custom_ui.audioplayer.view

import android.Manifest
import android.app.Activity
import androidx.core.content.res.ResourcesCompat
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.JcStatus
import kotlinx.android.synthetic.main.view_jcplayer.view.*

import android.content.Context
import android.content.res.TypedArray
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.audioplayer.JcPlayerManager
import com.joshtalks.joshskills.core.custom_ui.audioplayer.JcPlayerManagerListener
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.PlayerUtil.toTimeSongString
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.AudioListNullPointerException
import com.joshtalks.joshskills.core.custom_ui.audioplayer.general.errors.OnInvalidPathListener
import com.joshtalks.joshskills.core.custom_ui.audioplayer.model.JcAudio
import com.joshtalks.joshskills.core.interfaces.AudioPlayerInterface
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayerPauseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MediaEngageEventBus
import com.joshtalks.joshskills.repository.local.eventbus.PlayVideoEvent
import com.joshtalks.joshskills.repository.local.model.ListenGraph
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.disposables.CompositeDisposable

class JcPlayerView : LinearLayout, View.OnClickListener, SeekBar.OnSeekBarChangeListener,
    JcPlayerManagerListener {

    private val jcPlayerManager: JcPlayerManager by lazy {
        JcPlayerManager.getInstance(context).get()!!
    }

    internal var activity: Activity? = null

    internal lateinit var message: ChatModel
    private var audioPlayerInterface: AudioPlayerInterface? = null
    private var uri: Uri? = null
    private var isMedia = false


    val myPlaylist: List<JcAudio>?
        get() = jcPlayerManager.playlist

    val isPlaying: Boolean
        get() = jcPlayerManager.isPlaying()

    val isPaused: Boolean
        get() = jcPlayerManager.isPaused()

    val currentAudio: JcAudio?
        get() = jcPlayerManager.currentAudio

    var onInvalidPathListener: OnInvalidPathListener? = null


    var jcPlayerManagerListener: JcPlayerManagerListener? = null
        set(value) {
            jcPlayerManager.jcPlayerManagerListener = value
            field = value
        }

    var duration: Int = 0
    var startTime: Long = 0
    var endTime: Long = 0
    var drag = false
    var audioListenList = mutableListOf<ListenGraph>()
    private var compositeDisposable = CompositeDisposable()


    companion object {
        private const val PULSE_ANIMATION_DURATION = 200L
        private const val TITLE_ANIMATION_DURATION = 600
    }

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

        context.theme
            .obtainStyledAttributes(attrs, R.styleable.JcPlayerView, 0, 0)
            .also { setAttributes(it) }
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

        context.theme
            .obtainStyledAttributes(attrs, R.styleable.JcPlayerView, defStyle, 0)
            .also { setAttributes(it) }
    }

    private fun init() {
        View.inflate(context, R.layout.view_jcplayer, this)

        //  btnNext?.setOnClickListener(this)
        // btnPrev?.setOnClickListener(this)
        btnPlay?.setOnClickListener(this)
        btnPause?.setOnClickListener(this)
        startDownload?.setOnClickListener(this)
        cancelDownload?.setOnClickListener(this)
        // btnRandom?.setOnClickListener(this)
        // btnRepeat?.setOnClickListener(this)
        //btnRepeatOne?.setOnClickListener(this)
        seekBar?.setOnSeekBarChangeListener(this)
    }

    private fun setAttributes(attrs: TypedArray) {
        val defaultColor = ResourcesCompat.getColor(resources, R.color.black, null)

        txtCurrentDuration?.setTextColor(
            attrs.getColor(
                R.styleable.JcPlayerView_text_audio_current_duration_color,
                defaultColor
            )
        )
        txtDuration?.setTextColor(
            attrs.getColor(
                R.styleable.JcPlayerView_text_audio_duration_color,
                defaultColor
            )
        )

        //progressBarPlayer?.indeterminateDrawable?.setColorFilter(attrs.getColor(R.styleable.JcPlayerView_progress_color, defaultColor), PorterDuff.Mode.SRC_ATOP)
        seekBar?.progressDrawable?.setColorFilter(
            attrs.getColor(
                R.styleable.JcPlayerView_seek_bar_color,
                defaultColor
            ), PorterDuff.Mode.SRC_ATOP
        )

        seekBar?.thumb?.setColorFilter(
            attrs.getColor(
                R.styleable.JcPlayerView_seek_bar_color,
                defaultColor
            ), PorterDuff.Mode.SRC_ATOP
        )
        btnPlay.setColorFilter(
            attrs.getColor(
                R.styleable.JcPlayerView_play_icon_color,
                defaultColor
            )
        )
        /* btnPlay.setImageResource(
             attrs.getResourceId(
                 R.styleable.JcPlayerView_play_icon,
                 R.drawable.ic_play
             )
         )*/

        /*btnPause.setImageResource(
            attrs.getResourceId(
                R.styleable.JcPlayerView_pause_icon,
                R.drawable.ic_pause
            )
        )*/
        btnPause.setColorFilter(
            attrs.getColor(
                R.styleable.JcPlayerView_pause_icon_color,
                defaultColor
            )
        )

    }

    /**
     * Initialize the playlist and controls.
     *
     * @param playlist List of JcAudio objects that you want play
     * @param jcPlayerManagerListener The view status jcPlayerManagerListener (optional)
     */
    fun initPlaylist(
        playlist: List<JcAudio>,
        jcPlayerManagerListener: JcPlayerManagerListener? = null
    ) {
        /*Don't sort if the playlist have position number.
        We need to do this because there is a possibility that the user reload previous playlist
        from persistence storage like sharedPreference or SQLite.*/
        if (isAlreadySorted(playlist).not()) {
            sortPlaylist(playlist)
        }

        jcPlayerManager.playlist = playlist as ArrayList<JcAudio>
        jcPlayerManager.jcPlayerManagerListener = jcPlayerManagerListener
        jcPlayerManager.jcPlayerManagerListener = this
    }

    /**
     * Initialize an anonymous playlist with a default JcPlayer title for all audios
     *
     * @param playlist List of urls strings
     */
    fun initAnonPlaylist(playlist: List<JcAudio>) {
        generateTitleAudio(playlist, context.getString(R.string.track_number))
        initPlaylist(playlist)
    }

    /**
     * Initialize an anonymous playlist, but with a custom title for all audios
     *
     * @param playlist List of JcAudio files.
     * @param title    Default title for all audios
     */
    fun initWithTitlePlaylist(playlist: List<JcAudio>, title: String) {
        generateTitleAudio(playlist, title)
        initPlaylist(playlist)
    }

    /**
     * Add an audio for the playlist. We can track the JcAudio by
     * its id. So here we returning its id after adding to list.
     *
     * @param jcAudio audio file generated from [JcAudio]
     * @return jcAudio position.
     */
    fun addAudio(jcAudio: JcAudio): Int {
        jcPlayerManager.playlist.let {
            val lastPosition = it.size
            jcAudio.position = lastPosition + 1

            if (it.contains(jcAudio).not()) {
                it.add(lastPosition, jcAudio)
            }

            return jcAudio.position!!
        }
    }

    /**
     * Remove an audio for the playlist
     *
     * @param jcAudio JcAudio object
     */
    fun removeAudio(jcAudio: JcAudio) {
        jcPlayerManager.playlist.let {
            if (it.contains(jcAudio)) {
                if (it.size > 1) {
                    // play next audio when currently played audio is removed.
                    if (jcPlayerManager.isPlaying()) {
                        if (jcPlayerManager.currentAudio == jcAudio) {
                            it.remove(jcAudio)
                            pause()
                            resetPlayerInfo()
                        } else {
                            it.remove(jcAudio)
                        }
                    } else {
                        it.remove(jcAudio)
                    }
                } else {
                    it.remove(jcAudio)
                    pause()
                    resetPlayerInfo()
                }
            }
        }
    }

    fun prepareAudioPlayer(
        activity: Activity?,
        obj: ChatModel,
        audioPlayerInterface: AudioPlayerInterface
    ) {
        this.activity = activity
        this.message = obj
        this.audioPlayerInterface = audioPlayerInterface
        updateUI()

    }

    /**
     * Plays the give audio.
     * @param jcAudio The audio to be played.
     */
    fun playAudio(jcAudio: JcAudio) {
        showProgressBar()

        jcPlayerManager.playlist.let {
            if (it.contains(jcAudio).not()) {
                it.add(jcAudio)
            }


            jcPlayerManager.playAudio(jcAudio)
        }
    }

    /**
     * Shows the play button on player.
     */
    private fun showPlayButton() {
        btnPlay?.visibility = View.VISIBLE
        seekBar?.visibility = View.VISIBLE
        btnPause?.visibility = View.GONE
        download_container?.visibility = View.GONE
        seekBar_ph?.visibility = View.GONE

    }

    /**
     * Shows the pause button on player.
     */
    private fun showPauseButton() {
        btnPlay?.visibility = View.GONE
        btnPause?.visibility = View.VISIBLE
    }

    /**
     * Goes to next audio.
     */
    fun next() {
        jcPlayerManager.let { player ->
            player.currentAudio?.let {
                resetPlayerInfo()
                showProgressBar()

                try {
                    player.nextAudio()
                } catch (e: AudioListNullPointerException) {
                    dismissProgressBar()
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * Continues the current audio.
     */
    fun continueAudio() {
        showProgressBar()

        try {
            jcPlayerManager.continueAudio()
        } catch (e: AudioListNullPointerException) {
            dismissProgressBar()
            e.printStackTrace()
        }
    }

    /**
     * Pauses the current audio.
     */
    fun pause() {
        jcPlayerManager.pauseAudio()
        showPlayButton()
    }


    /**
     * Goes to precious audio.
     */
    fun previous() {
        resetPlayerInfo()
        showProgressBar()

        try {
            jcPlayerManager.previousAudio()
        } catch (e: AudioListNullPointerException) {
            dismissProgressBar()
            e.printStackTrace()
        }

    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnPlay ->


                btnPlay?.let {
                    Dexter.withActivity(activity)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                applyPulseAnimation(it)
                                continueAudio()
                                showPauseButton()
                                RxBus2.publish(AudioPlayerPauseEventBus(message.chatId))
                            }

                            override fun onPermissionDenied(response: PermissionDeniedResponse) {

                            }

                            override fun onPermissionRationaleShouldBeShown(
                                permission: PermissionRequest,
                                token: PermissionToken
                            ) {
                                token.continuePermissionRequest()
                            }
                        }).check()


                }

            R.id.btnPause -> {
                btnPause?.let {
                    applyPulseAnimation(it)
                    pause()
                    showPlayButton()
                }
            }
            R.id.startDownload -> {
                audioPlayerInterface?.downloadInQueue()
            }
            R.id.cancelDownload -> {
                audioPlayerInterface?.downloadStop()
            }

            /*  R.id.btnNext ->
                  btnNext?.let {
                      applyPulseAnimation(it)
                      next()
                  }

              R.id.btnPrev ->
                  btnPrev?.let {
                      applyPulseAnimation(it)
                      previous()
                  }

              R.id.btnRandom -> {
                  jcPlayerManager.onShuffleMode = jcPlayerManager.onShuffleMode.not()
                  btnRandomIndicator.visibility = if (jcPlayerManager.onShuffleMode) View.VISIBLE else View.GONE
              }
  */

            else -> { // Repeat case
                jcPlayerManager.activeRepeat()
                val active = jcPlayerManager.repeatPlaylist or jcPlayerManager.repeatCurrAudio

                //  btnRepeat?.visibility = View.VISIBLE
                // btnRepeatOne?.visibility = View.GONE
/*
                if (active) {
                    btnRepeatIndicator?.visibility = View.VISIBLE
                } else {
                    btnRepeatIndicator?.visibility = View.GONE
                }

                if (jcPlayerManager.repeatCurrAudio) {
                    btnRepeatOne?.visibility = View.VISIBLE
                    btnRepeat?.visibility = View.GONE
                }*/
            }
        }
    }

    /**
     * Create a notification player with same playlist with a custom icon.
     *
     * @param iconResource icon path.
     */
    fun createNotification(iconResource: Int) {
        jcPlayerManager.createNewNotification(iconResource)
    }

    /**
     * Create a notification player with same playlist with a default icon
     */
    fun createNotification() {
        jcPlayerManager.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // For light theme
                it.createNewNotification(R.drawable.ic_notification_default_black)
            } else {
                // For dark theme
                it.createNewNotification(R.drawable.ic_notification_default_white)
            }
        }
    }

    override fun onPreparedAudio(status: JcStatus) {
        dismissProgressBar()
        resetPlayerInfo()

        duration = status.duration.toInt()
        seekBar?.post { seekBar?.max = duration }
        txtDuration?.post { txtDuration?.text = toTimeSongString(duration) }
    }

    override fun onProgressChanged(seekBar: SeekBar, i: Int, fromUser: Boolean) {
        jcPlayerManager.let {
            if (fromUser) {
                it.seekTo(i)
            }
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar) {
        if (endTime > startTime) {
            audioListenList.add(ListenGraph(startTime, endTime))
            startTime = 0
            endTime = 0
        }
        drag = true
    }

    override fun onStopTrackingTouch(seekBar: SeekBar) {
        // dismissProgressBar()
        drag = false

        if (jcPlayerManager.isPaused()) {
            showPlayButton()
        }
    }

    override fun onCompletedAudio() {
        resetPlayerInfo()

        try {
            jcPlayerManager.nextAudio()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        txtDuration?.post { txtDuration?.text = toTimeSongString(duration) }

        showPlayButton()

    }

    override fun onContinueAudio(status: JcStatus) {
        dismissProgressBar()
    }

    override fun onPlaying(status: JcStatus) {
        dismissProgressBar()
        showPauseButton()
    }

    override fun onTimeChanged(status: JcStatus) {
        val currentPosition = status.currentPosition.toInt()
        if (drag.not()) {
            if (startTime <= 0) {
                startTime = status.currentPosition
            } else {
                endTime = status.currentPosition
            }
        }

        seekBar?.post { seekBar?.progress = currentPosition }
        txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(currentPosition) }
    }

    override fun onPaused(status: JcStatus) {
        endTime = status.currentPosition
    }

    override fun onStopped(status: JcStatus) {
        try {
            audioListenList.add(ListenGraph(startTime, endTime))
            startTime = 0
            endTime = 0
            if (audioListenList.isNullOrEmpty()) {
                return
            }
            RxBus2.publish(
                MediaEngageEventBus(
                    "AUDIO",
                    message.question?.audioList?.get(0)?.id!!,
                    audioListenList
                )
            )
            audioListenList.clear()
        } catch (ex: Exception) {

        }

    }

    override fun onJcpError(throwable: Throwable) {
        throwable.printStackTrace()
    }

    private fun showProgressBar() {
        progressBarPlayer?.visibility = ProgressBar.VISIBLE
        btnPlay?.visibility = Button.GONE
        btnPause?.visibility = Button.GONE
    }

    private fun dismissProgressBar() {
        progressBarPlayer?.visibility = ProgressBar.GONE
        showPauseButton()
    }


    private fun resetPlayerInfo() {
        seekBar?.post { seekBar?.progress = 0 }
        txtDuration?.post { txtDuration.text = context.getString(R.string.play_initial_time) }
        txtCurrentDuration?.post {
            txtCurrentDuration.text = context.getString(R.string.play_initial_time)
        }
    }

    /**
     * Sorts the playlist.
     */
    private fun sortPlaylist(playlist: List<JcAudio>) {
        for (i in playlist.indices) {
            val jcAudio = playlist[i]
            jcAudio.position = i
        }
    }

    /**
     * Check if playlist already sorted or not.
     * We need to check because there is a possibility that the user reload previous playlist
     * from persistence storage like sharedPreference or SQLite.
     *
     * @param playlist list of JcAudio
     * @return true if sorted, false if not.
     */
    private fun isAlreadySorted(playlist: List<JcAudio>?): Boolean {
        // If there is position in the first audio, then playlist is already sorted.
        return playlist?.let { it[0].position != -1 } == true
    }

    /**
     * Generates a default audio title for each audio on list.
     * @param playlist The audio list.
     * @param title The default title.
     */
    private fun generateTitleAudio(playlist: List<JcAudio>, title: String) {
        for (i in playlist.indices) {
            if (title == context.getString(R.string.track_number)) {
                playlist[i].title =
                    context.getString(R.string.track_number) + " " + (i + 1).toString()
            } else {
                playlist[i].title = title
            }
        }
    }

    private fun applyPulseAnimation(view: View?) {
        view?.postDelayed({
            YoYo.with(Techniques.Pulse)
                .duration(PULSE_ANIMATION_DURATION)
                .playOn(view)
        }, PULSE_ANIMATION_DURATION)
    }

    /**
     * Kills the player
     */
    private fun kill() {
        jcPlayerManager.kill()
    }

    private fun updateUI() {
        if (message.url != null) {
            if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED) {
                if (message.downloadedLocalPath != null && AppDirectory.isFileExist(message.downloadedLocalPath!!)) {
                    mediaDownloaded()
                } else {
                    mediaNotDownloaded()
                }
            } else if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADING) {
                mediaDownloading()
                audioPlayerInterface?.downloadStart(message.url!!)
            } else {
                mediaNotDownloaded()

            }
        } else {

            if (message.question != null && message.question!!.audioList != null && message.question!!.audioList!!.size > 0) {
                val audioTypeObj = message.question!!.audioList!![0]
                if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED) {

                    if (audioTypeObj.downloadedLocalPath != null && AppDirectory.isFileExist(
                            audioTypeObj.downloadedLocalPath!!
                        )
                    ) {
                        mediaDownloaded()
                    } else {
                        mediaNotDownloaded()
                    }

                } else if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADING) {
                    mediaDownloading()
                    audioPlayerInterface?.downloadStart(audioTypeObj.audio_url)
                } else {
                    mediaNotDownloaded()
                }
            }
        }
    }

    private fun mediaNotDownloaded() {
        download_container.visibility = View.VISIBLE
        progressBarPlayer.visibility = View.GONE
        cancelDownload.visibility = View.GONE
        startDownload.visibility = View.VISIBLE
        btnPlay?.visibility = View.GONE
        btnPause?.visibility = View.GONE


    }

    private fun mediaDownloading() {
        download_container.visibility = View.VISIBLE
        progressBarPlayer.visibility = View.VISIBLE
        cancelDownload.visibility = View.VISIBLE
        startDownload.visibility = View.GONE

    }

    private fun mediaDownloaded() {
        download_container.visibility = View.GONE
        btnPlay?.visibility = View.VISIBLE
        seekBar?.visibility = View.VISIBLE
        btnPause?.visibility = View.GONE
        seekBar_ph?.visibility = View.GONE
        updateUri()
    }

    private fun updateUri() {
        val jcAudios = java.util.ArrayList<JcAudio>()
        try {

            if (message.downloadedLocalPath == null || message.downloadedLocalPath!!.isEmpty()) {
                if (message.question!!.audioList!![0].downloadedLocalPath == null || message.question!!.audioList!![0].downloadedLocalPath!!.isEmpty()) {
                    this.uri = Uri.parse(message.question!!.audioList!![0].audio_url)
                    jcAudios.add(JcAudio.createFromURL(message.question!!.audioList!![0].audio_url))
                    isMedia=true
                    this.duration = Utils.getDurationOfMedia(
                        context,
                        message.question!!.audioList!![0].audio_url
                    )!!.toInt()

                } else {
                    jcAudios.add(JcAudio.createFromFilePath(message.question!!.audioList!![0].downloadedLocalPath!!))
                    isMedia=true

                    this.duration = Utils.getDurationOfMedia(
                        context,
                        message.question!!.audioList!![0].downloadedLocalPath!!
                    )!!.toInt()

                }
            } else {
                if (message.downloadedLocalPath!!.isEmpty()) {
                    jcAudios.add(JcAudio.createFromFilePath(message.url!!))
                    isMedia=true

                    this.duration = Utils.getDurationOfMedia(context, message.url!!)!!.toInt()

                } else {
                    jcAudios.add(JcAudio.createFromFilePath(message.downloadedLocalPath!!))
                    isMedia=true

                    this.duration =
                        Utils.getDurationOfMedia(context, message.downloadedLocalPath!!)!!.toInt()

                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        kill()
        // addAudio(jcAudios.get(0))
        //   jcPlayerManagerListener=this
        initPlaylist(jcAudios, this)

    }

    private fun updateController() {

        if (isMedia) {
            AppObjectController.uiHandler.post {
                btnPlay?.visibility = View.VISIBLE
                btnPause?.visibility = View.GONE
                seekBar?.progress = 0

            }
        }


    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        compositeDisposable.add(RxBus2.listen(AudioPlayerPauseEventBus::class.java).subscribe {
            it?.audioId?.let { audioId ->
                if (audioId != message.chatId) {
                    updateController()
                }
            }
        })
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }

}
