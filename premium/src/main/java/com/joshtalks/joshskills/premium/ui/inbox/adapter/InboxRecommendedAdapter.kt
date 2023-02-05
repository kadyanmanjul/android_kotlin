package com.joshtalks.joshskills.premium.ui.inbox.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.premium.core.AppObjectController
import com.joshtalks.joshskills.premium.databinding.InboxItemLayoutForRecommendedCourseBinding
import com.joshtalks.joshskills.premium.ui.special_practice.utils.CLICK_ON_RECOMMENDED_COURSE

class InboxRecommendedAdapter :
    RecyclerView.Adapter<InboxRecommendedAdapter.RecommendedCourseListViewHolder>() {
    var itemClick: ((InboxRecommendedCourse, Int, Int) -> Unit)? = null
    val courseList: ArrayList<InboxRecommendedCourse> = ArrayList()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecommendedCourseListViewHolder {
        val binding = InboxItemLayoutForRecommendedCourseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecommendedCourseListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecommendedCourseListViewHolder, position: Int) {
        try {
            val course = courseList[position]
            holder.setData(course)
            holder.binding.rootView.setOnClickListener {
                itemClick?.invoke(course, CLICK_ON_RECOMMENDED_COURSE, position)
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }


    override fun getItemCount(): Int {
        return if (courseList.size>=3)
            3
        else
            courseList.size
    }

    fun addRecommendedCourseList(recommendedCourseList: ArrayList<InboxRecommendedCourse>?) {
        courseList.clear()
        courseList.addAll(ArrayList(recommendedCourseList))
        this.notifyDataSetChanged()
    }

    fun setListener(function: ((InboxRecommendedCourse, Int, Int) -> Unit)?) {
        itemClick = function
    }

    inner class RecommendedCourseListViewHolder(val binding: InboxItemLayoutForRecommendedCourseBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun setData(courseList: InboxRecommendedCourse?) {
            binding.obj = courseList
            if (courseList?.id == 10) {
                binding.horizontalLine11.visibility = View.VISIBLE

                binding.tvSubHeading.text = "35,000 + students enrolled"
                TextViewCompat.setTextAppearance(binding.tvSubHeading, R.style.TextAppearance_JoshTypography_CaptionSemiBold)
                binding.tvSubHeading.setTextColor(AppObjectController.joshApplication.resources.getColor(R.color.success))
                binding.tvSubHeading.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_tranding,0,0,0)
            }
            else {
                binding.horizontalLine11.visibility = View.GONE
                binding.tvSubHeading.setCompoundDrawablesWithIntrinsicBounds(0,0,0,0)
            }
            binding.executePendingBindings()
        }
    }
}