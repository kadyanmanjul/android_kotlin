package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.getRandomName
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.ListItemBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.ui.callWithExpert.utils.gone
import com.joshtalks.joshskills.ui.callWithExpert.utils.visible
import java.util.*

class LeaderBoardPreviousWinnerAdapter(
    private val context: Context,
    private val leaderBoardMentorList: List<LeaderboardMentor> = listOf(),
    var isHeader: Boolean = false
) : RecyclerView.Adapter<LeaderBoardPreviousWinnerAdapter.LeaderBoardPreviousWinnerViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderBoardPreviousWinnerViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LeaderBoardPreviousWinnerViewHolder(binding)
    }

    override fun getItemCount() = leaderBoardMentorList.size

    override fun onBindViewHolder(holder: LeaderBoardPreviousWinnerViewHolder, position: Int) {
        if (position == 0) {
            holder.binding.award.setImage(leaderBoardMentorList[position].award_url ?: EMPTY, context)
            holder.binding.award.visibility = View.VISIBLE
        }
        holder.setData (leaderBoardMentorList[position])
    }

    inner class LeaderBoardPreviousWinnerViewHolder(val binding: ListItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun setData(leaderBoardMentorList: LeaderboardMentor) {
            if (isHeader) {
                binding.rank.text = "Rank"
                TextViewCompat.setTextAppearance(binding.rank, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                TextViewCompat.setTextAppearance(binding.name, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                TextViewCompat.setTextAppearance(binding.points, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                binding.name.text = "Students"
                binding.points.text = "Points"
                binding.userPic.visibility = android.view.View.GONE
                binding.container.isClickable = false
                binding.container.isEnabled = false
                binding.horizontalLine.visible()
                binding.onlineStatusIv.visibility = android.view.View.GONE
            } else {
                TextViewCompat.setTextAppearance(binding.rank, R.style.TextAppearance_JoshTypography_BodySemiBold20)
                TextViewCompat.setTextAppearance(binding.name, R.style.TextAppearance_JoshTypography_SubHeadingSemiBold24)
                TextViewCompat.setTextAppearance(binding.points, R.style.TextAppearance_JoshTypography_SubHeadingSemiBold24)
                binding.horizontalLine.gone()
                binding.container.isClickable = true
                binding.container.isEnabled = true
                if (leaderBoardMentorList.id.equals(Mentor.getInstance().getId())) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        binding.container.setBackgroundColor(context.getColor(R.color.surface_information))
                    }
                } else {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        binding.container.setBackgroundColor(context.getColor(R.color.pure_white))
                    }
                }
                when (leaderBoardMentorList.ranking) {
                    1 -> {
                        binding.rankBadge.visible()
                        binding.rank.gone()
                        binding.rankBadge.setImageResource(R.drawable.first)
                    }
                    2 -> {
                        binding.rank.gone()
                        binding.rankBadge.visible()
                        binding.rankBadge.setImageResource(R.drawable.two)
                    }
                    3 -> {
                        binding.rank.gone()
                        binding.rankBadge.visible()
                        binding.rankBadge.setImageResource(R.drawable.three)
                    }
                    else -> {
                        binding.rankBadge.gone()
                        binding.rank.visible()
                    }
                }
                binding.rank.text = leaderBoardMentorList.ranking.toString()
                val resp = StringBuilder()
                leaderBoardMentorList.name?.split(" ")?.forEach {
                    resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                        .append(" ")
                }
                binding.name.text = resp
                binding.points.text = leaderBoardMentorList.points.toString()
                if (leaderBoardMentorList.isSeniorStudent) {
                    binding.imgSeniorStudentBadge.visibility = android.view.View.VISIBLE
                } else {
                    binding.imgSeniorStudentBadge.visibility = android.view.View.GONE
                }
                binding.userPic.setImageDrawable(null)
                binding.userPic.setUserImageOrInitials(
                    leaderBoardMentorList.photoUrl,
                    leaderBoardMentorList.name ?: getRandomName(),
                    isRound = true
                )
                binding.userPic.visibility = android.view.View.VISIBLE
                if (leaderBoardMentorList.isOnline) {
                    binding.onlineStatusIv.visibility = android.view.View.VISIBLE
                } else {
                    binding.onlineStatusIv.visibility = android.view.View.GONE
                }
            }

            binding.userPic.setOnClickListener {
                if (isHeader.not())
                    leaderBoardMentorList.id?.let {
                        RxBus2.publish(OpenUserProfile(it, leaderBoardMentorList.isOnline))
                    }
            }

            binding.container.setOnClickListener {
                if (isHeader.not())
                    leaderBoardMentorList.id?.let {
                        RxBus2.publish(OpenUserProfile(it, leaderBoardMentorList.isOnline))
                    }
            }
        }
    }
}