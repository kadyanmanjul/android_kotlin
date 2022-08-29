package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CLICKED_CALL_BUTTON
import com.joshtalks.joshskills.databinding.FragmentExpertListBinding
import com.joshtalks.joshskills.ui.callWithExpert.utils.removeRupees
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.ExpertListViewModel
import com.joshtalks.joshskills.ui.fpp.constants.START_FPP_CALL_FROM_WALLET
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category

class ExpertListFragment:BaseFragment() {
    private lateinit var binding: FragmentExpertListBinding
    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[CallWithExpertViewModel::class.java]
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
//        expertListViewModel.getListOfExpert()
//        requireActivity().findViewById<TextView>(R.id.text_message_title).text = getString(R.string.call_with_expert)
        requireActivity().findViewById<TextView>(R.id.iv_earn).setOnClickListener {
            findNavController().navigate(R.id.action_expertListFragment_to_walletFragment)
        }

    }
    override fun initViewBinding() {

    }

    override fun initViewState() {
        liveData.observe(this) {
            when (it.what) {
                START_FPP_CALL_FROM_WALLET -> {
                    if (viewModel.creditsCount.value?.removeRupees()
                            ?.toInt() ?: 0 >= expertListViewModel.selectedUser?.expertPricePerMinute ?: 0
                    ) {
                        viewModel.saveMicroPaymentImpression(eventName = CLICKED_CALL_BUTTON)
                        val callIntent = Intent(AppObjectController.joshApplication, VoiceCallActivity::class.java)
                        callIntent.apply {
                            putExtra(STARTING_POINT, FROM_ACTIVITY)

                            putExtra(IS_EXPERT_CALLING, "true")
                            putExtra(INTENT_DATA_EXPERT_PRICE_PER_MIN, expertListViewModel.selectedUser?.expertPricePerMinute.toString())
                            putExtra(INTENT_DATA_TOTAL_AMOUNT,viewModel.creditsCount.value?.removeRupees())

                            putExtra(INTENT_DATA_CALL_CATEGORY, Category.FPP.ordinal)
                            putExtra(INTENT_DATA_FPP_MENTOR_ID, expertListViewModel.selectedUser?.mentorId)
                            putExtra(INTENT_DATA_FPP_NAME, expertListViewModel.selectedUser?.expertName)
                            putExtra(INTENT_DATA_FPP_IMAGE, expertListViewModel.selectedUser?.expertImage)

                        }
                        startActivity(callIntent)
                    } else {
                        showToast("You don't have amount")
                    }
                }
            }
        }

        expertListViewModel.canBeCalled.observe(this){ canBe ->
            if (!canBe){
                WalletBottomSheet(
                    expertListViewModel.neededAmount,
                    expertListViewModel.clickedSpeakerName
                ).show(requireActivity().supportFragmentManager, WalletBottomSheet.TAG)
            }
        }
    }

    override fun setArguments() {

    }

}