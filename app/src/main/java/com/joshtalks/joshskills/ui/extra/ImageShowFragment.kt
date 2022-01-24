package com.joshtalks.joshskills.ui.extra

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import kotlinx.android.synthetic.main.fragment_image_show.big_image_view

const val IMAGE_SOURCE = "image_source"
const val IMAGE_ID = "image_id"

class ImageShowFragment : DialogFragment() {
    private var imagePath: String? = null
    private var courseName: String? = null
    private var imageId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            it.getString(IMAGE_SOURCE)?.let { path ->
                imagePath = path
            }

            it.getString(COURSE_NAME)?.let { course ->
                courseName = course
            }
            it.getString(IMAGE_ID)?.let { id ->
                imageId = id
            }
        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        AppAnalytics.create(AnalyticsEvent.IMAGE_OPENED.NAME).push()
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
//        big_image_view.doubleTapToZoom = true
        courseName?.run {
            view.findViewById<AppCompatTextView>(R.id.text_message_title).text = courseName

        }
        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            dismiss()
        }
        if (imageId.isNullOrEmpty().not()) {
            EngagementNetworkHelper.engageImageApi(ImageEngage(imageId!!))
        }
//        big_image_view.setGestureDetectorInterface {
//            dismissAllowingStateLoss()
//        }
    }

    companion object {
        fun newInstance(path: String?, courseName: String?, imageId: String?) =
            ImageShowFragment().apply {
                arguments = Bundle().apply {
                    putString(IMAGE_SOURCE, path)
                    putString(COURSE_NAME, courseName)
                    putString(IMAGE_ID, imageId)

                }
            }
    }
}