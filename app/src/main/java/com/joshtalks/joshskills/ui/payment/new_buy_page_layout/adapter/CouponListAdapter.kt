package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemCouponCardBinding
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.special_practice.utils.APPLY
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_COUPON_APPLY

class CouponListAdapter(var offersList: List<Coupon>? = listOf()) :
    RecyclerView.Adapter<CouponListAdapter.CouponViewHolder>() {
    var itemClick: ((Coupon, Int, Int, String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CouponViewHolder {
        val binding = ItemCouponCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CouponViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CouponViewHolder, position: Int) {
        holder.setData(offersList?.get(position),position)
    }

    override fun getItemCount(): Int = offersList?.size ?: 0

    fun addOffersList(members: List<Coupon>?) {
        offersList = members
        notifyDataSetChanged()
    }

    fun setListener(function: ((Coupon, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class CouponViewHolder(private val binding: ItemCouponCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(members: Coupon?, position: Int) {
            with(binding) {
                this.txtCouponCode.text = members?.couponCode
                this.couponDesc.text =
                    "Use code " + "${members?.couponCode} " + "and get ${members?.amountPercent.toString()}% off on you purchase."
                this.saveMoney.text = "Save upto ₹" + members?.maxDiscountAmount.toString() + " with this code"

                this.btnApply.setOnSingleClickListener {
                    if (members != null) {
                        itemClick?.invoke(members, CLICK_ON_COUPON_APPLY, position, APPLY)
                    }
                }
            }
        }
    }
}