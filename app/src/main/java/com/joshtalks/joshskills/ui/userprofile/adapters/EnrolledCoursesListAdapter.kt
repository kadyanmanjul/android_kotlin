package com.joshtalks.joshskills.ui.userprofile.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.ui.userprofile.models.CourseEnrolled
import de.hdodenhof.circleimageview.CircleImageView

class EnrolledCoursesListAdapter(
    private val items: List<CourseEnrolled> = emptyList()
) : RecyclerView.Adapter<EnrolledCoursesListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.enrolled_courses_row_item, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        var courseIcon: CircleImageView = view.findViewById(R.id.profile_image)
        var courseName: AppCompatTextView = view.findViewById(R.id.tv_course_name)
        var courseText: AppCompatTextView = view.findViewById(R.id.tv_students_enrolled)
        fun bind(courseEnrolled: CourseEnrolled) {
            courseName.text=courseEnrolled.courseName
            courseText.text=AppObjectController.joshApplication.getString(R.string.enrolled_student_text,courseEnrolled.noOfStudents.toString())
            if(courseEnrolled.courseImage==null){
                courseIcon.setImageResource(R.drawable.group_default_icon)
            }else{
                courseIcon.setImage(courseEnrolled.courseImage,view.context)

            }

        }

    }

}