package com.joshtalks.joshskills.leaderboard.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.setUserImageOrInitials
import com.joshtalks.joshskills.common.databinding.ListItemBinding
import com.joshtalks.joshskills.common.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.common.repository.local.model.Mentor
import com.joshtalks.joshskills.leaderboard.LeaderboardMentor
import java.util.Locale

class LeaderboardSearchItemAdapter(val context: Context, val itemList: List<LeaderboardMentor>) :
    RecyclerView.Adapter<LeaderboardSearchItemAdapter.LeaderViewHolder>() {

    inner class LeaderViewHolder(val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(response: LeaderboardMentor) {

            binding.container.setOnClickListener {
                response.id?.let {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
                }
            }
            binding.userPic.setOnClickListener {
                response.id?.let {
                    com.joshtalks.joshskills.common.messaging.RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
                }
            }

            binding.container.isClickable = true
            binding.container.isEnabled = true
            if (response.isSeniorStudent) {
                binding.imgSeniorStudentBadge.visibility = View.VISIBLE
            } else {
                binding.imgSeniorStudentBadge.visibility = View.GONE
            }
            if (response.id.equals(Mentor.getInstance().getId())) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    binding.container.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.surface_information
                        )
                    )
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    binding.container.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.pure_white
                        )
                    )
                }
            }
            binding.rank.text = response.ranking.toString()
            val resp = StringBuilder()
            response.name?.split(" ")?.forEach {
                resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                    .append(" ")
            }
            binding.name.text = resp
            binding.points.text = response.points.toString()
            binding.userPic.setUserImageOrInitials(
                response.photoUrl,
                response.name ?: "User",
                16,
                true
            )

            if (response.isOnline != null && response.isOnline) {
                binding.onlineStatusIv.visibility = android.view.View.VISIBLE
            } else {
                binding.onlineStatusIv.visibility = android.view.View.GONE
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeaderViewHolder {
        return LeaderViewHolder(
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.list_item,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LeaderViewHolder, position: Int) {
        holder.bind(itemList[position])
    }

    override fun getItemCount(): Int {
        return itemList.size
    }
}