package com.joshtalks.joshskills.ui.day_wise_course.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.databinding.FragmentPraticeBinding
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.day_wise_course.adapter.PracticeAdapter
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.ExoAudioPlayer.ProgressUpdateListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.disposables.CompositeDisposable
import java.util.ArrayList
import kotlin.random.Random

const val PRACTISE_OBJECT = "practise_object"
const val IMAGE_OR_VIDEO_SELECT_REQUEST_CODE = 1081
const val TEXT_FILE_ATTACHMENT_REQUEST_CODE = 1082

class NewPracticeFragment : CoreJoshFragment(), Player.EventListener, AudioPlayerEventListener,
    ProgressUpdateListener, PracticeAdapter.PracticeClickListeners {
    private var currentPlayingPosition: Int = -1
    private lateinit var adapter: PracticeAdapter
    private var compositeDisposable = CompositeDisposable()

    private lateinit var binding: FragmentPraticeBinding
    private var chatModelList: ArrayList<ChatModel>? = null
    private var sBound = false
    private var mUserIsSeeking = false
    private var isAudioRecordDone = false
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var scaleAnimation: Animation? = null
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private lateinit var appAnalytics: AppAnalytics
    private var audioManager: ExoAudioPlayer? = null
    private var currentChatModel: ChatModel? = null


    private val DOCX_FILE_MIME_TYPE = arrayOf(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/msword", "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "text/*",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.oasis.opendocument.text",
        "application/vnd.oasis.opendocument.spreadsheet"
    )


    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }

    companion object {
        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = NewPracticeFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList<ChatModel>(PRACTISE_OBJECT)
        }
        if (chatModelList == null) {
            requireActivity().finish()
        }
        totalTimeSpend = System.currentTimeMillis()

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_pratice, container, false)
        binding.lifecycleOwner = this
        binding.handler = this

        adapter = PracticeAdapter(requireContext(), chatModelList!!, this)
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter

        return binding.root
    }

    override fun onPlayerPause() {
        TODO("Not yet implemented")
    }

    override fun onPlayerResume() {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    override fun onProgressUpdate(progress: Long) {
        currentChatModel?.playProgress = progress.toInt()
        if (currentPlayingPosition != -1) {
            adapter.notifyItemChanged(currentPlayingPosition)
        }
    }

    override fun onDurationUpdate(duration: Long?) {
        TODO("Not yet implemented")
    }

    private fun checkIsPlayer(): Boolean {
        return audioManager != null
    }

    private fun isAudioPlaying(): Boolean {
        return this.checkIsPlayer() && this.audioManager!!.isPlaying()
    }

    private fun onPlayAudio(chatModel: ChatModel, audioObject: AudioType, position: Int) {
        currentPlayingPosition = position
        if (currentChatModel != null && currentChatModel?.isPlaying == true) {
            audioManager?.onPause()
        } else {
            currentChatModel = chatModel
            val audioList = java.util.ArrayList<AudioType>()
            audioList.add(audioObject)
            audioManager = ExoAudioPlayer.getInstance()
            audioManager?.playerListener = this
            audioManager?.play(audioObject.audio_url)
            audioManager?.setProgressUpdateListener(this)
        }

        chatModel.isPlaying = chatModel.isPlaying.not()
        adapter.notifyDataSetChanged()
    }

    fun playPracticeAudio() {
    }

    fun removeAudioPractise() {
        filePath = null
        coreJoshActivity?.currentAudio = null
        isAudioRecordDone = false
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }
        appAnalytics.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio practise removed")

    }

    override fun playPracticeAudio(chatModel: ChatModel, position: Int) {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }
        appAnalytics.addParam(AnalyticsEvent.PRACTICE_EXTRA.NAME, "Audio Played")

        if (currentChatModel == null) {
            onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!, position)
        } else {
            if (currentChatModel == chatModel) {
                if (checkIsPlayer()) {
                    audioManager?.setProgressUpdateListener(this)
                    audioManager?.resumeOrPause()
                } else {
                    onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!, position)
                }
            } else {
                onPlayAudio(chatModel, chatModel.question?.audioList?.getOrNull(0)!!, position)
            }
        }
    }

    override fun playSubmitPracticeAudio(chatModel: ChatModel, position: Int) {
        try {
            val audioType = AudioType()
            audioType.audio_url = filePath!!
            audioType.downloadedLocalPath = filePath!!
            audioType.duration =
                Utils.getDurationOfMedia(requireContext(), filePath!!)?.toInt() ?: 0
            audioType.id = Random.nextInt().toString()
            appAnalytics.addParam(
                AnalyticsEvent.PRACTICE_EXTRA.NAME,
                "Already Submitted audio Played"
            )

            if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
                StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
            }

            if (currentChatModel == null) {
                onPlayAudio(chatModel, audioType, position)
            } else {
                if (currentChatModel == chatModel) {
                    if (checkIsPlayer()) {
                        audioManager?.setProgressUpdateListener(this)
                        chatModel.isPlaying = chatModel.isPlaying.not()
                        audioManager?.resumeOrPause()
                        adapter.notifyDataSetChanged()
                    } else {
                        onPlayAudio(chatModel, audioType, position)
                    }
                } else {
                    onPlayAudio(chatModel, audioType, position)

                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

    }

    override fun removeAudioPractise(chatModel: ChatModel) {
        TODO("Not yet implemented")
    }

    override fun submitPractise(chatModel: ChatModel) {
        TODO("Not yet implemented")
    }

    override fun onSeekChange(seekTo: Long) {
        audioManager?.seekTo(seekTo)
    }
}
