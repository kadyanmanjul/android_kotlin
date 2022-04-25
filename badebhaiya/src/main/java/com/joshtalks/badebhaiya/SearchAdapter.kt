package com.joshtalks.badebhaiya

import android.app.PendingIntent.getActivity
import android.content.Context
import android.os.Bundle
import android.os.Message
import android.provider.Settings.Global.getString
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.os.persistableBundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.badebhaiya.core.showToast
import com.joshtalks.badebhaiya.databinding.ActivityProfileBinding
import com.joshtalks.badebhaiya.databinding.LiSearchEventBinding
import com.joshtalks.badebhaiya.feed.model.SearchRoomsResponse
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.profile.ProfileActivity
import com.joshtalks.badebhaiya.profile.ProfileViewModel
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.utils.setImage
import com.joshtalks.badebhaiya.utils.setUserInitialInRect
import kotlinx.android.synthetic.main.activity_profile.view.*
import kotlinx.android.synthetic.main.activity_profile.view.btnFollow
import kotlinx.android.synthetic.main.li_search_event.*
import kotlinx.android.synthetic.main.li_search_event.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.security.AccessController.getContext


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

    val speakerFollowed = MutableLiveData(false)

    inner class SearchViewHolder(var item: LiSearchEventBinding) :
        RecyclerView.ViewHolder(item.root) {
        fun onBind(user: SearchRoomsResponse) {
            item.roomData = user.users
            val name = user.users.full_name
            val bio = user.users.bio
            var profilePic = user.users.profilePic
            item.ivProfilePic.setImage(profilePic)
            item.userName.setText(name)
            item.tvProfileBio.setText(bio)

        }

    }

    override fun getItemCount(): Int = searchResult?.size?:0


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val view = DataBindingUtil.inflate<LiSearchEventBinding>(
            LayoutInflater.from(parent.context),
            R.layout.li_search_event,
            parent,
            false
        )
//        view.btnFollow.setOnClickListener{
//            showToast("hum bhi bana lenge ")
//        }
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

//     fun addList(user:List<Users>){
//        searchResult=user
//         notifyDataSetChanged()
//
//    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {

        searchResult.let {
                searchResult->
            holder.item.tvProfileBio.text = searchResult[position].bio
            holder.item.userName.text = searchResult[position].full_name
            //holder.item.btnFollow.visibility=viewModel.isBadeBhaiyaSpeaker && !viewModel.isSelfProfile) ? View.VISIBLE : View.GONE
            //holder.item.btnFollow.visibility=(searchResult[position].is_speaker_followed)
    //            holder.item.btnFollow.setOnClickListener {
    //                showToast("followed")
    //            }
            holder.item.user.setOnClickListener {
                ProfileActivity.openProfileActivity(
                    holder.item.user.context,
                    searchResult[position].user_id
                )
            }

            if (searchResult[position].profilePic.isNullOrEmpty().not())
                Glide.with(holder.itemView.getContext())
                    .load(searchResult[position].profilePic)
                    .into(holder.item.ivProfilePic)
            else
                holder.item.ivProfilePic.setUserInitialInRect(searchResult[position].short_name, 24)

            if (searchResult[position].is_speaker_followed) {
                holder.item.user.apply {
                    holder.item.user.btnFollow.text = "Following"
                    btnFollow.setTextColor(resources.getColor(R.color.white))
                    btnFollow.background = AppCompatResources.getDrawable(
                        holder.item.user.btnFollow.context,
                        R.drawable.following_button_background
                    )
                }
            } else
                holder.item.user.apply {
                    holder.item.user.btnFollow.text = "Follow"
                    btnFollow.setTextColor(resources.getColor(R.color.follow_button_stroke))
                    btnFollow.background = AppCompatResources.getDrawable(
                        holder.item.user.btnFollow.context,
                        R.drawable.follow_button_background
                    )
                }

            holder.item.btnFollow.setOnClickListener {
                speakerFollowed.value=searchResult[position].is_speaker_followed
                speakerFollowed.value?.let {
                    GlobalScope.launch {
                        if (it.not()) {
                            try {
                                val followRequest =
                                    FollowRequest(searchResult[position].user_id, User.getInstance().userId)
                                val response =
                                    RetrofitInstance.profileNetworkService.updateFollowStatus(followRequest)
                                if (response.isSuccessful) {
                                    speakerFollowed.value = true
                                }
                            }catch (ex:Exception){

                            }

                        } else {
                            GlobalScope.launch {
                                try {
                                    val followRequest =
                                        FollowRequest(
                                            searchResult[position].user_id,
                                            User.getInstance().userId
                                        )
                                    val response = RetrofitInstance.profileNetworkService.updateUnfollowStatus(followRequest)
                                    if (response.isSuccessful) {
                                        speakerFollowed.value = false
                                    }
                                } catch (ex: Exception) {

                                }

                            }
                        }
                    }
                }
            }
        }


    }
}