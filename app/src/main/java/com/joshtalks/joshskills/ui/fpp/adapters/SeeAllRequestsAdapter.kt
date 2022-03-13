package com.joshtalks.joshskills.ui.fpp.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.FppRequestsListItemBinding
import com.joshtalks.joshskills.ui.fpp.constants.*
import com.joshtalks.joshskills.ui.fpp.model.PendingRequestDetail
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity

class SeeAllRequestsAdapter(
    private val items: List<PendingRequestDetail>,
    var callback: AdapterCallback,
    var activity: Activity,
    var lifecycleProvider: LifecycleOwner
) : RecyclerView.Adapter<SeeAllRequestsAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<FppRequestsListItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.fpp_requests_list_item,
            parent,
            false
        )
        view.apply {
            lifecycleOwner = lifecycleProvider
        }

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val binding: FppRequestsListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pendingRequestDetail: PendingRequestDetail, position: Int) {
            binding.handler = this@SeeAllRequestsAdapter
            binding.itemData = pendingRequestDetail
            binding.rootView.setCardBackgroundColor(
                ContextCompat.getColor(
                    activity,
                    R.color.white
                )
            )
            listeners( pendingRequestDetail, position)
        }

        fun listeners(
            pendingRequestDetail: PendingRequestDetail,
            position: Int
        ) {
            with(binding) {
                btnConfirmRequest.setOnClickListener {
                    btnNotNow.visibility = GONE
                    btnConfirmRequest.visibility = GONE
                    tvSpokenTime.text = AppObjectController.joshApplication.getString(R.string.now_fpp)
                    groupItemContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.request_respond
                        )
                    );
                    callback.onClickCallback(
                        IS_ACCEPTED,
                        pendingRequestDetail.senderMentorId,
                        position,
                        null
                    )
                }
                btnNotNow.setOnClickListener {
                    btnNotNow.visibility = GONE
                    btnConfirmRequest.visibility = GONE
                    tvSpokenTime.text =
                        AppObjectController.joshApplication.getString(R.string.request_removed)
                    groupItemContainer.setBackgroundColor(
                        ContextCompat.getColor(
                            activity,
                            R.color.request_respond
                        )
                    )
                    callback.onClickCallback(
                        IS_REJECTED,
                        pendingRequestDetail.senderMentorId,
                        position,
                        null
                    )
                }

            }

        }
    }

    fun openUserProfileActivity(id: String?) {
        if (id != null) {
            UserProfileActivity.startUserProfileActivity(
                activity,
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                null,
                REQUESTS_SCREEN,
                conversationId = null
            )
        }

    }
}