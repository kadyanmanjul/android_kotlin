package com.joshtalks.joshskills.ui.lesson.reading

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentReadingFullScreenBinding

class ReadingFullScreenFragment : BaseFragment() {

    lateinit var binding: FragmentReadingFullScreenBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_reading_full_screen, container, false)
        return binding.root
    }

    override fun initViewBinding() {
        binding.overlayMergedVideo.setOnCompletionListener {
            binding.overlayMergedVideo.start()
        }
    }

    override fun initViewState() {
        //TODO("Not yet implemented")
    }

    override fun setArguments() {
        //TODO("Not yet implemented")
    }
}