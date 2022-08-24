package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FragmentExpertListBinding
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.ExpertListViewModel
import com.joshtalks.joshskills.ui.fpp.constants.START_FPP_CALL
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category

class ExpertListFragment:BaseFragment() {
    private lateinit var binding: FragmentExpertListBinding
    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpertListBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.vm = expertListViewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        expertListViewModel.getListOfExpert()
    }

    override fun initViewBinding() {

    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {
                START_FPP_CALL ->{
                    val callIntent = Intent(AppObjectController.joshApplication, VoiceCallActivity::class.java)
                    callIntent.apply {
                        putExtra(STARTING_POINT, FROM_ACTIVITY)
                        putExtra(INTENT_DATA_CALL_CATEGORY, Category.FPP.ordinal)
                        putExtra(INTENT_DATA_FPP_MENTOR_ID, expertListViewModel.selectedUser?.mentorId)
                        putExtra(INTENT_DATA_FPP_NAME, expertListViewModel.selectedUser?.expertName)
                        putExtra(INTENT_DATA_FPP_IMAGE, expertListViewModel.selectedUser?.expertImage)

                    }
                    startActivity(callIntent)
                }

            }
        }
    }

    override fun setArguments() {

    }

}