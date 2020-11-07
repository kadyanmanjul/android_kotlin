package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView

@Layout(R.layout.winner_list_item)
class LeaderBoardWinnerItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context
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

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        title.text = response.title.toString()
        name.text = response.name.toString()
        points.text = (response.points.toString()).plus(" points")
        response.photoUrl?.let {
            userPic.setImage(it)
        }
        response.award_url?.let {
            award.setImage(it)
        }
    }

    @Click(R.id.user_pic)
    fun onClick() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
    }

    @Click(R.id.view_profile)
    fun onSecondClick() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
    }
}
