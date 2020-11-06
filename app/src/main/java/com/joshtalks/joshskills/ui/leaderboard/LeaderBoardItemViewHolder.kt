package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
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

@Layout(R.layout.list_item)
class LeaderBoardItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    var currentUser: Boolean = false
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
    lateinit var user_pic: CircleImageView

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        if (currentUser) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                container.setBackgroundColor(context.getColor(R.color.lightest_blue))
            }
        }
        rank.text = response.ranking.toString()
        name.text = response.name.toString()
        points.text = response.points.toString()
        response.photoUrl?.let {
            user_pic.setImage(it)
        }
    }

    @Click(R.id.user_pic)
    fun onClick() {
        response?.id?.let {
            RxBus2.publish(OpenUserProfile(it))
        }
    }
}
