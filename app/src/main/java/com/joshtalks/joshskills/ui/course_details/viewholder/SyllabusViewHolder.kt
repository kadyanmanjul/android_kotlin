package com.joshtalks.joshskills.ui.course_details.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.google.gson.JsonObject
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.databinding.LayoutSyllabusViewBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DownloadSyllabusEvent
import com.joshtalks.joshskills.repository.server.course_detail.Syllabus
import com.joshtalks.joshskills.repository.server.course_detail.SyllabusData


class SyllabusViewHolder(val item: LayoutSyllabusViewBinding) : DetailsBaseViewHolder(item) {
    override fun bindData(sequence: Int, cardData: JsonObject) {
        val data = AppObjectController.gsonMapperForLocal.fromJson(
            cardData.toString(),
            SyllabusData::class.java
        )
        item.title.text = data.title
        if (item.multiLinelayout.childCount == 0) {
            data.syllabusList.sortedBy { it.sortOrder }.forEach {
                item.multiLinelayout.addView(addLinerLayout(it))
            }
        }
        item.downloadSyllabus.setOnClickListener {
            RxBus2.publish(DownloadSyllabusEvent(data))
        }
    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(it: Syllabus): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.layout_landing_page_multi_line, item.rootView, false)
        val joshTextView = view.findViewById(R.id.landing_text) as JoshTextView
        val image = view.findViewById(R.id.landing_image) as ImageView
        joshTextView.text = it.text
        setDefaultImageView(image, it.iconUrl)
        return view
    }
}
