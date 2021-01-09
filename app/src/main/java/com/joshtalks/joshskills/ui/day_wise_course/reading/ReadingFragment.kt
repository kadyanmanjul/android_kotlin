package com.joshtalks.joshskills.ui.day_wise_course.reading

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.text.Spannable
import android.text.SpannableString
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.github.razir.progressbutton.DrawableButton
import com.github.razir.progressbutton.hideProgress
import com.github.razir.progressbutton.showProgress
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.custom_ui.custom_textview.AutoLinkMode
import com.joshtalks.joshskills.core.custom_ui.recorder.OnAudioRecordListener
import com.joshtalks.joshskills.core.custom_ui.recorder.RecordingItem
import com.joshtalks.joshskills.core.io.AppDirectory
import com.joshtalks.joshskills.databinding.ReadingPracticeFragmentBinding
import com.joshtalks.joshskills.repository.local.entity.*
import com.joshtalks.joshskills.ui.day_wise_course.CapsuleActivityCallback
import com.joshtalks.joshskills.ui.practise.PracticeViewModel
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.muddzdev.styleabletoast.StyleableToast
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_reminder.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class ReadingFragment : CoreJoshFragment(), OnAudioRecordListener, OnWordClick {

    companion object {
        const val PRACTISE_OBJECT = "practise_object"

        @JvmStatic
        fun instance(chatModelList: ArrayList<ChatModel>) = ReadingFragment().apply {
            arguments = Bundle().apply {
                putParcelableArrayList(PRACTISE_OBJECT, chatModelList)
            }
        }
    }

    private var compositeDisposable = CompositeDisposable()
    private lateinit var binding: ReadingPracticeFragmentBinding
    private var chatModel: ChatModel? = null
    private var chatList: ArrayList<ChatModel>? = null
    private var isRecording = false
    private var startTime: Long = 0
    private var filePath: String? = null
    var activityCallback: CapsuleActivityCallback? = null

    val separatorRegex = "<a>([\\s\\S]*?)<\\/a>"

    private val defaultSelectedColor = Color.LTGRAY

    private val selectedColor = Color.BLUE

    private val practiceViewModel: PracticeViewModel by lazy {
        ViewModelProvider(this).get(PracticeViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            arguments?.let {
                chatList = it.getParcelableArrayList<ChatModel>(PRACTISE_OBJECT)
            }
        }
        chatModel = chatList?.getOrNull(0)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.reading_practice_fragment,
                container,
                false
        )

        binding.lifecycleOwner = this
        binding.handler = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        setUpAudioRecordTouchListener()
        addObserver()
        chatModel?.question?.run {
            coreJoshActivity?.feedbackEngagementStatus(this)
        }
    }

    private fun initView() {
        chatModel?.question?.run {
            binding.txtReadingParagraph.addAutoLinkMode(AutoLinkMode.MODE_CUSTOM)

            val sourString = "I <a>promise</a> to <a>improve 114 *^& 8s</a> my English with JOSH I will complete my practice everyday. cow always gives milk" +
                    "↵\n" +
                    "↵I will not make any <a>excuses</a>.\n" +
                    "↵\n" +
                    "↵- English will"

            binding.txtReadingParagraph.enableUnderLine()
            binding.txtReadingParagraph.setCustomRegex(separatorRegex)
            binding.txtReadingParagraph.text = getSpannableString(sourString)

            audioList?.getOrNull(0)?.let {
                binding.readingAudioNote.initAudioPlayer(it.audio_url, it.duration)
            }
        }
        binding.txtReadingParagraph.setAutoLinkOnClickListener { autoLinkMode, matchedText ->

        }
    }

    private fun getSpannableString(string: String): SpannableString {
        var sourString = string
        val pattern: Pattern = Pattern.compile(separatorRegex)
        val splitted = ArrayList<String>()
        val matcher = pattern.matcher(sourString)
        while (matcher.find()) {
            splitted.add(matcher.group())
        }

        sourString = sourString.replace("<a>", "")
        sourString = sourString.replace("</a>", "")

        val generatedSpanString = SpannableString(sourString)

        splitted.forEach { s ->
            val word = s.removePrefix("<a>").removeSuffix("</a>")
            val index = sourString.indexOf(word)
            generatedSpanString.setSpan(getTouchableSpannable(s.removePrefix("<a>").removeSuffix("</a>"), selectedColor,
                    defaultSelectedColor, true, this@ReadingFragment), index, index + word.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        return generatedSpanString
    }

    private fun setUpAudioRecordTouchListener() {
        binding.imgRecordButton.setOnClickListener {
            if (isRecording) {
                stopAudioRecording()
            } else {
                if (PermissionUtils.isAudioAndStoragePermissionEnable(requireContext()).not()) {
                    requestAudioRecord()
                    return@setOnClickListener
                }
                startAudioRecording()
            }
        }
    }

    private fun requestAudioRecord() {
        PermissionUtils.audioRecordStorageReadAndWritePermission(
                requireActivity(),
                object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        report?.areAllPermissionsGranted()?.let { flag ->
                            if (flag) {
                                startAudioRecording()
                                return
                            }
                            if (report.isAnyPermissionPermanentlyDenied) {
                                PermissionUtils.permissionPermanentlyDeniedDialog(
                                        requireActivity(),
                                        R.string.record_permission_message
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

    private fun startAudioRecording() {
        binding.rootView.requestDisallowInterceptTouchEvent(true)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        practiceViewModel.startRecordAudio(this)
    }

    private fun stopAudioRecording() {
        practiceViewModel.stopRecordingAudio(false)
    }


    override fun onRecordingStarted() {
        binding.counterTv.visibility = View.VISIBLE
        isRecording = true
        binding.counterTv.base = SystemClock.elapsedRealtime()
        startTime = System.currentTimeMillis()
        binding.counterTv.start()
        binding.txtCaptionRecord.text = getString(R.string.recording_stop)
    }

    override fun onRecordFinished(recordingItem: RecordingItem?) {
        isRecording = false
        binding.txtCaptionRecord.text = getString(R.string.recording_start)
        binding.rootView.requestDisallowInterceptTouchEvent(false)
        binding.counterTv.stop()
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val timeDifference =
                TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - TimeUnit.MILLISECONDS.toSeconds(
                        startTime
                )
        if (timeDifference > 1) {
            practiceViewModel.recordFile?.let {
                isRecording = true
                filePath = AppDirectory.getAudioSentFile(null).absolutePath
                AppDirectory.copy(it.absolutePath, filePath!!)
                afterRecordCompleteUI()
            }
        } else {
            practiceViewModel.recordFile?.let {
                AppDirectory.deleteFile(it.absolutePath)
            }
            showUIUserRecordingFailed()
        }
    }

    override fun onError(errorCode: Int) {
    }

    private fun showUIUserRecordingFailed() {
        CoroutineScope(Dispatchers.Main).launch {
        }
    }

    private fun afterRecordCompleteUI() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.counterTv.visibility = View.GONE
            binding.groupRecordView.visibility = View.GONE
            binding.btnSubmitButton.visibility = View.VISIBLE
            binding.cardViewAnswerVoiceNote.visibility = View.VISIBLE
            filePath?.let {
                binding.submitAudioNote.initAudioPlayer(
                        it,
                        Utils.getDurationOfMedia(requireActivity(), filePath)?.toInt() ?: 0
                )
            }

        }
    }


    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }


    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun subscribeRXBus() {
    }


    private fun addObserver() {
        practiceViewModel.requestStatusLiveData.observe(viewLifecycleOwner, Observer {
        })

        practiceViewModel.practiceFeedback2LiveData.observe(viewLifecycleOwner, Observer {
        })

        practiceViewModel.practiceEngagementData.observe(viewLifecycleOwner, Observer {
        })
    }


    fun playPracticeAudio() {
        if (Utils.getCurrentMediaVolume(AppObjectController.joshApplication) <= 0) {
            StyleableToast.Builder(AppObjectController.joshApplication).gravity(Gravity.BOTTOM)
                    .text(getString(R.string.volume_up_message)).cornerRadius(16)
                    .length(Toast.LENGTH_LONG)
                    .solidBackground().show()
        }
    }


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is CapsuleActivityCallback)
            activityCallback = context
    }

    fun onReadingContinueClick() {
        activityCallback?.onNextTabCall(3)
    }

    /** Start  View onclicks function **/
    fun cancelAudio() {
        try {
            binding.counterTv.visibility = View.GONE
            binding.groupRecordView.visibility = View.VISIBLE
            binding.cardViewAnswerVoiceNote.visibility = View.GONE
            AppDirectory.deleteFile(filePath)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun submitAnswer() {
        startSubmitProgress()
        chatModel?.question?.questionId?.let {
            practiceViewModel.submitReadingPractise(it, filePath!!)
        }
    }

    private fun startSubmitProgress() {
        binding.btnSubmitButton.showProgress {
            progressColors =
                    intArrayOf(ContextCompat.getColor(requireContext(), R.color.text_color_10))
            gravity = DrawableButton.GRAVITY_CENTER
            progressRadiusRes = R.dimen.dp8
            progressStrokeRes = R.dimen.dp2
            textMarginRes = R.dimen.dp8
        }
        binding.btnSubmitButton.isEnabled = false

    }


    private fun hideProgress() {
        binding.btnSubmitButton.isEnabled = true
        binding.btnSubmitButton.hideProgress(getString(R.string.submit_answer))
    }

    override fun clickedWord(word: String) {
        showToast(word)
    }


    /**   end **/


}
