package com.joshtalks.badebhaiya

import android.app.PendingIntent.getActivity
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.LiSearchEventBinding
import com.joshtalks.badebhaiya.feed.model.SearchRoomsResponse
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.utils.setImage
import com.joshtalks.badebhaiya.utils.setUserInitialInRect


class SearchAdapter(private val searchResult: List<Users>): ListAdapter<SearchRoomsResponse, SearchAdapter.SearchViewHolder>(SearchAdapter){

    companion object DIFF_CALLBACK : DiffUtil.ItemCallback<SearchRoomsResponse>() {
        override fun areItemsTheSame(
            oldItem: SearchRoomsResponse,
            newItem: SearchRoomsResponse
        ): Boolean {
            return oldItem.users == newItem.users
        }

        override fun areContentsTheSame(
            oldItem: SearchRoomsResponse,
            newItem: SearchRoomsResponse
        ): Boolean {
            return oldItem == newItem
        }

    }

    inner class SearchViewHolder( var item:LiSearchEventBinding):
        RecyclerView.ViewHolder(item.root) {
            fun onBind(user:SearchRoomsResponse)
            {
                item.roomData=user.users
                val name =user.users.full_name
                val bio = user.users.bio
                var profilePic = user.users.profilePic
                item.ivProfilePic.setImage(profilePic)
                item.userName.setText(name)
                item.tvProfileBio.setText(bio)

            }

    }

    override fun getItemCount()=searchResult.size


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = DataBindingUtil.inflate<LiSearchEventBinding>(
            LayoutInflater.from(parent.context),
            R.layout.li_search_event,
            parent,
            false
        )
        return SearchViewHolder(view)
    }
    var message = Message()

    var singleLiveEvent: MutableLiveData<Message> = MutableLiveData()
    fun onProfileClicked() {
        message.what = OPEN_PROFILE
        message.data = Bundle().apply {
            putString(
                com.joshtalks.badebhaiya.feed.USER_ID,
                User.getInstance().userId
            )
        }
        singleLiveEvent.postValue(message)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {

        holder.item.tvProfileBio.text=searchResult[position].bio
        holder.item.userName.text=searchResult[position].full_name
        //holder.item.btnFollow.visibility=viewModel.isBadeBhaiyaSpeaker && !viewModel.isSelfProfile) ? View.VISIBLE : View.GONE
        //holder.item.btnFollow.visibility=(searchResult[position].is_speaker_followed)
        holder.item.btnFollow.setOnClickListener{
            showToast("followed")
        }
        holder.item.user.setOnClickListener{
            showToast("${searchResult[position].short_name}")
            //ProfileActivity.openProfileActivity(getActivity(), searchResult[position].user_id)
        }
        if(searchResult[position].profilePic.isNullOrEmpty().not())
        Glide.with(holder.itemView.getContext())
            .load(searchResult[position].profilePic)
            .into(holder.item.ivProfilePic)
        else
            holder.item.ivProfilePic.setUserInitialInRect(searchResult[position].short_name,24)


        
    }
}