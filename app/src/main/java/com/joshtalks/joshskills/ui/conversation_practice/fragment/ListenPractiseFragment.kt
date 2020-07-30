package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.setRoundImage
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.IMAGE_URL
import com.joshtalks.joshskills.ui.conversation_practice.adapter.ARG_PRACTISE_OBJ
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import com.vanniktech.emoji.Utils
import kotlinx.android.synthetic.main.fragment_listen_practise.audio_player
import kotlinx.android.synthetic.main.fragment_listen_practise.image_view
import kotlinx.android.synthetic.main.fragment_listen_practise.recycler_view
import kotlinx.android.synthetic.main.fragment_listen_practise.sub_title_tv
import kotlinx.android.synthetic.main.fragment_listen_practise.title_tv
import java.util.*

class ListenPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {

    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private var audioPractiseAdapter: AudioPractiseAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
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
    }

    private fun initRV() {
        recycler_view.layoutManager = SmoothLinearLayoutManager(context)
        recycler_view.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(requireContext(), 6f)))
        audioPractiseAdapter =
            AudioPractiseAdapter(ArrayList(conversationPractiseModel.listen.toMutableList())).apply {
                setHasStableIds(true)
            }
        recycler_view.adapter = audioPractiseAdapter
    }

    private fun initAudioPlayer() {
        val list: LinkedList<AudioModel> = LinkedList()
        conversationPractiseModel.listen.forEach {
            list.add(AudioModel(it.audio.audio_url, it.id.toString(), it.audio.duration))
        }
        audio_player.addAudios(list)
        audio_player.setAudioPlayerEventListener(this)
    }

    override fun onPlayerPause() {
        RxBus2.publish(ViewPagerDisableEventBus(false))

    }

    override fun onPlayerResume() {
        RxBus2.publish(ViewPagerDisableEventBus(true))
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
        Log.e("audioo", "onCurrentTimeUpdated    " + lastPosition)

    }

    override fun onTrackChange(tag: String?) {
        Log.e("audioo", "onTrackChange " + tag)
        if (tag.isNullOrEmpty().not()) {
            audioPractiseAdapter?.items?.indexOfFirst { it.id == tag?.toInt() }?.run {
                if (this > -1) {
                    recycler_view.smoothScrollToPosition(this)
                }
            }
        }
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
        Log.e("audioo", "onPositionDiscontinuity    " + lastPos)

    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {

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