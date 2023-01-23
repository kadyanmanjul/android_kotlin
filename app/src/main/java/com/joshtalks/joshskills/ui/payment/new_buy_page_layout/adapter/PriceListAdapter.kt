package com.joshtalks.joshskills.ui.payment.new_buy_page_layout.adapter

import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.ItemNewPriceCardBinding
import com.joshtalks.joshskills.ui.callWithExpert.utils.toRupeesWithoutSpace
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_PRICE_CARD
import com.joshtalks.joshskills.ui.special_practice.utils.REMOVE
import java.util.*

class PriceListAdapter(var priceList: List<CourseDetailsList>? = listOf()) :
    RecyclerView.Adapter<PriceListAdapter.PriceListViewHolder>() {
    var itemClick: ((CourseDetailsList, Int, Int, String) -> Unit)? = null
    var prevHolder: PriceListViewHolder? = null
    var expireAt: Date?=null
    var couponType: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PriceListViewHolder {
        val binding = ItemNewPriceCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PriceListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PriceListViewHolder, position: Int) {
        holder.setData(priceList?.get(position))

        Log.d("PriceListAdapter.kt", "SAGAR => onBindViewHolder:29 ${prevHolder?.layoutPosition} ${holder.layoutPosition}}")
        if (priceList?.get(position) != null && priceList?.get(position)!!.isRecommended == true && (prevHolder == holder || prevHolder == null)) {
            itemClick?.invoke(priceList?.get(position)!!, CLICK_ON_PRICE_CARD, position, REMOVE)
            prevHolder = holder
            holder.binding.priceCardView.strokeColor  = AppObjectController.joshApplication.resources.getColor(R.color.primary_500)
            holder.binding.checkIcon.setBackgroundResource(R.drawable.ic_radio_button_checked)
            holder.binding.checkIcon.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.primary_500)
            holder.binding.priceRootView.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.surface_information))
        }else{
            if (prevHolder != null && prevHolder == holder) {
                itemClick?.invoke(priceList?.get(position)!!, CLICK_ON_PRICE_CARD, position, REMOVE)
            }
        }
        holder.binding.priceCardView.setOnClickListener {
            if (priceList?.get(position) != null) {
                itemClick?.invoke(priceList?.get(position)!!, CLICK_ON_PRICE_CARD, position, REMOVE)

                holder.binding.priceCardView.strokeColor = AppObjectController.joshApplication.resources.getColor(R.color.primary_500)
                holder.binding.checkIcon.setBackgroundResource(R.drawable.ic_radio_button_checked)
                holder.binding.checkIcon.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.primary_500)
                holder.binding.priceRootView.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.surface_information))

                if (prevHolder != null && prevHolder != holder) {
                    prevHolder?.binding?.priceCardView?.strokeColor = AppObjectController.joshApplication.resources.getColor(R.color.disabled)

                    prevHolder?.binding?.checkIcon?.setBackgroundResource(R.drawable.ic_radio_button_unchecked)

                    prevHolder?.binding?.checkIcon?.backgroundTintList = ContextCompat.getColorStateList(AppObjectController.joshApplication, R.color.disabled)

                    prevHolder?.binding?.priceRootView?.background =  ColorDrawable(ContextCompat.getColor(AppObjectController.joshApplication, R.color.pure_white))

                }
                prevHolder = holder
            }
        }
    }

    override fun getItemCount(): Int = priceList?.size ?: 0

    fun addPriceList(members: List<CourseDetailsList>?, validDuration:Date?, type:String?) {
        Log.d("PriceListAdapter.kt", "SAGAR => addPriceList:60 ")
        priceList = members
        expireAt = validDuration
        couponType = type
        notifyDataSetChanged()
    }

    fun setListener(function: ((CourseDetailsList, Int, Int, String) -> Unit)?) {
        itemClick = function
    }

    inner class PriceListViewHolder(val binding: ItemNewPriceCardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        var countdownTimerBack: CountDownTimer? = null
        var courseDescListCard: View? = null

        fun setData(priceList: CourseDetailsList?) {
            binding.itemData = priceList
            binding.discountPrice.text = "MRP: ${priceList?.actualAmount.toString().toRupeesWithoutSpace()} "
            binding.discountPrice.paintFlags = binding.discountPrice.paintFlags.or(Paint.STRIKE_THRU_TEXT_FLAG)
            binding.originalPrice.text = priceList?.discountedPrice.toString().toRupeesWithoutSpace()
            if (priceList?.perDayPrice != null)
                binding.pricePerDay.text = priceList.perDayPrice + " "

            val savings = (priceList?.actualAmount)?.minus((priceList.discountedPrice) ?: 0)

            if (savings != null) {
                val percent = (savings / ((priceList.actualAmount).toDouble()) * 100).toInt()
                binding.couponSavePercent.text = "Save $percent%"
            }

            if (priceList?.subText != null) {
                binding.priceDescList.visibility = View.VISIBLE
                binding.priceDescList.removeAllViews()
                priceList.subText?.forEach { it ->
                    val view = getCourseDescriptionList(it)
                    if (view != null) {
                        binding.priceDescList.addView(view)
                    }
                }
            }
            binding.executePendingBindings()
            if (couponType.equals(EXPIRABLE) && (expireAt?.time?.minus(System.currentTimeMillis())?:0) > 0L){
                startFreeTrialTimer(expireAt?.time?.minus(System.currentTimeMillis())?: 0)
            }else{
                binding.discountTxt.visibility = View.GONE
            }
        }
        private fun getCourseDescriptionList(featureText: String): View? {
            val courseDescListInflate: LayoutInflater = AppObjectController.joshApplication.getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            courseDescListCard = courseDescListInflate.inflate(R.layout.layout_expert_feature, null, true)
            val courseDesc = courseDescListCard?.findViewById<TextView>(R.id.feature_text)
            courseDesc?.compoundDrawablesRelative?.get(0)?.setTint(AppObjectController.joshApplication.resources.getColor(R.color.primary_500))
            courseDesc?.text = HtmlCompat.fromHtml(featureText, HtmlCompat.FROM_HTML_MODE_LEGACY)
            return courseDescListCard
        }

        fun startFreeTrialTimer(endTimeInMilliSeconds: Long) {
            try {
                if (countdownTimerBack!=null)
                    countdownTimerBack?.cancel()

                countdownTimerBack = object : CountDownTimer(endTimeInMilliSeconds, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        if (countdownTimerBack != null) {
                            AppObjectController.uiHandler.post {
                                if (couponType.equals(EXPIRABLE)) {
                                    binding.discountTxt.visibility = View.VISIBLE
                                    binding.discountTxt.text =
                                        "Offer ends in " + UtilTime.timeFormatted(millisUntilFinished) + ". Hurry up!"
                                }else{
                                    binding.discountTxt.visibility = View.GONE
                                }
                            }
                        }
                    }

                    override fun onFinish() {
                        AppObjectController.uiHandler.post {
                            binding.discountTxt.visibility = View.GONE
                        }
                    }
                }
                countdownTimerBack?.start()
            } catch (ex: Exception) {
                Log.e("sagar", "startFreeTrialTimer: ${ex.message}")
            }
        }
    }
}