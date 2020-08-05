package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.exoplayer2.Player.DISCONTINUITY_REASON_PERIOD_TRANSITION
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.SelfPractiseLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import com.vanniktech.emoji.Utils
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import java.util.*

class SelfPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: SelfPractiseLayoutBinding
    private var audioPractiseAdapter: AudioPractiseAdapter? = null
    private lateinit var viewModel: ConversationPracticeViewModel
    private val audioList: LinkedList<AudioModel> = LinkedList()
    private var listenModelList: ArrayList<ListenModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)

        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
            listenModelList =
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
                R.layout.self_practise_layout,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initRV()
        initAudioList()
        logSelfPractiseAnalyticsEvents()
    }

    private fun logSelfPractiseAnalyticsEvents() {
        AppAnalytics.create(AnalyticsEvent.PRACTISE_OPENED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("flow", "Conversational prac")
            .push()
    }

    private fun logPatnerSelectedEvent(patner: String) {
        AppAnalytics.create(AnalyticsEvent.PATNER_SELECTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("patner side",patner)
            .addParam("flow", "self practise")
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

    private fun initAudioList() {
        listenModelList.sortedBy { it.sortOrder }.forEach {
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
        if (viewModel.isPractise) {
            binding.ivTickFirstUser.visibility = View.GONE
            enableView(binding.ivSecondUser)
            resetPlayer()
        } else {
            viewModel.practiseWho = PractiseUser.FIRST
            viewModel.isPractise = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            disableView(binding.ivSecondUser)
            filterProperty(ViewTypeForPractiseUser.FIRST.type)
            initAudioPlayer(PractiseUser.FIRST)
        }
        logPatnerSelectedEvent("first")
    }

    fun practiseWithSecondUser() {
        if (viewModel.isPractise) {
            binding.ivTickSecondUser.visibility = View.GONE
            enableView(binding.ivFirstUser)
            resetPlayer()
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isPractise = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            disableView(binding.ivFirstUser)
            filterProperty(ViewTypeForPractiseUser.SECOND.type)
            initAudioPlayer(PractiseUser.SECOND)
        }
        logPatnerSelectedEvent("second")
    }

    private fun resetPlayer() {
        viewModel.practiseWho = null
        viewModel.isPractise = false
        audioPractiseAdapter?.clear()
        filterProperty(null)
        binding.audioPlayer.addAudios(LinkedList())
    }

    private fun filterProperty(viewType: Int?) {
        listenModelList.forEach {
            it.disable = false
        }
        if (viewType != null) {
            listenModelList.filter { it.viewType == viewType }.forEach {
                it.disable = true
            }
        }
    }

    override fun onPlayerPause() {
        RxBus2.publish(ViewPagerDisableEventBus(false))

    }

    override fun onPlayerResume() {
        RxBus2.publish(ViewPagerDisableEventBus(true))
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


    override fun onCurrentTimeUpdated(lastPosition: Long) {

    }

    override fun onTrackChange(tag: String?) {
        if (tag.isNullOrEmpty().not()) {
            listenModelList.indexOfFirst { it.id == tag?.toInt() }.run {
                val startPos = audioPractiseAdapter?.items?.size ?: 0
                if (this == -1) {
                    return@run
                }
                if (this >= startPos) {
                    val endPos = this
                    if (startPos == endPos) {
                        audioPractiseAdapter?.addItem(listenModelList[startPos])
                        audioPractiseAdapter?.notifyItemInserted(startPos + 1)
                        binding.recyclerView.smoothScrollToPosition(startPos + 1)
                    } else {
                        val items = listenModelList.subList(startPos, endPos)
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
        try {
            if (reason == DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                val startPos = audioPractiseAdapter?.items?.size ?: 0
                audioPractiseAdapter?.addItem(listenModelList[startPos])
                audioPractiseAdapter?.notifyItemInserted(startPos + 1)
                binding.recyclerView.smoothScrollToPosition(startPos + 1)
            }
        } catch (th: Throwable) {
            th.printStackTrace()
        }
    }

    override fun onPlayerReleased() {

    }

    override fun onPlayerEmptyTrack() {
        showToast(getString(R.string.select_character), Toast.LENGTH_LONG)
    }

    override fun onPause() {
        super.onPause()
        viewModel.isPractise = false
        RxBus2.publish(ViewPagerDisableEventBus(false))
    }

    companion object {
        private const val ARG_PRACTISE_OBJ = "practise-obj"

        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            SelfPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }
}