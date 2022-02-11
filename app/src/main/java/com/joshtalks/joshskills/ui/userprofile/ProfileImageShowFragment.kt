package com.joshtalks.joshskills.ui.userprofile

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import kotlinx.android.synthetic.main.fragment_image_show.big_image_view

const val IMAGE_SOURCE = "image_source"
const val IMAGE_ID = "image_id"
const val MENTOR_ID = "mentor_id"
const val IS_PREVIOUS_PROFILE = "is_previous_profile"

class ProfileImageShowFragment : DialogFragment() {
    private var imagePath: String? = null
    private var courseName: String? = null
    private var imageId: String? = null
    private var mentorId: String? = null
    private var isPreviousProfile = false
    private val viewModel by lazy {
        ViewModelProvider(activity as UserProfileActivity).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { it ->
            it.getString(IMAGE_SOURCE)?.let { path ->
                imagePath = path
            }

            it.getString(COURSE_NAME)?.let { course ->
                courseName = course
            }
            it.getString(IMAGE_ID)?.let { id ->
                imageId = id
            }
            it.getString(MENTOR_ID)?.let {
                mentorId = it
            }
            it.getBoolean(IS_PREVIOUS_PROFILE).let {
                isPreviousProfile = it
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
        return inflater.inflate(R.layout.fragment_profile_image_show, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (isPreviousProfile && mentorId.equals(Mentor.getInstance().getId())) {
            view.findViewById<LinearLayout>(R.id.parent_layout).visibility = View.VISIBLE
        } else {
            view.findViewById<LinearLayout>(R.id.parent_layout).visibility = View.GONE
        }
        Utils.setImage(big_image_view, imagePath)
        courseName?.run {
            view.findViewById<AppCompatTextView>(R.id.text_message_title).text = courseName

        }
        view.findViewById<View>(R.id.delete_layout).setOnClickListener {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.delete_popup)
                .setNegativeButton(Html.fromHtml("<font color='#E10717'><b>Delete<b></font>")) { dialog, id ->
                    viewModel.deletePreviousProfilePic(imageId!!)
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            dismiss()
                        } catch (ex:Exception){

                        }}, 1000)
                }
                .setPositiveButton(Html.fromHtml("<font color='#107BE5'><b>Cancel<b></font>")) { dialog, id -> }
            builder.create()
            builder.show()
        }
        view.findViewById<View>(R.id.set_as_profile_layout).setOnClickListener {
            imageId?.let {
                viewModel.updateProfilePicFromPreviousProfile(it)
            }
            dismiss()

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
    }

    companion object {
        fun newInstance(
            path: String?,
            courseName: String?,
            imageId: String?,
            mentorId: String,
            isPreviousProfile: Boolean
        ) =
            ProfileImageShowFragment().apply {
                arguments = Bundle().apply {
                    putString(IMAGE_SOURCE, path)
                    putString(COURSE_NAME, courseName)
                    putString(IMAGE_ID, imageId)
                    putString(MENTOR_ID, mentorId)
                    putBoolean(IS_PREVIOUS_PROFILE, isPreviousProfile)


                }
            }
    }

}
