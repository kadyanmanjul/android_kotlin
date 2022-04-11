package com.joshtalks.joshskills.ui.userprofile.fragments

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
import androidx.viewpager.widget.ViewPager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.engage.ImageEngage
import com.joshtalks.joshskills.repository.service.EngagementNetworkHelper
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.joshtalks.joshskills.ui.userprofile.adapters.AdapterCallback
import com.joshtalks.joshskills.ui.userprofile.adapters.ViewPagerAdapter
import com.joshtalks.joshskills.ui.userprofile.viewmodel.UserProfileViewModel

const val MENTOR_ID = "mentor_id"
const val IS_PREVIOUS_PROFILE = "is_previous_profile"
const val List_OF_IMAGES ="list_of_images"
const val LIST_OF_IDS="List_of_ids"
const val POSITION ="Position"

class ProfileImageShowFragment : DialogFragment(),AdapterCallback {
    private var mentorId: String? = null
    private var isPreviousProfile = false
    private var position:Int=0
    var imagesUrls : Array<String> = arrayOf()
    var imageIds :  Array<String> = arrayOf()
    lateinit var viewPager: ViewPager

    lateinit var viewPagerAdapter: ViewPagerAdapter
    private val viewModel by lazy {
        ViewModelProvider(activity as UserProfileActivity).get(
            UserProfileViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let { it ->
            it.getString(MENTOR_ID)?.let {
                mentorId = it
            }
            it.getBoolean(IS_PREVIOUS_PROFILE)?.let {
                isPreviousProfile = it
            }
            it.getStringArray(List_OF_IMAGES)?.let{
                if (it != null) {
                    imagesUrls=it
                }
            }
            it.getStringArray(LIST_OF_IDS)?.let {
                if (it != null) {
                    imageIds=it
                }
            }
            it.getInt(POSITION)?.let {
                position=it
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
        viewPager = view.findViewById(R.id.viewPagerMain) as ViewPager
        viewPagerAdapter =  ViewPagerAdapter(AppObjectController.joshApplication, imagesUrls,::dismissAllowingStateLoss,this)
        viewPager.adapter = viewPagerAdapter
        viewPager.currentItem = position

        if (isPreviousProfile && mentorId.equals(Mentor.getInstance().getId())) {
            view.findViewById<LinearLayout>(R.id.parent_layout).visibility = View.VISIBLE
        } else {
            view.findViewById<LinearLayout>(R.id.parent_layout).visibility = View.GONE
        }
        view.findViewById<View>(R.id.delete_layout).setOnClickListener {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.delete_popup)
                .setNegativeButton(Html.fromHtml("<font color='#E10717'><b>Delete<b></font>")) { dialog, id ->
                    imageIds[position]?.let { it -> mentorId?.let { it1 ->
                        viewModel.deletePreviousProfilePic(it,
                            it1
                        )
                    } }
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
            imageIds[position]?.let {
                mentorId?.let { it1 -> viewModel.updateProfilePicFromPreviousProfile(it, it1) }

            }
            dismiss()

        }
        view.findViewById<View>(R.id.iv_back).setOnClickListener {
            AppAnalytics.create(AnalyticsEvent.BACK_PRESSED.NAME)
                .addParam("name", javaClass.simpleName)
                .push()
            dismiss()
        }

        if (imageIds.isNullOrEmpty().not()) {
            EngagementNetworkHelper.engageImageApi(ImageEngage(imageIds[position]!!))
        }

    }

    companion object {
        fun newInstance(
            mentorId: String,
            isPreviousProfile: Boolean,
            imageUrls: Array<String>?,
            position: Int,
            imageIds: Array<String>?
        ) =
            ProfileImageShowFragment().apply {
                arguments = Bundle().apply {
                    putString(MENTOR_ID, mentorId)
                    putBoolean(IS_PREVIOUS_PROFILE, isPreviousProfile)
                    putStringArray(List_OF_IMAGES,imageUrls)
                    putInt(POSITION,position)
                    putStringArray(LIST_OF_IDS,imageIds)


                }
            }
    }

    override fun onSwipeCallback(position: Int) {
        this.position=position
    }

}
