package com.joshtalks.joshskills.ui.fpp.adapters

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.SINGLE_SPACE
import com.joshtalks.joshskills.databinding.FppRecentItemListBinding
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class RecentCallsAdapter(
    var lifecycleProvider: LifecycleOwner,
    var callback: AdapterCallback,
    var activity: Activity,
    var conversationID: String?,
    var items: ArrayList<RecentCall>?
) :

    RecyclerView.Adapter<RecentCallsAdapter.RecentItemViewHolder>() {
    private val context = AppObjectController.joshApplication
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecentCallsAdapter.RecentItemViewHolder {
        val binding =
            FppRecentItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        binding.apply {
            lifecycleOwner = lifecycleProvider
        }
        return RecentItemViewHolder(binding)
    }

    fun updateList(list: ArrayList<RecentCall>, recyclerView: RecyclerView?, position: Int) {
        items = list
        notifyDataSetChanged()
        recyclerView?.smoothScrollToPosition(position)
    }

    override fun getItemCount(): Int = items?.size ?: 0


    override fun onBindViewHolder(holder: RecentCallsAdapter.RecentItemViewHolder, position: Int) {
        holder.setIsRecyclable(false)
        holder.bind(items?.get(position)!!, position)
    }

    fun getItemAtPosition(position: Int): RecentCall {
        return items?.get(position)!!
    }

    inner class RecentItemViewHolder(val binding: FppRecentItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForColorStateLists")
        fun bind(recentCall: RecentCall, position: Int) {
            binding.rootView.setCardBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.white
                )
            )
            initView(recentCall)
            addListener(recentCall)
        }

        fun initView(recentCall: RecentCall) {
            with(binding) {
                when (recentCall.fppRequestStatus) {
                    SENT_REQUEST -> {
                        btnSentRequest.visibility = View.VISIBLE
                        btnSentRequest.setBackgroundColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.colorAccent
                            )
                        )
                        btnSentRequest.text = context.resources.getText(R.string.send_request)
                        btnSentRequest.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.white
                            )
                        )
                    }
                    ALREADY_FPP -> {
                        btnSentRequest.visibility = View.INVISIBLE
                    }
                    REQUESTED -> {
                        btnSentRequest.visibility = View.VISIBLE
                        btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                        btnSentRequest.text = "Requested"
                        btnSentRequest.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.black_quiz
                            )
                        )
                    }
                    HAS_RECIEVED_REQUEST -> {
                        btnSentRequest.visibility = View.VISIBLE
                        btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                        btnSentRequest.text = "Respond"
                        btnSentRequest.setCompoundDrawablesWithIntrinsicBounds(
                            0,
                            0,
                            R.drawable.ic_reverse_polygon,
                            0
                        )
                        btnSentRequest.setTextColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.black_quiz
                            )
                        )
                    }
                    NONE -> {
                        btnSentRequest.visibility = View.INVISIBLE
                    }
                }
                obj = recentCall
                handler = this@RecentCallsAdapter
                tvSpokenTime.text = SINGLE_SPACE + recentCall.textToShow
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

        fun addListener(recentCall: RecentCall) {
            with(binding) {
                imgBlock.setOnClickListener {
                    callback.onUserBlock(recentCall.receiverMentorId, recentCall.firstName)
                }
                btnSentRequest.setOnClickListener {
                    when (recentCall.fppRequestStatus) {
                        SENT_REQUEST -> {
                            btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                                AppObjectController.joshApplication,
                                R.color.not_now
                            )
                            btnSentRequest.text = context.resources.getText(R.string.requested)
                            btnSentRequest.setTextColor(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.black_quiz
                                )
                            )
                            callback.onClickCallback(
                                recentCall.fppRequestStatus,
                                recentCall.receiverMentorId,
                                position,
                                null
                            )
                        }
                        REQUESTED -> {
                            btnSentRequest.setBackgroundColor(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.colorAccent
                                )
                            )
                            btnSentRequest.text = context.resources.getText(R.string.send_request)
                            btnSentRequest.setTextColor(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.white
                                )
                            )
                            callback.onClickCallback(
                                recentCall.fppRequestStatus,
                                recentCall.receiverMentorId,
                                position,
                                null,
                            )

                        }
                        HAS_RECIEVED_REQUEST -> {
                            callback.onClickCallback(
                                recentCall.fppRequestStatus,
                                recentCall.receiverMentorId,
                                position,
                                recentCall.firstName
                            )
                        }
                    }
                }

            }
        }
    }

    fun openUserProfileActivity(id: String) {
        UserProfileActivity.startUserProfileActivity(
            activity,
            id,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            null,
            RECENT_CALL,
            conversationId = conversationID
        )
    }
}
