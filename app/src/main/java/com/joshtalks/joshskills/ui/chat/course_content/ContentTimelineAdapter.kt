package com.joshtalks.joshskills.ui.chat.course_content

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.github.vipulasri.timelineview.TimelineView
import com.joshtalks.joshskills.core.convertCamelCase
import com.joshtalks.joshskills.databinding.ContentTimelineItemBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ContentClickEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.CourseContentEntity

class ContentTimelineAdapter(private var items: List<CourseContentEntity>) :
    RecyclerView.Adapter<ContentTimelineAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ContentTimelineItemBinding.inflate(inflater, parent, false)
        return ViewHolder(binding, viewType)
    }

    override fun getItemViewType(position: Int): Int {
        return TimelineView.getTimeLineViewType(position, itemCount)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(items[position])

    fun addItem(items: List<CourseContentEntity>) {
        this.items = items
        notifyDataSetChanged()
    }

    inner class ViewHolder(val binding: ContentTimelineItemBinding, private val viewType: Int) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(courseContentEntity: CourseContentEntity) {
            with(binding) {
                timeline.initLine(viewType)
                this.textTitle.text = convertCamelCase(courseContentEntity.title!!)
                this.textTitle.setOnClickListener {
                    RxBus2.publish(ContentClickEventBus(courseContentEntity))
                }
                this.rootView.setOnClickListener {
                    RxBus2.publish(ContentClickEventBus(courseContentEntity))
                }
            }
        }
    }

}