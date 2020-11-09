package com.joshtalks.joshskills.ui.day_wise_course.spaking


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.MutableLiveData
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopicModel
import com.joshtalks.joshskills.ui.voip.COURSE_ID
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.joshtalks.joshskills.ui.voip.TOPIC_ID
import com.joshtalks.joshskills.ui.voip.voip_rating.LAST_VOIP_CALL_ID
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.speaking_practise_fragment.btn_start
import kotlinx.android.synthetic.main.speaking_practise_fragment.group_one
import kotlinx.android.synthetic.main.speaking_practise_fragment.group_two
import kotlinx.android.synthetic.main.speaking_practise_fragment.progress_bar
import kotlinx.android.synthetic.main.speaking_practise_fragment.text_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_practise_time
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_today_topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val LESSON_ID = "lesson_id"

class SpeakingPractiseFragment : CoreJoshFragment() {
    private var lessonId: String = EMPTY
    private var courseId: String = EMPTY
    private var topicId: String? = null

    private val speakingTopicModelLiveData: MutableLiveData<SpeakingTopicModel> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(LAST_VOIP_CALL_ID)?.run {
            lessonId = this
        }
        arguments?.getString(COURSE_ID)?.run {
            courseId = this
        }
        arguments?.getString(TOPIC_ID)?.run {
            topicId = this
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(
            R.layout.speaking_practise_fragment,
            container,
            false
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btn_start.setOnClickListener {
            startPractise()
        }
        speakingTopicModelLiveData.observe(viewLifecycleOwner, { response ->
            try {
                tv_today_topic.text = response.topicName
                tv_practise_time.text =
                    response.alreadyTalked.toString() + "/" + response.duration.toString()
                progress_bar.progress = response.alreadyTalked.toFloat()
                progress_bar.progressMax = response.duration.toFloat()
                text_view.text =
                    getString(R.string.pp_message, response.duration.toString())
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            group_two.visibility = View.VISIBLE
            group_one.visibility = View.GONE
        })
        topicId?.let {
            getTopicDetail(it)
        }

    }

    private fun startPractise() {
        if (PermissionUtils.isCallingPermissionEnabled(requireContext())) {
            startPractiseSearchScreen()
            return
        }
        PermissionUtils.callingFeaturePermission(
            requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            startPractiseSearchScreen()
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                requireActivity(),
                                message = R.string.call_request_permission_permanent_message
                            )
                            return
                        }
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    token?.continuePermissionRequest()
                }
            })
    }

    private fun startPractiseSearchScreen() {
        speakingTopicModelLiveData.value?.run {
            SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                requireActivity(),
                courseId = courseId,
                topicId = id,
                topicName = topicName
            )
        }

    }


    private fun getTopicDetail(topicId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = AppObjectController.commonNetworkService.getTopicDetail(topicId)
                speakingTopicModelLiveData.postValue(response)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance(courseId: String, lessonId: Int, topicId: String?) =
            SpeakingPractiseFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(COURSE_ID, courseId)
                        putString(LESSON_ID, lessonId.toString())
                        putString(TOPIC_ID, topicId)
                    }
                }
    }
}