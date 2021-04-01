package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.getRandomName
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import java.util.Locale

@Layout(R.layout.list_item)
class LeaderBoardItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    var currentUser: Boolean = response.id.equals(Mentor.getInstance().getId()),
    var isHeader: Boolean = false
) {

    @View(R.id.rank)
    lateinit var rank: AppCompatTextView

    @View(R.id.container)
    lateinit var container: ConstraintLayout

    @View(R.id.name)
    lateinit var name: AppCompatTextView

    @View(R.id.points)
    lateinit var points: AppCompatTextView

    @View(R.id.user_pic)
    lateinit var user_pic: AppCompatImageView

    @View(R.id.online_status_iv)
    lateinit var onlineStatusLayout: FrameLayout

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
                    container.setBackgroundColor(context.getColor(R.color.lightest_blue))
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    container.setBackgroundColor(context.getColor(R.color.white))
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
            user_pic.setUserImageOrInitials(response.photoUrl, response.name ?: getRandomName(), isRound = true)
            user_pic.visibility = android.view.View.VISIBLE
            if (response.isOnline != null && response.isOnline!!) {
                onlineStatusLayout.visibility = android.view.View.VISIBLE
            } else {
                onlineStatusLayout.visibility = android.view.View.GONE
            }
        }
    }

    @Click(R.id.user_pic)
    fun onClick() {
        if (isHeader.not())
            response.id?.let {
                RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }

    @Click(R.id.container)
    fun onContainerClick() {
        if (isHeader.not())
            response.id?.let {
                RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }
}
