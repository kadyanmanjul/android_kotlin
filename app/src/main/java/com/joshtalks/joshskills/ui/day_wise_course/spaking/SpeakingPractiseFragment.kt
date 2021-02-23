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
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.google.android.material.snackbar.Snackbar
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SPEAKING_POINTS
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.speaking_practise_fragment.btn_continue
import kotlinx.android.synthetic.main.speaking_practise_fragment.btn_start
import kotlinx.android.synthetic.main.speaking_practise_fragment.group_two
import kotlinx.android.synthetic.main.speaking_practise_fragment.progress_bar
import kotlinx.android.synthetic.main.speaking_practise_fragment.progress_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.root_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.text_view
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_practise_time
import kotlinx.android.synthetic.main.speaking_practise_fragment.tv_today_topic

class SpeakingPractiseFragment : CoreJoshFragment(), LifecycleObserver {

    var lessonActivityListener: LessonActivityListener? = null
    private var compositeDisposable = CompositeDisposable()
    private var lessonId: Int? = null
    private var courseId: String = EMPTY
    private var topicId: String? = null
    private var questionId: String? = null

    private var openCallActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
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

        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner, {
            val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
            questionId = spQuestion?.id
            lessonId = spQuestion?.lessonId
            topicId = spQuestion?.topicId
        })
        viewModel.courseId.observe(viewLifecycleOwner, {
            courseId = it
        })
        viewLifecycleOwner.lifecycle.addObserver(this)

        btn_start.setOnClickListener {
            startPractise()
        }
        btn_continue.setOnClickListener {
            lessonActivityListener?.onNextTabCall(3)
        }

        viewModel.speakingTopicLiveData.observe(viewLifecycleOwner, { response ->
            progress_view.visibility = View.GONE
            if (response == null) {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            } else {
                try {
                    tv_today_topic.text = response.topicName
                    tv_practise_time.text =
                        response.alreadyTalked.toString().plus(" / ")
                            .plus(response.duration.toString())
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
                val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
                if (points.isEmpty().not()) {
                    showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                    PrefManager.put(SPEAKING_POINTS, EMPTY)
                }

                if (response.alreadyTalked >= response.duration) {
                    btn_continue.visibility = View.VISIBLE
                    lessonActivityListener?.onQuestionStatusUpdate(
                        QUESTION_STATUS.AT,
                        questionId
                    )
                    lessonActivityListener?.onSectionStatusUpdate(3, true)
                }
            }
        })
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onFragmentResume() {
        topicId?.let { viewModel.getTopicDetail(it) }
        lessonId?.let { viewModel.getCourseIdByLessonId(it) }
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
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.callingPermissionPermanentlyDeniedDialog(
                                requireActivity(),
                                message = R.string.call_start_permission_message
                            )
                            return
                        }
                        if (flag) {
                            startPractiseSearchScreen()
                            return
                        } else {
                            MaterialDialog(requireActivity()).show {
                                message(R.string.call_start_permission_message_rational)
                                positiveButton(R.string.ok)
                            }
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
        viewModel.speakingTopicLiveData.value?.run {
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
        fun newInstance() =
            SpeakingPractiseFragment()
    }
}
