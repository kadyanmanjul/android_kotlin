package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ItemCouponCardNewBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_COUPON_APPLY

class CouponListAdapter(val offersList: MutableList<Coupon> = mutableListOf()) :
    RecyclerView.Adapter<CouponListAdapter.CouponViewHolder>() {
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val binding = ItemCouponCardNewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CouponViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {

        holder.setUpUI(position, offersList[position], offersList[position].isCouponSelected)

        holder.binding.btnApply.setOnSingleClickListener {
            offersList[position].isCouponSelected = 1
            itemClick?.invoke(offersList[position], CLICK_ON_COUPON_APPLY, position, APPLY)
        }
    }

    override fun getItemCount(): Int = offersList.size

    fun addOffersList(members: MutableList<Coupon>) {
        if (offersList.isEmpty()) {
            offersList.addAll(members)
        }
        notifyDataSetChanged()
    }

    private fun shouldDisableCoupon(coupon: Coupon): Boolean {
        return coupon.isEnable == false
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class CouponViewHolder(val binding: ItemCouponCardNewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var countdownTimerBack: CountDownTimer? = null

        private fun startFreeTrialTimer(endTimeInMilliSeconds: Long, coupon: Coupon?, position: Int) {
            try {
                if (countdownTimerBack != null)
                    countdownTimerBack?.cancel()

                countdownTimerBack = object : CountDownTimer(endTimeInMilliSeconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        AppObjectController.uiHandler.post {
                            binding.txtCouponExpireTime.text =
                                "Coupon will expire in " + UtilTime.timeFormatted(millisUntilFinished)
                        }
                    }

                    override fun onFinish() {
                        AppObjectController.uiHandler.post {
                            setUpUI(position, offersList[position], offersList[position].isCouponSelected)
                        }
                    }
                }
                countdownTimerBack?.start()
            } catch (ex: Exception) {
                Log.e("sagar", "startFreeTrialTimer: ${ex.message}")
            }
        }

        fun setUpUI(position: Int, coupon: Coupon, isCouponSelected: Int) {
            val state = validateCoupon(position)
            coupon.setUI(position, state)
        }

        private fun validateCoupon(
            position: Int,
        ): CouponEnum {
            return if (shouldDisableCoupon(offersList[position])) {
                disableCoupon()
                CouponEnum.DISABLE
            } else {
                CouponEnum.ENABLE
            }
        }

        private fun Coupon.setUI(
            position: Int,
            condition: CouponEnum
        ) {
            val coupon = this
            binding.txtCouponCode.text = coupon.couponCode
            val title = coupon.title
            binding.couponDesc.text = "Use the coupon and $title"
            binding.saveMoney.text = "Save upto ₹" + coupon?.maxDiscountAmount.toString() + " with this code"
            binding.couponPercent.text = coupon.couponCode
            when (offersList[position].couponType) {
                CONDITIONAL -> {
                    binding.txtCouponExpireTime.text = coupon.couponDesc
                    countdownTimerBack?.cancel()
                    countdownTimerBack = null
                }
                EXPIRABLE -> {
                    if (condition == CouponEnum.ENABLE && coupon.validDuration?.time?.minus(System.currentTimeMillis())!! > 0) {
                        startFreeTrialTimer(coupon.validDuration.time.minus(System.currentTimeMillis()), coupon, position)
                    } else {
                        disableCoupon()
                        countdownTimerBack?.cancel()
                        countdownTimerBack = null
                        binding.txtCouponExpireTime.text = "Coupon Expired"
                    }
                }
                NON_EXPIRABLE -> {
                    binding.txtCouponExpireTime.text = coupon.couponDesc
                    countdownTimerBack?.cancel()
                    countdownTimerBack = null
                }
            }
        }

        private fun disableCoupon() {
            binding.rootCard.isEnabled = false
            binding.btnApply.isEnabled = false
            val grayColor = ContextCompat.getColor(binding.txtCouponExpireTime.context, R.color.text_subdued)
            binding.saveMoney.setTextColor(grayColor)
            binding.btnApply.text = "APPLY"
            binding.btnApply.alpha = 0.8f
            binding.btnApply.setTextColor(ContextCompat.getColor(binding.txtCouponExpireTime.context, R.color.disabled))
            binding.viewSide.setImageResource(R.drawable.disable_coupon_side_bar)
        }
    }
}