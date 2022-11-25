package com.joshtalks.joshskills.common.ui.help

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.common.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.common.core.analytics.ParamKeys
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.server.FAQ

class FaqAdapter(
    private val values: ArrayList<FAQ>
) : RecyclerView.Adapter<com.joshtalks.joshskills.common.ui.help.FaqAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): _root_ide_package_.com.joshtalks.joshskills.common.ui.help.FaqAdapter.ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.faq_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: _root_ide_package_.com.joshtalks.joshskills.common.ui.help.FaqAdapter.ViewHolder, position: Int) {
        val item = values[position]
        holder.question.text = item.question.trim()
        holder.itemView.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.FAQ_QUESTION_CLICKED)
                .addParam(ParamKeys.QUESTION,item.question)
                .addParam(ParamKeys.CATEGORY_ID,item.categoryId)
                .push()
            com.joshtalks.joshskills.common.messaging.RxBus2.publish(item)
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
