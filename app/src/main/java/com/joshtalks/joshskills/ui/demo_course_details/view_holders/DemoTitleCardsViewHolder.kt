package com.joshtalks.joshskills.ui.demo_course_details.view_holders

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.repository.server.course_detail.CardType
import com.joshtalks.joshskills.repository.server.course_detail.demoCourseDetails.FactsResponse
import com.joshtalks.joshskills.ui.course_details.viewholder.CourseDetailsBaseCell
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import io.github.inflationx.calligraphy3.CalligraphyTypefaceSpan
import io.github.inflationx.calligraphy3.TypefaceUtils


@Layout(R.layout.layout_demo_lesson_course_detail_title_card_view)
class DemoTitleCardsViewHolder(
    override val type: CardType,
    override val sequenceNumber: Int,
    private var factsResponse: FactsResponse,
    private val context: Context = AppObjectController.joshApplication
) : CourseDetailsBaseCell(type, sequenceNumber) {

    @com.mindorks.placeholderview.annotations.View(R.id.title)
    lateinit var title: TextView

    @com.mindorks.placeholderview.annotations.View(R.id.rating_card)
    lateinit var ratingCard: MaterialCardView

    @com.mindorks.placeholderview.annotations.View(R.id.count_card)
    lateinit var countCard: MaterialCardView

    @com.mindorks.placeholderview.annotations.View(R.id.image)
    lateinit var image: AppCompatImageView

    @com.mindorks.placeholderview.annotations.View(R.id.text)
    lateinit var text: JoshTextView

    @com.mindorks.placeholderview.annotations.View(R.id.text2)
    lateinit var text2: JoshTextView

    private val typefaceSpan =
        CalligraphyTypefaceSpan(
            TypefaceUtils.load(
                getAppContext().assets,
                "fonts/OpenSans-Bold.ttf"
            )
        )

    private val sBuilder = SpannableStringBuilder().append("")
    private val sBuilder2 = SpannableStringBuilder().append("")


    @Resolve
    fun onResolved() {
        title.text = factsResponse.heading
        factsResponse.facts_list?.forEachIndexed { index, facts ->
            when (index) {
                0 -> {
                    if (facts != null) {
                        sBuilder.clear()
                        ratingCard.visibility = View.VISIBLE
                        facts.imageUrl?.let { image.setImage(it, context) }
                        facts.text?.replace("<b>","")
                        facts.text?.replace("</b>","")
                        sBuilder.append(facts.text)
                        val words = sBuilder.split(" ")
                        sBuilder.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.demo_app_black_color
                                )
                            ),
                            0,
                            words.size,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        sBuilder.setSpan(typefaceSpan, 0, words.size, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        text.setText(sBuilder, TextView.BufferType.SPANNABLE)
                    } else {
                        ratingCard.visibility = View.GONE
                    }
                }
                else -> {
                    if (facts != null) {
                        sBuilder2.clear()
                        countCard.visibility = View.VISIBLE
                        sBuilder2.append(facts.text)

                        val words = sBuilder2.split(" ")
                        sBuilder2.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    AppObjectController.joshApplication,
                                    R.color.demo_app_black_color
                                )
                            ),
                            0,
                            words.size,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        sBuilder2.setSpan(typefaceSpan, 0, words.size, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        text2.setText(sBuilder2, TextView.BufferType.SPANNABLE)

                       // text2.text = HtmlCompat.fromHtml(facts.text.toString(), HtmlCompat.FROM_HTML_MODE_LEGACY)
                    } else {
                        countCard.visibility = View.GONE
                    }
                }
            }
        }
    }
}
