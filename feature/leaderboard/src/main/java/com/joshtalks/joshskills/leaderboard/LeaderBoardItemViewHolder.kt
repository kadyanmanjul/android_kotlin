package com.joshtalks.joshskills.leaderboard

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.getRandomName
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.common.repository.local.model.User
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Resolve
import java.util.Locale

class LeaderBoardItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    var currentUser: Boolean = response.id.equals(Mentor.getInstance().getId()),
    var isHeader: Boolean = false
) {

    
    lateinit var rank: AppCompatTextView

    
    lateinit var container: ConstraintLayout

    
    lateinit var name: AppCompatTextView

    
    lateinit var points: AppCompatTextView

    
    lateinit var user_pic: AppCompatImageView

    
    lateinit var onlineStatusLayout: FrameLayout

    
    lateinit var imgSeniorStudentBadge: AppCompatImageView

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        if (isHeader) {
            rank.text = "Rank"
            name.text = "Students"
            points.text = "Points"
            user_pic.visibility = android.view.View.GONE
            container.isClickable = false
            container.isEnabled = false
            onlineStatusLayout.visibility = android.view.View.GONE
        } else {
            container.isClickable = true
            container.isEnabled = true
            if (currentUser) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    container.setBackgroundColor(context.getColor(R.color.surface_information))
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    container.setBackgroundColor(context.getColor(R.color.pure_white))
                }
            }
            rank.text = response.ranking.toString()
            val resp = StringBuilder()
            response.name?.split(" ")?.forEach {
                resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                    .append(" ")
            }
            name.text = resp
            points.text = response.points.toString()
            if (response.isSeniorStudent) {
                imgSeniorStudentBadge.visibility = android.view.View.VISIBLE
            } else {
                imgSeniorStudentBadge.visibility = android.view.View.GONE
            }
            user_pic.setImageDrawable(null)
            user_pic.setUserImageOrInitials(
                response.photoUrl,
                response.name ?: getRandomName(),
                isRound = true
            )
            user_pic.visibility = android.view.View.VISIBLE
            if (response.isOnline != null && response.isOnline!!) {
                onlineStatusLayout.visibility = android.view.View.VISIBLE
            } else {
                onlineStatusLayout.visibility = android.view.View.GONE
            }
        }
    }

    
    fun onClick() {
        if (currentUser && User.getInstance().isVerified.not()) {
            //return
        }
        if (isHeader.not())
            response.id?.let {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }

    
    fun onContainerClick() {
        if (currentUser && User.getInstance().isVerified.not()) {
            //return
        }
        if (isHeader.not())
            response.id?.let {
                com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }
}
