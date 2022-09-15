package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ItemOfffersCardBinding
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_OFFER_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE

class OffersListAdapter(val offersList: MutableList<Coupon> = mutableListOf()) :
    RecyclerView.Adapter<OffersListAdapter.OfferListViewHolder>() {
    private val TAG = "OffersListAdapter"
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null
    var prevHolder: OfferListViewHolder? = null
    var scrollToFirst : (() -> Unit?)? = null
    var manager: RecyclerView.LayoutManager? = null
    var isAnySelected = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferListViewHolder {
        val binding = ItemOfffersCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferListViewHolder, position: Int) {
        holder.setData()
        holder.binding.couponDiscountPercent.text = "Get " + offersList[position].amountPercent.toString() + "% Off"

        if (offersList[position].isCouponSelected == 1) {
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            holder.binding.btnApply.text = REMOVE
            offersList[position]
                .let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        } else {
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            holder.binding.btnApply.text = APPLY
        }

        holder.binding.btnApply.setOnClickListener {
            if(offersList[holder.bindingAdapterPosition].isCouponSelected == 0) {
                offersList[0].isCouponSelected = 0
                offersList[holder.bindingAdapterPosition].isCouponSelected = 1
                val selectedItem = offersList.get(holder.bindingAdapterPosition)
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
        val couponIndex = offersList.indexOf(coupon)
        val item = offersList.getOrNull(couponIndex)
        offersList[0].isCouponSelected = 0
        notifyItemChanged(0)
        if(item == null) {
            coupon.isCouponSelected = 1
            offersList.add(0, coupon)
            notifyItemInserted(0)
            scrollToFirst?.invoke()
        } else {
            item.isCouponSelected = 1
            offersList.remove(item)
            offersList.add(0, item)
            notifyItemRemoved(couponIndex)
            notifyItemInserted(0)
            scrollToFirst?.invoke()
        }
    }

    override fun getItemCount(): Int = offersList.size

    fun addOffersList(members: MutableList<Coupon>) {
        offersList.clear()
        offersList.addAll(members)
        notifyDataSetChanged()
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    class OfferListViewHolder(val binding: ItemOfffersCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData() {
            binding.executePendingBindings()
        }
    }

    fun scroll(func :() -> Unit?) {
        scrollToFirst = func
    }
}