package com.joshtalks.joshskills.ui.callWithExpert.fragment

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseFragment
import com.joshtalks.joshskills.base.constants.*
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CLICKED_CALL_BUTTON
import com.joshtalks.joshskills.databinding.FragmentExpertListBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.CallWithExpertViewModel
import com.joshtalks.joshskills.ui.callWithExpert.viewModel.ExpertListViewModel
import com.joshtalks.joshskills.ui.fpp.constants.CAN_BE_CALL
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.joshtalks.joshskills.voip.constant.Category
import com.skydoves.balloon.ArrowPositionRules
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import kotlinx.android.synthetic.main.activity_call_with_expert.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ExpertListFragment : BaseFragment() {
    private lateinit var binding: FragmentExpertListBinding
    val expertListViewModel by lazy {
        ViewModelProvider(requireActivity())[ExpertListViewModel::class.java]
    }

    private val viewModel by lazy {
        ViewModelProvider(requireActivity())[CallWithExpertViewModel::class.java]
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
        requireActivity().findViewById<TextView>(R.id.iv_earn).setOnClickListener {
            findNavController().navigate(R.id.action_expertListFragment_to_walletFragment)
        }
    }

    override fun initViewBinding() {}

    override fun initViewState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                expertListViewModel.startExpertCall.collectLatest { start ->
                    if (start) {
                        startExpertCall()
                    }
                }
            }
        }

        lifecycleScope.launchWhenStarted {
            expertListViewModel.bbTipText.collectLatest {
                if (it.isNotEmpty())
                    showBbTip(it)
            }
        }

        liveData.observe(this) {
            when (it.what) {
                CAN_BE_CALL -> {
                    if (it.obj == false) {
                        WalletBottomSheet(
                            expertListViewModel.neededAmount,
                            expertListViewModel.clickedSpeakerName
                        ).show(requireActivity().supportFragmentManager, WalletBottomSheet.TAG)
                        expertListViewModel.updateCanBeCalled(true)
                    }
                }

            }
        }

        viewModel.paymentSuccessful.observe(viewLifecycleOwner) {
            if (it) {
//                findNavController().navigate(R.id.paymentProcessingFragment)
                viewModel.paymentSuccess(false)
            }
        }
    }

    private fun showBbTip(tipText: String) {
        try {
            val balloon = Balloon.Builder(requireActivity())
                .setLayout(R.layout.layout_bb_tip)
                .setHeight(BalloonSizeSpec.WRAP)
                .setIsVisibleArrow(true)
                .setBackgroundColorResource(R.color.surface_tip)
                .setArrowDrawableResource(R.drawable.ic_arrow_yellow_stroke)
                .setWidthRatio(0.85f)
                .setDismissWhenTouchOutside(true)
                .setBalloonAnimation(BalloonAnimation.OVERSHOOT)
                .setLifecycleOwner(this)
                .setDismissWhenClicked(true)
                .setAutoDismissDuration(4000L)
                .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                .build()
            val textView = balloon.getContentView().findViewById<MaterialTextView>(R.id.balloon_text)
            textView.text =
                tipText.replace("__username__", Mentor.getInstance().getUser()?.firstName ?: "User")
            activity?.toolbar_container?.findViewById<AppCompatTextView>(R.id.iv_earn)?.let {
                balloon.showAlignBottom(it)
            }
        }catch (ex:Exception){
            Log.e("ExpertListFragment", "showBuyCourseTooltip: ${ex.message}")
        }
    }

    override fun setArguments() {}

    private fun startExpertCall() {
        if ((viewModel.walletAmount.value?: 0) >= (expertListViewModel.selectedUser?.expertPricePerMinute ?: 0)) {
            viewModel.saveMicroPaymentImpression(eventName = CLICKED_CALL_BUTTON)
            val callIntent = Intent(AppObjectController.joshApplication, VoiceCallActivity::class.java)
            callIntent.apply {
                putExtra(STARTING_POINT, FROM_ACTIVITY)
                putExtra(IS_EXPERT_CALLING, "true")
                putExtra(INTENT_DATA_EXPERT_PRICE_PER_MIN, expertListViewModel.selectedUser?.expertPricePerMinute.toString())
                putExtra(INTENT_DATA_TOTAL_AMOUNT, viewModel.walletAmount.value?.toString())
                putExtra(INTENT_DATA_CALL_CATEGORY, Category.EXPERT.ordinal)
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