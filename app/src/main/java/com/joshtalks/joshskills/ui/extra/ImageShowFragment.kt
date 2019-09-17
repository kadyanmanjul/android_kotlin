package com.joshtalks.joshskills.ui.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import kotlinx.android.synthetic.main.fragment_image_show.*


const val IMAGE_SOURCE = "image_source"

class ImageShowFragment : DialogFragment() {
    private lateinit var imagePath: String
    private lateinit var courseName: String


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(IMAGE_SOURCE)?.let { path ->
                imagePath = path
            }

            it.getString(COURSE_NAME)?.let { course ->
                courseName = course
            }

        }
        setStyle(STYLE_NO_FRAME, R.style.AppTheme_FullScreenDialog)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.setLayout(width, height)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_show, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Glide.with(this)
            .load(imagePath)
            .into(big_image_view)
        view.findViewById<AppCompatTextView>(R.id.text_message_title).text = courseName
        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            dismiss()
        }
        AppAnalytics.create(AnalyticsEvent.IMAGE_CLICKED.NAME).push()
    }


    companion object {
        fun newInstance(path: String, courseName: String) = ImageShowFragment().apply {
            arguments = Bundle().apply {
                putString(IMAGE_SOURCE, path)
                putString(COURSE_NAME, courseName)

            }
        }
    }
}