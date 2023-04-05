package com.joshtalks.joshskills.ui.lesson

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemLessonActivitiesBinding

class LessonSectionsListAdapter(var items: List<SectionModel> = listOf(), private val onSectionClickLister: OnSectionClickLister) :
    RecyclerView.Adapter<LessonSectionsListAdapter.ActivitiesViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivitiesViewHolder {
        val view = ItemLessonActivitiesBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ActivitiesViewHolder(view, onSectionClickLister)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ActivitiesViewHolder, position: Int) {
        holder.setData(items[position])
    }

    inner class ActivitiesViewHolder(
        val binding: ItemLessonActivitiesBinding,
        private val onSectionClickLister: OnSectionClickLister
    ) : RecyclerView.ViewHolder(binding.root) {

        fun setData(list: SectionModel) {
            binding.txtSectionName.text = list.lessonTitle
            binding.txtSectionSubheading.text = list.lessonSubTitle

            binding.sectionCardRootView.setOnClickListener {
                onSectionClickLister.onSectionClick(list.lessonTitle)
            }
        }

    }

    interface OnSectionClickLister{
        fun onSectionClick(sectionName:String)
    }
}