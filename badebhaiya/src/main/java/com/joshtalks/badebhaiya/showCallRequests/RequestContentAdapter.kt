package com.joshtalks.badebhaiya.showCallRequests

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.badebhaiya.databinding.ItemRequestContentBinding
import com.joshtalks.badebhaiya.showCallRequests.model.ReqeustData
import com.joshtalks.badebhaiya.showCallRequests.model.RequestData
import com.joshtalks.badebhaiya.showCallRequests.model.User
import com.joshtalks.badebhaiya.showCallRequests.model.UserX
import com.joshtalks.badebhaiya.utils.Utils
import com.joshtalks.badebhaiya.utils.setUserImageOrInitials
import timber.log.Timber

class RequestContentAdapter(
    private val requestList: List<ReqeustData>,
    private val callRequestUser: UserX,
): RecyclerView.Adapter<RequestContentAdapter.RequestViewHolder>()  {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val binding = ItemRequestContentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RequestViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(requestList[position])
    }

    override fun getItemCount(): Int = requestList.size

    inner class RequestViewHolder(private val binding: ItemRequestContentBinding): RecyclerView.ViewHolder(binding.root){
        fun bind(item: ReqeustData){
            binding.requestContent = item
            Timber.d("REQUEST CONTENT ITEM => $item")
            binding.requestUser = callRequestUser
            if (callRequestUser.photo_url.isNullOrEmpty().not())
                Utils.setImage(binding.requestContentDp, callRequestUser.photo_url)
            else
                Utils.setImage(binding.requestContentDp, callRequestUser.short_name)
            binding.requestContentDp.setUserImageOrInitials(callRequestUser.photo_url,
                callRequestUser.short_name[0],30)
        }
    }
}