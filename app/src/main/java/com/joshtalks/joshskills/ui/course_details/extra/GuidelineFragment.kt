package com.joshtalks.joshskills.ui.course_details.extra

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.IconMarginSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.course_detail.Guideline

const val GUIDELINE_OBJECT = "guideline_obj"

class GuidelineFragment : Fragment() {

    companion object {
        fun newInstance(guideLine: Guideline) = GuidelineFragment().apply {
            arguments = Bundle().apply {
                putParcelable(GUIDELINE_OBJECT, guideLine)
            }
        }
    }

    private lateinit var guideLine: Guideline

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            guideLine = it.getParcelable<Guideline>(GUIDELINE_OBJECT) as Guideline
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.guideline_item_view, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        guideLine.text.forEach {
            view.findViewById<LinearLayout>(R.id.container).addView(getTextView(it))
        }
    }

    private fun getTextView(text: String): AppCompatTextView {
        val textView = AppCompatTextView(requireContext())
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_default))
        TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_JoshTypography_BodyRegular20)
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(
                    requireContext(),
                    R.drawable.ic_small_tick,
                    R.color.success
                ),
                22
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(0, 4, 0, 4)
        textView.setLineSpacing(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                2.0f,
                requireContext().resources.displayMetrics
            ), 1.0f
        )
        return textView

    }

}