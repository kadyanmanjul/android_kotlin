package com.joshtalks.joshskills.ui.course_details.view_holders

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DownloadSyllabusEvent
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.Syllabus
import com.joshtalks.joshskills.repository.server.course_detail.SyllabusData
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.layout_syllabus_view)
class SyllabusViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var syllabusData: SyllabusData,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.root_view)
    lateinit var rootView: ConstraintLayout

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.multi_linelayout)
    lateinit var linearLayout: LinearLayout

    @com.mindorks.placeholderview.annotations.View(R.id.download_syllabus)
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

    @Click(R.id.download_syllabus)
    fun onClick() {
        RxBus2.publish(DownloadSyllabusEvent(syllabusData))
    }
}
