package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.databinding.ItemNewPriceCardBinding
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_PRICE_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE

class PriceListAdapter(var priceList: List<CourseDetailsList>? = listOf()) :
    RecyclerView.Adapter<PriceListAdapter.PriceListViewHolder>() {
    var itemClick: ((CourseDetailsList, Int, Int, String) -> Unit)? = null
    var prevHolder: PriceListViewHolder? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceListViewHolder {
        val binding = ItemNewPriceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PriceListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceListViewHolder, position: Int) {
        holder.setData(priceList?.get(position), position)

        Log.d("PriceListAdapter.kt", "SAGAR => onBindViewHolder:29 ${prevHolder?.layoutPosition} ${holder.layoutPosition}}")
        if (priceList?.get(position) != null && priceList?.get(position)!!.isRecommended == true && (prevHolder == holder || prevHolder == null)) {
            itemClick?.invoke(priceList?.get(position)!!, CLICK_ON_PRICE_CARD, position, REMOVE)
            prevHolder = holder
            holder.binding.priceCardView.strokeColor  = AppObjectController.joshApplication.resources.getColor(R.color.colorAccent)
            holder.binding.checkIcon.setBackgroundResource(R.drawable.ic_radio_button_checked)
            holder.binding.checkIcon.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.colorAccent)
            holder.binding.priceRootView.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.selected_color_trans))
        }
        holder.binding.priceCardView.setOnClickListener {
            if (priceList?.get(position) != null) {
                itemClick?.invoke(priceList?.get(position)!!, CLICK_ON_PRICE_CARD, position, REMOVE)

                holder.binding.priceCardView.strokeColor = AppObjectController.joshApplication.resources.getColor(R.color.colorAccent)
                holder.binding.checkIcon.setBackgroundResource(R.drawable.ic_radio_button_checked)
                holder.binding.checkIcon.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.colorAccent)
                holder.binding.priceRootView.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.selected_color_trans))

                if (prevHolder != null && prevHolder != holder) {
                    prevHolder?.binding?.priceCardView?.strokeColor = AppObjectController.joshApplication.resources.getColor(R.color.price_card_stroke)

                    prevHolder?.binding?.checkIcon?.setBackgroundResource(R.drawable.ic_radio_button_unchecked)

                    prevHolder?.binding?.checkIcon?.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.price_card_stroke)

                    prevHolder?.binding?.priceRootView?.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.white))

                }
                prevHolder = holder
            }
        }
    }

    override fun getItemCount(): Int = priceList?.size ?: 0

    fun addPriceList(members: List<CourseDetailsList>?) {
        Log.d("PriceListAdapter.kt", "SAGAR => addPriceList:60 ")
        priceList = members
        notifyDataSetChanged()
    }

    fun setListener(function: ((CourseDetailsList, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class PriceListViewHolder(val binding: ItemNewPriceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(priceList: CourseDetailsList?, position: Int) {
            binding.itemData = priceList
            binding.discountPrice.text = priceList?.actualAmount + " "
            binding.discountPrice.paintFlags = binding.discountPrice.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
            binding.originalPrice.text = priceList?.discountedPrice + " "
            binding.pricePerDay.text = priceList?.perDayPrice +" "
            if (priceList?.couponText != EMPTY) {
                binding.discountTxt.visibility = View.VISIBLE
            }
            else {
                binding.discountTxt.visibility = View.GONE
            }
            binding.executePendingBindings()
        }
    }
}