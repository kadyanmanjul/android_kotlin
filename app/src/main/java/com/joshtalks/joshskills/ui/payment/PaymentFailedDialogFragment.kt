package com.joshtalks.joshskills.ui.payment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HELP_ACTIVITY_REQUEST_CODE
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.databinding.FragmentPaymentFailedDialogBinding
import com.joshtalks.joshskills.ui.help.HelpActivity
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryViewModel
import com.joshtalks.joshskills.ui.payment.order_summary.TRANSACTION_ID

const val WHATSAPP_URL_PAYMENT_FAILED = "http://english-new.joshtalks.org/whats_app/201"

class PaymentFailedDialogFragment : BottomSheetDialogFragment() {
    private lateinit var binding: FragmentPaymentFailedDialogBinding
    private var transactionId: Int = 0
    private var testId: String = EMPTY
    private val viewModel: PaymentSummaryViewModel by lazy {
        ViewModelProvider(requireActivity()).get(PaymentSummaryViewModel::class.java)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            transactionId = it.getInt(TRANSACTION_ID)
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
        binding.retry.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.RETRY_PAYMENT)
                .addParam(ParamKeys.TEST_ID,testId)
                .addParam(ParamKeys.COURSE_NAME, viewModel.responsePaymentSummary.value?.courseName)
                .addParam(ParamKeys.COURSE_PRICE, viewModel.responsePaymentSummary.value?.amount)
                .addParam(ParamKeys.IS_COUPON_APPLIED, viewModel.responsePaymentSummary.value?.couponDetails?.isPromoCode)
                .addParam(ParamKeys.AMOUNT_PAID, viewModel.responsePaymentSummary.value?.discountedAmount)
                .addParam(ParamKeys.COURSE_ID, PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
                .push()
            dismiss()
        }
        binding.chatPay.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.WHATSAPP_CLICKED_PAYMENT_FAILED)
                .addParam(ParamKeys.TEST_ID,testId)
                .addParam(ParamKeys.COURSE_NAME, viewModel.responsePaymentSummary.value?.courseName)
                .addParam(ParamKeys.COURSE_PRICE, viewModel.responsePaymentSummary.value?.amount)
                .addParam(ParamKeys.IS_COUPON_APPLIED, viewModel.responsePaymentSummary.value?.couponDetails?.isPromoCode)
                .addParam(ParamKeys.AMOUNT_PAID, viewModel.responsePaymentSummary.value?.discountedAmount)
                .addParam(ParamKeys.COURSE_ID,PrefManager.getStringValue(CURRENT_COURSE_ID, false, DEFAULT_COURSE_ID))
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
            AppAnalytics.create(AnalyticsEvent.WHATSAPP_CLICKED_PAYMENT_FAILED.NAME)
                .addUserDetails()
                .addBasicParam()
                .push()
            val intent = Intent().apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(WHATSAPP_URL_PAYMENT_FAILED)
            }
            startActivity(intent)
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
        fun newInstance(transactionId: Int) =
            PaymentFailedDialogFragment().apply {
                arguments = Bundle().apply {
                    putInt(TRANSACTION_ID, transactionId)
                }
            }
    }
}
