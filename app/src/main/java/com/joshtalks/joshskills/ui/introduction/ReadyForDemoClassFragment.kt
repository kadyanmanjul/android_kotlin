package com.joshtalks.joshskills.ui.introduction

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.INTRODUCTION_YES_EXCITED_CLICKED
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.databinding.FragmentReadyForDemoBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.StartDemoSpeakingLessonEventBus
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P

class ReadyForDemoClassFragment : Fragment() {
    private lateinit var binding: FragmentReadyForDemoBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_ready_for_demo, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance() = ReadyForDemoClassFragment()
    }

    public fun startOnBoading() {
        val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
            putExtra(FLOW_FROM, "new demo Onboarding flow")
        }
        startActivity(intent)
        requireActivity().finish()
    }

    public fun startDemoSpeakingClass() {
        PrefManager.put(INTRODUCTION_YES_EXCITED_CLICKED,true)
        PrefManager.put(IS_DEMO_P2P, true)
        RxBus2.publish(StartDemoSpeakingLessonEventBus(118))
    }

}
