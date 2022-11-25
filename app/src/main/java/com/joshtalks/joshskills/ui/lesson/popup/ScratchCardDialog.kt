package com.joshtalks.joshskills.ui.lesson.popup

import android.content.res.Resources
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.lifecycleScope
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseDialogFragment
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey
import com.joshtalks.joshskills.databinding.ScratchCardDialogBinding
import com.joshtalks.joshskills.repository.server.PurchaseDataResponse
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.BuyPageActivity
import com.joshtalks.joshskills.util.scratch.ScratchView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ScratchCardDialog : BaseDialogFragment() {

    private lateinit var binding: ScratchCardDialogBinding
    private lateinit var cardData: PurchaseDataResponse

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ScratchCardDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = true
        savePopupImpression("SCRATCH_CARD_SHOWN")

        arguments?.getParcelable<PurchaseDataResponse>(SCRATCH_CARD_DATA)?.let {
            cardData = it
            binding.cardTitle.text = it.popUpTitle
            binding.cardBody.text = it.popUpBody
            if (it.couponCode.isNullOrBlank().not()) {
                binding.cardContinue.text = "CLAIM NOW!"
                binding.cardImage.setImageResource(R.drawable.ic_coin)
            }
        }

        lifecycleScope.launch(Dispatchers.Main) {
            delay(1500)
            binding.tvScratchHere.startAnimation(AnimationUtils.loadAnimation(requireActivity(), R.anim.fade_in))
            binding.tvScratchHere.visibility = VISIBLE
        }

        binding.cardContinue.setOnClickListener {
            if (binding.cardContinue.text != requireActivity().getString(R.string.got_it))
                BuyPageActivity.startBuyPageActivity(
                    requireActivity(),
                    AppObjectController.getFirebaseRemoteConfig().getString(
                        FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID
                    ),
                    "SCRATCH_CARD"
                )
            dismiss()
        }

        binding.scratchView.setRevealListener(object : ScratchView.IRevealListener {
            override fun onRevealed(scratchView: ScratchView) {
                scratchView.reveal()
                savePopupImpression("SCRATCH_CARD_UNLOCKED")
                if (binding.cardContinue.text != requireActivity().getString(R.string.got_it)) {
                    binding.cardConfetti.visibility = VISIBLE
                    binding.cardConfetti.playAnimation()
                }
            }

            override fun onRevealPercentChangedListener(scratchView: ScratchView, percent: Float) {
                binding.tvScratchHere.visibility = GONE
                if (percent >= 0.5) {
                    scratchView.reveal()
                }
            }
        })
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

    private fun savePopupImpression(eventName: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                AppObjectController.commonNetworkService.savePopupImpression(
                    mapOf(
                        "event_name" to eventName,
                        "popup_key" to (cardData.popUpKey ?: "scratch_card")
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun show(manager: FragmentManager, tag: String?) {
        if (!manager.isDestroyed && !manager.isStateSaved) {
            super.show(manager, tag)
        }
    }

    companion object {

        private const val SCRATCH_CARD_DATA = "SCRATCH_CARD_DATA"

        @JvmStatic
        fun newInstance(cardData: PurchaseDataResponse?): ScratchCardDialog =
            ScratchCardDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(SCRATCH_CARD_DATA, cardData)
                }
            }
    }
}