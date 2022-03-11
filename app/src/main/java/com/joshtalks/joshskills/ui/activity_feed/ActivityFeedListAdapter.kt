package com.joshtalks.joshskills.ui.activity_feed

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.databinding.ActivityFeedRowItemBinding
import com.joshtalks.joshskills.repository.server.ProfilePicture
import com.joshtalks.joshskills.ui.activity_feed.model.ActivityFeedResponseFirebase
import com.joshtalks.joshskills.ui.userprofile.PreviousPicsAdapter
import com.joshtalks.joshskills.ui.userprofile.ProfileImageShowFragment
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import java.util.*

class ActivityFeedListAdapter(
    private val items: ArrayList<ActivityFeedResponseFirebase>,
    val context:Context,
    val activity: Activity
) : RecyclerView.Adapter<ActivityFeedListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view=DataBindingUtil.inflate<ActivityFeedRowItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.activity_feed_row_item,
            parent,
            false
        )
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
    inner class ViewHolder(val view:ActivityFeedRowItemBinding) : RecyclerView.ViewHolder(view.root) {
        fun bind(activityFeedResponse: ActivityFeedResponseFirebase) {
            view.itemData=activityFeedResponse
            if(activityFeedResponse.eventId=="5"){
                view.updatedPic.visibility=VISIBLE
                view.updatedPicBorder.visibility= VISIBLE
            }else{
                view.updatedPic.visibility=GONE
                view.updatedPicBorder.visibility= GONE
            }
            view.rootView.setOnClickListener{
                activityFeedResponse.mentorId?.let { it1 -> openUserProfileActivity(it1, "RECENT_CALL") }
            }
            view.updatedPic.setOnClickListener{
                activityFeedResponse.mentorId?.let { it1 ->
                    ProfileImageShowFragment.newInstance(activityFeedResponse.mediaUrl, null, null,
                        it1,false)
                        .show((context as ActivityFeedMainActivity).supportFragmentManager.beginTransaction(), "ImageShow")
                }
            }
        }
    }
    private fun openUserProfileActivity(id: String, previousPage: String?) {
        previousPage?.let {
            UserProfileActivity.startUserProfileActivity(
                activity,
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                null,
                it,
                conversationId = null
            )
        }
    }
}