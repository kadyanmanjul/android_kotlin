package com.joshtalks.joshskills.ui.lesson.speaking


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ViewModelProvider
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SPEAKING_POINTS
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.SpeakingPractiseFragmentBinding
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

class SpeakingPractiseFragment : CoreJoshFragment(), LifecycleObserver {

    private lateinit var binding: SpeakingPractiseFragmentBinding
    var lessonActivityListener: LessonActivityListener? = null
    private var compositeDisposable = CompositeDisposable()
    private var courseId: String = EMPTY
    private var topicId: String? = EMPTY
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
        binding =
            DataBindingUtil.inflate(inflater, R.layout.speaking_practise_fragment, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)

        addObservers()

        return binding.rootView
    }

    private fun addObservers() {

        viewModel.lessonQuestionsLiveData.observe(viewLifecycleOwner, {
            val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
            questionId = spQuestion?.id

            spQuestion?.topicId?.let {
                this.topicId = it
                viewModel.getTopicDetail(it)
            }
            spQuestion?.lessonId?.let { viewModel.getCourseIdByLessonId(it) }

        })
        viewModel.courseId.observe(viewLifecycleOwner, {
            courseId = it
        })
        viewLifecycleOwner.lifecycle.addObserver(this)

        binding.btnStart.setOnClickListener {
            startPractise(true)
        }
        binding.btnContinue.setOnClickListener {
            lessonActivityListener?.onNextTabCall(3)
        }

        viewModel.speakingTopicLiveData.observe(viewLifecycleOwner, { response ->
            binding.progressView.visibility = View.GONE
            if (response == null) {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            } else {
                try {
                    binding.tvTodayTopic.text = response.topicName
                    binding.tvPractiseTime.text =
                        response.alreadyTalked.toString().plus(" / ")
                            .plus(response.duration.toString())
                            .plus("\n Minutes")
                    binding.progressBar.progress = response.alreadyTalked.toFloat()
                    binding.progressBar.progressMax = response.duration.toFloat()

                    binding.textView.text = if (response.duration >= 10) {
                        getString(R.string.pp_messages, response.duration.toString())
                    } else {
                        getString(R.string.pp_message, response.duration.toString())
                    }
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
                binding.groupTwo.visibility = View.VISIBLE

                val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
                if (points.isNullOrEmpty().not()) {
                    //showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                    PrefManager.put(SPEAKING_POINTS, EMPTY)
                }

                if (response.alreadyTalked >= response.duration && response.isFromDb.not()) {
                    binding.btnContinue.visibility = View.VISIBLE
                    lessonActivityListener?.onQuestionStatusUpdate(
                        QUESTION_STATUS.AT,
                        questionId
                    )
                    lessonActivityListener?.onSectionStatusUpdate(3, true)
                }
            }
        })
        binding.btnFavorite.setOnClickListener {
            viewModel.grammarAssessmentLiveData
            viewModel.isFavoriteCallerExist(::callback)
        }
    }

    private fun callback(exist: Boolean) {
        if (exist) {
            startPractise(favoriteUserCall = true)
        } else {
            showToast(getString(R.string.empty_favorite_list_message))
        }
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onFragmentResume() {
        if (topicId.isNullOrBlank().not()) {
            viewModel.getTopicDetail(topicId!!)
        }
    }

    private fun startPractise(favoriteUserCall: Boolean = false) {
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
                                message(R.string.call_start_permission_message)
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

    private fun startPractiseSearchScreen(favoriteUserCall: Boolean = false) {
        viewModel.speakingTopicLiveData.value?.run {
            openCallActivity.launch(
                SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                    requireActivity(),
                    courseId = courseId,
                    topicId = id,
                    topicName = topicName,
                    favoriteUserCall = favoriteUserCall
                )
            )
        }

    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    // showSnackBar(root_view, Snackbar.LENGTH_LONG, it.pointsSnackBarText)
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
