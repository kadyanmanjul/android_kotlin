package com.joshtalks.joshskills.ui.referral

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FragmentValidReferralCodeBinding
import com.joshtalks.joshskills.repository.server.ReferralCouponDetailResponse
import com.joshtalks.joshskills.ui.signup.OnBoardActivity


class ValidReferralCodeFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentValidReferralCodeBinding
    private var name: String? = null
    private var offerText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            name = it.getString(REFERRER_NAME)
            offerText = it.getString(REFERRER_DETAILS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_valid_referral_code,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.description.text= SpannableStringBuilder(
            getString(
                R.string.referral_success_info,
                name
            )
        )
        binding.offerText.text=offerText
    }

    fun openCourseExplore(){
        (requireActivity() as OnBoardActivity).openCourseExplore()
    }
    fun signUp(){
        (requireActivity() as OnBoardActivity).signUp()
    }

    companion object {
        @JvmStatic
        fun newInstance(res: ReferralCouponDetailResponse) =
            ValidReferralCodeFragment().apply {
                arguments = Bundle().apply {
                    putString(REFERRER_NAME, res.referrerName)
                    putString(REFERRER_DETAILS, res.offerText)
                }
            }
        private const val REFERRER_NAME="referrer_name"
        private const val REFERRER_DETAILS="referrer_details"
    }
}
