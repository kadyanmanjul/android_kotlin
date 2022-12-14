package com.joshtalks.joshskills.leaderboard

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenPreviousLeaderboard
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Resolve
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

class LeaderBoardWinnerItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    val type: String,
    val onViewInflated: ViewInflated?
) {

    
    lateinit var award: ImageView

    
    lateinit var title: AppCompatTextView

    
    lateinit var container: ConstraintLayout

    
    lateinit var name: AppCompatTextView

    
    lateinit var points: AppCompatTextView

    
    lateinit var userPic: CircleImageView

    
    lateinit var onlineStatusLayout: FrameLayout

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        title.text = response.title.toString()
        val resp = StringBuilder()
        response.name?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        name.text = resp
        points.text = (response.points.toString()).plus(" points")
        userPic.post {
            userPic.setUserImageOrInitials(response.photoUrl, response.name?:"User")
        }
        response.award_url?.let {
            award.setImage(it)
        }
        if (response.isOnline != null && response.isOnline!!) {
            onlineStatusLayout.visibility = android.view.View.VISIBLE
        } else {
            onlineStatusLayout.visibility = android.view.View.GONE
        }

        onViewInflated?.onViewInflated(response)
    }

    
    fun onClick() {
        /*response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }*/
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenPreviousLeaderboard(type))
    }

    
    fun onSecondClick() {
        /*response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }*/
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenPreviousLeaderboard(type))
    }

    
    fun onSecondClickContainer() {
        /*response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }*/
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenPreviousLeaderboard(type))
    }
}

interface ViewInflated {
    fun onViewInflated(response: LeaderboardMentor)
}