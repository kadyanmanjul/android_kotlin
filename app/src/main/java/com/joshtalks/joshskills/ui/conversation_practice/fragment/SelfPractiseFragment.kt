package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.SelfPractiseLayoutBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import kotlinx.android.synthetic.main.fragment_listen_practise.audio_player
import kotlinx.android.synthetic.main.fragment_listen_practise.recycler_view
import java.util.*

class SelfPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: SelfPractiseLayoutBinding
    private var audioPractiseAdapter: AudioPractiseAdapter? = null
    private lateinit var viewModel: ConversationPracticeViewModel
    private val audioList: LinkedList<AudioModel> = LinkedList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel =
            ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)

        arguments?.let {
            conversationPractiseModel =
                it.getParcelable<ConversationPractiseModel>(ARG_PRACTISE_OBJ) as ConversationPractiseModel
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
    }

    private fun initAudioList() {
        conversationPractiseModel.listen.forEach {
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
        binding.recyclerView.layoutManager = SmoothLinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(false)
        audioPractiseAdapter =
            AudioPractiseAdapter(ArrayList(conversationPractiseModel.listen.toMutableList()))
        binding.recyclerView.adapter = audioPractiseAdapter
    }

    private fun initView() {
        binding.ivFirstUser.setImage(conversationPractiseModel.characterUrlA)
        binding.ivSecondUser.setImage(conversationPractiseModel.characterUrlB)
        binding.tvFirstUser.text = conversationPractiseModel.characterNameA
        binding.tvSecondUser.text = conversationPractiseModel.characterNameB
        audio_player.setAudioPlayerEventListener(this)
    }

    private fun initAudioPlayer(practiseWho: PractiseUser?) {
        practiseWho?.run {
            when (this) {
                PractiseUser.FIRST -> {
                    audioList.listIterator().forEach {
                        it.isSilent = !it.subTag.equals(
                            conversationPractiseModel.characterNameA,
                            ignoreCase = true
                        )
                    }
                }
                PractiseUser.SECOND -> {
                    audioList.listIterator().forEach {
                        it.isSilent = !it.subTag.equals(
                            conversationPractiseModel.characterNameB,
                            ignoreCase = true
                        )
                    }
                }
            }
        }
        audio_player.addAudios(audioList)
    }


    fun practiseWithFirstUser() {
        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivTickFirstUser.visibility = View.GONE
            enableView(binding.ivSecondUser)
            initAudioPlayer(null)
            disableViewTypeInAdapter(null)
        } else {
            viewModel.practiseWho = PractiseUser.FIRST
            viewModel.isPractise = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            disableView(binding.ivSecondUser)
            initAudioPlayer(PractiseUser.FIRST)
            disableViewTypeInAdapter(ViewTypeForPractiseUser.FIRST.type)
        }
    }

    fun practiseWithSecondUser() {
        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivTickSecondUser.visibility = View.GONE
            enableView(binding.ivFirstUser)
            initAudioPlayer(null)
            disableViewTypeInAdapter(null)
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isPractise = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            disableView(binding.ivFirstUser)
            initAudioPlayer(PractiseUser.SECOND)
            disableViewTypeInAdapter(ViewTypeForPractiseUser.SECOND.type)
        }
    }

    private fun disableViewTypeInAdapter(viewType: Int?) {
        audioPractiseAdapter?.filter?.filter(viewType?.toString() ?: "")

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

    override fun onCurrentTimeUpdated(lastPosition: Long) {

    }

    override fun onTrackChange(tag: String?) {
        Log.e("audiotag", tag)
        if (tag.isNullOrEmpty().not()) {
            audioPractiseAdapter?.items?.indexOfFirst { it.id == tag?.toInt() }?.run {
                if (this > -1) {
                    recycler_view.smoothScrollToPosition(this)
                }
            }
        }
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {

    }

    override fun onPlayerReleased() {

    }

    override fun onPlayerEmptyTrack() {
        showToast(getString(R.string.select_partner), Toast.LENGTH_LONG)
    }

}
