package com.joshtalks.joshskills.ui.referral

import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.INSTANCE_ID
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.REFERRED_REFERRAL_CODE
import com.joshtalks.joshskills.core.SINGLE_SPACE
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.hideKeyboard
import com.joshtalks.joshskills.databinding.FragmentEnterReferralCodeBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.PromoCodeSubmitEventBus
import com.joshtalks.joshskills.util.showAppropriateMsg
import java.util.HashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val IS_PROMO_CODE_FRAGMENT = "is_promo_code_fragment"

class EnterReferralCodeFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentEnterReferralCodeBinding
    private lateinit var appAnalyticsP: AppAnalytics
    private var is_promo_code_fragment: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, R.style.BaseBottomSheetDialogResizable)
        changeDialogConfiguration()
        arguments?.let {
            is_promo_code_fragment = it.getBoolean(IS_PROMO_CODE_FRAGMENT)
        }
        appAnalyticsP = AppAnalytics.create(AnalyticsEvent.HAVE_COUPON_CODE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COUPON_INSERTED.NAME, false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_enter_referral_code,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvReferralCode.filters =
            arrayOf(InputFilter.AllCaps(), InputFilter.LengthFilter(10))
        is_promo_code_fragment.let {
            if(it){
                binding.textviewNumber.text=getString(R.string.enter_promo_code)
            }
        }
        setListeners()
    }

    private fun setListeners() {
        binding.tvReferralCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    if (it.length > 0) {
                        binding.next.visibility = View.VISIBLE

                    } else binding.next.visibility = View.INVISIBLE
                    appAnalyticsP
                        .addParam(AnalyticsEvent.COUPON_INSERTED.NAME, true)
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.next.setOnClickListener {
            if (is_promo_code_fragment) {
                RxBus2.publish(PromoCodeSubmitEventBus(binding.tvReferralCode.text.toString()))
                requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
            } else {
                validateAndMoveToNextFragment()
            }
        }
    }


    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    fun validateAndMoveToNextFragment() {
        CoroutineScope(Dispatchers.Main).launch(Dispatchers.Main) {
            hideKeyboard(requireActivity(), binding.tvReferralCode)
            showProgress()
            try {
                val data = HashMap<String, String>()
                data["instance_id"] = PrefManager.getStringValue(INSTANCE_ID, false)
                data["coupon"] = binding.tvReferralCode.text.toString()

                val res =
                    AppObjectController.signUpNetworkService.validateAndGetReferralDetails(data)
                if (res.referralStatus) {
                    appAnalyticsP
                        .addParam(AnalyticsEvent.COUPON_VALID.NAME, true)
                        .addParam(
                            AnalyticsEvent.REFERRAL_CODE.NAME,
                            binding.tvReferralCode.text.toString().plus(
                                SINGLE_SPACE
                            ).plus(res.referrerName)
                        )
                    binding.wrongCode.visibility = View.GONE
                    PrefManager.put(REFERRED_REFERRAL_CODE, data["coupon"].toString())
                    requireActivity().supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.parent_Container,
                            ValidReferralCodeFragment.newInstance(res),
                            "Congratulation"
                        )
                        .commit()
                    this@EnterReferralCodeFragment.dismiss()
                } else binding.wrongCode.visibility = View.VISIBLE
            } catch (ex: Exception) {
                ex.showAppropriateMsg()
            }
            binding.progressBarButton.visibility = View.GONE
            hideProgress()
        }
    }

    private fun showProgress() {
        binding.progressBarButton.visibility = View.VISIBLE
        binding.next.visibility = View.GONE

    }

    private fun hideProgress() {
        binding.progressBarButton.visibility = View.INVISIBLE
        binding.next.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        appAnalyticsP.push()
    }

    companion object {
        @JvmStatic
        fun newInstance(isPromoCodeFragment: Boolean = false) =
            EnterReferralCodeFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(IS_PROMO_CODE_FRAGMENT, isPromoCodeFragment)
                }
            }
    }
}
