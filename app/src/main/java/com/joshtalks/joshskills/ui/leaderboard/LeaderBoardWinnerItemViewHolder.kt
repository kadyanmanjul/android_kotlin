package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

@Layout(R.layout.winner_list_item)
class LeaderBoardWinnerItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    val type: String
) {

    @View(R.id.award)
    lateinit var award: ImageView

    @View(R.id.title)
    lateinit var title: AppCompatTextView

    @View(R.id.container)
    lateinit var container: ConstraintLayout

    @View(R.id.name)
    lateinit var name: AppCompatTextView

    @View(R.id.points)
    lateinit var points: AppCompatTextView

    @View(R.id.user_pic)
    lateinit var userPic: CircleImageView

    @View(R.id.online_status_iv)
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
            userPic.setUserImageOrInitials(response.photoUrl, response.name!!)
        }
        response.award_url?.let {
            award.setImage(it)
        }
        if (response.isOnline != null && response.isOnline!!) {
            onlineStatusLayout.visibility = android.view.View.VISIBLE
        } else {
            onlineStatusLayout.visibility = android.view.View.GONE
        }

    }

    @Click(R.id.user_pic)
    fun onClick() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
        //RxBus2.publish(OpenPreviousLeaderboard(type))
    }

    @Click(R.id.view_profile)
    fun onSecondClick() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
        //RxBus2.publish(OpenPreviousLeaderboard(type))
    }

    @Click(R.id.container)
    fun onSecondClickContainer() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
        //RxBus2.publish(OpenPreviousLeaderboard(type))
    }
}
