package com.joshtalks.joshskills.ui.day_wise_course.practice

import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.exoplayer2.Player
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.custom_ui.PointSnackbar
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.service.WorkManagerAdmin
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentPraticeBinding
import com.joshtalks.joshskills.repository.local.entity.AudioType
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.entity.EXPECTED_ENGAGE_TYPE
import com.joshtalks.joshskills.repository.local.entity.NPSEvent
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.RequestEngage
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.joshtalks.joshskills.util.ExoAudioPlayer
import com.joshtalks.joshskills.util.ExoAudioPlayer.ProgressUpdateListener
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.concurrent.TimeUnit
import kotlin.random.Random

const val PRACTISE_OBJECT = "practise_object"

class NewPracticeFragment : CoreJoshFragment(), Player.EventListener, AudioPlayerEventListener,
    ProgressUpdateListener, PracticeAdapter.PracticeClickListeners {
    private var currentPlayingPosition: Int = -1
    private lateinit var adapter: PracticeAdapter

    private lateinit var binding: FragmentPraticeBinding
    private var chatModelList: ArrayList<ChatModel>? = null
    private var isAudioRecordDone = false
    private var isVideoRecordDone = false
    private var isDocumentAttachDone = false
    private var startTime: Long = 0
    private var totalTimeSpend: Long = 0
    private var filePath: String? = null
    private var audioManager: ExoAudioPlayer? = null
    private var currentChatModel: ChatModel? = null

    private var activityCallback: CapsuleActivityCallback? = null

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

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            chatModelList = arguments?.getParcelableArrayList(PRACTISE_OBJECT)
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
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_pratice, container, false)
        binding.lifecycleOwner = this
        binding.handler = this

        adapter = PracticeAdapter(requireContext(), practiceViewModel, chatModelList!!, this)
        binding.practiceRv.layoutManager = LinearLayoutManager(requireContext())
        binding.practiceRv.adapter = adapter

        addObserver()
        binding.progressLayout.setOnClickListener {

        }

        binding.vocabularyCompletedTv.text = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.VOCABULARY_COMPLETED)
        return binding.root
    }

    private fun addObserver() {
        practiceViewModel.requestStatusLiveData.observe(viewLifecycleOwner, {
            if (it) {
                CoroutineScope(Dispatchers.IO).launch {
                    currentChatModel?.question?.interval?.run {
                        WorkManagerAdmin.determineNPAEvent(NPSEvent.PRACTICE_COMPLETED, this)
                    }
                }

                activityCallback?.onQuestionStatusUpdate(
                    QUESTION_STATUS.AT,
                    currentChatModel?.question?.questionId?.toIntOrNull() ?: 0
                )

                var openNextScreen = true
                chatModelList?.forEach { item ->
                    if (item.question?.status == QUESTION_STATUS.NA) {
                        openNextScreen = false
                        return@forEach
                    }
                }

                if (openNextScreen && isVisible)
                    binding.vocabularyCompleteLayout.visibility = View.VISIBLE

                currentChatModel = null

                adapter.notifyDataSetChanged()

            }

            binding.progressLayout.visibility = View.GONE
        })

        practiceViewModel.practiceEngagementData.observe(viewLifecycleOwner, Observer {
            if(it.pointsList.isNullOrEmpty().not()){
                PointSnackbar.make(binding.rootView, Snackbar.LENGTH_LONG,it.pointsList?.get(0))?.show()
            }
        })
    }

    override fun onPlayerPause() {
    }

    override fun onPlayerResume() {
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    fun onContinueClick() {
        activityCallback?.onNextTabCall(2)
    }

    fun onCloseDialog() {
        binding.vocabularyCompleteLayout.visibility = View.GONE
    }

    override fun complete() {
        audioManager?.onPause()
        audioManager?.setProgressUpdateListener(null)
    }

    override fun onProgressUpdate(progress: Long) {
        currentChatModel?.playProgress = progress.toInt()
        if (currentPlayingPosition != -1) {
            adapter.notifyItemChanged(currentPlayingPosition)
        }
    }

    override fun onDurationUpdate(duration: Long?) {
        currentChatModel?.playProgress = duration?.toInt() ?: 0
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
            val audioList = ArrayList<AudioType>()
            audioList.add(audioObject)
            audioManager = ExoAudioPlayer.getInstance()
            audioManager?.playerListener = this
            audioManager?.play(audioObject.audio_url)
            audioManager?.setProgressUpdateListener(this)
        }

        chatModel.isPlaying = chatModel.isPlaying.not()
        adapter.notifyDataSetChanged()
    }

    fun removeAudioPractise() {
        filePath = null
        coreJoshActivity?.currentAudio = null
        isAudioRecordDone = false
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }

    }

    override fun playPracticeAudio(chatModel: ChatModel, position: Int) {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                .text(getString(R.string.volume_up_message)).cornerRadius(16)
                .length(Toast.LENGTH_LONG)
                .solidBackground().show()
        }

        if (currentChatModel == null) {
            chatModel.question?.audioList?.getOrNull(0)
                ?.let { onPlayAudio(chatModel, it, position) }
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
        if (isAudioPlaying()) {
            audioManager?.resumeOrPause()
        }
    }

    override fun submitPractice(chatModel: ChatModel): Boolean {
        if (chatModel.question != null && chatModel.question!!.expectedEngageType != null) {
            val engageType = chatModel.question?.expectedEngageType
            chatModel.question?.expectedEngageType?.let {
                if (EXPECTED_ENGAGE_TYPE.TX == it) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.AU == it && chatModel.filePath == null) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.VI == it && isVideoRecordDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                } else if (EXPECTED_ENGAGE_TYPE.DX == it && isDocumentAttachDone.not()) {
                    showToast(getString(R.string.submit_practise_msz))
                    return false
                }

                currentChatModel = chatModel
                val requestEngage = RequestEngage()
//                requestEngage.text = binding.etPractise.text.toString()
                requestEngage.localPath = chatModel.filePath
                requestEngage.duration =
                    Utils.getDurationOfMedia(requireActivity(), chatModel.filePath)?.toInt()
                requestEngage.feedbackRequire = chatModel.question?.feedback_require
                requestEngage.question = chatModel.question?.questionId!!
                requestEngage.mentor = Mentor.getInstance().getId()
                if (it == EXPECTED_ENGAGE_TYPE.AU || it == EXPECTED_ENGAGE_TYPE.VI || it == EXPECTED_ENGAGE_TYPE.DX) {
                    requestEngage.answerUrl = chatModel.filePath
                }
                practiceViewModel.submitPractise(chatModel, requestEngage, engageType)

                binding.progressLayout.visibility = View.VISIBLE

                return true
            }
        }
        return false
    }

    override fun onSeekChange(seekTo: Long) {
        audioManager?.seekTo(seekTo)
    }

    override fun startRecording(chatModel: ChatModel, position: Int, startTimeUnit: Long) {
        this.startTime = startTimeUnit
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        practiceViewModel.startRecordAudio(null)
    }

    override fun stopRecording(chatModel: ChatModel, position: Int, stopTime: Long) {
        practiceViewModel.stopRecordingAudio(false)
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val timeDifference =
            TimeUnit.MILLISECONDS.toSeconds(stopTime) - TimeUnit.MILLISECONDS.toSeconds(
                startTime
            )
        if (timeDifference > 1) {
            practiceViewModel.recordFile?.let {
//                                isAudioRecordDone = true
                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                chatModel.filePath = filePath
                AppDirectory.copy(it.absolutePath, filePath!!)
            }

        }
    }

    override fun askRecordPermission() {

        PermissionUtils.audioRecordStorageReadAndWritePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let {
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(
                                requireActivity(),
                                R.string.record_permission_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    override fun focusChild(position: Int) {
        binding.practiceRv.smoothScrollToPosition(position)
    }
}
