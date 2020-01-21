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
import kotlinx.android.synthetic.main.fragment_image_show.*


const val IMAGE_SOURCE = "image_source"
const val IMAGE_ID = "image_id"

class ImageShowFragment : DialogFragment() {
    private lateinit var imagePath: String
    private lateinit var courseName: String
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
            it.getString(IMAGE_ID)?.let {
                imageId = it
            }

        }
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
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
        big_image_view.doubleTapToZoom = true
        view.findViewById<AppCompatTextView>(R.id.text_message_title).text = courseName
        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            dismiss()
        }
        AppAnalytics.create(AnalyticsEvent.IMAGE_CLICKED.NAME).push()
        imageId?.let {
            EngagementNetworkHelper.engageImageApi(ImageEngage(it))
        }
        /*big_image_view.setOnClickListener(object : View.OnClickListener {
            var show = true
            override fun onClick(v: View) {
                show = if (show) {
                    toolbar.animate().translationY((-toolbar.bottom).toFloat())
                        .setInterpolator(AccelerateInterpolator()).start()
                    hideSystemUI()
                    false
                } else {
                    toolbar.animate().translationY(0f).setInterpolator(DecelerateInterpolator())
                        .start()
                    showSystemUI()
                    true
                }
            }
        })*/
    }
    /* private fun hideSystemUI() { // Set the IMMERSIVE flag.
 // Set the content to appear under the system bars so that the content
 // doesn't resize when the system bars hide and show.
         val decorView: View = activity!!.getWindow().getDecorView()
         decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                 or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                 or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                 or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                 or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                 or View.SYSTEM_UI_FLAG_IMMERSIVE)
     }
     private fun showSystemUI() {
         val decorView: View =  activity!!.getWindow().getDecorView()
         decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                 or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                 or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
     }
 */

    companion object {
        fun newInstance(path: String, courseName: String, imageId: String?) =
            ImageShowFragment().apply {
                arguments = Bundle().apply {
                    putString(IMAGE_SOURCE, path)
                    putString(COURSE_NAME, courseName)
                    putString(IMAGE_ID, imageId)

                }
            }
    }
}