package com.joshtalks.joshskills.userprofile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.core.setPreviousProfileImage
import com.joshtalks.joshskills.common.ui.userprofile.models.ProfilePicture
import com.joshtalks.joshskills.userprofile.R

class PreviousPicsAdapter(
    private val items: List<ProfilePicture> = emptyList(),
    private val onPreviousPicClickListener: OnPreviousPicClickListener,
    private val mentorId: String
) : RecyclerView.Adapter<PreviousPicsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.previous_pic_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position],position)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var imgPreviousPic: AppCompatImageView = view.findViewById(R.id.previous_p)
        var previousPicShimmer: LottieAnimationView =view.findViewById<LottieAnimationView>(R.id.previous_pic_shimmer_layout)
        var profilePicture: ProfilePicture? = null
        fun bind(profilePicture: ProfilePicture,position: Int) {
            this.profilePicture = profilePicture
            imgPreviousPic.setPreviousProfileImage(profilePicture.photoUrl, view.context,previousPicShimmer)
            view.setOnClickListener {
                MixPanelTracker.publishEvent(MixPanelEvent.VIEW_PHOTO)
                    .addParam(ParamKeys.MENTOR_ID,mentorId)
                    .push()
                onPreviousPicClickListener.onPreviousPicClick(profilePicture,position)
            }
        }

    }
    interface OnPreviousPicClickListener {
        fun onPreviousPicClick(profilePicture: ProfilePicture,position: Int)
    }

}