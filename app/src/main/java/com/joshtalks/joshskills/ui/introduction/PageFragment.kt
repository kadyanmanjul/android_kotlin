package com.joshtalks.joshskills.ui.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.IntroFragmentLayout1Binding
import com.joshtalks.joshskills.repository.server.introduction.Screen
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity

class PageFragment : Fragment() {
    private lateinit var binding: IntroFragmentLayout1Binding
    private var screen :Screen?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            screen = it.getParcelable(PAGE_POSITION) as Screen?
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.intro_fragment_layout_1, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        screen?.let { screen->
            binding.text.text = screen.text
            screen.imageUrl?.let {
                binding.image.setImage(it)
            }
            if (screen.videoUrl.isNullOrBlank()){
                binding.playBtnContainer.visibility=View.GONE
            } else {
                binding.playBtnContainer.visibility=View.VISIBLE
                binding.playBtnContainer.setOnClickListener {
                    VideoPlayerActivity.startVideoActivity(
                        requireActivity(),
                        null,
                        null,
                        screen.videoUrl
                    )
                }
            }
        }
    }

    companion object {
        private const val PAGE_POSITION = "page_position"

        @JvmStatic
        fun newInstance(screen: Screen): PageFragment {
            val args = Bundle()
            args.putParcelable(PAGE_POSITION, screen)
            val f = PageFragment()
            f.arguments = args
            return f
        }
    }

}
