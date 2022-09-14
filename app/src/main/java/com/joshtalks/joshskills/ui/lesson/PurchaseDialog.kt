package com.joshtalks.joshskills.ui.lesson

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.databinding.PurchaseCourseDialogBinding
import com.joshtalks.joshskills.repository.server.PurchaseDataResponse
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PurchaseDialog : BaseDialogFragment() {

    private lateinit var binding: PurchaseCourseDialogBinding
    private var countdownTimerBack: CountdownTimerBack? = null
    private lateinit var purchaseDataResponse: PurchaseDataResponse

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PurchaseCourseDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        private const val PURCHASE_DATA = "purchase_data"

        @JvmStatic
        fun newInstance(purchaseDataResponse: PurchaseDataResponse): PurchaseDialog =
            PurchaseDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(PURCHASE_DATA, purchaseDataResponse)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        initView()
        savePopupImpression("POPUP_SHOWN")
        binding.btnBuy.setOnClickListener {
            savePopupImpression("POPUP_CLICKED")
            showFreeTrialPaymentScreen()
        }
    }

    private fun initView() {
        arguments?.getParcelable<PurchaseDataResponse>(PURCHASE_DATA)?.let {
            purchaseDataResponse = it
        } ?: run {
            dismiss()
        }
        binding.purchaseData = purchaseDataResponse
        binding.executePendingBindings()
        if (purchaseDataResponse.expireTime?.time != null) {
            if (purchaseDataResponse.expireTime?.time!! >= System.currentTimeMillis()) {
                if (purchaseDataResponse.expireTime?.time!! > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                    binding.txtFtEndsIn.visibility = View.GONE
                } else {
                    binding.txtFtEndsIn.visibility = View.VISIBLE
                    startTimer(purchaseDataResponse.expireTime?.time!! - System.currentTimeMillis())
                }
            } else {
                binding.txtFtEndsIn.visibility = View.VISIBLE
                binding.txtFtEndsIn.text = getString(R.string.free_trial_ended)
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            }
        } else {
            binding.txtFtEndsIn.visibility = View.GONE
        }
        binding.close.setOnClickListener {
            closeDialog()
        }
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        try {
            countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
                override fun onTimerTick(millis: Long) {
                    try {
                        AppObjectController.uiHandler.post {
                            binding.txtFtEndsIn.text = getString(
                                R.string.free_trial_end_in,
                                UtilTime.timeFormatted(millis)
                            )
                        }
                    } catch (ex: Exception) {

                    }
                }

                override fun onTimerFinish() {
                    try {
                        PrefManager.put(IS_FREE_TRIAL_ENDED, true)
                        binding.txtFtEndsIn.visibility = View.VISIBLE
                        binding.txtFtEndsIn.text = getString(R.string.free_trial_ended)
                    } catch (ex: Exception) {

                    }
                }
            }
            countdownTimerBack?.startTimer()
        } catch (ex: Exception) {

        }
    }

    fun showFreeTrialPaymentScreen() {
        try {
//            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
//                requireActivity(),
//                AppObjectController.getFirebaseRemoteConfig().getString(
//                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
//                ),
//                purchaseDataResponse.expireTime?.time ?: 0
//            )
            BuyPageActivity.startBuyPageActivity(
                requireActivity(),
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                )
            )
            closeDialog(isPopupIgnored = false)
        } catch (ex: Exception) {
            showToast(getString(R.string.something_went_wrong))
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    fun closeDialog(isPopupIgnored: Boolean = true) {
        countdownTimerBack?.stop()
        if(isPopupIgnored)
            savePopupImpression("POPUP_IGNORED")
        super.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
    }


    override fun onStart() {
        super.onStart()
        val d = dialog
        d?.let {
            try {
                val rect = Resources.getSystem().displayMetrics.run { Rect(0, 0, widthPixels, heightPixels) }
                val percentWidth = rect.width() * 0.8
                d.window?.setLayout(percentWidth.toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            } catch (e: Exception) {
                val width = ViewGroup.LayoutParams.MATCH_PARENT
                val height = ViewGroup.LayoutParams.WRAP_CONTENT
                d.window?.setLayout(width, height)
            }
        }
    }

    fun savePopupImpression(eventName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AppObjectController.commonNetworkService.savePopupImpression(
                    mapOf(
                        "popup_key" to (purchaseDataResponse.popUpKey ?: ""),
                        "event_name" to eventName
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}