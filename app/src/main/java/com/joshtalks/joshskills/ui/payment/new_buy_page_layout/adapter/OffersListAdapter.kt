package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

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

class OffersListAdapter(var offersList: List<ListOfCoupon>? = listOf()) :
    RecyclerView.Adapter<OffersListAdapter.OfferListViewHolder>() {
    var itemClick: ((ListOfCoupon, Int, Int, String) -> Unit)? = null
    var prevHolder: OffersListAdapter.OfferListViewHolder? = null
    var holder: OfferListViewHolder? = null
    var listSize =0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OfferListViewHolder {
        val binding = ItemOfffersCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OfferListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OfferListViewHolder, position: Int) {
        holder.setData()
        this.holder = holder
        holder.binding.couponDiscountPercent.text = "Get " + offersList?.get(position)?.amountPercent.toString() + "% Off"

        holder.binding.btnApply.setOnClickListener {
            if (offersList?.get(position) != null) {
                if (offersList?.size!! > 1) {
                    couponClickListener(holder, position)
                } else {
                    setBackgroundColorAndText(holder, position)
                }
            }
        }
    }

    private fun couponClickListener(holder: OfferListViewHolder, position: Int) {
        if (prevHolder != null && prevHolder != holder) {
            prevHolder?.binding?.rootCard?.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            if (prevHolder != holder)
                prevHolder?.binding?.btnApply?.text = APPLY
        } else {
            prevHolder?.binding?.rootCard?.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            if (prevHolder != holder)
                prevHolder?.binding?.btnApply?.text = REMOVE
        }

        setBackgroundColorAndText(holder, position)
        prevHolder = holder
    }

    fun setBackgroundColorAndText(holder: OfferListViewHolder, position: Int) {
        if (holder.binding.btnApply.text == REMOVE) {
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_gary)
            holder.binding.btnApply.text = APPLY
            offersList?.get(position)?.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, REMOVE) }
        } else {
            holder.binding.rootCard.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
            holder.binding.btnApply.text = REMOVE
            offersList?.get(position)?.let { itemClick?.invoke(it, CLICK_ON_OFFER_CARD, position, APPLY) }
        }
    }

    override fun getItemCount(): Int = offersList?.size ?: 0

    fun addOffersList(members: List<ListOfCoupon>?) {
        listSize = members?.size?:0
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

    fun setBackgroundUI(view:View?,position: Int){
        val rootCardView = view?.findViewById<ConstraintLayout>(R.id.root_card)
        val buttonText = view?.findViewById<TextView>(R.id.btn_apply)
        buttonText?.text = REMOVE
        rootCardView?.setBackgroundResource(R.drawable.ic_coupon_card_bg_green)
        notifyItemChanged(position)
    }
}