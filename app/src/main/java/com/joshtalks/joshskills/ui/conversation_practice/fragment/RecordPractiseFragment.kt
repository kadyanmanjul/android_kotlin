package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.google.android.exoplayer2.Player
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.custom_ui.recorder.AudioRecorder
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.FragmentRecordPractiseBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseSubmitEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.vanniktech.emoji.Utils
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import java.util.*

class RecordPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {
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
        if (viewModel.isRecord) {
            binding.ivTickFirstUser.visibility = View.GONE
            enableView(binding.ivSecondUser)
            resetPlayer()
        } else {
            binding.placeholderBg.visibility = View.GONE
            binding.placeholderBg.setImageResource(0)
            viewModel.practiseWho = PractiseUser.FIRST
            viewModel.isRecord = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            disableView(binding.ivSecondUser)
            filterProperty(ViewTypeForPractiseUser.FIRST.type)
            initAudioPlayer(PractiseUser.FIRST)
        }
    }

    fun practiseWithSecondUser() {
        if (viewModel.isRecord) {
            binding.ivTickSecondUser.visibility = View.GONE
            enableView(binding.ivFirstUser)
            resetPlayer()
        } else {
            binding.placeholderBg.visibility = View.GONE
            binding.placeholderBg.setImageResource(0)
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isRecord = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            disableView(binding.ivFirstUser)
            filterProperty(ViewTypeForPractiseUser.SECOND.type)
            initAudioPlayer(PractiseUser.SECOND)
        }
    }


    private fun resetAllState() {
        RxBus2.publish(ViewPagerDisableEventBus(false))
        binding.ivTickFirstUser.visibility = View.GONE
        binding.ivTickSecondUser.visibility = View.GONE
        enableView(binding.ivFirstUser)
        enableView(binding.ivSecondUser)
        viewModel.isRecord = false
        audioPractiseAdapter?.clear()
        filterProperty(null)
    }

    private fun resetPlayer() {
        viewModel.isRecord = false
        audioPractiseAdapter?.clear()
        filterProperty(null)
    }

    private fun enableView(view: View) {
        view.isClickable = true
        view.isEnabled = true
        view.alpha = ALPHA_MAX
    }

    private fun disableView(view: View) {
        view.isClickable = false
        view.isEnabled = false
        view.alpha = ALPHA_MIN
        binding.placeholderBg.visibility = View.GONE
        binding.placeholderBg.setImageResource(0)
    }


    private fun filterProperty(viewType: Int?) {
        recordListenList.forEach {
            it.disable = false
        }
        if (viewType != null) {
            recordListenList.filter { it.viewType == viewType }.forEach {
                it.disable = true
            }
        }
    }

    private fun startRecording() {
        if (viewModel.practiseWho == null) {
            showToast(getString(R.string.select_your_character))
            return
        }
        if (viewModel.isPlayerInit.not()) {
            viewModel.initRecorder()
        }

        if (viewModel.isRecordingRunning) {
            binding.audioPlayer.onPause()
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.button_primary_color
            )
            viewModel.isRecordingRunning = false
            viewModel.stopRecording(object : AudioRecorder.OnPauseListener {
                override fun onPaused(activeRecordFileName: String?) {
                    completePractise()
                    resetAllState()
                    binding.audioPlayer.onPause()
                }

                override fun onException(e: Exception?) {
                    viewModel.practiseWho = null
                    resetAllState()
                }
            })
        } else {
            binding.btnRecord.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                R.color.recording_9D
            )
            viewModel.startRecording(object : AudioRecorder.OnStartListener {

                override fun onStarted() {
                    RxBus2.publish(ViewPagerDisableEventBus(true))
                    viewModel.isRecordingRunning = true
                    binding.audioPlayer.onPlay()
                }

                override fun onException(e: Exception?) {
                }
            })
        }
    }

    private fun completePractise() {
        val bottomSheet = BottomSheet(LayoutMode.WRAP_CONTENT)
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.conversation_submit_dialog, null, false)
        val dialog = MaterialDialog(requireActivity(), bottomSheet).show {
            customView(view = view)
            cancelOnTouchOutside(false)
        }

        view.findViewById<View>(R.id.btn_no).setOnClickListener {
            AppDirectory.deleteRecordingFile()
            viewModel.practiseWho = null
            dialog.cancel()
        }
        view.findViewById<View>(R.id.btn_yes).setOnClickListener {
            RxBus2.publish(ConversationPractiseSubmitEventBus(getTextWithTalk() ?: EMPTY))
            dialog.cancel()
        }
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
        viewModel.stopRecording(null)
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
        Log.e("napta", "onTrackChange  " + tag)
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

    override fun onResume() {
        super.onResume()
        if (viewModel.isRecord.not()) {
            recordListenList.forEach {
                it.disable = false
            }
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
}