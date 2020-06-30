package com.joshtalks.joshskills.ui.course_details.extra

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.IconMarginSpan
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.repository.server.course_detail.Guideline
import io.github.inflationx.calligraphy3.TypefaceUtils
import kotlinx.android.synthetic.main.guideline_item_view.container

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
        val typefaceSpan = TypefaceUtils.load(requireContext().assets, "fonts/Poppins-Regular.ttf")
        guideLine.text.forEach {
            container.addView(getTextView(it, typefaceSpan))
        }
    }

    private fun getTextView(text: String, typefaceSpan: Typeface): AppCompatTextView {
        val textView = AppCompatTextView(requireContext())
        textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_48))
        textView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14F)
        textView.typeface = typefaceSpan
        val spanString = SpannableString(text)
        spanString.setSpan(
            IconMarginSpan(
                Utils.getBitmapFromVectorDrawable(
                    requireContext(),
                    R.drawable.ic_small_tick,
                    R.color.green
                ),
                22
            ), 0, text.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spanString
        textView.setPadding(0, 8, 0, 8)
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