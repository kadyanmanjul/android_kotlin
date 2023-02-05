package com.joshtalks.joshskills.premium.ui.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.freshchat.consumer.sdk.Freshchat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.*
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.databinding.FragmentPaymentFailedDialogBinding
import com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.premium.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.premium.core.FRESH_CHAT_UNREAD_MESSAGES
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.ui.help.HelpActivity
import com.joshtalks.joshskills.premium.ui.payment.order_summary.ERROR_MSG
import com.joshtalks.joshskills.premium.ui.payment.order_summary.PaymentSummaryViewModel
import com.joshtalks.joshskills.premium.ui.payment.order_summary.TRANSACTION_ID

class PaymentFailedDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentFailedDialogBinding
    private var transactionId: Int = 0
    private var errorMsg :String = EMPTY
    private var testId: String = EMPTY
    private val viewModel: PaymentSummaryViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PaymentSummaryViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            transactionId = it.getInt(TRANSACTION_ID)
            errorMsg = it.getString(ERROR_MSG)?: EMPTY
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isCancelable = false
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_payment_failed_dialog,
            container,
            false
        )
        binding.lifecycleOwner = this
        binding.fragment = this
        binding.transationId.text = resources.getString(R.string.trx_id, transactionId.toString())
        setListeners()
        return binding.root
    }

    private fun setListeners() {
        viewModel.testId.observe(
            viewLifecycleOwner
        ){
            testId = it
        }
        binding.textView3.text = errorMsg
        binding.retry.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.RETRY_PAYMENT)
                .addParam(ParamKeys.TEST_ID,viewModel.getPaymentTestId())
                .addParam(ParamKeys.COURSE_NAME, viewModel.getCourseName())
                .addParam(ParamKeys.COURSE_PRICE, viewModel.getCourseActualAmount())
                .addParam(ParamKeys.IS_COUPON_APPLIED, viewModel.responsePaymentSummary.value?.couponDetails?.isPromoCode)
                .addParam(ParamKeys.AMOUNT_PAID, viewModel.getCourseDiscountedAmount())
                .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                .push()
            dismiss()
        }
        binding.chatPay.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.WHATSAPP_CLICKED_PAYMENT_FAILED)
                .addParam(ParamKeys.TEST_ID,viewModel.getPaymentTestId())
                .addParam(ParamKeys.COURSE_NAME, viewModel.getCourseName())
                .addParam(ParamKeys.COURSE_PRICE, viewModel.getCourseActualAmount())
                .addParam(ParamKeys.IS_COUPON_APPLIED, viewModel.responsePaymentSummary.value?.couponDetails?.isPromoCode)
                .addParam(ParamKeys.AMOUNT_PAID, viewModel.getCourseDiscountedAmount())
                .addParam(ParamKeys.COURSE_ID,
                    PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                .push()
            openWhatsapp()
        }
        binding.close.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.CANCEL).push()
            dismissAndCloseActivity()
        }
        binding.help.setOnClickListener { openHelpActivity() }
    }

    private fun dismissAndCloseActivity() {
        AppAnalytics.create(AnalyticsEvent.RETRY_PAYMENT.NAME)
            .addUserDetails()
            .addBasicParam()
            .push()
        dismiss()
        activity?.finish()
    }

    private fun openWhatsapp() {
        try {
            AppAnalytics.create(AnalyticsEvent.HELP_CHAT.NAME)
                .addBasicParam()
                .addUserDetails()
                .push()
            Freshchat.showConversations(requireContext())
            PrefManager.put(FRESH_CHAT_UNREAD_MESSAGES, 0)
            activity?.finish()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    private fun openHelpActivity() {
        val i = Intent(activity, HelpActivity::class.java)
        startActivityForResult(i, HELP_ACTIVITY_REQUEST_CODE)
    }

    companion object {
        @JvmStatic
        fun newInstance(transactionId: Int, errorMsg:String) =
            PaymentFailedDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(TRANSACTION_ID, transactionId)
                    putString(ERROR_MSG, errorMsg)
                }
            }
    }
}
