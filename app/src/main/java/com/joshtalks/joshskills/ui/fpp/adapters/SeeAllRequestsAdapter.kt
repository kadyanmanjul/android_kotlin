package com.joshtalks.joshskills.ui.fpp.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.databinding.FppRequestsListItemBinding
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.fpp.ISACCEPTED
import com.joshtalks.joshskills.ui.fpp.ISREJECTED
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import java.util.ArrayList

class SeeAllRequestsAdapter(
    private val items: List<PendingRequestDetail>,var callback: AdapterCallback
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
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    inner class ViewHolder(val binding: FppRequestsListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(pendingRequestDetail: PendingRequestDetail) {
            binding.itemData=pendingRequestDetail
            binding.btnConfirmRequest.setOnClickListener{
                callback.onClickCallback(ISACCEPTED,pendingRequestDetail.senderMentorId)
            }
            binding.btnNotNow.setOnClickListener{
                callback.onClickCallback(ISREJECTED,pendingRequestDetail.senderMentorId)
            }

        }
    }
}