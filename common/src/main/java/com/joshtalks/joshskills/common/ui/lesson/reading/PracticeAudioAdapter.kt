package com.joshtalks.joshskills.common.ui.lesson.reading

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.DD_MM_YYYY
import com.joshtalks.joshskills.common.core.PermissionUtils
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.common.core.io.AppDirectory
import com.joshtalks.joshskills.common.databinding.PracticeAudioItemBinding
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.entity.AudioType
import com.joshtalks.joshskills.common.repository.local.entity.PracticeEngagement
import com.joshtalks.joshskills.common.repository.local.entity.PracticeEngagementWrapper
import com.joshtalks.joshskills.common.repository.local.eventbus.RemovePracticeAudioEventBus
import com.joshtalks.joshskills.common.util.ExoAudioPlayer
import com.muddzdev.styleabletoast.StyleableToast
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseButton
import me.zhanghai.android.materialplaypausedrawable.MaterialPlayPauseDrawable

class PracticeAudioAdapter(
    private var context: Context?
) : RecyclerView.Adapter<PracticeAudioAdapter.PracticeAudioViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PracticeAudioViewHolder {
        val view = DataBindingUtil.inflate<PracticeAudioItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.practice_audio_item,
            parent,
            false
        )
        return PracticeAudioViewHolder(view)
    }

    override fun onBindViewHolder(holder: PracticeAudioViewHolder, position: Int) {
        holder.bind(list.get(position)!!, position)
    }

    private var list: ArrayList<PracticeEngagementWrapper> = arrayListOf()
    override fun getItemCount(): Int = list.size

    fun updateList(list: ArrayList<PracticeEngagementWrapper>) {
        this.list = list
    }

    fun addNewItem (practiceEngagementWrapper:PracticeEngagementWrapper) {
        val list = ArrayList<PracticeEngagementWrapper>()
        list.add(practiceEngagementWrapper)
        this.list = list
        notifyDataSetChanged()
    }

    fun removeItem (practiceEngagementWrapper:PracticeEngagementWrapper) {
        val list = ArrayList(list)
        list.remove(practiceEngagementWrapper)
        this.list = list
        notifyDataSetChanged()
    }

    fun removeItemFromList(position: Int) {
        list.removeAt(position)
        notifyItemRemoved(position)
    }

    inner class PracticeAudioViewHolder(val view: PracticeAudioItemBinding) :
        RecyclerView.ViewHolder(view.root), AudioPlayerEventListener,
        com.joshtalks.joshskills.common.util.ExoAudioPlayer.ProgressUpdateListener {
        private var mUserIsSeeking = false

        fun getAppContext() = AppObjectController.joshApplication

        private var audioManager: com.joshtalks.joshskills.common.util.ExoAudioPlayer? = null
        private val rootView: RelativeLayout by lazy { view.audioViewContainer }
        private val playPauseBtn: MaterialPlayPauseButton by lazy { view.btnPlayInfo }
        private val seekBar: SeekBar by lazy { view.practiseSeekbar }
        private val time: AppCompatTextView by lazy { view.txtInfoDuration }
        private val date: AppCompatTextView by lazy { view.submitTxtInfoDate }
        private val ivCancel: AppCompatImageView by lazy { view.ivCancel }
        private var practiceEngagement: PracticeEngagement? = null
        private var filePath: String? = null

        fun bind(engagement: PracticeEngagementWrapper, position: Int) {
            this.practiceEngagement = engagement.practiceEngagement
            this.filePath = engagement.filePath
            playPauseBtn.setOnClickListener {
                playSubmitPracticeAudio()
            }
            ivCancel.setOnClickListener {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(RemovePracticeAudioEventBus(position))
            }
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
                date.text = DD_MM_YYYY.format(Date()).lowercase(Locale.getDefault())
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

                    override fun onProgressChanged(
                        seekBar: SeekBar,
                        progress: Int,
                        fromUser: Boolean
                    ) {
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

        fun playSubmitPracticeAudio() {
            if (ReadingFragmentWithoutFeedback.isAudioRecording.not()) {
                try {
                    val audioType = AudioType()
                    audioType.audio_url = filePath!!
                    audioType.downloadedLocalPath = filePath!!
                    audioType.duration =
                        Utils.getDurationOfMedia(context!!, filePath!!)?.toInt() ?: 0
                    audioType.id = Random.nextInt().toString()

                    val state =
                        if (playPauseBtn.state == MaterialPlayPauseDrawable.State.Pause && audioManager?.isPlaying() == true) {
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
                                if (it != audioManager?.currentPlayingUrl) {
                                    audioType.audio_url = filePath!!
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
        }

        private fun checkIsPlayer(): Boolean {
            return audioManager != null
        }

        fun setSeekToZero() {
            seekBar.max =
                Utils.getDurationOfMedia(context!!, filePath!!)?.toInt() ?: 0
        }

        fun hideCancelButtons() {
            ivCancel.visibility = android.view.View.GONE
        }

        fun isEmpty(): Boolean {
            return practiceEngagement == null
        }

        fun updatePracticeEngagement(practiceEngagement: PracticeEngagement) {
            this.practiceEngagement = practiceEngagement
        }

        fun setPlayPauseBtnState(materialPlayPauseButtonState: MaterialPlayPauseDrawable.State) {
            playPauseBtn.state = materialPlayPauseButtonState
        }

        fun pauseAudio() {
            audioManager?.isPlaying()?.let {
                if (it) {
                    audioManager?.onPause()
                    playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
                }
            }
        }

        fun onPlayAudio(audioObject: AudioType) {
            val audioList = java.util.ArrayList<AudioType>()
            audioList.add(audioObject)
            audioManager = com.joshtalks.joshskills.common.util.ExoAudioPlayer.getInstance()
            audioManager?.playerListener = this
            audioManager?.play(filePath!!)
            filePath?.let {
                if (it != audioManager?.currentPlayingUrl) {
                    audioManager?.play(filePath!!)
                }
            }
            audioManager?.setProgressUpdateListener(this)
            if (filePath.isNullOrEmpty().not()) {
                playPauseBtn.state = MaterialPlayPauseDrawable.State.Pause
            }
        }

        override fun onPlayerPause() {
            if (audioManager?.isPlaying() == true)
                playPauseBtn.state = MaterialPlayPauseDrawable.State.Play
        }

        override fun onPlayerResume() {
            if (audioManager?.isPlaying() == true)
                playPauseBtn.state = MaterialPlayPauseDrawable.State.Pause
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
}
