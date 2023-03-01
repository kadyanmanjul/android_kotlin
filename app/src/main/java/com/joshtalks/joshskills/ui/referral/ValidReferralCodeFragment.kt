package com.joshtalks.joshskills.ui.referral

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.databinding.FragmentValidReferralCodeBinding
import com.joshtalks.joshskills.repository.server.ReferralCouponDetailResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.ui.signup.SignUpActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


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
        binding.description.text = SpannableStringBuilder(
            getString(
                R.string.referral_success_info,
                name
            )
        )
        binding.offerText.text = offerText
    }

    fun openCourseExplore(v:View) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.EXPLORE_BTN_CLICKED.NAME)
                .addParam("name", this.javaClass.simpleName)
                .addBasicParam()
                .addUserDetails()
                .push()
            startActivity(Intent(requireActivity(), CourseExploreActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            })
        }
    }

    fun signUp(v:View) {
        lifecycleScope.launch(Dispatchers.IO) {
            AppAnalytics.create(AnalyticsEvent.LOGIN_INITIATED.NAME)
                .addBasicParam()
                .addUserDetails()
                .addParam(AnalyticsEvent.FLOW_FROM_PARAM.NAME, this.javaClass.simpleName)
                .push()
            val intent = Intent(requireActivity(), SignUpActivity::class.java).apply {
                putExtra(FLOW_FROM, "onboarding journey")
            }
            startActivity(intent)
        }
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

        private const val REFERRER_NAME = "referrer_name"
        private const val REFERRER_DETAILS = "referrer_details"
    }
}
