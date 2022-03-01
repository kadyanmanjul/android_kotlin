package com.joshtalks.joshskills.ui.fpp.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.FppRequestsListItemBinding
import com.joshtalks.joshskills.ui.fpp.constants.ALL_REQUESTS
import com.joshtalks.joshskills.ui.fpp.constants.IS_ACCEPTED
import com.joshtalks.joshskills.ui.fpp.constants.IS_REJECTED
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class SeeAllRequestsAdapter(
    private val items: List<PendingRequestDetail>,var callback: AdapterCallback,var activity:Activity
) : RecyclerView.Adapter<SeeAllRequestsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view= DataBindingUtil.inflate<FppRequestsListItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fpp_requests_list_item,
            parent,
            false
        )
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position],position)
    }

    override fun getItemCount(): Int = items.size
    inner class ViewHolder(val binding: FppRequestsListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pendingRequestDetail: PendingRequestDetail,position: Int) {
            binding.itemData=pendingRequestDetail
            binding.rootView.setOnClickListener{
                pendingRequestDetail.senderMentorId?.let { it1 ->
                    openUserProfileActivity(
                        it1,
                        ALL_REQUESTS
                    )
                }
            }
            binding.rootView.setCardBackgroundColor(
                ContextCompat.getColor(
                    activity,
                    R.color.white
                )
            )
            binding.btnConfirmRequest.setOnClickListener{
                binding.btnNotNow.visibility=GONE
                binding.btnConfirmRequest.visibility=GONE
                binding.tvSpokenTime.text="You are now favorite practice partners"
                binding.groupItemContainer.setBackgroundColor(ContextCompat.getColor(activity, R.color.request_respond));
                callback.onClickCallback(
                    IS_ACCEPTED,
                    pendingRequestDetail.senderMentorId,
                    position,
                    null
                )
            }
            binding.btnNotNow.setOnClickListener{
                binding.btnNotNow.visibility=GONE
                binding.btnConfirmRequest.visibility=GONE
                binding.tvSpokenTime.text="Request Removed"
                binding.groupItemContainer.setBackgroundColor(ContextCompat.getColor(activity, R.color.request_respond));
                callback.onClickCallback(
                    IS_REJECTED,
                    pendingRequestDetail.senderMentorId,
                    position,
                    null
                )
            }

        }
    }
    private fun openUserProfileActivity(id: String, previousPage: String?) {
        previousPage?.let {
            UserProfileActivity.startUserProfileActivity(
                activity ,
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                null,
                it,
                conversationId = null
            )
        }
    }
}