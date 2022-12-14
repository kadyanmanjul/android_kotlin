package com.joshtalks.joshskills.leaderboard

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.common.core.setImage
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenUserProfile
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Resolve
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

class LeaderBoardPreviousWinnerItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    val awardUrl: String
) {

    
    lateinit var rank: AppCompatTextView

    
    lateinit var container: ConstraintLayout

    
    lateinit var name: AppCompatTextView

    
    lateinit var points: AppCompatTextView

    
    lateinit var user_pic: CircleImageView

    
    lateinit var awardIV: AppCompatImageView

    
    lateinit var onlineStatusLayout: FrameLayout

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        container.isClickable = true
        container.isEnabled = true
        rank.text = response.ranking.toString()
        val resp = StringBuilder()
        response.name?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        name.text = resp
        points.text = response.points.toString()
        user_pic.post {
            user_pic.setUserImageOrInitials(response.photoUrl, response.name!!)
            user_pic.visibility = android.view.View.VISIBLE
        }
        if (response.isOnline != null && response.isOnline!!) {
            onlineStatusLayout.visibility = android.view.View.VISIBLE
        } else {
            onlineStatusLayout.visibility = android.view.View.GONE
        }
        awardIV.setImage(awardUrl, context)
    }

    
    fun onClick() {
        response.id?.let {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it,response.isOnline?:false))
        }
    }

    
    fun onContainerClick() {
        response.id?.let {
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it,response.isOnline?:false))
        }
    }
}
