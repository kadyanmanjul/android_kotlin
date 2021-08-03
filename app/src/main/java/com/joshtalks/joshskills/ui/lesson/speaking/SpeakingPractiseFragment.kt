package com.joshtalks.joshskills.ui.lesson.speaking

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HAS_SEEN_SPEAKING_TOOLTIP
import com.joshtalks.joshskills.core.PermissionUtils
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.SPEAKING_POINTS
import com.joshtalks.joshskills.core.isCallOngoing
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.SpeakingPractiseFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.CHAT_TYPE
import com.joshtalks.joshskills.repository.local.entity.QUESTION_STATUS
import com.joshtalks.joshskills.repository.local.eventbus.DBInsertion
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SpeakingPractiseFragment : CoreJoshFragment() {

    private lateinit var binding: SpeakingPractiseFragmentBinding
    var lessonActivityListener: LessonActivityListener? = null
    private var compositeDisposable = CompositeDisposable()
    private var courseId: String = EMPTY
    private var topicId: String? = EMPTY
    private var questionId: String? = null
    private var haveAnyFavCaller = false

    private var openCallActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
    }

    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(requireActivity()).get(LessonViewModel::class.java)
    }

    private var currentTooltipIndex = 0
    private val lessonTooltipList by lazy {
        listOf(
            "यहाँ हम एक प्रैक्टिस पार्टनर के साथ निडर होकर इंग्लिश बोलने का अभ्यास करेंगे"
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is LessonActivityListener) {
            lessonActivityListener = context
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.speaking_practise_fragment, container, false)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        addObservers()
        showTooltip()
        return binding.rootView
    }

    override fun onResume() {
        super.onResume()
        if (topicId.isNullOrBlank().not()) {
            viewModel.getTopicDetail(topicId!!)
        }
        viewModel.isFavoriteCallerExist()
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(DBInsertion::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe(
                    {
                        viewModel.isFavoriteCallerExist()
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun addObservers() {
        viewModel.lessonQuestionsLiveData.observe(
            viewLifecycleOwner,
            {
                val spQuestion = it.filter { it.chatType == CHAT_TYPE.SP }.getOrNull(0)
                questionId = spQuestion?.id

                spQuestion?.topicId?.let {
                    this.topicId = it
                    viewModel.getTopicDetail(it)
                }
                spQuestion?.lessonId?.let { viewModel.getCourseIdByLessonId(it) }
            }
        )
        viewModel.courseId.observe(
            viewLifecycleOwner,
            {
                courseId = it
            }
        )
        binding.btnStart.setOnClickListener {
            startPractise(favoriteUserCall = false)
        }

        binding.btnContinue.setOnClickListener {
            lessonActivityListener?.onNextTabCall(3)
        }

        viewModel.speakingTopicLiveData.observe(
            viewLifecycleOwner,
            { response ->
                binding.progressView.visibility = GONE
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
                    binding.groupTwo.visibility = VISIBLE

                    val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
                    if (points.isNotEmpty()) {
                        // showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                        PrefManager.put(SPEAKING_POINTS, EMPTY)
                    }

                    if (response.alreadyTalked >= response.duration && response.isFromDb.not()) {
                        binding.btnContinue.visibility = VISIBLE
                        lessonActivityListener?.onQuestionStatusUpdate(
                            QUESTION_STATUS.AT,
                            questionId
                        )
                        lessonActivityListener?.onSectionStatusUpdate(3, true)
                    }
                }
            }
        )
        binding.btnFavorite.setOnClickListener {
            if (haveAnyFavCaller) {
                startPractise(favoriteUserCall = true)
            } else {
                showToast(getString(R.string.empty_favorite_list_message))
            }
        }
        lifecycleScope.launchWhenStarted {
            viewModel.favoriteCaller.collect {
                haveAnyFavCaller = it
                binding.btnFavorite.visibility = if (haveAnyFavCaller) VISIBLE else GONE
            }
        }
        binding.btnNextStep.setOnClickListener {
            showNextTooltip()
        }
    }

    private fun showTooltip() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (PrefManager.getBoolValue(HAS_SEEN_SPEAKING_TOOLTIP, defValue = false)) {
                withContext(Dispatchers.Main) {
                    binding.lessonTooltipLayout.visibility = GONE
                }
            } else {
                delay(DEFAULT_TOOLTIP_DELAY_IN_MS)
                if (viewModel.lessonLiveData.value?.lessonNo == 1) {
                    withContext(Dispatchers.Main) {
                        binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
                        binding.txtTooltipIndex.text =
                            "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
                        binding.lessonTooltipLayout.visibility = VISIBLE
                    }
                }
            }
        }
    }

    private fun showNextTooltip() {
        if (currentTooltipIndex < lessonTooltipList.size - 1) {
            currentTooltipIndex++
            binding.joshTextView.text = lessonTooltipList[currentTooltipIndex]
            binding.txtTooltipIndex.text =
                "${currentTooltipIndex + 1} of ${lessonTooltipList.size}"
        } else {
            binding.lessonTooltipLayout.visibility = GONE
            PrefManager.put(HAS_SEEN_SPEAKING_TOOLTIP, true)
        }
    }

    fun hideTooltip() {
        binding.lessonTooltipLayout.visibility = GONE
        PrefManager.put(HAS_SEEN_SPEAKING_TOOLTIP, true)
    }

    private fun startPractise(favoriteUserCall: Boolean = false) {
        if (PermissionUtils.isCallingPermissionEnabled(requireContext())) {
            startPractiseSearchScreen(favoriteUserCall = favoriteUserCall)
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
                            startPractiseSearchScreen(favoriteUserCall = favoriteUserCall)
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
            }
        )
    }

    private fun startPractiseSearchScreen(favoriteUserCall: Boolean = false) {
        viewModel.speakingTopicLiveData.value?.run {
            if (isCallOngoing(R.string.call_engage_initiate_call_message).not()) {
                openCallActivity.launch(
                    SearchingUserActivity.startUserForPractiseOnPhoneActivity(
                        requireActivity(),
                        courseId = courseId,
                        topicId = id,
                        topicName = topicName,
                        favoriteUserCall = favoriteUserCall,
                        conversationId = getConversationId()
                    )
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            SpeakingPractiseFragment()
    }
}
