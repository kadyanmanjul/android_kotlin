package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.interfaces.OnConversationPractiseSubmit
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.core.textColorSet
import com.joshtalks.joshskills.databinding.FragmentRecordPractiseBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseSubmitEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import com.joshtalks.joshskills.ui.conversation_practice.extra.ConversationSubmitBottomSheet
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import java.util.ArrayList
import java.util.LinkedList

class RecordPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener,
    OnConversationPractiseSubmit {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: FragmentRecordPractiseBinding
    private var audioPractiseAdapter: AudioPractiseAdapter? = null
    private val audioList: LinkedList<AudioModel> = LinkedList()
    private var recordListenList: ArrayList<ListenModel> = arrayListOf()

    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(ConversationPracticeViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
            recordListenList =
                (ArrayList(conversationPractiseModel.listen.sortedBy { sort -> sort.sortOrder }))

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_record_practise,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        initView()
        initAudioList()
        logRecordPractiseAnalyticsEvents()
    }

    private fun logRecordPractiseAnalyticsEvents() {
        AppAnalytics.create(AnalyticsEvent.RECORD_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("flow", "Conversational prac")
            .push()
    }

    private fun logPatnerSelectedEvent(patner: String) {
        AppAnalytics.create(AnalyticsEvent.PATNER_SELECTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("patner side", patner)
            .addParam("flow", "record practise")
            .push()
    }

    private fun logRecordStartedEvent() {
        AppAnalytics.create(AnalyticsEvent.RECORD_STARTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("flow", "record practise")
            .push()
    }

    private fun initView() {
        binding.audioPlayer.setAudioPlayerEventListener(this)
        binding.audioPlayer.addAudios(LinkedList())
        binding.ivFirstUser.setImage(conversationPractiseModel.characterUrlA)
        binding.ivSecondUser.setImage(conversationPractiseModel.characterUrlB)
        binding.tvFirstUser.text = conversationPractiseModel.characterNameA
        binding.tvSecondUser.text = conversationPractiseModel.characterNameB
        binding.placeholderBg.setOnClickListener {
            if (viewModel.practiseWho == null) {
                showToast(getString(R.string.select_character), Toast.LENGTH_LONG)
                return@setOnClickListener
            }
            initAudioPlayer(viewModel.practiseWho)
        }
    }

    private fun initRV() {
        binding.recyclerView.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        binding.recyclerView.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        binding.recyclerView.layoutManager = SmoothLinearLayoutManager(context)
        binding.recyclerView.addItemDecoration(
            LayoutMarginDecoration(
                Utils.dpToPx(
                    requireContext(),
                    6f
                )
            )
        )
        audioPractiseAdapter = AudioPractiseAdapter().apply {
            setHasStableIds(true)
        }
        binding.recyclerView.adapter = audioPractiseAdapter
    }

    private fun initAudioList() {
        conversationPractiseModel.listen.sortedBy { it.sortOrder }.forEach {
            audioList.add(
                AudioModel(
                    it.audio.audio_url,
                    it.id.toString(),
                    it.audio.duration,
                    subTag = it.name
                )
            )
        }
    }

    fun requestForRecording() {
        if (PermissionUtils.isAudioAndStoragePermissionEnable(requireActivity()).not()) {
            PermissionUtils.audioRecordStorageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                startRecording()
                                return
                            }
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
        } else {
            startRecording()
        }
    }


    private fun initAudioPlayer(practiseWho: PractiseUser?) {
        practiseWho?.run {
            when (this) {
                PractiseUser.FIRST -> {
                    audioList.listIterator().forEach {
                        it.isSilent = it.subTag.equals(
                            conversationPractiseModel.characterNameA,
                            ignoreCase = true
                        )
                    }
                }
                PractiseUser.SECOND -> {
                    audioList.listIterator().forEach {
                        it.isSilent = it.subTag.equals(
                            conversationPractiseModel.characterNameB,
                            ignoreCase = true
                        )
                    }
                }
            }
        }
        binding.audioPlayer.addAudios(audioList)
    }

    fun practiseWithFirstUser() {
        if (viewModel.isRecordingStarted()) {
            return
        }
        if (viewModel.isRecord) {
            binding.ivTickFirstUser.visibility = View.GONE
            enableView(binding.ivSecondUser)
            resetPlayer()
        } else {
            viewModel.practiseWho = PractiseUser.FIRST
            viewModel.isRecord = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            disableView(binding.ivSecondUser)
            filterProperty(ViewTypeForPractiseUser.SECOND.type)
            initAudioPlayer(PractiseUser.FIRST)
            changeStatusOfButton(true)
            nameStateViewChange()
        }
        logPatnerSelectedEvent("first")
    }

    fun practiseWithSecondUser() {
        if (viewModel.isRecordingStarted()) {
            return
        }
        if (viewModel.isRecord) {
            binding.ivTickSecondUser.visibility = View.GONE
            enableView(binding.ivFirstUser)
            resetPlayer()
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isRecord = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            disableView(binding.ivFirstUser)
            filterProperty(ViewTypeForPractiseUser.FIRST.type)
            initAudioPlayer(PractiseUser.SECOND)
            changeStatusOfButton(true)
            nameStateViewChange()
        }
        logPatnerSelectedEvent("second")
    }


    private fun resetAllState() {
        RxBus2.publish(ViewPagerDisableEventBus(false))
        binding.ivTickFirstUser.visibility = View.GONE
        binding.ivTickSecondUser.visibility = View.GONE
        binding.btnRecord.backgroundTintList =
            ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.light_grey)
        enableView(binding.ivFirstUser)
        enableView(binding.ivSecondUser)
        viewModel.practiseWho = null
        viewModel.isRecord = false
        audioPractiseAdapter?.clear()
        filterProperty(null)
        changeStatusOfButton(false)
        nameStateViewChange()
    }

    private fun nameStateViewChange() {
        if (viewModel.practiseWho == null) {
            binding.tvFirstUser.text = conversationPractiseModel.characterNameA
            binding.tvSecondUser.text = conversationPractiseModel.characterNameB
            binding.tvFirstUser.textColorSet(R.color.black)
            binding.tvSecondUser.textColorSet(R.color.black)
        } else {
            if (viewModel.practiseWho == PractiseUser.FIRST) {
                binding.tvFirstUser.text = getString(R.string.me)
                binding.tvFirstUser.textColorSet(R.color.button_primary_color)
            } else {
                binding.tvSecondUser.text = getString(R.string.me)
                binding.tvSecondUser.textColorSet(R.color.button_primary_color)
            }
        }
    }

    private fun resetPlayer() {
        viewModel.practiseWho = null
        viewModel.isRecord = false
        audioPractiseAdapter?.clear()
        filterProperty(null)
        nameStateViewChange()
        changeStatusOfButton(false)

    }

    private fun enableView(view: View) {
        view.isClickable = true
        view.isEnabled = true
        view.alpha = ALPHA_MAX
        binding.placeholderBg.visibility = View.VISIBLE
    }

    private fun disableView(view: View) {
        view.isClickable = false
        view.isEnabled = false
        view.alpha = ALPHA_MIN
        binding.placeholderBg.visibility = View.GONE
    }

    private fun changeStatusOfButton(active: Boolean) {
        if (active) {
            binding.btnRecord.backgroundTintList =
                ContextCompat.getColorStateList(
                    AppObjectController.joshApplication,
                    R.color.button_primary_color
                )

        } else {
            binding.btnRecord.backgroundTintList =
                ContextCompat.getColorStateList(
                    AppObjectController.joshApplication,
                    R.color.light_grey
                )
        }
    }

    private fun filterProperty(viewType: Int?) {
        recordListenList.forEach {
            it.disable = false
            it.hasPractising = false
        }
        if (viewType != null) {
            recordListenList.filter { it.viewType == viewType }.forEach {
                it.disable = true
            }
            recordListenList.filter { it.viewType != viewType }.forEach {
                it.hasPractising = true
            }
        }
    }

    private fun startRecording() {
        if (viewModel.practiseWho == null) {
            showToast(getString(R.string.select_your_character))
            return
        }

        if (viewModel.isRecordingRunning) {
            binding.audioPlayer.onPause()
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.button_primary_color
            )
            viewModel.isRecordingRunning = false
            viewModel.stopRecording(false)
        } else {
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.recording_9D
            )
            viewModel.startRecord(object :
                OnAudioRecordListener {
                override fun onRecordingStarted() {
                    AppObjectController.uiHandler.post {
                        RxBus2.publish(ViewPagerDisableEventBus(true))
                        viewModel.isRecordingRunning = true
                        binding.audioPlayer.onPlay()
                    }
                    logRecordStartedEvent()
                }

                override fun onRecordFinished(recordingItem: RecordingItem?) {
                    AppObjectController.uiHandler.post {
                        resetAllState()
                        binding.audioPlayer.onPause()
                        completePractise()
                    }
                }

                override fun onError(errorCode: Int) {
                    AppObjectController.uiHandler.post {
                        resetAllState()
                        binding.audioPlayer.onPause()
                        completePractise()
                    }
                }

            })
        }
    }

    private fun completePractise() {
        val prev =
            childFragmentManager.findFragmentByTag(ConversationSubmitBottomSheet::class.java.name)
        if (prev != null) {
            return
        }

        val bottomSheetFragment = ConversationSubmitBottomSheet.newInstance()
        bottomSheetFragment.show(
            childFragmentManager,
            ConversationSubmitBottomSheet::class.java.name
        )
    }

    private fun getTextWithTalk(): String? {
        return viewModel.practiseWho?.let {
            if (it == PractiseUser.FIRST) {
                "Talk With ".plus(conversationPractiseModel.characterNameA)
            } else {
                "Talk With ".plus(conversationPractiseModel.characterNameB)
            }
        }

    }

    override fun onPause() {
        super.onPause()
        resetAllState()
        viewModel.stopRecording(true)
        RxBus2.publish(ViewPagerDisableEventBus(false))
    }

    override fun onPlayerPause() {
        RxBus2.publish(ViewPagerDisableEventBus(false))
    }

    override fun onPlayerResume() {
        RxBus2.publish(ViewPagerDisableEventBus(true))
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {

    }

    override fun onTrackChange(tag: String?) {
        if (binding.audioPlayer.isPlaying().not()) {
            return
        }
        if (tag.isNullOrEmpty().not()) {
            recordListenList.indexOfFirst { it.id == tag?.toInt() }.run {
                val startPos = audioPractiseAdapter?.items?.size ?: 0
                if (this == -1) {
                    return@run
                }
                if (this >= startPos) {
                    val endPos = this
                    if (startPos == endPos) {
                        audioPractiseAdapter?.addItem(recordListenList[startPos])
                        audioPractiseAdapter?.notifyItemInserted(startPos + 1)
                        binding.recyclerView.smoothScrollToPosition(startPos + 1)
                    } else {
                        val items = recordListenList.subList(startPos, endPos)
                        audioPractiseAdapter?.addItems(items)
                        audioPractiseAdapter?.notifyItemRangeInserted(startPos, items.size)
                        binding.recyclerView.smoothScrollToPosition(startPos + items.size)
                    }
                }
            }
        }
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
            val startPos = audioPractiseAdapter?.items?.size ?: 0
            audioPractiseAdapter?.addItem(recordListenList[startPos])
            audioPractiseAdapter?.notifyItemInserted(startPos + 1)
            binding.recyclerView.smoothScrollToPosition(startPos + 1)
        }
    }

    override fun onPlayerReleased() {

    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
        if (viewModel.isRecordingRunning) {
            viewModel.isRecordingRunning = false
            binding.audioPlayer.onPause()
            completePractise()
            resetAllState()
        }

    }

    companion object {

        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            RecordPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }

    override fun onDone() {
        RxBus2.publish(ConversationPractiseSubmitEventBus(getTextWithTalk() ?: EMPTY))
    }

    override fun onCancel() {
        viewModel.practiseWho = null
    }
}