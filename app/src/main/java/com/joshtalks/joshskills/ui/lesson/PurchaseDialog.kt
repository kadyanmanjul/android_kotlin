package com.joshtalks.joshskills.ui.lesson

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.CALL_POPUP_CLICKED
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.CALL_POPUP_IGNORED
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.CALL_POPUP_SEEN
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.databinding.PurchaseCourseDialogBinding
import com.joshtalks.joshskills.ui.payment.FreeTrialPaymentActivity
import java.util.*

class PurchaseDialog: BaseDialogFragment()  {

    private lateinit var binding: PurchaseCourseDialogBinding
    val POP_TEXT = "POP_TEXT"
    val TITLE_TEXT = "TITLE_TEXT"
    val PRICE_TEXT = "PRICE_TEXT"
    var expireDate:Date? = null
    private var countdownTimerBack: CountdownTimerBack? = null

    private val vm by lazy {
        ViewModelProvider(requireActivity())[LessonViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PurchaseCourseDialogBinding.inflate(inflater,container,false)
        return binding.root
    }

    companion object {
        @JvmStatic
        fun newInstance(timerPopText:String = EMPTY, timerTitlePopText:String = EMPTY, pricePopUpText:String = EMPTY, expireTime:Date? = null): PurchaseDialog {
            val fragment = PurchaseDialog().apply {
                vm.saveImpression(CALL_POPUP_SEEN)
                arguments = Bundle().apply {
                    putString(POP_TEXT, timerPopText)
                    putString(TITLE_TEXT, timerTitlePopText)
                    putString(PRICE_TEXT, pricePopUpText)
                    expireDate = expireTime
                }
            }
            return fragment
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        binding.btnCancel.setOnClickListener {
            closeDialog()
            vm.saveImpression(CALL_POPUP_IGNORED)
        }

        binding.btnBuy.setOnClickListener {
            showFreeTrialPaymentScreen()
            vm.saveImpression(CALL_POPUP_CLICKED)
        }
    }

    private fun initView() {
        val mArgs = arguments
        binding.txtPopUpBody.text = mArgs?.getString(POP_TEXT).toString()
        binding.txtPopUpTitle.text = mArgs?.getString(TITLE_TEXT).toString()
        binding.btnBuy.text = mArgs?.getString(PRICE_TEXT)
        if (expireDate?.time!=null) {
            if (expireDate?.time!! >= System.currentTimeMillis()) {
                if (expireDate?.time!! > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                    binding.txtFtEndsIn.visibility = View.GONE
                } else {
                    binding.txtFtEndsIn.visibility = View.VISIBLE
                    startTimer(expireDate?.time!! - System.currentTimeMillis())
                }
            } else {
                binding.txtFtEndsIn.visibility = View.VISIBLE
                binding.txtFtEndsIn.text = getString(R.string.free_trial_ended)
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            }
        }else{
            binding.txtFtEndsIn.visibility = View.GONE
        }
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                try {
                    AppObjectController.uiHandler.post {
                        binding.txtFtEndsIn.text = getString(
                            R.string.free_trial_end_in,
                            UtilTime.timeFormatted(millis)
                        )
                    }
                }catch (ex:Exception){

                }
            }

            override fun onTimerFinish() {
                try {
                    PrefManager.put(IS_FREE_TRIAL_ENDED, true)
                    binding.txtFtEndsIn.visibility = View.VISIBLE
                    binding.txtFtEndsIn.text = getString(R.string.free_trial_ended)
                }catch (ex:Exception){

                }
            }
        }
        countdownTimerBack?.startTimer()
    }

    fun showFreeTrialPaymentScreen() {
        try {
            FreeTrialPaymentActivity.startFreeTrialPaymentActivity(
                requireActivity(),
                AppObjectController.getFirebaseRemoteConfig().getString(
                    FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                ),
                expireDate?.time?:0
            )
            closeDialog()
        }catch (ex:Exception){
            showToast(getString(R.string.something_went_wrong))
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    fun closeDialog() {
        countdownTimerBack?.stop()
        super.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimerBack?.stop()
    }
}