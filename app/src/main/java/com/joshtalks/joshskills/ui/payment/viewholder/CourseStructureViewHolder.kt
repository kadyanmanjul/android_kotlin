package com.joshtalks.joshskills.ui.payment.viewholder

import android.content.Context
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.course_detail.CourseStructure
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve


@Layout(R.layout.course_structure_layout)
class CourseStructureViewHolder(
    private var courseStructure: CourseStructure,
    private val context: Context = AppObjectController.joshApplication
) {

    @com.mindorks.placeholderview.annotations.View(R.id.tv_name)
    lateinit var nameTv: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.iv_expand)
    lateinit var ivExpand: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.tv_detail)
    lateinit var tvDetail: JoshTextView
    val vi: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    var expand = false

    @Resolve
    fun onResolved() {
        nameTv.text = courseStructure.title
        if (courseStructure.id == 1) {
            expand = true
            ivExpand.setImageResource(R.drawable.ic_remove_expand)
            tvDetail.visibility = View.VISIBLE
        }
        val sp = SpannableStringBuilder()
        courseStructure.cDetail.forEachIndexed { index, value ->
            val spannable = SpannableString(value)
            spannable.setSpan(BulletSpan(24), 0, value.length, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            sp.append(spannable)
            if (index != courseStructure.cDetail.size - 1) {
                sp.append("\n")
            }
        }

        tvDetail.text = sp

    }

    @Click(R.id.iv_expand)
    fun onClick() {
        expand = if (expand) {
            ivExpand.setImageResource(R.drawable.ic_expand)
            tvDetail.visibility = View.GONE
            false
        } else {
            tvDetail.visibility = View.VISIBLE
            ivExpand.setImageResource(R.drawable.ic_remove_expand)
            true
        }
    }


}