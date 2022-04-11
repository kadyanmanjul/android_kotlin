package com.joshtalks.joshskills.ui.userprofile.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.EnrolledCoursesRowItemBinding
import com.joshtalks.joshskills.ui.userprofile.models.CourseEnrolled

class EnrolledCoursesListAdapter(
    var items: ArrayList<CourseEnrolled>? = arrayListOf()
) : RecyclerView.Adapter<EnrolledCoursesListAdapter.ViewHolder>() {
    var itemClick: ((CourseEnrolled, Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = DataBindingUtil.inflate<EnrolledCoursesRowItemBinding>(
            LayoutInflater.from(parent.context),
            R.layout.enrolled_courses_row_item,
            parent,
            false
        )
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items?.get(position)!!)
    }

    fun setListener(function: ((CourseEnrolled, Int) -> Unit)?) {
        itemClick = function
    }

    override fun getItemCount(): Int = items?.size?:0

    inner class ViewHolder(val view: EnrolledCoursesRowItemBinding) :
        RecyclerView.ViewHolder(view.root) {
        fun bind(courseEnrolled: CourseEnrolled) {
            view.itemData = courseEnrolled
        }
    }

    fun addEnrolledCoursesToList(enrolledCourseList: ArrayList<CourseEnrolled>?) {
        items = enrolledCourseList
        notifyDataSetChanged()
    }
}