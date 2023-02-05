package com.joshtalks.joshskills.premium.ui.leaderboard

import android.content.Context
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.getRandomName
import com.joshtalks.joshskills.premium.core.setImage
import com.joshtalks.joshskills.premium.core.setUserImageOrInitials
import com.joshtalks.joshskills.premium.messaging.RxBus2
import com.joshtalks.joshskills.premium.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.premium.repository.local.model.Mentor
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.premium.ui.callWithExpert.utils.visible
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

    @View(R.id.horizontal_line)
    lateinit var horizontalLine: LinearLayout

    @View(R.id.img_senior_student_badge)
    lateinit var imgSeniorStudentBadge: AppCompatImageView

    @View(R.id.rankBadge)
    lateinit var rankBadge: ImageView

    lateinit var linearLayoutManager: SmoothLinearLayoutManager

    @Resolve
    fun onViewInflated() {
        if (isHeader) {
            rank.text = "Rank"
            TextViewCompat.setTextAppearance(rank, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
            TextViewCompat.setTextAppearance(name, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
            TextViewCompat.setTextAppearance(points, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
            name.text = "Students"
            points.text = "Points"
            user_pic.visibility = android.view.View.GONE
            container.isClickable = false
            container.isEnabled = false
            horizontalLine.visible()
            onlineStatusLayout.visibility = android.view.View.GONE
        } else {
            TextViewCompat.setTextAppearance(rank, R.style.TextAppearance_JoshTypography_BodySemiBold20)
            TextViewCompat.setTextAppearance(name, R.style.TextAppearance_JoshTypography_SubHeadingSemiBold24)
            TextViewCompat.setTextAppearance(points, R.style.TextAppearance_JoshTypography_SubHeadingSemiBold24)
            horizontalLine.gone()
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
            if (response.ranking == 1) {
                rankBadge.visible()
                rank.gone()
                rankBadge.setImageResource(R.drawable.first)
            } else if (response.ranking == 2) {
                rank.gone()
                rankBadge.visible()
                rankBadge.setImageResource(R.drawable.two)
            } else if (response.ranking == 3) {
                rank.gone()
                rankBadge.visible()
                rankBadge.setImageResource(R.drawable.three)
            } else {
                rankBadge.gone()
                rank.visible()
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

    @Click(R.id.user_pic)
    fun onClick() {
        if (currentUser && User.getInstance().isVerified.not()) {
            //return
        }
        if (isHeader.not())
            response.id?.let {
                RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }

    @Click(R.id.container)
    fun onContainerClick() {
        if (currentUser && User.getInstance().isVerified.not()) {
            //return
        }
        if (isHeader.not())
            response.id?.let {
                RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
            }
    }
}
