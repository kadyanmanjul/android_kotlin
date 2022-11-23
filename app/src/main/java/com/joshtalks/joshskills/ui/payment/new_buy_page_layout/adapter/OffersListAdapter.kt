package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ItemOfffersCardBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_OFFER_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job

class OffersListAdapter(val offersList: MutableList<Coupon> = mutableListOf()) :
    RecyclerView.Adapter<OffersListAdapter.OfferListViewHolder>() {
    private val TAG = "OffersListAdapter"
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null
    var prevHolder: OfferListViewHolder? = null
    var scrollToFirst: (() -> Unit?)? = null
    var manager: RecyclerView.LayoutManager? = null
    var isAnySelected = false
    private var freeTrialTimerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferListViewHolder {
        val binding =
            ItemOfffersCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferListViewHolder, position: Int) {
        holder.setData(offersList[position], position)
        holder.binding.couponDiscountPercent.text = "Get Extra ${offersList[position].amountPercent} % Off"

        if (offersList[position].isCouponSelected == 1) {
            holder.binding.rootCard.isEnabled = true
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            holder.binding.btnApply.text = REMOVE
            holder.binding.btnApply.isEnabled = true
            holder.binding.imgLogo.alpha = 1.0f
            val colorAccent =
                ContextCompat.getColor(holder.binding.couponExpireText.context, R.color.primary_500)
            holder.binding.btnApply.setTextColor(colorAccent)
            val blackColor =
                ContextCompat.getColor(holder.binding.couponExpireText.context, R.color.pure_black)
            holder.binding.couponDiscountPercent.setTextColor(blackColor)
            offersList[position]
                .let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        } else {
            if (offersList[position].couponDesc != null) {
                holder.binding.couponExpireText.text = offersList[position].couponDesc
                holder.binding.couponExpireText.visibility = View.VISIBLE
                holder.disableCouponWithoutApiCal(holder.binding, offersList[position], position)
            } else if (offersList[position].validDuration != null && (offersList[position].validDuration?.time?.minus(
                    System.currentTimeMillis()
                ) ?: 0) > 0L && offersList[position].isMentorSpecificCoupon != null
            ) {
                holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                holder.binding.btnApply.text = APPLY
                holder.binding.couponExpireText.visibility = View.VISIBLE
            } else {
                if (offersList[position].couponDesc == null && offersList[position].isMentorSpecificCoupon==null){
                    holder.enableCoupon(holder.binding, offersList[position], position)
                    holder.binding.couponExpireText.text = "Get extra 20% off"
                    holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                }else{
                    offersList[position].let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
                    holder.changeTextColor(holder.binding, offersList[position], position)
                    holder.binding.couponExpireText.visibility = View.VISIBLE
                }
            }
        }

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
                offersList[position]
                    .let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
            }
//            Log.e("sagar", "onBindViewHolder: onBindViewHolder", )
//            notifyDataSetChanged()
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
//        Log.e("sagar", "setData: applyCoupon")
//        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = offersList.size

    fun addOffersList(members: MutableList<Coupon>) {
        val listToAdd = mutableListOf<Coupon>()
        if (offersList.isNullOrEmpty()) {
            offersList.addAll(members)
        } else {
            members.forEach { coupon ->
                val itemInOffer = offersList.filter { it.couponCode == coupon.couponCode }
                if (itemInOffer.isNullOrEmpty().not()) {
                    listToAdd.addAll(itemInOffer)
                }
            }
            offersList.clear()
            offersList.addAll(listToAdd)
        }
        freeTrialTimerJob?.cancel()
        Log.e("sagar", "setData: addOffersList", )
        notifyDataSetChanged()
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class OfferListViewHolder(val binding: ItemOfffersCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var countdownTimerBack: CountDownTimer? = null
        fun setData(coupon: Coupon?, position: Int) {
            binding.executePendingBindings()
            if (offersList[position].couponDesc != null) {
                countdownTimerBack?.cancel()
                countdownTimerBack = null
                binding.couponExpireText.visibility = View.VISIBLE
                binding.couponExpireText.text = offersList[position].couponDesc
                disableCouponWithoutApiCal(binding, offersList[position], position)
            } else if (offersList[position].validDuration != null && (offersList[position].validDuration?.time?.minus(System.currentTimeMillis()) ?: 0) > 0L && coupon?.isMentorSpecificCoupon != null) {
                startFreeTrialTimer(coupon.validDuration?.time?.minus(System.currentTimeMillis())?:0, coupon, position)
            } else {
                if (offersList[position].couponDesc == null && offersList[position].isMentorSpecificCoupon==null){
                    enableCoupon(binding, offersList[position], position)
                    binding.couponExpireText.text = "Get extra 20% off"
                    binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                    countdownTimerBack?.cancel()
                    countdownTimerBack = null
                }else {
                    changeTextColor(binding, coupon, position)
                    if (coupon?.isMentorSpecificCoupon != null)
                        binding.couponExpireText.visibility = View.VISIBLE
                    else
                        binding.couponExpireText.visibility = View.GONE
                }
            }
        }

        fun startFreeTrialTimer(endTimeInMilliSeconds: Long, coupon: Coupon?, position: Int) {
            try {
                if (countdownTimerBack != null)
                    countdownTimerBack?.cancel()

                countdownTimerBack = object : CountDownTimer(endTimeInMilliSeconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (countdownTimerBack != null) {
                            AppObjectController.uiHandler.post {
                                if (coupon?.couponDesc != null)
                                    binding.couponExpireText.text = coupon.couponDesc
                                else if (coupon?.isMentorSpecificCoupon != null) {
                                    binding.couponExpireText.text =
                                        "Coupon will expire in " + UtilTime.timeFormatted(millisUntilFinished)
                                } else {
                                    binding.couponExpireText.visibility = View.GONE
                                }
                            }
                        }
                    }

                    override fun onFinish() {
                        AppObjectController.uiHandler.post {
                            if (offersList[position].couponDesc == null && offersList[position].isMentorSpecificCoupon==null){
                                enableCoupon(binding, offersList[position], position)
                                binding.couponExpireText.text = "Get extra 20% off"
                                binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                                countdownTimerBack?.cancel()
                                countdownTimerBack = null
                            }else {
                                changeTextColor(binding, coupon, position)
                                binding.couponExpireText.visibility = View.VISIBLE
                                countdownTimerBack?.cancel()
                            }
                        }
                    }
                }
                countdownTimerBack?.start()
            } catch (ex: Exception) {
                Log.e("sagar", "startFreeTrialTimer: ${ex.message}")
            }
        }

        fun changeTextColor(binding: ItemOfffersCardBinding, coupon: Coupon?, position: Int) {
            if (coupon?.couponDesc != null)
                binding.couponExpireText.text = coupon.couponDesc
            else if (coupon?.isMentorSpecificCoupon != null) {
                binding.couponExpireText.text = "Coupon expired"
                disableCoupon(binding, coupon, position)
            } else {
                binding.couponExpireText.visibility = View.GONE
            }
        }

        fun disableCoupon(binding: ItemOfffersCardBinding, coupon: Coupon, position: Int) {
            if (coupon.isCouponSelected == 1)
                coupon.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
            val grayColor =
                ContextCompat.getColor(binding.couponExpireText.context, R.color.dark_grey)
            binding.rootCard.isEnabled = false
            binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            freeTrialTimerJob?.cancel()
            binding.couponExpireText.setTextColor(grayColor)
            binding.couponDiscountPercent.setTextColor(grayColor)
            binding.btnApply.isEnabled = false
            binding.btnApply.text = APPLY
            binding.imgLogo.alpha = 0.5f
            binding.btnApply.setTextColor(grayColor)
            binding.couponExpireText.visibility = View.VISIBLE
        }

        fun enableCoupon(binding: ItemOfffersCardBinding, coupon: Coupon, position: Int) {
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
            binding.couponExpireText.visibility = View.VISIBLE
        }

        fun disableCouponWithoutApiCal(binding: ItemOfffersCardBinding, coupon: Coupon, position: Int) {
            binding.rootCard.isEnabled = false
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
            binding.couponExpireText.visibility = View.VISIBLE
        }
    }

    fun scroll(func: () -> Unit?) {
        scrollToFirst = func
    }
}