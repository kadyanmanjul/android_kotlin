package com.joshtalks.joshskills.ui.day_wise_course.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.LayoutLessonItemBinding
import com.joshtalks.joshskills.repository.local.entity.LessonModel

class LessonsAdapter(val context: Context) :
    PagedListAdapter<LessonModel, LessonsAdapter.LessonsViewHolder>(MyDiffUtilCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonsViewHolder {
        return LessonsViewHolder(
            LayoutLessonItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: LessonsViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonsViewHolder(val binding: LayoutLessonItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(lessonModel: LessonModel?) {
            lessonModel?.let {
                binding.lessonNameTv.text = context.getString(
                    R.string.lesson_name,
                    lessonModel.lessonNo,
                    lessonModel.lessonName
                )
                Utils.setImage(binding.lessonIv, lessonModel.varthumbnail)
            }
        }

    }

}

class MyDiffUtilCallback : DiffUtil.ItemCallback<LessonModel>() {
    override fun areItemsTheSame(
        oldTeamObject: LessonModel,
        newTeamObject: LessonModel
    ): Boolean {
        return (oldTeamObject.id
                == newTeamObject.id)
    }

    override fun areContentsTheSame(
        oldTeamObject: LessonModel,
        newTeamObject: LessonModel
    ): Boolean {
        return (oldTeamObject.lessonName == newTeamObject.lessonName)
    }
}
