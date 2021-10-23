package com.joshtalks.joshskills.ui.leaderboard.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.DEFAULT_NAME
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.ListItemBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import java.util.Locale

class LeaderboardSearchItemAdapter(val context: Context, val itemList: List<LeaderboardMentor>) :
    RecyclerView.Adapter<LeaderboardSearchItemAdapter.LeaderViewHolder>() {

    inner class LeaderViewHolder(val binding: ListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(response: LeaderboardMentor) {

            binding.container.setOnClickListener {
                response.id?.let {
                    RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
                }
            }
            binding.userPic.setOnClickListener {
                response.id?.let {
                    RxBus2.publish(OpenUserProfile(it, response.isOnline ?: false))
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
                            R.color.lightest_blue
                        )
                    )
                }
            } else {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    binding.container.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.white
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
                response.name ?: DEFAULT_NAME,
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