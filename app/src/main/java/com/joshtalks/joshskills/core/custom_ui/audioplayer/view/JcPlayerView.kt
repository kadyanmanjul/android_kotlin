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
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.*
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
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
import com.joshtalks.joshskills.repository.local.entity.BASE_MESSAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.DOWNLOAD_STATUS
import com.joshtalks.joshskills.repository.local.entity.MESSAGE_DELIVER_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.AudioPlayerPauseEventBus
import com.joshtalks.joshskills.repository.local.eventbus.MediaEngageEventBus
import com.joshtalks.joshskills.repository.local.model.ListenGraph
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import io.reactivex.disposables.CompositeDisposable
import java.util.*
import kotlin.collections.ArrayList

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
    private var cAudioObj: JcAudio? = null


    companion object {
        private const val PULSE_ANIMATION_DURATION = 200L
        private const val TITLE_ANIMATION_DURATION = 600
        @JvmStatic
        @Volatile
        private var videoId: String? = ""


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
        btnPlay?.setOnClickListener(this)
        btnPause?.setOnClickListener(this)
        startDownload?.setOnClickListener(this)
        cancelDownload?.setOnClickListener(this)
        seekBar?.setOnSeekBarChangeListener(this)
    }

    private fun setAttributes(attrs: TypedArray) {
        val defaultColor = ResourcesCompat.getColor(resources, R.color.black, null)
        btnPlay.setColorFilter(
            attrs.getColor(
                R.styleable.JcPlayerView_play_icon_color,
                defaultColor
            )
        )
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
    private fun initPlaylist(
        playlist: List<JcAudio>,
        jcPlayerManagerListener: JcPlayerManagerListener? = null
    ) {
        /*Don't sort if the playlist have position number.
        We need to do this because there is a possibility that the user reload previous playlist
        from persistence storage like sharedPreference or SQLite.*/
        /* if (isAlreadySorted(playlist).not()) {
             sortPlaylist(playlist)
         }*/

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
        setDefaultUi()
        updateUI()
        updateTime(message_time)
        //message_time

    }

    private fun setDefaultUi() {
        seekBar.visibility = View.INVISIBLE
        download_container.visibility = View.INVISIBLE
        progressBarPlayer.visibility = View.INVISIBLE
        cancelDownload.visibility = View.INVISIBLE
        startDownload.visibility = View.INVISIBLE
        btnPlay?.visibility = View.INVISIBLE
        btnPause?.visibility = View.INVISIBLE
        seekBar_ph?.visibility = View.INVISIBLE
        txtCurrentDuration.text = EMPTY
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
        btnPause?.visibility = View.INVISIBLE
        download_container?.visibility = View.INVISIBLE
        seekBar_ph?.visibility = View.INVISIBLE

    }

    /**
     * Shows the pause button on player.
     */
    private fun showPauseButton() {
        btnPlay?.visibility = View.INVISIBLE
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
            R.id.btnPlay -> {

                if (videoId.isNullOrEmpty().not() && videoId.equals(
                        message.chatId,
                        ignoreCase = true
                    ).not()
                ) {
                    updateUri()
                }
                if (cAudioObj == null || AppDirectory.isFileExist(cAudioObj!!.path).not()) {
                    MaterialDialog(context).show {
                        message(R.string.media_not_found_message)
                        positiveButton(R.string.ok)

                    }
                    return
                }


                btnPlay?.let {
                    Dexter.withActivity(activity)
                        .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                        .withListener(object : PermissionListener {
                            override fun onPermissionGranted(response: PermissionGrantedResponse) {
                                //  applyPulseAnimation(it)
                                continueAudio()
                                showPauseButton()
                                RxBus2.publish(AudioPlayerPauseEventBus(message.chatId))
                                videoId = message.chatId
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
                //updateUI()
            }


            else -> { // Repeat case
                jcPlayerManager.activeRepeat()
                val active = jcPlayerManager.repeatPlaylist or jcPlayerManager.repeatCurrAudio

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
        txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(duration) }
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
        txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(duration) }
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
        Log.e("ontime", status.toString())

        seekBar?.post { seekBar?.progress = currentPosition }
        txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(currentPosition) }
    }

    override fun onPaused(status: JcStatus) {
        endTime = status.currentPosition
        txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(duration) }


    }

    override fun onStopped(status: JcStatus) {
        try {
            txtCurrentDuration?.post { txtCurrentDuration?.text = toTimeSongString(duration) }
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
        btnPlay?.visibility = Button.INVISIBLE
        btnPause?.visibility = Button.INVISIBLE
    }

    private fun dismissProgressBar() {
        progressBarPlayer?.visibility = ProgressBar.INVISIBLE
        showPauseButton()
    }


    private fun resetPlayerInfo() {
        seekBar?.post { seekBar?.progress = 0 }
        txtCurrentDuration.text = toTimeSongString(duration)
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
        duration = 0
        if (message.type == BASE_MESSAGE_TYPE.Q) {
            val audioTypeObj = message.question!!.audioList!![0]
            this.duration = audioTypeObj.duration
            if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED) {
                if (audioTypeObj.downloadedLocalPath.isNullOrEmpty().not() && AppDirectory.isFileExist(
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

        } else {
            if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADED || message.downloadStatus === DOWNLOAD_STATUS.UPLOADED) {
                mediaDownloaded()
            } else if (message.downloadStatus === DOWNLOAD_STATUS.DOWNLOADING) {
                mediaDownloading()
                audioPlayerInterface?.downloadStart(message.url!!)
            } else if (message.downloadStatus === DOWNLOAD_STATUS.UPLOADING) {
                mediaUploading()
            } else {
                mediaNotDownloaded()
            }
        }
    }

    private fun mediaUploading() {
        seekBar.visibility = View.INVISIBLE
        seekBar_ph?.visibility = View.VISIBLE
        download_container.visibility = View.VISIBLE
        progressBarPlayer.visibility = View.VISIBLE
        cancelDownload.visibility = View.VISIBLE
        startDownload.visibility = View.INVISIBLE
        btnPlay?.visibility = View.INVISIBLE
        btnPause?.visibility = View.INVISIBLE
    }

    private fun mediaNotAvailableDownloaded() {
        seekBar.visibility = View.VISIBLE
        seekBar_ph?.visibility = View.INVISIBLE
        download_container.visibility = View.INVISIBLE
        startDownload.visibility = View.INVISIBLE
        progressBarPlayer.visibility = View.INVISIBLE
        cancelDownload.visibility = View.INVISIBLE
        btnPlay?.visibility = View.VISIBLE
        btnPause?.visibility = View.INVISIBLE
        txtCurrentDuration.text = EMPTY
        if (duration > 0) {
            txtCurrentDuration.text = toTimeSongString(duration)
        } else {
            txtCurrentDuration.text = "00:00"
        }
    }

    private fun mediaNotDownloaded() {
        download_container.visibility = View.VISIBLE
        seekBar_ph?.visibility = View.VISIBLE
        startDownload.visibility = View.VISIBLE
        seekBar.visibility = View.INVISIBLE
        progressBarPlayer.visibility = View.INVISIBLE
        cancelDownload.visibility = View.INVISIBLE
        btnPlay?.visibility = View.INVISIBLE
        btnPause?.visibility = View.INVISIBLE
        txtCurrentDuration.text = EMPTY
        if (duration > 0) {
            txtCurrentDuration.text = toTimeSongString(duration)
        }
    }

    private fun mediaDownloading() {
        download_container.visibility = View.VISIBLE
        progressBarPlayer.visibility = View.VISIBLE
        cancelDownload.visibility = View.VISIBLE
        seekBar_ph?.visibility = View.VISIBLE
        startDownload.visibility = View.INVISIBLE
    }

    private fun mediaDownloaded() {
        btnPlay?.visibility = View.VISIBLE
        seekBar.visibility = View.VISIBLE
        btnPause?.visibility = View.INVISIBLE
        seekBar_ph?.visibility = View.INVISIBLE
        download_container.visibility = View.INVISIBLE
        updateUri()
    }

    private fun updateUri() {
        cAudioObj = getAudioObject()
        kill()
        if (cAudioObj != null) {
            val jcAudios = java.util.ArrayList<JcAudio>()
            jcAudios.add(cAudioObj!!)
            initPlaylist(jcAudios, this@JcPlayerView)
        }
        if (duration > 0) {
            txtCurrentDuration.text = toTimeSongString(duration)
        }
    }


    private fun getAudioObject(): JcAudio? {
        try {
            if (message.type == BASE_MESSAGE_TYPE.Q) {
                cAudioObj =
                    JcAudio.createFromFilePath(message.question!!.audioList!![0].downloadedLocalPath!!)
                isMedia = true
                this.duration = message.question!!.audioList!![0].duration
            } else {
                cAudioObj = JcAudio.createFromFilePath(message.downloadedLocalPath!!)
                isMedia = true
                this.duration =
                    Utils.getDurationOfMedia(context, message.downloadedLocalPath!!)!!.toInt()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
        return cAudioObj
    }


    private fun updateController() {
        if (isMedia) {
            AppObjectController.uiHandler.post {
                btnPlay?.visibility = View.VISIBLE
                btnPause?.visibility = View.INVISIBLE
                //seekBar?.progress = 0

            }
        }


    }
/*
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        compositeDisposable.add(RxBus2.listen(AudioPlayerPauseEventBus::class.java).subscribe {
            it?.audioId?.let { audioId ->
                if (audioId != message.chatId) {
                 //   updateController()
                }
            }
        })
    }*/

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
        pause()
    }

    private fun getDrawablePadding() = com.vanniktech.emoji.Utils.dpToPx(context, 4f)


    private fun updateTime(text_message_time: TextView) {
        text_message_time.text =
            Utils.getMessageTimeInHours(message.created).toUpperCase(Locale.getDefault())

        if (message.sender?.id.equals(Mentor.getInstance().getId(), ignoreCase = true)) {
            text_message_time.compoundDrawablePadding = getDrawablePadding()
            if (message.isSync.not()) {
                text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_unsync_msz,
                    0
                )
                return
            }
            when {
                message.messageDeliverStatus == MESSAGE_DELIVER_STATUS.SENT -> {

                    text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_tick,
                        0
                    )
                }
                message.messageDeliverStatus == MESSAGE_DELIVER_STATUS.SENT_RECEIVED -> text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                    0,
                    0,
                    R.drawable.ic_sent_message_d_tick,
                    0
                )
                else -> {
                    text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        R.drawable.ic_sent_message_s_r_tick,
                        0
                    )
                }
            }


        } else {
            text_message_time.setCompoundDrawablesWithIntrinsicBounds(
                0,
                0,
                0,
                0
            )
        }
    }

}
