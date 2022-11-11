package com.joshtalks.joshskills.ui.fpp.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.databinding.FppRequestsListItemBinding
import com.joshtalks.joshskills.ui.fpp.constants.CONFIRM_REQUEST_TYPE
import com.joshtalks.joshskills.ui.fpp.constants.FPP_OPEN_USER_PROFILE
import com.joshtalks.joshskills.ui.fpp.constants.NOT_NOW_REQUEST_TYPE
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.inbox.mentor_id

class SeeAllRequestsAdapter(var items: List<PendingRequestDetail> = listOf()) :
    RecyclerView.Adapter<SeeAllRequestsAdapter.ViewHolder>() {
    var itemClick: ((PendingRequestDetail, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<FppRequestsListItemBinding>(
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

    fun setListener(function: ((PendingRequestDetail, Int) -> Unit)?) {
        itemClick = function
    }

    fun addSeeAllRequestToList(members: List<PendingRequestDetail>) {
        items = members
        notifyDataSetChanged()
    }

    inner class ViewHolder(private val binding: FppRequestsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pendingRequestDetail: PendingRequestDetail) {
            binding.itemData = pendingRequestDetail
            binding.btnConfirmRequest.setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUEST_CONFIRM)
                    .addParam(ParamKeys.MENTOR_ID, pendingRequestDetail.senderMentorId)
                    .addParam(ParamKeys.VIA,"fav request")
                    .push()
                itemClick?.invoke(pendingRequestDetail, CONFIRM_REQUEST_TYPE)
                changeVisibility(false)
            }
            binding.rootView.setOnClickListener {
                itemClick?.invoke(pendingRequestDetail, FPP_OPEN_USER_PROFILE)
            }
            binding.btnNotNow.setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.FPP_REQUEST_NOT_NOW)
                    .addParam(ParamKeys.MENTOR_ID, pendingRequestDetail.senderMentorId)
                    .addParam(ParamKeys.VIA,"fav request")
                    .push()
                itemClick?.invoke(pendingRequestDetail, NOT_NOW_REQUEST_TYPE)
                changeVisibility(false)
            }
        }

        private fun changeVisibility(isVisible: Boolean) {
            if (!isVisible)
                binding.afterAccepted.visibility = View.GONE
            binding.groupItemContainer.setBackgroundResource(R.color.surface_warning)
        }
    }
}