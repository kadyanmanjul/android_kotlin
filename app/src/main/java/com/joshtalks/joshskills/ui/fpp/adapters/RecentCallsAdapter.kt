package com.joshtalks.joshskills.ui.fpp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FppRecentItemListBinding
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import kotlinx.android.synthetic.main.fpp_recent_item_list.view.btn_sent_request

class RecentCallsAdapter(var items: List<RecentCall> = listOf()) :
    RecyclerView.Adapter<RecentCallsAdapter.RecentItemViewHolder>() {

    private val context = AppObjectController.joshApplication
    var itemClick: ((RecentCall, Int,Int) -> Unit)? = null
    var isFreeTrial:Boolean = false

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentCallsAdapter.RecentItemViewHolder {
        val binding =
            FppRecentItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)

        return RecentItemViewHolder(binding)
    }

    fun updateList(list: ArrayList<RecentCall>) {
        items = list
        notifyDataSetChanged()
    }

    fun setListener(function: ((RecentCall, Int,Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: RecentCallsAdapter.RecentItemViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.bind(items[position],position)
    }

    fun getItemAtPosition(position: Int): RecentCall {
        return items[position]
    }

    inner class RecentItemViewHolder(val binding: FppRecentItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(recentCall: RecentCall,itemPosition: Int) {
            binding.itemData = recentCall
            binding.imgBlock.setOnClickListener {
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
                if (!isFreeTrial){
                    when (recentCall.fppRequestStatus) {
                        SENT_REQUEST -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.colorAccent,
                                R.color.white,
                                R.string.send_request
                            )
                        }
                        ALREADY_FPP -> {
                            btnSentRequest.visibility = View.INVISIBLE
                        }
                        REQUESTED -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.not_now,
                                R.color.black_quiz,
                                R.string.requested
                            )
                        }
                        HAS_RECIEVED_REQUEST -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.not_now,
                                R.color.black_quiz,
                                R.string.responsd
                            )
                            btnSentRequest.setCompoundDrawablesWithIntrinsicBounds(
                                0,
                                0,
                                R.drawable.ic_reverse_polygon,
                                0
                            )
                        }
                        NONE -> {
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

        fun addListener(recentCall: RecentCall,pos:Int) {
            with(binding) {
                btnSentRequest.setOnClickListener {
                    when (recentCall.fppRequestStatus) {
                        SENT_REQUEST -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.not_now,
                                R.color.black_quiz,
                                R.string.requested
                            )
                            itemClick?.invoke(recentCall, RECENT_CALL_SENT_REQUEST,pos)
                        }
                        REQUESTED -> {
                            setBtnVisibilityAndText(
                                btnSentRequest,
                                R.color.colorAccent,
                                R.color.white,
                                R.string.send_request
                            )
                            itemClick?.invoke(recentCall, RECENT_CALL_REQUESTED,pos)
                        }
                        HAS_RECIEVED_REQUEST -> {
                            itemClick?.invoke(recentCall, RECENT_CALL_HAS_RECIEVED_REQUESTED,pos)
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
    fun addRecentCallToList(members: List<RecentCall>,isFreeTrial:Boolean) {
        items = members
        this.isFreeTrial = isFreeTrial
        notifyDataSetChanged()
    }

}
