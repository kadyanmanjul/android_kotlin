package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.repository.server.conversation_practice.ListenModel
import com.joshtalks.joshskills.ui.conversation_practice.IMAGE_URL
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import java.util.ArrayList
import java.util.LinkedList
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import kotlinx.android.synthetic.main.fragment_listen_practise.audio_player
import kotlinx.android.synthetic.main.fragment_listen_practise.image_view
import kotlinx.android.synthetic.main.fragment_listen_practise.placeholder_bg
import kotlinx.android.synthetic.main.fragment_listen_practise.recycler_view
import kotlinx.android.synthetic.main.fragment_listen_practise.sub_title_tv
import kotlinx.android.synthetic.main.fragment_listen_practise.title_tv

class ListenPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {

    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private var audioPractiseAdapter: AudioPractiseAdapter? = null
    private val listenModelList: ArrayList<ListenModel> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
            listenModelList.addAll(ArrayList(conversationPractiseModel.listen.sortedBy { sort -> sort.sortOrder }))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_listen_practise, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initRV()
        initAudioPlayer()
    }

    private fun initView() {
        title_tv.text = conversationPractiseModel.title
        sub_title_tv.text = conversationPractiseModel.subTitle
        requireActivity().intent?.getStringExtra(IMAGE_URL)?.run {
            if (this.isNotEmpty()) {
                image_view.setRoundImage(this)
            }
        }
        placeholder_bg.setOnClickListener {
            logConversationTapToContinueEvent(conversationPractiseModel.id.toString())
            hidePlaceHolderView()
            initAudioPlayer()
        }
    }

    private fun hidePlaceHolderView() {
        placeholder_bg.visibility = View.GONE
        placeholder_bg.setImageResource(0)
    }

    private fun logConversationTapToContinueEvent(id: String) {
        AppAnalytics.create(AnalyticsEvent.CONVERSATION_PRACTISE_TAP_TO_START.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.CONVERSATION_PRACTISE_ID.NAME, id)
            .push()
    }

    private fun initRV() {
        recycler_view.itemAnimator?.apply {
            addDuration = 2000
            changeDuration = 2000
        }
        recycler_view.itemAnimator = SlideInUpAnimator(OvershootInterpolator(2f))
        recycler_view.layoutManager = SmoothLinearLayoutManager(context)
        recycler_view.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(requireContext(), 6f)))
        audioPractiseAdapter = AudioPractiseAdapter().apply {
            setHasStableIds(true)
        }
        recycler_view.adapter = audioPractiseAdapter
    }

    private fun initAudioPlayer() {
        val list: LinkedList<AudioModel> = LinkedList()
        listenModelList.forEach {
            list.add(AudioModel(it.audio.audio_url, it.id.toString(), it.audio.duration))
        }
        audio_player.addAudios(list)
        audio_player.setAudioPlayerEventListener(this)
    }


    private fun logConversationPracticeAudioEvent(isResumed: Boolean) {
        AppAnalytics.create(AnalyticsEvent.CONVERSATION_PLAY_BACK.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.AUDIO_PLAYING_STATUS.NAME, isResumed)
            .push()
    }

    override fun onPlayerPause() {
        logConversationPracticeAudioEvent(false)
        RxBus2.publish(ViewPagerDisableEventBus(false))
    }

    override fun onPlayerResume() {
        hidePlaceHolderView()
        logConversationPracticeAudioEvent(true)
        RxBus2.publish(ViewPagerDisableEventBus(true))
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {

        if (!isVisible)
            return

        if (placeholder_bg != null && placeholder_bg.visibility == View.VISIBLE) {
            return
        }

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
                        recycler_view.smoothScrollToPosition(startPos + 1)
                    } else {
                        val items = listenModelList.subList(startPos, endPos)
                        audioPractiseAdapter?.addItems(items)
                        audioPractiseAdapter?.notifyItemRangeInserted(startPos, items.size)
                        recycler_view.smoothScrollToPosition(startPos + items.size)
                    }
                }
            }
        }
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {

    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {

    }

    override fun complete() {

    }

    override fun onPause() {
        super.onPause()
        RxBus2.publish(ViewPagerDisableEventBus(false))
    }

    override fun onResume() {
        super.onResume()
        listenModelList.forEach {
            it.disable = false
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(conversationPractiseModel: ConversationPractiseModel) =
            ListenPractiseFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_PRACTISE_OBJ, conversationPractiseModel)
                }
            }
    }

}