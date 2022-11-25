package com.joshtalks.joshskills.common.ui.fpp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.ui.fpp.constants.*
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.common.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.common.core.PrefManager
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.databinding.FppRecentItemListBinding
import com.joshtalks.joshskills.common.ui.fpp.constants.*
import com.joshtalks.joshskills.common.ui.fpp.model.RecentCall
import kotlinx.android.synthetic.main.fpp_recent_item_list.view.btn_sent_request

class RecentCallsAdapter(var items: List<RecentCall> = listOf()) :
    RecyclerView.Adapter<RecentCallsAdapter.RecentItemViewHolder>() {

    private val context = AppObjectController.joshApplication
    var itemClick: ((RecentCall, Int, Int) -> Unit)? = null
    var isFreeTrial:Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentItemViewHolder {
        val binding =
            FppRecentItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return RecentItemViewHolder(binding)
    }

    fun updateList(list: ArrayList<RecentCall>) {
        items = list
        notifyDataSetChanged()
    }

    fun setListener(function: ((RecentCall, Int, Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: RecentItemViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.bind(items[position],position)
    }

    fun getItemAtPosition(position: Int): RecentCall {
        return items[position]
    }

    inner class RecentItemViewHolder(val binding: FppRecentItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recentCall: RecentCall, itemPosition: Int) {
            binding.itemData = recentCall
            binding.imgBlock.setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.BLOCK_USER_CLICKED)
                    .addParam(ParamKeys.MENTOR_ID, recentCall.receiverMentorId)
                    .push()
                itemClick?.invoke(recentCall, RECENT_CALL_USER_BLOCK,itemPosition)
            }

            binding.rootView.setOnClickListener {
                itemClick?.invoke(recentCall, RECENT_OPEN_USER_PROFILE,itemPosition)
            }
            initView(recentCall)
            addListener(recentCall,itemPosition)
        }

        fun initView(recentCall: RecentCall) {
            with(binding) {
                if (!isFreeTrial) {
                    when (recentCall.fppRequestStatus) {
                        com.joshtalks.joshskills.common.ui.fpp.constants.SENT_REQUEST -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.primary_500,
                                R.color.pure_white,
                                R.string.send_request
                            )
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.ALREADY_FPP -> {
                            btnSentRequest.visibility = View.INVISIBLE
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.REQUESTED -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.disabled,
                                R.color.pure_black,
                                R.string.requested
                            )
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.HAS_RECIEVED_REQUEST -> {
                            MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUEST_RESPOND)
                                .addParam(ParamKeys.MENTOR_ID, recentCall.receiverMentorId)
                                .push()
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.disabled,
                                R.color.pure_black,
                                R.string.responsd
                            )
                            btnSentRequest.setCompoundDrawablesWithIntrinsicBounds(
                                0,
                                0,
                                R.drawable.ic_reverse_polygon,
                                0
                            )
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.NONE -> {
                            btnSentRequest.visibility = View.INVISIBLE
                        }
                    }
                }
                if (recentCall.callType == "incoming") {
                    tvSpokenTime.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_incoming_call,
                        0,
                        0,
                        0
                    )
                } else {
                    tvSpokenTime.setCompoundDrawablesWithIntrinsicBounds(
                        R.drawable.ic_outgoing_call,
                        0,
                        0,
                        0
                    )
                }
            }
        }

        fun addListener(recentCall: RecentCall, pos:Int) {
            with(binding) {
                btnSentRequest.setOnClickListener {
                    when (recentCall.fppRequestStatus) {
                        com.joshtalks.joshskills.common.ui.fpp.constants.SENT_REQUEST -> {
                            MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUEST_SEND)
                                .addParam(ParamKeys.MENTOR_ID, recentCall.receiverMentorId)
                                .addParam(ParamKeys.VIA,"recent call")
                                .push()
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.disabled,
                                R.color.pure_black,
                                R.string.requested
                            )
                            itemClick?.invoke(recentCall,
                                com.joshtalks.joshskills.common.ui.fpp.constants.RECENT_CALL_SENT_REQUEST,pos)
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.REQUESTED -> {
                            MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUEST_CANCEL)
                                .addParam(ParamKeys.MENTOR_ID, recentCall.receiverMentorId)
                                .addParam(ParamKeys.VIA,"recent call")
                                .push()
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.primary_500,
                                R.color.pure_white,
                                R.string.send_request
                            )
                            itemClick?.invoke(recentCall,
                                com.joshtalks.joshskills.common.ui.fpp.constants.RECENT_CALL_REQUESTED,pos)
                        }
                        com.joshtalks.joshskills.common.ui.fpp.constants.HAS_RECIEVED_REQUEST -> {
                            itemClick?.invoke(recentCall,
                                com.joshtalks.joshskills.common.ui.fpp.constants.RECENT_CALL_HAS_RECIEVED_REQUESTED,pos)
                        }
                    }
                }
            }
        }

        fun setBtnVisibilityAndText(
            view: View,
            backgroundColor: Int,
            textColor: Int,
            textData: Int
        ) {
            view.visibility = View.VISIBLE
            view.backgroundTintList = ContextCompat.getColorStateList(
                AppObjectController.joshApplication,
                backgroundColor
            )
            view.btn_sent_request.text = context.resources.getText(textData)
            view.btn_sent_request.setTextColor(
                ContextCompat.getColor(
                    AppObjectController.joshApplication,
                    textColor
                )
            )
        }
    }
    fun addRecentCallToList(members: List<RecentCall>, isFreeTrial:Boolean) {
        items = members
        this.isFreeTrial = isFreeTrial
        notifyDataSetChanged()
    }

}
