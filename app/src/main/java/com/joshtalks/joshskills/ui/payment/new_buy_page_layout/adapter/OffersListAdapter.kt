package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ItemOfffersCardBinding
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.ListOfCoupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_OFFER_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE

class OffersListAdapter(var offersList: MutableList<ListOfCoupon>? = mutableListOf()) :
    RecyclerView.Adapter<OffersListAdapter.OfferListViewHolder>() {
    var itemClick: ((ListOfCoupon, Int, Int, String) -> Unit)? = null
    var prevHolder: OffersListAdapter.OfferListViewHolder? = null
    var holder: OfferListViewHolder? = null
    var listSize = 0
    var manager: RecyclerView.LayoutManager? = null
    var isAnySelected = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferListViewHolder {
        val binding = ItemOfffersCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferListViewHolder, position: Int) {
        holder.setData()
        this.holder = holder
        holder.binding.couponDiscountPercent.text = "Get " + offersList?.get(position)?.amountPercent.toString() + "% Off"

        if (offersList?.get(position)?.isCouponSelected == 1) {
            isAnySelected = true
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            holder.binding.btnApply.text = REMOVE
            offersList?.get(position)?.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        }

        holder.binding.btnApply.setOnClickListener {
            if (isAnySelected && position != 0) {
                offersList?.get(0)?.isCouponSelected = 0
                notifyItemChanged(0)
                itemSortWhenNothingSelected(holder, position)
            } else if (isAnySelected && position == 0) {
                if (holder.binding.btnApply.text == REMOVE) {
                    isAnySelected = false
                    offersList?.get(0)?.isCouponSelected = 0
                    notifyItemChanged(position)
                    holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
                    holder.binding.btnApply.text = APPLY
                    offersList?.get(position)?.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
                } else {
                    offersList?.get(0)?.isCouponSelected = 1
                    offersList?.sortByDescending { it.isCouponSelected }
                    notifyItemChanged(0)
                }
            } else {
                offersList?.get(0)?.isCouponSelected = 0
                notifyItemChanged(0)
                itemSortWhenNothingSelected(holder, position)
            }
        }
    }

    fun itemSortWhenNothingSelected(holder: OfferListViewHolder, position: Int) {
        if (holder.binding.btnApply.text == REMOVE) {
            isAnySelected = false
            offersList?.get(position)?.isCouponSelected = 0
            notifyItemChanged(position)
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            holder.binding.btnApply.text = APPLY
            offersList?.get(position)?.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
        } else {
            offersList?.get(position)?.isCouponSelected = 1
            offersList?.sortByDescending { it.isCouponSelected }
            notifyItemRangeChanged(0, itemCount)
            manager?.scrollToPosition(0)
        }
    }

    override fun getItemCount(): Int = offersList?.size ?: 0

    fun addOffersList(members: MutableList<ListOfCoupon>?) {
        listSize = members?.size ?: 0
        offersList = members
        notifyDataSetChanged()
    }

    fun setListener(function: ((ListOfCoupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class OfferListViewHolder(val binding: ItemOfffersCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData() {
            binding.executePendingBindings()
        }
    }

    fun setBackgroundUI(view: View?, position: Int, couponList:List<ListOfCoupon>?) {
        val rootCardView = view?.findViewById<ConstraintLayout>(R.id.root_card)
        val buttonText = view?.findViewById<TextView>(R.id.btn_apply)
        buttonText?.text = REMOVE
        rootCardView?.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
        if (couponList != null) {
            offersList?.addAll(couponList)
            offersList?.get(0)?.isCouponSelected = 0
            notifyItemChanged(0)
            offersList?.get(position)?.isCouponSelected = 1
            offersList?.sortByDescending { it.isCouponSelected }
            notifyItemRangeChanged(0, itemCount)
            manager?.scrollToPosition(0)
            rootCardView?.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            buttonText?.text = APPLY
            Log.d("OffersListAdapter.kt", "SAGAR => setBackgroundUI:114 ${offersList}")
        }
    }

    fun setLayoutManager(layoutManager: RecyclerView.LayoutManager?) {
        this.manager = layoutManager
    }
}