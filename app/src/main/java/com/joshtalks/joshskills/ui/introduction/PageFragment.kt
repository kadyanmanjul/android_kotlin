package com.joshtalks.joshskills.ui.introduction

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.extension.setImageAndFitCenter
import com.joshtalks.joshskills.databinding.IntroFragmentLayout1Binding
import com.joshtalks.joshskills.repository.server.introduction.Screen
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.muddzdev.styleabletoast.StyleableToast

class PageFragment : Fragment() {
    private lateinit var binding: IntroFragmentLayout1Binding
    private var screen: Screen? = null

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
        screen?.let { screen ->
            binding.text.text = screen.text
            screen.imageUrl?.let {
                //binding.image.setRoundImageInOnbaordingView(it)
                binding.image.setImageAndFitCenter(it)
            }
            if (screen.videoUrl.isNullOrBlank()) {
                binding.playBtnContainer.visibility = View.GONE
            } else {
                binding.playBtnContainer.visibility = View.VISIBLE
                binding.playBtnContainer.setOnClickListener {

                    if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) * 2 <= Utils.getCurrentMediaMaxVolume(
                            AppObjectController.joshApplication
                        )
                    ) {
                        StyleableToast.Builder(AppObjectController.joshApplication)
                            .gravity(Gravity.BOTTOM)
                            .text(getString(R.string.volume_up_message)).cornerRadius(16)
                            .length(Toast.LENGTH_LONG)
                            .solidBackground().show()
                    }

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
