package com.joshtalks.joshskills.ui.conversation_practice.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ALPHA_MAX
import com.joshtalks.joshskills.core.ALPHA_MIN
import com.joshtalks.joshskills.core.PractiseUser
import com.joshtalks.joshskills.core.custom_ui.SmoothLinearLayoutManager
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.SelfPractiseLayoutBinding
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.AudioPractiseAdapter
import kotlinx.android.synthetic.main.fragment_listen_practise.audio_player
import java.util.*

class SelfPractiseFragment private constructor() : Fragment(), AudioPlayerEventListener {
    private lateinit var conversationPractiseModel: ConversationPractiseModel
    private lateinit var binding: SelfPractiseLayoutBinding
    private var audioPractiseAdapter: AudioPractiseAdapter? = null
    private lateinit var viewModel: ConversationPracticeViewModel

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
    }

    private fun initRV() {
        binding.recyclerView.layoutManager = SmoothLinearLayoutManager(context)
        binding.recyclerView.setHasFixedSize(false)
        audioPractiseAdapter =
            AudioPractiseAdapter(conversationPractiseModel.listen.toMutableList())
        binding.recyclerView.adapter = audioPractiseAdapter
    }

    private fun initView() {
        binding.ivFirstUser.setImage(conversationPractiseModel.characterUrlA)
        binding.ivSecondUser.setImage(conversationPractiseModel.characterUrlB)
        binding.tvFirstUser.text = conversationPractiseModel.characterNameA
        binding.tvSecondUser.text = conversationPractiseModel.characterNameB

    }

    private fun initAudioPlayer() {
        val list: LinkedList<AudioModel> = LinkedList()
        conversationPractiseModel.listen.forEach {
            list.add(AudioModel(it.audioUrl, it.id.toString(), it.duration))
        }
        audio_player.addAudios(list)
        audio_player.setAudioPlayerEventListener(this)
    }


    fun practiseWithFirstUser() {
        if (viewModel.practiseWho != null) {
            viewModel.isPractise = false
        }

        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivSecondUser.alpha = ALPHA_MIN
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isPractise = true
            binding.ivTickFirstUser.visibility = View.VISIBLE
            binding.ivSecondUser.alpha = ALPHA_MAX
        }
    }

    fun practiseWithSecondUser() {
        if (viewModel.practiseWho != null) {
            viewModel.isPractise = false
        }

        if (viewModel.isPractise) {
            viewModel.practiseWho = null
            viewModel.isPractise = false
            binding.ivFirstUser.alpha = ALPHA_MIN
        } else {
            viewModel.practiseWho = PractiseUser.SECOND
            viewModel.isPractise = true
            binding.ivTickSecondUser.visibility = View.VISIBLE
            binding.ivFirstUser.alpha = ALPHA_MAX
        }
        audioPractiseAdapter?.filter?.filter("")
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

    override fun onPlayerReleased() {

    }

}
