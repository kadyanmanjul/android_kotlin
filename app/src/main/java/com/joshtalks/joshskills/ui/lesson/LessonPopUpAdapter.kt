package com.joshtalks.joshskills.ui.lesson

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.databinding.ItemLessonPopupBinding

class LessonPopUpAdapter(var items: List<CompleteLessonPopupModel> = listOf(), val clickListener:(Int)->Unit) :
    RecyclerView.Adapter<LessonPopUpAdapter.LessonViewHolder>() {
    inner class LessonViewHolder(val itemLessonPopUpListBinding: ItemLessonPopupBinding) :
        RecyclerView.ViewHolder(itemLessonPopUpListBinding.root) {
        fun bind(item: CompleteLessonPopupModel) {
            with(itemLessonPopUpListBinding) {
                itemLessonPopUpListBinding.item = item
                crdView.setOnClickListener {
                    clickListener.invoke(item.id)
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonPopUpAdapter.LessonViewHolder {
        val binding = ItemLessonPopupBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LessonViewHolder(binding)
    }


    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(items[position])
        holder.itemLessonPopUpListBinding.item = items[position]
    }
}