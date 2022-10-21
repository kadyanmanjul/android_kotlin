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
import com.joshtalks.joshskills.databinding.ItemCouponCardBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_COUPON_APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE
import kotlinx.coroutines.*

class CouponListAdapter(var offersList: List<Coupon>? = listOf()) :
    RecyclerView.Adapter<CouponListAdapter.CouponViewHolder>() {
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val binding = ItemCouponCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CouponViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.setData(offersList?.get(position),position)
        if ((offersList?.get(position)?.validDuration?.time?.minus(System.currentTimeMillis()) ?: 0) < 0L) {
            holder.changeTextColors(holder.binding, offersList?.get(position), position)
        }
    }

    override fun getItemCount(): Int = offersList?.size ?: 0

    fun addOffersList(members: List<Coupon>?) {
        offersList = members
        notifyDataSetChanged()
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class CouponViewHolder(val binding: ItemCouponCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var countdownTimerBack: CountDownTimer? = null

        fun setData(members: Coupon?, position: Int) {
            if (members?.validDuration?.time?.minus(System.currentTimeMillis())!! > 0L)
                startFreeTrialTimer(
                    members?.validDuration?.time?.minus(System.currentTimeMillis()) ?: 0,
                    members,
                    position
                )
            else
                binding.txtCouponExpireTime.text = "Coupon expired"
            with(binding) {
                this.txtCouponCode.text = members?.couponCode
                this.couponDesc.text =
                    "Use code " + "${members?.couponCode} " + "and get ${members?.amountPercent.toString()}% off on you purchase."
                this.saveMoney.text = "Save upto ₹" + members?.maxDiscountAmount.toString() + " with this code"

                this.btnApply.setOnSingleClickListener {
                    if (members != null) {
                        members.isCouponSelected = 1
                        itemClick?.invoke(members, CLICK_ON_COUPON_APPLY, position, APPLY)
                    }
                }
            }
        }

        fun startFreeTrialTimer(endTimeInMilliSeconds: Long, coupon: Coupon?, position: Int) {
            try {
                if (countdownTimerBack!=null)
                    countdownTimerBack?.cancel()

                countdownTimerBack = object : CountDownTimer(endTimeInMilliSeconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (countdownTimerBack != null) {
                            AppObjectController.uiHandler.post {
                                binding.txtCouponExpireTime.text = "Coupon will expire in " + UtilTime.timeFormatted(millisUntilFinished)
                            }
                        }
                    }

                    override fun onFinish() {
                        AppObjectController.uiHandler.post {
                            changeTextColors(binding, coupon, position)
                            countdownTimerBack?.cancel()
                        }
                    }
                }
                countdownTimerBack?.start()
            } catch (ex: Exception) {
                Log.e("sagar", "startFreeTrialTimer: ${ex.message}")
            }
        }

        fun changeTextColors(binding: ItemCouponCardBinding, coupon: Coupon?, position: Int){
            Log.e("sagar", "changeTextColors: ")
            if (coupon?.isCouponSelected == 1)
                coupon.let { itemClick?.invoke(it, CLICK_ON_COUPON_APPLY, position, REMOVE) }
            val grayColor = ContextCompat.getColor(binding.txtCouponExpireTime.context, R.color.gray_8D)
            binding.rootCard.isEnabled = false
            binding.txtCouponCode.setTextColor(grayColor)
            binding.txtCouponExpireTime.text = "Coupon expired"
            binding.txtCouponExpireTime.setTextColor(grayColor)
            binding.couponDesc.setTextColor(grayColor)
            binding.saveMoney.setTextColor(grayColor)
            binding.rootContainer.background = ContextCompat.getDrawable(binding.rootContainer.context,R.drawable.gray_rectangle_without_solid)
            binding.btnApply.isEnabled = false
            binding.btnApply.setTextColor(grayColor)
        }
    }
}