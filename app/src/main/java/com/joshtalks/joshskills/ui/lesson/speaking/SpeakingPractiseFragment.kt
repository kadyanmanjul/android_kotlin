package com.joshtalks.joshskills.ui.lesson.speaking

import android.content.Context
import android.content.Intent
import android.graphics.Paint
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
import com.joshtalks.joshskills.base.constants.INTENT_DATA_COURSE_ID
import com.joshtalks.joshskills.base.constants.INTENT_DATA_TOPIC_ID
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshFragment
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.HAS_SEEN_SPEAKING_TOOLTIP
import com.joshtalks.joshskills.core.HOW_TO_SPEAK_TEXT_CLICKED
import com.joshtalks.joshskills.core.IMPRESSION_TRUECALLER_P2P
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
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.chat.DEFAULT_TOOLTIP_DELAY_IN_MS
import com.joshtalks.joshskills.ui.group.views.JoshVoipGroupActivity
import com.joshtalks.joshskills.ui.lesson.LessonActivityListener
import com.joshtalks.joshskills.ui.lesson.LessonSpotlightState
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.senior_student.SeniorStudentActivity
import com.joshtalks.joshskills.ui.voip.SearchingUserActivity
import com.joshtalks.joshskills.ui.voip.new_arch.ui.views.VoiceCallActivity
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.Calendar
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
    private var isAnimationShown = false

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
            "कोर्स का सबसे मज़ेदार हिस्सा।",
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
        binding.vm = viewModel
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        addObservers()
        // showTooltip()
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
        viewModel.lessonSpotlightStateLiveData.observe(requireActivity(), {
            when (it) {
                LessonSpotlightState.SPEAKING_SPOTLIGHT_PART2 -> {
                    binding.nestedScrollView.scrollTo(0, binding.nestedScrollView.bottom)
                }
            }
        })
        viewModel.courseId.observe(
            viewLifecycleOwner,
            {
                courseId = it
            }
        )
        binding.btnStart.setOnClickListener {
            viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
            startPractise(favoriteUserCall = false)
        }

        binding.btnGroupCall.setOnClickListener {
            viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
            if(isCallOngoing(R.string.call_engage_initiate_call_message))
                return@setOnClickListener
            val intent = Intent(requireActivity(), JoshVoipGroupActivity::class.java).apply {
                putExtra(CONVERSATION_ID, getConversationId())
            }
            startActivity(intent)
        }

        viewModel.speakingSpotlightClickLiveData.observe(viewLifecycleOwner, {
            startPractise(favoriteUserCall = false)
        })

        binding.btnContinue.setOnClickListener {
            lessonActivityListener?.onNextTabCall(SPEAKING_POSITION)
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
                        /*binding.progressBar.visibility = GONE
                        binding.tvPractiseTime.visibility = GONE
                        binding.progressBarAnim.visibility = VISIBLE
                        binding.progressBarAnim.playAnimation()*/
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                    binding.groupTwo.visibility = VISIBLE
                    if (response.alreadyTalked.toFloat() >= response.duration.toFloat()) {
                        binding.progressBar.visibility = View.INVISIBLE
                        binding.tvPractiseTime.visibility = GONE
                        binding.progressBarAnim.visibility = VISIBLE
                        if (!isAnimationShown) {
                            binding.progressBarAnim.playAnimation()
                            isAnimationShown = true
                        }
                    }

                    val points = PrefManager.getStringValue(SPEAKING_POINTS, defaultValue = EMPTY)
                    if (points.isNotEmpty()) {
                        // showSnackBar(root_view, Snackbar.LENGTH_LONG, points)
                        PrefManager.put(SPEAKING_POINTS, EMPTY)
                    }

                    if (response.alreadyTalked >= response.duration && response.isFromDb.not()) {
                        binding.btnContinue.visibility = VISIBLE
                        binding.btnStart.pauseAnimation()
                        binding.btnContinue.playAnimation()
                        lessonActivityListener?.onQuestionStatusUpdate(
                            QUESTION_STATUS.AT,
                            questionId
                        )
                        lessonActivityListener?.onSectionStatusUpdate(SPEAKING_POSITION, true)
                    } else {
                        binding.btnStart.playAnimation()
                    }

                    if (response.isNewStudentCallsActivated) {
                        binding.txtLabelNewStudentCalls.visibility = VISIBLE
                        binding.progressNewStudentCalls.visibility = VISIBLE
                        binding.progressNewStudentCalls.progress = response.totalNewStudentCalls
                        binding.progressNewStudentCalls.max = response.requiredNewStudentCalls
                        binding.txtProgressCount.visibility = VISIBLE
                        binding.txtProgressCount.text =
                            "${response.totalNewStudentCalls}/${response.requiredNewStudentCalls}"
                        binding.txtCallsLeft.visibility = VISIBLE
                        binding.txtCallsLeft.text = when (val dayOfWeek =
                            Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
                            Calendar.SUNDAY ->
                                "1 day left"
                            else -> {
                                "${7 - (dayOfWeek - 1)} days left"
                            }
                        }
                        binding.txtLabelBecomeSeniorStudent.paintFlags =
                            binding.txtLabelBecomeSeniorStudent.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        binding.txtLabelBecomeSeniorStudent.visibility = VISIBLE
                        binding.btnNewStudent.visibility = VISIBLE
                        binding.infoContainer.visibility = GONE
                    } else {
                        binding.txtLabelNewStudentCalls.visibility = GONE
                        binding.progressNewStudentCalls.visibility = GONE
                        binding.txtProgressCount.visibility = GONE
                        binding.txtCallsLeft.visibility = GONE
                        binding.txtLabelBecomeSeniorStudent.visibility = GONE
                        binding.btnNewStudent.visibility = GONE
                        binding.infoContainer.visibility = VISIBLE
                    }
                }
            }
        )
        binding.btnFavorite.setOnClickListener {
            viewModel.saveTrueCallerImpression(IMPRESSION_TRUECALLER_P2P)
            if (haveAnyFavCaller) {
                startPractise(favoriteUserCall = true)
            } else {
                showToast(getString(R.string.empty_favorite_list_message))
            }
        }
        binding.btnNewStudent.setOnClickListener {
            startPractise(favoriteUserCall = false, isNewUserCall = true)
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

        viewModel.lessonLiveData.observe(viewLifecycleOwner, {
            if(it.lessonNo == 1){
                binding.btnCallDemo.visibility = View.GONE
                binding.txtHowToSpeak.visibility = View.VISIBLE
                binding.txtHowToSpeak.setOnClickListener {
                    viewModel.isHowToSpeakClicked(true)
                    binding.btnCallDemo.visibility = View.VISIBLE
                    viewModel.saveIntroVideoFlowImpression(HOW_TO_SPEAK_TEXT_CLICKED)
                }

                viewModel.callBtnHideShowLiveData.observe(viewLifecycleOwner, {
                    if(it == 1){
                        binding.nestedScrollView.visibility = View.INVISIBLE
                        binding.btnCallDemo.visibility = View.VISIBLE
                    }
                    if(it == 2){
                        binding.nestedScrollView.visibility = View.VISIBLE
                        binding.btnCallDemo.visibility = View.GONE
                    }
                })
            }else{
                binding.btnCallDemo.visibility = View.GONE
            }
        })

        viewModel.introVideoCompleteLiveData.observe(viewLifecycleOwner, {
            if(it == true){
                binding.btnCallDemo.visibility = View.GONE
            }
        })
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

    private fun startPractise(favoriteUserCall: Boolean = false, isNewUserCall: Boolean = false) {
        if (PermissionUtils.isCallingPermissionEnabled(requireContext())) {
            startPractiseSearchScreen(
                favoriteUserCall = favoriteUserCall,
                isNewUserCall = isNewUserCall
            )
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
                            val callIntent = Intent(requireContext(), VoiceCallActivity::class.java)
                            startActivity(callIntent)
//                            startPractiseSearchScreen(
//                                favoriteUserCall = favoriteUserCall,
//                                isNewUserCall = isNewUserCall
//                            )
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

    private fun startPractiseSearchScreen(
        favoriteUserCall: Boolean = false,
        isNewUserCall: Boolean = false,
    ) {
        // P2P Call
        val callIntent = Intent(requireContext(), VoiceCallActivity::class.java)
        callIntent.apply {
            putExtra(INTENT_DATA_COURSE_ID, courseId)
            putExtra(INTENT_DATA_TOPIC_ID, topicId)
        }
        startActivity(callIntent)
//        viewModel.speakingTopicLiveData.value?.run {
//            if (isCallOngoing(R.string.call_engage_initiate_call_message).not()) {
//                openCallActivity.launch(
//                    SearchingUserActivity.startUserForPractiseOnPhoneActivity(
//                        requireActivity(),
//                        courseId = courseId,
//                        topicId = id,
//                        topicName = topicName,
//                        favoriteUserCall = favoriteUserCall,
//                        isNewUserCall = isNewUserCall,
//                        conversationId = getConversationId()
//                    )
//                )
//            }
//        }
    }

    fun showSeniorStudentScreen() {
        SeniorStudentActivity.startSeniorStudentActivity(requireActivity())
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            SpeakingPractiseFragment()
    }
}
