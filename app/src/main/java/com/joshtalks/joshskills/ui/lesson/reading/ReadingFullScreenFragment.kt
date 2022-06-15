package com.joshtalks.joshskills.ui.lesson.reading

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.constants.SEND_OUTPUT_FILE
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.FragmentReadingFullScreenBinding
import com.joshtalks.joshskills.ui.lesson.LessonActivity
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import kotlinx.android.synthetic.main.activity_reminder_notifier.*
import kotlinx.android.synthetic.main.fragment_reading_full_screen.*
import timber.log.Timber
import java.io.IOException
import java.nio.ByteBuffer

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
        binding.mergedVideo.setOnCompletionListener {
            binding.mergedVideo.start()
        }
        binding.mergedVideo.start()
        binding.ivBack.setOnClickListener {
            viewModel.closeCurrentFragment()
        }
        binding.submitAnswerBtn.setOnClickListener {
            viewModel.submitButton()
            viewModel.closeCurrentFragment()
        }
        binding.ivClose.setOnClickListener {
            viewModel.closeCurrentFragment()
            viewModel.cancelButton()
        }
    }

    override fun initViewState() {
        liveData.observe(this) {
            when(it.what) {
                SEND_OUTPUT_FILE -> {
                    binding.loadingGroup.visibility = View.GONE
                    binding.mergedVideo.setVideoPath(it.obj as String)
                    binding.submitAnswerBtn.visibility = View.VISIBLE
                }
            }
        }
    }

    override fun setArguments() {
        //TODO("Not yet implemented")
    }
}