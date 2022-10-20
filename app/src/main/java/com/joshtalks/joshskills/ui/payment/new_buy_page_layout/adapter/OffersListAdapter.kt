package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ItemOfffersCardBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_OFFER_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE
import kotlinx.coroutines.*

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
                ContextCompat.getColor(holder.binding.couponExpireText.context, R.color.colorAccent)
            holder.binding.btnApply.setTextColor(colorAccent)
            val blackColor =
                ContextCompat.getColor(holder.binding.couponExpireText.context, R.color.pure_black)
            holder.binding.couponDiscountPercent.setTextColor(blackColor)
            offersList[position]
                .let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        } else {
            if (offersList[position].validDuration.time.minus(System.currentTimeMillis()) > 0L && offersList[position].isMentorSpecificCoupon!=null) {
                holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                holder.binding.btnApply.text = APPLY
                holder.binding.couponExpireText.visibility = View.VISIBLE
            } else {
                offersList[position].let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
                holder.changeTextColor(holder.binding, offersList[position], position)
                holder.binding.couponExpireText.visibility = View.VISIBLE
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
        offersList[0].isCouponSelected = 0
        notifyItemChanged(0)
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
        if (offersList.isNullOrEmpty()){
            offersList.addAll(members)
        }else {
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
        notifyDataSetChanged()
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class OfferListViewHolder(val binding: ItemOfffersCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(coupon: Coupon?, position: Int) {
            binding.executePendingBindings()
            if (offersList[position].validDuration.time.minus(System.currentTimeMillis()) > 0L && coupon?.isMentorSpecificCoupon!=null) {
                startFreeTrialTimer(coupon.validDuration.time.minus(System.currentTimeMillis()), coupon, position)
            }else{
                changeTextColor(binding, coupon,position)
                if (coupon?.isMentorSpecificCoupon != null)
                    binding.couponExpireText.visibility = View.VISIBLE
                else
                    binding.couponExpireText.visibility = View.GONE
            }
        }

        fun startFreeTrialTimer(endTimeInMilliSeconds: Long, coupon: Coupon? ,position: Int) {
            try {
                var newTime = endTimeInMilliSeconds - 1000
                if (coupon?.isMentorSpecificCoupon!=null) {
                    binding.couponExpireText.text =
                        "Coupon will expire in " + UtilTime.timeFormatted(newTime)
                }else{
                    binding.couponExpireText.visibility = View.GONE
                }
                freeTrialTimerJob = scope.launch {
                    while (true) {
                        delay(1000)
                        newTime -= 1000
                        if (isActive) {
                            withContext(Dispatchers.Main) {
                                if (coupon?.isMentorSpecificCoupon!=null) {
                                    binding.couponExpireText.text =
                                        "Coupon will expire in " + UtilTime.timeFormatted(newTime)
                                }else{
                                    binding.couponExpireText.visibility = View.GONE
                                }
                            }
                            if (newTime <= 0) {
                                withContext(Dispatchers.Main) {
                                    changeTextColor(binding, coupon,position)
                                    binding.couponExpireText.visibility = View.VISIBLE
                                }
                                break
                            }
                        } else
                            break
                    }
                }
            } catch (ex: Exception) {
                Log.e("sagar", "startFreeTrialTimer: ${ex.message}")
            }
        }

        fun changeTextColor(binding: ItemOfffersCardBinding, coupon: Coupon?, position: Int){
            if (coupon?.isMentorSpecificCoupon!=null) {
                Log.e("sagar", "changeTextColor: " )
                if (coupon.isCouponSelected == 1)
                    coupon.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
                val grayColor =
                    ContextCompat.getColor(binding.couponExpireText.context, R.color.gray_8D)
                binding.rootCard.isEnabled = false
                binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                binding.couponExpireText.text = "Coupon expired"
                freeTrialTimerJob?.cancel()
                binding.couponExpireText.setTextColor(grayColor)
                binding.couponDiscountPercent.setTextColor(grayColor)
                binding.btnApply.isEnabled = false
                binding.btnApply.text = APPLY
                binding.imgLogo.alpha = 0.5f
                binding.couponDiscountPercent.setTextColor(grayColor)
                binding.btnApply.setTextColor(grayColor)
                binding.couponExpireText.visibility = View.VISIBLE
            }else{
                binding.couponExpireText.visibility = View.GONE
            }
        }
    }

    fun scroll(func :() -> Unit?) {
        scrollToFirst = func
    }
}