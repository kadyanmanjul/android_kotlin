package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.content.Context
import android.view.Gravity
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.DD_MM_YYYY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.repository.local.eventbus.RemovePracticeAudioEventBus
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.muddzdev.styleabletoast.StyleableToast
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable
import java.util.Date
import java.util.Locale
import kotlin.random.Random

@Layout(R.layout.practice_audio_item)
class PraticeAudioViewHolder(
    private var practiceEngagement: PracticeEngagement?,
    private var context: Context?,
    private var filePath: String?
) : AudioPlayerEventListener, ExoAudioPlayer.ProgressUpdateListener {

    @View(R.id.audio_view_container)
    lateinit var rootView: RelativeLayout

    @View(R.id.btn_play_info)
    lateinit var playPauseBtn: MaterialPlayPauseButton

    @View(R.id.practise_seekbar)
    lateinit var seekBar: SeekBar

    @View(R.id.txt_info_duration)
    lateinit var time: AppCompatTextView

    @View(R.id.submit_txt_info_date)
    lateinit var date: AppCompatTextView

    @View(R.id.iv_cancel)
    lateinit var ivCancel: AppCompatImageView

    private var mUserIsSeeking = false

    fun getAppContext() = AppObjectController.joshApplication

    private var audioManager: ExoAudioPlayer? = null

    @Resolve
    fun onResolved() {

        if (practiceEngagement != null) {
            date.text = practiceEngagement!!.practiceDate
            time.text = practiceEngagement!!.id.toString()
            practiceEngagement?.answerUrl?.let {
                filePath = it
            }

            if (PermissionUtils.isStoragePermissionEnabled(context!!) && AppDirectory.isFileExist(
                    practiceEngagement?.localPath
                )
            ) {
                filePath = practiceEngagement?.localPath
                seekBar.max =
                    Utils.getDurationOfMedia(context!!, filePath!!)
                        ?.toInt() ?: 0
            } else {
                if (practiceEngagement?.duration != null) {
                    seekBar.max = practiceEngagement!!.duration!!
                } else {
                    seekBar.max = 1_00_000
                }
            }
            ivCancel.visibility = android.view.View.GONE
        } else {
            date.text = DD_MM_YYYY.format(Date()).toLowerCase(Locale.getDefault())
            ivCancel.visibility = android.view.View.VISIBLE
        }
        initializePractiseSeekBar()

    }

    fun initializePractiseSeekBar() {
        seekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                var userSelectedPosition = 0
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = true
                }

                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        userSelectedPosition = progress
                    }
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    mUserIsSeeking = false
                    if (audioManager?.currentPlayingUrl == filePath)
                        audioManager?.seekTo(userSelectedPosition.toLong())
                }
            })
    }

    fun setSeekToZero() {
        seekBar.max =
            Utils.getDurationOfMedia(context!!, filePath!!)?.toInt() ?: 0
    }

    fun hideCancelButtons() {
        ivCancel.visibility = android.view.View.GONE
    }

    fun isEmpty() :Boolean {
        return practiceEngagement==null
    }

    fun onPlayAudio(audioObject: AudioType) {
        val audioList = java.util.ArrayList<AudioType>()
        audioList.add(audioObject)
        audioManager = ExoAudioPlayer.getInstance()
        audioManager?.playerListener = this
        audioManager?.play(filePath!!)
        filePath?.let {
            if(it!=audioManager?.currentPlayingUrl){
                audioManager?.play(filePath!!)
            }
        }
        audioManager?.setProgressUpdateListener(this)
        if (filePath.isNullOrEmpty().not()) {
            playPauseBtn.state = MaterialPlayPauseDrawable.State.Pause
        }
    }

    @Click(R.id.btn_play_info)
    fun playSubmitPracticeAudio() {
        try {
            val audioType = AudioType()
            audioType.audio_url = filePath!!
            audioType.downloadedLocalPath = filePath!!
            audioType.duration =
                Utils.getDurationOfMedia(context!!, filePath!!)?.toInt() ?: 0
            audioType.id = Random.nextInt().toString()

            val state =
                if (playPauseBtn.state == MaterialPlayPauseDrawable.State.Pause && audioManager!!.isPlaying()) {
                    audioManager?.setProgressUpdateListener(this)
                    MaterialPlayPauseDrawable.State.Play
                } else {
                    MaterialPlayPauseDrawable.State.Pause
                }
            playPauseBtn.state = state

            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication)
                    .gravity(Gravity.BOTTOM)
                    .text(context!!.getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }
            if (audioManager?.currentPlayingUrl?.isNotEmpty() == true && audioManager?.currentPlayingUrl == audioType.audio_url) {

                if (checkIsPlayer()) {
                    filePath?.let {
                        if(it!=audioManager?.currentPlayingUrl){
                            audioType.audio_url=filePath!!
                        }
                    }
                    audioManager?.setProgressUpdateListener(this)
                    audioManager?.resumeOrPause()
                } else {
                    onPlayAudio(audioType)
                }
            } else {
                onPlayAudio(audioType)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    @Click(R.id.iv_cancel)
    fun removeAudioPractise() {
        RxBus2.publish(RemovePracticeAudioEventBus(practiceEngagement?.id, this))
        /*filePath = null
        //currentAudio = null
        //binding.practiseSubmitLayout.visibility = android.view.View.GONE
        //binding.submitAudioViewContainer.visibility = android.view.View.GONE
        //isAudioRecordDone = false

        seekBar?.progress = 0
        seekBar?.max = 0
        playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }*/
        //disableSubmitButton()
    }

    fun isSeekBaarInitialized(): Boolean {
        return ::seekBar.isInitialized
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    override fun onPlayerPause() {
        if (audioManager?.isPlaying()!!)
            playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
    }

    override fun onPlayerResume() {
        if (audioManager?.isPlaying()!!)
            playPauseBtn.state = MaterialPlayPauseDrawable.State.Pause
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
        TODO("Not yet implemented")
    }

    override fun onTrackChange(tag: String?) {
        TODO("Not yet implemented")
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
        TODO("Not yet implemented")
    }

    override fun onPositionDiscontinuity(reason: Int) {
        TODO("Not yet implemented")
    }

    override fun onPlayerReleased() {
        TODO("Not yet implemented")
    }

    override fun onPlayerEmptyTrack() {
        TODO("Not yet implemented")
    }

    override fun complete() {
        playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
        seekBar.progress = 0
        audioManager?.seekTo(0)
        audioManager?.onPause()
        audioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        seekBar.progress = progress.toInt()
    }

    override fun onDurationUpdate(duration: Long?) {
        duration?.toInt()?.let { seekBar.max = it }
    }
}