package com.joshtalks.joshskills.ui.lesson.reading

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.CLOSE_VIDEO_VIEW
import com.joshtalks.joshskills.constants.SEND_OUTPUT_FILE
import com.joshtalks.joshskills.constants.VIDEO_AUDIO_MUX_FAILED
import com.joshtalks.joshskills.databinding.FragmentReadingFullScreenBinding
import com.joshtalks.joshskills.ui.lesson.LessonViewModel

class ReadingFullScreenFragment : BaseFragment() {

    lateinit var binding: FragmentReadingFullScreenBinding
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reading_full_screen, container, false)
        binding.videoCard.clipToOutline = true
        return binding.root
    }

    override fun initViewBinding() {
        binding.ivBack.setOnClickListener {
            viewModel.closeCurrentFragment()
            viewModel.showVideoView()
            binding.mergedVideo.onStop()
        }
        binding.submitAnswerBtn.setOnClickListener {
            binding.mergedVideo.onStop()
            viewModel.submitButton()
            viewModel.closeCurrentFragment()
            viewModel.showVideoView()
        }
        binding.ivClose.setOnClickListener {
            viewModel.closeCurrentFragment()
            viewModel.cancelButton()
            binding.mergedVideo.onStop()
        }
        binding.mergedVideo.setOnTouchListener(View.OnTouchListener { v, event ->
            true
        })
    }

    override fun initViewState() {
        liveData.observe(requireActivity()) {
            if (this.isVisible) {
                when (it.what) {
                    SEND_OUTPUT_FILE -> {
                        Log.d("Manjul", "initViewState() called")
                        binding.loadingGroup.visibility = View.GONE
                        binding.mergedVideo.visibility = View.VISIBLE
                        binding.mergedVideo.setUrl(it.obj as String)
                        binding.mergedVideo.fitToScreen()
                        binding.mergedVideo.onStart()
                        binding.submitAnswerBtn.visibility = View.VISIBLE
                        binding.ivBack.visibility = View.VISIBLE
                        binding.ivClose.visibility = View.VISIBLE

                    }
                    VIDEO_AUDIO_MUX_FAILED -> {
                        viewModel.closeCurrentFragment()
                        binding.mergedVideo.onStop()
                    }
                    CLOSE_VIDEO_VIEW -> {
                        binding.mergedVideo.onStop()
                    }
                }
            }
        }
    }

    override fun setArguments() {
        //TODO("Not yet implemented")
    }

    fun isFragmentUIActive(): Boolean {
        return isAdded && !isDetached && !isRemoving
    }

    override fun onPause() {
        super.onPause()
        binding.mergedVideo.onPause()
    }
}
