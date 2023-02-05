package com.joshtalks.joshskills.premium.ui.leaderboard

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.setImage
import com.joshtalks.joshskills.premium.core.setUserImageOrInitials
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.premium.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.visible
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

@Layout(R.layout.leaderboard_pervious_winner_list_item)
class LeaderBoardPreviousWinnerItemViewHolder(
    var response: LeaderboardMentor,
    var context: Context,
    val awardUrl: String
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

    @View(R.id.award)
    lateinit var awardIV: AppCompatImageView

    @View(R.id.rankBadge)
    lateinit var rankBadge: ImageView

    @View(R.id.online_status_iv)
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

        // TODO: put rank
        when (response.ranking) {
            1 -> {
                rankBadge.visible()
                rank.gone()
                rankBadge.setImageResource(R.drawable.first)
            }
            2 -> {
                rank.gone()
                rankBadge.visible()
                rankBadge.setImageResource(R.drawable.two)
            }
            3 -> {
                rank.gone()
                rankBadge.visible()
                rankBadge.setImageResource(R.drawable.three)
            }
            else -> {
                rankBadge.gone()
                rank.visible()
            }
        }

    }

    @Click(R.id.user_pic)
    fun onClick() {
        response.id?.let {
            RxBus2.publish(OpenUserProfile(it,response.isOnline?:false))
        }
    }

    @Click(R.id.container)
    fun onContainerClick() {
        response.id?.let {
            RxBus2.publish(OpenUserProfile(it,response.isOnline?:false))
        }
    }
}
