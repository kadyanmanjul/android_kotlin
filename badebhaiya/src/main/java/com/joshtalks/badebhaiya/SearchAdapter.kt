package com.joshtalks.badebhaiya

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.joshtalks.badebhaiya.databinding.LiSearchEventBinding
import com.joshtalks.badebhaiya.feed.Call
import com.joshtalks.badebhaiya.feed.model.SearchRoomsResponse
import com.joshtalks.badebhaiya.feed.model.Users
import com.joshtalks.badebhaiya.impressions.Impression
import com.joshtalks.badebhaiya.liveroom.OPEN_PROFILE
import com.joshtalks.badebhaiya.profile.request.FollowRequest
import com.joshtalks.badebhaiya.repository.model.User
import com.joshtalks.badebhaiya.repository.service.RetrofitInstance
import com.joshtalks.badebhaiya.utils.setUserInitialInRect
import kotlinx.android.synthetic.main.li_search_event.*
import kotlinx.android.synthetic.main.li_search_event.view.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class SearchAdapter(private val searchResult: List<Users>,var call: Call): ListAdapter<SearchRoomsResponse, SearchAdapter.SearchViewHolder>(SearchAdapter){

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

    //var speakerFollowed = false

    inner class SearchViewHolder(var item: LiSearchEventBinding) :
        RecyclerView.ViewHolder(item.root) {

    }

    override fun getItemCount(): Int = searchResult?.size?:0


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

//     fun addList(user:List<Users>){
//        searchResult=user
//         notifyDataSetChanged()
//
//    }

    @SuppressLint("ResourceAsColor")
    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
//        if(searchResult.isNullOrEmpty())
//        {
//            SearchFragment().displayNull()
//            //holder.noresult.visibility= View.VISIBLE
//        }
        searchResult.let {
                searchResult->
            holder.item.tvProfileBio.text = searchResult[position].bio
            holder.item.userName.text = searchResult[position].full_name
            holder.item.user.setOnClickListener {
                com.joshtalks.badebhaiya.utils.hideKeyboard(holder.item.user.context)
                call.itemClick(searchResult[position].user_id)
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
                    holder.item.user.btnFollow.textSize=12F
                    //holder.item.user.btnFollow.setTextAppearance(R.style.BB_Typography_Nunito_Semi_Bold)
                    btnFollow.setTextColor(resources.getColor(R.color.white))
                    btnFollow.background = AppCompatResources.getDrawable(
                        holder.item.user.btnFollow.context,
                        R.drawable.following_button_background
                    )
                }
            } else
                holder.item.user.apply {
                    holder.item.user.btnFollow.text = "Follow"
                    holder.item.user.btnFollow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                    //holder.item.user.btnFollow.setTextAppearance(R.style.BB_Typography_Nunito_Bold)
                    //holder.item.user.btnFollow.setTextAppearance(R.style.BB_Typography_Nunito_Sans_Bold)
                    btnFollow.setTextColor(resources.getColor(R.color.follow_button_stroke))
                    btnFollow.background = AppCompatResources.getDrawable(
                        holder.item.user.btnFollow.context,
                        R.drawable.follow_button_background
                    )
                }
            holder.item.btnFollow.setOnClickListener {
                    GlobalScope.launch {
                        if (searchResult[position].is_speaker_followed.not()) {
                            try {
                                val followRequest =
                                    FollowRequest(
                                        searchResult[position].user_id,
                                        User.getInstance().userId,
                                        false,
                                        false,
                                        "SEARCH_FRAGMENT"
                                    )
                                try {
//                                    RetrofitInstance.profileNetworkService.sendEvent(Impression("SEARCH_FRAGMENT","CLICKED_FOLLOW"))
                                } catch (e: Exception){

                                }
                                val response =
                                    RetrofitInstance.profileNetworkService.updateFollowStatus(followRequest)
                                if (response.isSuccessful) {
                                    searchResult[position].is_speaker_followed=true
                                    //notifyDataSetChanged()
                                }
                            }catch (ex:Exception){

                            }
                        } else {
                                try {
                                    val followRequest =
                                        FollowRequest(
                                            searchResult[position].user_id,
                                            User.getInstance().userId,
                                            false,
                                            false,
                                            "SEARCH_FRAGMENT"
                                        )

                                    try {
                                        RetrofitInstance.profileNetworkService.sendEvent(Impression("SEARCH_FRAGMENT","CLICKED_UNFOLLOW"))
                                    } catch (e: Exception){

                                    }

                                    val response = RetrofitInstance.profileNetworkService.updateUnfollowStatus(followRequest)
                                    if (response.isSuccessful) {
                                        searchResult[position].is_speaker_followed=false
                                        //notifyDataSetChanged()
                                    }
                                } catch (ex: Exception) {

                                }
                        }
                    }
                if (searchResult[position].is_speaker_followed.not()==true) {
                    holder.item.user.apply {
                        holder.item.user.btnFollow.setText("Following")
                        holder.item.user.btnFollow.textSize=12F
                        holder.item.user.btnFollow.setTextColor(resources.getColor(R.color.white))
                        //holder.item.user.btnFollow.setTextAppearance(R.style.BB_Typography_Nunito_Semi_Bold)
                        holder.item.user.btnFollow.background = AppCompatResources.getDrawable(
                            holder.item.user.btnFollow.context,
                            R.drawable.following_button_background
                        )
                    }
                }
                else {
                    holder.item.user.apply {
                        holder.item.user.btnFollow.setText("Follow")
                        //holder.item.user.btnFollow.textSize=11F
                        holder.item.user.btnFollow.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14F)
                        //holder.item.user.btnFollow.setTextAppearance(R.style.BB_Typography_Nunito_Bold)
                        holder.item.user.btnFollow.setTextColor(resources.getColor(R.color.follow_button_stroke))
                        //holder.item.user.btnFollow.setBackgroundDrawable(R.drawable.follow_button_background)
                        holder.item.user.btnFollow.background = AppCompatResources.getDrawable(
                            holder.item.user.btnFollow.context,
                            R.drawable.follow_button_background
                        )
                    }
                }

            }
        }
    }
}