package com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.adapter

import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.databinding.ItemOfffersCardBinding
import com.joshtalks.joshskills.premium.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.premium.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.premium.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.premium.ui.special_practice.utils.CLICK_ON_OFFER_CARD
import com.joshtalks.joshskills.premium.ui.special_practice.utils.REMOVE
import com.joshtalks.joshskills.premium.util.UtilTime
import kotlinx.coroutines.Job

const val CONDITIONAL = "CONDITIONAL"
const val NON_EXPIRABLE = "NON_EXPIRABLE"
const val EXPIRABLE = "EXPIRABLE"

class OffersListAdapter(val offersList: MutableList<Coupon> = mutableListOf()) :
    RecyclerView.Adapter<OffersListAdapter.OfferListViewHolder>() {

    var manager: RecyclerView.LayoutManager? = null
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null
    private var scrollToFirst: (() -> Unit?)? = null
    private var freeTrialTimerJob: Job? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferListViewHolder {
        val binding = ItemOfffersCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferListViewHolder, position: Int) {
        holder.binding.couponDiscountPercent.text = offersList[position].title

        holder.setUpUI(position, offersList[position], offersList[position].isCouponSelected)

        holder.binding.btnApply.setOnSingleClickListener {
            if (offersList[holder.bindingAdapterPosition].isCouponSelected == 0) {
                offersList[0].isCouponSelected = 0
                offersList[holder.bindingAdapterPosition].isCouponSelected = 1
                val selectedItem = offersList[holder.bindingAdapterPosition]
                offersList.remove(selectedItem)
                offersList.add(0, selectedItem)
                notifyItemChanged(0)
                notifyItemRemoved(holder.bindingAdapterPosition)
                notifyItemInserted(0)
                scrollToFirst?.invoke()
            } else {
                offersList[holder.bindingAdapterPosition].isCouponSelected = 0
                notifyItemChanged(holder.bindingAdapterPosition)
                offersList[position].let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
            }
        }
    }

    fun applyCoupon(coupon: Coupon) {
        var hasCoupon = false
        var couponIndex = -1
        for (i in offersList.indices) {
            if (offersList[i].couponCode == coupon.couponCode) {
                hasCoupon = true
                couponIndex = i
            }
        }
        try {
            offersList.firstOrNull()?.isCouponSelected = 0
            notifyItemChanged(0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (hasCoupon) {
            val item = offersList[couponIndex]
            item.isCouponSelected = 1
            offersList.remove(item)
            offersList.add(0, item)
            notifyItemRemoved(couponIndex)
            notifyItemInserted(0)
            scrollToFirst?.invoke()
        } else {
            coupon.isCouponSelected = 1
            offersList.add(0, coupon)
            notifyItemInserted(0)
            scrollToFirst?.invoke()
        }
    }

    override fun getItemCount(): Int = offersList.size

    fun addOffersList(members: MutableList<Coupon>) {
        val listToAdd = mutableListOf<Coupon>()
        if (offersList.isEmpty()) {
            offersList.addAll(members)
        } else {
            members.forEach { coupon ->
                val itemInOffer = offersList.filter { it.couponCode == coupon.couponCode }
                if (itemInOffer.isEmpty().not()) {
                    listToAdd.addAll(itemInOffer)
                }
            }
            offersList.clear()
            offersList.addAll(listToAdd)
        }
        freeTrialTimerJob?.cancel()
        notifyDataSetChanged()
    }

    private fun shouldDisableCoupon(coupon: Coupon): Boolean {
        return coupon.isEnable == false
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class OfferListViewHolder(val binding: ItemOfffersCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var countdownTimerBack: CountDownTimer? = null
        private fun startFreeTrialTimer(endTimeInMilliSeconds: Long, position: Int) {
            try {
                if (countdownTimerBack != null)
                    countdownTimerBack?.cancel()

                countdownTimerBack = object : CountDownTimer(endTimeInMilliSeconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        AppObjectController.uiHandler.post {
                            binding.couponExpireText.visibility = View.VISIBLE
                            binding.couponExpireText.text = "Coupon will expire in " + UtilTime.timeFormatted(millisUntilFinished)
                        }
                    }

                    override fun onFinish() {
                        AppObjectController.uiHandler.post {
                            setUpUI(position, offersList[position], offersList[position].isCouponSelected)
                            if (offersList[position].isCouponSelected == 1)
                                offersList[position]
                                    .let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
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
            coupon.setUI(position, state, isCouponSelected)
        }

        private fun validateCoupon(
            position: Int,
        ): CouponEnum {
            return if (shouldDisableCoupon(offersList[position])) {
                disableCoupon()
                CouponEnum.DISABLE
            } else {
                enableCoupon()
                CouponEnum.ENABLE
            }
        }

        private fun Coupon.setUI(
            position: Int,
            condition: CouponEnum,
            isCouponSelected: Int
        ) {
            val coupon = this
            when (offersList[position].couponType) {
                CONDITIONAL -> {
                    if (isCouponSelected == 1 && condition == CouponEnum.ENABLE) {
                        appliedCoupon(position)
                    }
                    countdownTimerBack?.cancel()
                    countdownTimerBack = null
                    binding.couponExpireText.visibility = View.VISIBLE
                    binding.couponExpireText.text = offersList[position].couponDesc
                }
                EXPIRABLE -> {
                    if (isCouponSelected == 1 && coupon.validDuration?.time?.minus(System.currentTimeMillis())!! > 0) {
                        appliedCoupon(position)
                        startFreeTrialTimer(coupon.validDuration.time.minus(System.currentTimeMillis()), position)
                    }
                    else if (condition == CouponEnum.ENABLE && coupon.validDuration?.time?.minus(System.currentTimeMillis())!! > 0) {
                        startFreeTrialTimer(coupon.validDuration.time.minus(System.currentTimeMillis()), position)
                    }
                    else {
                        disableCoupon()
                        countdownTimerBack?.cancel()
                        countdownTimerBack = null
                        binding.couponExpireText.visibility = View.VISIBLE
                        binding.couponExpireText.text = "Coupon Expired"
                    }
                }
                NON_EXPIRABLE -> {
                    if (isCouponSelected == 1 && condition == CouponEnum.ENABLE) {
                        appliedCoupon(position)
                    }
                    binding.couponExpireText.visibility = View.GONE
                    countdownTimerBack?.cancel()
                    countdownTimerBack = null
                }
            }
        }

        private fun disableCoupon() {
            countdownTimerBack?.cancel()
            countdownTimerBack = null
            binding.rootCard.isEnabled = false
            binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            binding.couponExpireText.setTextColor(
                ContextCompat.getColor(
                    binding.couponExpireText.context,
                    R.color.text_subdued
                )
            )
            binding.couponDiscountPercent.setTextColor(
                ContextCompat.getColor(
                    binding.couponExpireText.context,
                    R.color.text_subdued
                )
            )
            binding.btnApply.isEnabled = false
            binding.btnApply.text = APPLY
            binding.imgLogo.alpha = 0.5f
            binding.btnApply.setTextColor(ContextCompat.getColor(binding.couponExpireText.context, R.color.text_subdued))
        }

        private fun enableCoupon() {
            countdownTimerBack?.cancel()
            countdownTimerBack = null
            binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            binding.rootCard.isEnabled = true
            binding.couponExpireText.setTextColor(
                ContextCompat.getColor(
                    binding.couponExpireText.context,
                    R.color.text_subdued
                )
            )
            binding.couponDiscountPercent.setTextColor(
                ContextCompat.getColor(
                    binding.couponExpireText.context,
                    R.color.pure_black
                )
            )
            binding.btnApply.isEnabled = true
            binding.btnApply.text = APPLY
            binding.imgLogo.alpha = 1f
            binding.btnApply.setTextColor(ContextCompat.getColor(binding.couponExpireText.context, R.color.primary_500))
        }

        private fun appliedCoupon(position: Int) {
            binding.rootCard.isEnabled = true
            binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            binding.btnApply.text = REMOVE
            binding.btnApply.isEnabled = true
            binding.imgLogo.alpha = 1.0f
            if (offersList[position].couponDesc != null)
                binding.couponExpireText.text = offersList[position].couponDesc
            val colorAccent = ContextCompat.getColor(binding.couponExpireText.context, R.color.primary_500)
            val blackColor = ContextCompat.getColor(binding.couponExpireText.context, R.color.pure_black)
            binding.btnApply.setTextColor(colorAccent)
            binding.couponDiscountPercent.setTextColor(blackColor)
            offersList[position].let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        }

    }

    fun scroll(func: () -> Unit?) {
        scrollToFirst = func
    }
}

enum class CouponEnum {
    ENABLE,
    DISABLE
}