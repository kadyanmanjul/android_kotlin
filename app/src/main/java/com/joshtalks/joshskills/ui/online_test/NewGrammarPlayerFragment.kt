package com.joshtalks.joshskills.ui.online_test

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.transition.TransitionInflater
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.custom_ui.JoshGrammarVideoPlayer
import com.joshtalks.joshskills.databinding.FragmentNewGrammarPlayerBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.video_player.VIDEO_ID
import com.joshtalks.joshskills.ui.video_player.VIDEO_URL
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

class NewGrammarPlayerFragment : CoreJoshFragment() {
    lateinit var binding: FragmentNewGrammarPlayerBinding
    var videoUrl: String? = null
    var videoId: String? = null
    var convId: String? = null

    companion object {
        fun getFragment(
            videoId: String?,
            videoUrl: String?,
            conversationId: String?
        ): NewGrammarPlayerFragment {
            val fragment = NewGrammarPlayerFragment()
            val args = Bundle().apply {
                putString(VIDEO_URL, videoUrl)
                putString(VIDEO_ID, videoId)
                putString(CONVERSATION_ID, conversationId)
            }

            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val transaction =
            TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        //val transaction = ChangeBounds()
        transaction.duration = 1000
        sharedElementEnterTransition = transaction
        sharedElementReturnTransition = transaction
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        arguments?.let {
            videoId = it.getString(VIDEO_ID)
            videoUrl = it.getString(VIDEO_URL)
            convId = it.getString(CONVERSATION_ID)
        }
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_new_grammar_player,
            container,
            false
        )
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.videoPlayer.apply {
            setUrl(videoUrl)
            setVideoId(videoId)
            fitToScreen()
            downloadStreamPlay()
            setPlayListener(object : JoshGrammarVideoPlayer.PlayerFullScreenListener {

                override fun onFullScreen() {
                    val currentVideoProgressPosition = binding.videoPlayer.progress
                    startActivity(
                        VideoPlayerActivity.getActivityIntent(
                            requireActivity(),
                            "",
                            videoId,
                            videoUrl,
                            currentVideoProgressPosition,
                            conversationId = convId
                        )
                    )
                    visibility = View.GONE
                }

                override fun onClose() {
                    onPause()
                    visibility = View.GONE
                    activity?.supportFragmentManager?.popBackStack()
                }
            })
        }
    }

    override fun onPause() {
        super.onPause()
        binding.videoPlayer.onPause()
    }
}