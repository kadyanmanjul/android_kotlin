package com.joshtalks.joshskills.ui.help

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.server.FAQ

class FaqAdapter(
    private val values: ArrayList<FAQ>
) : RecyclerView.Adapter<FaqAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.faq_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = values[position]
        holder.question.text = item.question.trim()
        holder.itemView.setOnClickListener {
            RxBus2.publish(item)
        }
    }

    override fun getItemCount(): Int = values.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val question: TextView = view.findViewById(R.id.question)

        override fun toString(): String {
            return super.toString() + " '" + question.text + "'"
        }
    }

    fun updateList(faqList: List<FAQ>) {
        values.clear()
        values.addAll(faqList)
        notifyDataSetChanged()
    }
}
