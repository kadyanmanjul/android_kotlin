package com.joshtalks.joshskills.explore.course_details.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import com.google.gson.JsonObject
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.DownloadSyllabusEvent
import com.joshtalks.joshskills.common.repository.local.model.explore.Syllabus
import com.joshtalks.joshskills.common.repository.local.model.explore.SyllabusData
import com.joshtalks.joshskills.explore.R
import com.joshtalks.joshskills.explore.databinding.LayoutSyllabusViewBinding

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
