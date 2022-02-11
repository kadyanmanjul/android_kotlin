package com.joshtalks.joshskills.ui.fpp.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat.setBackgroundTintList
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FppRecentItemListBinding
import com.joshtalks.joshskills.ui.fpp.HAS_RECIEVED_REQUEST
import com.joshtalks.joshskills.ui.fpp.IS_ALREADY_FPP
import com.joshtalks.joshskills.ui.fpp.REQUESTED
import com.joshtalks.joshskills.ui.fpp.SENT_REQUEST
import com.joshtalks.joshskills.ui.fpp.model.RecentCall
import kotlin.collections.ArrayList

class RecentCallsAdapter(private var lifecycleProvider: LifecycleOwner) :

    RecyclerView.Adapter<RecentCallsAdapter.RecentItemViewHolder>() {
    private var items: ArrayList<RecentCall> = arrayListOf()
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

    fun addItems(newList: ArrayList<RecentCall>) {
        if (newList.isEmpty()) {
            return
        }
        val diffCallback = RecentDiffCallback(items, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        items.clear()
        items.addAll(newList)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int = items.size


    override fun onBindViewHolder(holder: RecentCallsAdapter.RecentItemViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    fun getItemAtPosition(position: Int): RecentCall {
        return items[position]
    }

    inner class RecentItemViewHolder(val binding: FppRecentItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("UseCompatLoadingForColorStateLists")
        fun bind(recentCall: RecentCall, position: Int) {
            with(binding) {
                when (recentCall.fppRequestStatus) {
                    SENT_REQUEST -> {
                        btnSentRequest.setBackgroundColor(
                            ContextCompat.getColor(
                                AppObjectController.joshApplication,
                                R.color.colorAccent
                            )
                        )
                        btnSentRequest.text = context.resources.getText(R.string.sent_request)
                        btnSentRequest.setTextColor(ContextCompat.getColor(
                            AppObjectController.joshApplication,
                            R.color.white
                        ))
                    }
                    IS_ALREADY_FPP -> {
                        btnSentRequest.visibility = View.INVISIBLE
                    }
                    REQUESTED -> {
                        btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                        btnSentRequest.text = "Requested"
                    }
                    HAS_RECIEVED_REQUEST -> {
                        btnSentRequest.backgroundTintList = ContextCompat.getColorStateList(
                            AppObjectController.joshApplication,
                            R.color.not_now
                        )
                        btnSentRequest.text = "Respond"
                    }
                }
                obj = recentCall
                tvName.text = recentCall.firstName
                tvSpokenTime.text = spokenTimeText(recentCall.callDuration ?: 0)
            }
        }

        private fun spokenTimeText(minute: Int): String {
            val string = StringBuilder()
            string.append("Total time Spoken: $minute ")
            if (minute > 1) {
                string.append("minutes")
            } else {
                string.append("minute")
            }
            return string.toString()
        }
    }
}
