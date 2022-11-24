package com.joshtalks.joshskills.ui.inbox.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.databinding.InboxItemLayoutForRecommendedCourseBinding
import com.joshtalks.joshskills.ui.inbox.adapter.InboxBindingAdapter.imageUrl
import com.joshtalks.joshskills.ui.special_practice.utils.CLICK_ON_RECOMMENDED_COURSE

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
        Log.e("sagar", "onBindViewHolder: ", )
        holder.setData(courseList[position])
        holder.binding.rootView.setOnClickListener {
            itemClick?.invoke(courseList[position], CLICK_ON_RECOMMENDED_COURSE, position)
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
            imageUrl(binding.profileImage, courseList?.courseIcon)
            with(binding) {
                this.tvCourseName.text = courseList?.courseName
            }
            if (courseList?.id == 10) {
                binding.horizontalLine11.visibility = View.VISIBLE
                binding.isBestCourse.visibility = View.VISIBLE
                binding.tvSubHeading.text = "35,000 + students enrolled"
                binding.tvSubHeading.setTextColor(AppObjectController.joshApplication.resources.getColor(R.color.success))
            }
            else {
                binding.horizontalLine11.visibility = View.GONE
                binding.isBestCourse.visibility = ViewGroup.GONE
            }
            binding.executePendingBindings()
        }
    }
}