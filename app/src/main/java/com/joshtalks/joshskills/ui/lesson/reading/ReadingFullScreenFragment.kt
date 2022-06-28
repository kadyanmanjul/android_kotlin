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
import com.joshtalks.joshskills.databinding.FragmentReadingFullScreenBinding
import com.joshtalks.joshskills.ui.assessment.fragment.QuizSuccessFragment
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
        Log.e("Ayaaz","initviewbinding")
        binding.mergedVideo.setOnCompletionListener {
            binding.mergedVideo.start()
        }
        binding.mergedVideo.start()
        binding.ivBack.setOnClickListener {
            Log.e("Ayaaz","backpressed")
            viewModel.closeCurrentFragment()
            viewModel.showVideoView()
            binding.mergedVideo.stopPlayback()
        }
        binding.submitAnswerBtn.setOnClickListener {
            binding.mergedVideo.stopPlayback()
            viewModel.submitButton()
            viewModel.closeCurrentFragment()
            viewModel.showVideoView()
        }
        binding.ivClose.setOnClickListener {
            viewModel.closeCurrentFragment()
            viewModel.cancelButton()
            binding.mergedVideo.stopPlayback()
        }
        binding.mergedVideo.setOnTouchListener(View.OnTouchListener { v, event ->
            true
        })
    }

    override fun onStart() {
        super.onStart()
        Log.e("Ayaaz","start")
        binding.mergedVideo.start()
    }

    override fun initViewState() {
        liveData.observe(this) {
            when(it.what) {
                SEND_OUTPUT_FILE -> {
                    binding.loadingGroup.visibility = View.GONE
                    binding.mergedVideo.setVideoPath(it.obj as String)
                    binding.submitAnswerBtn.visibility = View.VISIBLE
                }
                CLOSE_VIDEO_VIEW -> {
                    binding.mergedVideo.stopPlayback()
                }
            }
        }
    }

    override fun setArguments() {
        //TODO("Not yet implemented")
    }

    override fun onStop() {
        super.onStop()
        Log.e("Ayaaz","onstop")
    }

    override fun onPause() {
        super.onPause()
        Log.e("Ayaaz","onpause")
    }

//    companion object{
//        var instance: ReadingFullScreenFragment? = null
//
//        fun newInstance(): ReadingFullScreenFragment {
//            if (instance == null)
//                instance = ReadingFullScreenFragment()
//            return instance!!
//        }
//    }
//    companion object {
//    @JvmStatic
//    fun newInstance() =
//        ReadingFullScreenFragment()
//}
}