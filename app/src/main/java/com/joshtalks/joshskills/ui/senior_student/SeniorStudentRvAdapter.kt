package com.joshtalks.joshskills.ui.senior_student

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.databinding.SeniorStudentRvItemBinding

class SeniorStudentRvAdapter :
    RecyclerView.Adapter<SeniorStudentRvAdapter.SeniorStudentViewHolder>() {
    private val dataList = mutableListOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SeniorStudentViewHolder {
        val view = DataBindingUtil.inflate<SeniorStudentRvItemBinding>(LayoutInflater.from(parent.context), R.layout.senior_student_rv_item, parent, false)
        return SeniorStudentViewHolder(view)
    }

    override fun onBindViewHolder(holder: SeniorStudentViewHolder, position: Int) {
        holder.onBind(dataList[position])
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    fun setData(dataList : List<String>) {
        this.dataList.clear()
        this.dataList.addAll(dataList)
        notifyDataSetChanged()
    }

    class SeniorStudentViewHolder(val view: SeniorStudentRvItemBinding) : RecyclerView.ViewHolder(view.root) {
        fun onBind(string : String) {
            view.data = string
        }
    }
}