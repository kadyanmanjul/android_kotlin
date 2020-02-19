package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.view.View
import androidx.core.text.HtmlCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.course_detail.CourseInformation
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


const val MIN_LINES = 4

@Layout(R.layout.course_details_data_view_holder)
class CourseDetailDataViewHeader(
    private var courseInformation: CourseInformation,
    private val context: Context = AppObjectController.joshApplication
) {

    @com.mindorks.placeholderview.annotations.View(R.id.tv_detail)
    lateinit var detailsTV: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text_title)
    lateinit var titleTV: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text_read_more)
    lateinit var readMoreTV: JoshTextView

    @Resolve
    fun onResolved() {
        titleTV.text = courseInformation.title
        readMoreTV.visibility = View.GONE

        if (courseInformation.isList) {
            detailsTV.maxLines = Integer.MAX_VALUE
            val list = courseInformation.desc.split("~").toTypedArray()
            val sb = StringBuilder()
            list.forEachIndexed { index, value ->
                sb.append("&#x2713;&nbsp;&nbsp;").append(value)
                if (index != list.size - 1) {
                    sb.append("</br><br/><br/>")
                }
            }
            detailsTV.text = HtmlCompat.fromHtml(sb.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)

        } else {
            detailsTV.text =
                HtmlCompat.fromHtml(courseInformation.desc, HtmlCompat.FROM_HTML_MODE_LEGACY)
            readMoreTV.visibility = View.VISIBLE
            detailsTV.maxLines = MIN_LINES
        }
    }

    @Click(R.id.text_read_more)
    fun onClick() {
        if (detailsTV.maxLines == MIN_LINES) {
            detailsTV.maxLines = Integer.MAX_VALUE
            readMoreTV.text = context.getString(R.string.show_less)
        } else {
            detailsTV.maxLines = MIN_LINES
            readMoreTV.text = context.getString(R.string.show_more)

        }

    }


}