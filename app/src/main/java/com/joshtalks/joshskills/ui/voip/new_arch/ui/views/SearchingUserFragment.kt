package com.joshtalks.joshskills.ui.voip.new_arch.ui.views

import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.databinding.FragmentSearchingUserBinding
import com.joshtalks.joshskills.ui.voip.new_arch.ui.viewmodels.VoiceCallViewModel

class SearchingUserFragment : BaseFragment() {

    lateinit var searchingUserBinding: FragmentSearchingUserBinding
    private var timer: CountDownTimer? = null

    val voiceCallViewModel by lazy {
        ViewModelProvider(requireActivity())[VoiceCallViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        searchingUserBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_searching_user, container, false)
        return searchingUserBinding.root
    }

    override fun initViewBinding() {
        searchingUserBinding.progressBar.max = 100
        searchingUserBinding.progressBar.progress = 0
        searchingUserBinding.executePendingBindings()
    }

    override fun initViewState() {
        startProgressBarCountDown()
    }

    override fun setArguments() {
    }

    private fun startProgressBarCountDown() {
        requireActivity().runOnUiThread {
            searchingUserBinding.progressBar.max = 100
            searchingUserBinding.progressBar.progress = 0
            timer = object : CountDownTimer(5000, 500) {
                override fun onTick(millisUntilFinished: Long) {
                    val diff = searchingUserBinding.progressBar.progress + 10
                    fillProgressBar(diff)
                }
                override fun onFinish() {
                    startProgressBarCountDown()
                }
            }
            timer?.start()
        }
    }

    private fun fillProgressBar(diff: Int) {
        val animation: ObjectAnimator =
            ObjectAnimator.ofInt(
                searchingUserBinding.progressBar,
                "progress",
                searchingUserBinding.progressBar.progress,
                diff
            )
        animation.startDelay = 0
        animation.duration = 250
        animation.interpolator = AccelerateDecelerateInterpolator()
        animation.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timer?.cancel()
        timer = null
    }
}