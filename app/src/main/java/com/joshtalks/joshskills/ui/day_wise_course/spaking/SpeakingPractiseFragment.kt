package com.joshtalks.joshskills.ui.day_wise_course.spaking


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SPEAKING_POINTS
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.repository.server.voip.SpeakingTopicModel
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.feedback.QUESTION_ID
import com.joshtalks.joshskills.ui.voip.COURSE_ID
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.joshtalks.joshskills.ui.voip.TOPIC_ID
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.speaking_practise_fragment.btn_continue
import kotlinx.android.synthetic.main.speaking_practise_fragment.btn_start
import kotlinx.android.synthetic.main.speaking_practise_fragment.group_one
import kotlinx.android.synthetic.main.speaking_practise_fragment.group_two
import kotlinx.android.synthetic.main.speaking_practise_fragment.progress_bar
import kotlinx.android.synthetic.main.speaking_practise_fragment.root_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.text_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_practise_time
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_today_topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

const val LESSON_ID = "lesson_id"

class SpeakingPractiseFragment : CoreJoshFragment(), LifecycleObserver {

    var activityCallback: CapsuleActivityCallback? = null
    private var compositeDisposable = CompositeDisposable()
    private var lessonId: String = EMPTY
    private var courseId: String = EMPTY
    private var topicId: String? = null
    private var questionId: String? = null

    private val speakingTopicModelLiveData: MutableLiveData<SpeakingTopicModel> = MutableLiveData()
    private var openCallActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString(LESSON_ID)?.run {
            lessonId = this
        }
        arguments?.getString(COURSE_ID)?.run {
            courseId = this
        }
        arguments?.getString(TOPIC_ID)?.run {
            topicId = this
        }
        arguments?.getString(QUESTION_ID)?.run {
            questionId = this
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
        viewLifecycleOwner.lifecycle.addObserver(this)
        btn_start.setOnClickListener {
            startPractise()
        }
        btn_continue.setOnClickListener {
            activityCallback?.onNextTabCall(3)
        }
        speakingTopicModelLiveData.observe(viewLifecycleOwner, { response ->
            try {
                tv_today_topic.text = response.topicName
                tv_practise_time.text =
                    response.alreadyTalked.toString().plus(" / ").plus(response.duration.toString())
                        .plus("\n Minutes")
                progress_bar.progress = response.alreadyTalked.toFloat()
                progress_bar.progressMax = response.duration.toFloat()

                text_view.text = if (response.duration >= 10) {
                    getString(R.string.pp_messages, response.duration.toString())
                } else {
                    getString(R.string.pp_message, response.duration.toString())
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            group_two.visibility = View.VISIBLE
            group_one.visibility = View.GONE
            val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
            if (points.isNullOrEmpty().not()) {
                showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                PrefManager.put(SPEAKING_POINTS, EMPTY)
            }

            if (response.alreadyTalked >= response.duration) {
                btn_continue.visibility = View.VISIBLE
                activityCallback?.onQuestionStatusUpdate(
                    QUESTION_STATUS.AT,
                    questionId?.toInt() ?: 0
                )
                activityCallback?.onSectionStatusUpdate(3, true)
            }
        })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onFragmentResume() {
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
            openCallActivity.launch(
                SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                    requireActivity(),
                    courseId = courseId,
                    topicId = id,
                    topicName = topicName
                )
            )
        }

    }


    private fun getTopicDetail(topicId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                delay(1500)
                val response = AppObjectController.commonNetworkService.getTopicDetail(topicId)
                speakingTopicModelLiveData.postValue(response)
            } catch (ex: Throwable) {
                ex.showAppropriateMsg()
            }
        }
    }


    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    showSnackBar(root_view, Snackbar.LENGTH_LONG, it.pointsSnackBarText)
                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }


    companion object {
        @JvmStatic
        fun newInstance(courseId: String, lessonId: Int, topicId: String?, questionId: String?) =
            SpeakingPractiseFragment()
                .apply {
                    arguments = Bundle().apply {
                        putString(COURSE_ID, courseId)
                        putString(LESSON_ID, lessonId.toString())
                        putString(TOPIC_ID, topicId)
                        putString(QUESTION_ID, questionId)
                    }
                }
    }
}