package com.joshtalks.joshskills.common.ui.course_details.viewholder

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.AppObjectController
import com.joshtalks.joshskills.common.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.DownloadSyllabusEvent
import com.joshtalks.joshskills.common.repository.server.course_detail.CardType
import com.joshtalks.joshskills.common.repository.server.course_detail.Syllabus
import com.joshtalks.joshskills.common.repository.server.course_detail.SyllabusData
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve



class SyllabusViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var syllabusData: SyllabusData,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    
    lateinit var rootView: ConstraintLayout

    
    lateinit var title: JoshTextView

    
    lateinit var linearLayout: LinearLayout

    
    lateinit var downloadSyllabus: MaterialTextView

    @Resolve
    fun onResolved() {
        title.text = syllabusData.title
        if (linearLayout.childCount == 0) {
            syllabusData.syllabusList.sortedBy { it.sortOrder }.forEach {
                linearLayout.addView(addLinerLayout(it))
            }
        }
    }

    @SuppressLint("WrongViewCast")
    private fun addLinerLayout(it: Syllabus): View? {
        val layoutInflater =
            AppObjectController.joshApplication.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = layoutInflater.inflate(R.layout.layout_landing_page_multi_line, rootView, false)
        val joshTextView = view.findViewById(R.id.landing_text) as JoshTextView
        val image = view.findViewById(R.id.landing_image) as ImageView
        joshTextView.text = it.text
        setDefaultImageView(image, it.iconUrl)
        return view
    }

    
    fun onClick() {
        com.joshtalks.joshskills.common.messaging.RxBus2.publish(DownloadSyllabusEvent(syllabusData))
    }
}
