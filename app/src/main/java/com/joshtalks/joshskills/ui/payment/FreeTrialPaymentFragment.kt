package com.joshtalks.joshskills.ui.payment

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.abTest.CampaignKeys
import com.joshtalks.joshskills.core.abTest.VariantKeys
import com.joshtalks.joshskills.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.core.analytics.ParamKeys
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.databinding.FragmentFreeTrialPaymentBinding
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.startcourse.TEST_ID
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.tonyodev.fetch2core.isNetworkAvailable
import kotlinx.android.synthetic.main.price_card.view.*
import kotlinx.android.synthetic.main.syllabus_pdf_layout.view.*
import java.io.File

class FreeTrialPaymentFragment : CoreJoshFragment() {
    private lateinit var binding: FragmentFreeTrialPaymentBinding
    private val viewModel: FreeTrialPaymentViewModel by lazy {
        ViewModelProvider(requireActivity()).get(FreeTrialPaymentViewModel::class.java)
    }
    var index = 1
    var expiredTime: Long = -1
    lateinit var fileName: String
    private var downloadID: Long = -1
    lateinit var englishCard: View
    lateinit var subscriptionCard: View
    private var isNewFlowActive = false
    private var isSyllabusActive = false
    private var is100PointsActive = false
    private var currentTime: Long = 0L
    var isPointsScoredMoreThanEqualTo100 = false
    var totalPointsScored: Int? = null
    private var countdownTimerBack: CountdownTimerBack? = null
    private var pdfUrl: String? = null
    lateinit var testId: String
    var buttonText = mutableListOf<String>()
    var headingText = mutableListOf<String>()

    companion object {
        fun newInstance(testId: String): FreeTrialPaymentFragment {
            val args = Bundle()
            args.putString(TEST_ID, testId)
            val fragment = FreeTrialPaymentFragment()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = DataBindingUtil.inflate(
            inflater,
            R.layout.fragment_free_trial_payment,
            container,
            false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (requireActivity().intent.hasExtra(FreeTrialPaymentActivity.EXPIRED_TIME)) {
            expiredTime = requireActivity().intent.getLongExtra(FreeTrialPaymentActivity.EXPIRED_TIME, -1)
        }
        testId = arguments?.getString(TEST_ID, FREE_TRIAL_PAYMENT_TEST_ID) ?: FREE_TRIAL_PAYMENT_TEST_ID
        initABTest()
        viewModel.getPaymentDetails(Integer.parseInt(testId))
        setObservers()
        dynamicCardCreation()
        setListeners()
    }

    private fun setListeners() {
        englishCard.card.setOnClickListener {
            viewModel.event.postValue(Message().apply {
                what = ENGLISH_CARD_TAPPED
            })
            performActionOnEnglishTapped()
        }

        englishCard.iv_expand.setOnClickListener {
            viewModel.event.postValue(Message().apply {
                what = ENGLISH_CARD_TAPPED
            })
            performActionOnEnglishTapped()
        }

        englishCard.iv_minimise.setOnClickListener {
            performActionOntapped(englishCard)
        }

        subscriptionCard.card.setOnClickListener {
            viewModel.event.postValue(Message().apply {
                what = SUBSCRIPTION_CARD_TAPPED
            })
            performActionOnSubscriptionTapped()
            viewModel.event.postValue(Message().apply {
                what = ENABLE_BUY_BUTTON
            })

        }
        subscriptionCard.iv_expand.setOnClickListener {
            viewModel.event.postValue(Message().apply {
                what = SUBSCRIPTION_CARD_TAPPED
            })
            performActionOnSubscriptionTapped()
            viewModel.event.postValue(Message().apply {
                what = ENABLE_BUY_BUTTON
            })
        }

        binding.seeCourseList.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.SEE_COURSE_LIST).push()
            viewModel.event.postValue(Message().apply {
                what = OPEN_COURSE_EXPLORE_ACTIVITY
            })
        }
        subscriptionCard.see_course_list_new.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.SEE_COURSE_LIST).push()
            viewModel.saveImpression(SEE_COURSE_LIST_BUTTON_CLICKED)
            viewModel.event.postValue(Message().apply {
                what = OPEN_COURSE_EXPLORE_ACTIVITY
            })
        }

        subscriptionCard.iv_minimise.setOnClickListener {
            performActionOntapped(subscriptionCard)
            subscriptionCard.see_course_list_new.visibility = View.GONE
            viewModel.event.postValue(Message().apply {
                what = ENABLE_BUY_BUTTON
            })
        }

        englishCard.syllabus_layout_new.english_syllabus_pdf.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.DOWNLOAD_SYLLABUS)
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                .addParam(
                    ParamKeys.COURSE_PRICE,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                )
                .addParam(
                    ParamKeys.COURSE_NAME,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                )
                .push()
            if (requireContext().isNetworkAvailable()) {
                startPdfDownload()
            } else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }

        binding.syllabusLayout.english_syllabus_pdf.setOnClickListener {
            MixPanelTracker.publishEvent(MixPanelEvent.DOWNLOAD_SYLLABUS)
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(0)?.testId)
                .addParam(ParamKeys.COURSE_PRICE, viewModel.paymentDetailsLiveData.value?.courseData?.get(0)?.discount)
                .addParam(ParamKeys.COURSE_NAME, viewModel.paymentDetailsLiveData.value?.courseData?.get(0)?.courseName)
                .push()
            if (requireContext().isNetworkAvailable()) {
                startPdfDownload()
            } else {
                showToast(getString(R.string.internet_not_available_msz))
            }
        }
    }

    private fun setObservers() {
        viewModel.index.observe(viewLifecycleOwner) {
            index = it
        }
        viewModel.paymentDetailsLiveData.observe(viewLifecycleOwner) { it ->
            try {
                buttonText = mutableListOf<String>()
                headingText = mutableListOf<String>()
                it.courseData?.let {
                    val data1 = it[0]
                    data1.buttonText?.let { it1 -> buttonText.add(it1) }
                    data1.heading.let { it1 -> headingText.add(it1) }

                    englishCard.title.text = data1.courseHeading
                    englishCard.txt_currency.text = data1.discount?.get(0).toString()
                    englishCard.txt_final_price.text = data1.discount?.substring(1)
                    englishCard.txt_og_price.text = getString(R.string.price, data1.actualAmount)
                    englishCard.txt_og_price.paintFlags =
                        englishCard.txt_og_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    englishCard.txt_saving.text = getString(R.string.savings, data1.savings)
                    englishCard.course_rating.rating = data1.rating?.toFloat() ?: 4f
                    englishCard.txt_total_reviews.text =
                        "(" + String.format("%,d", data1.ratingsCount) + ")"


                    val data2 = it.getOrNull(1)
                    if (data2 == null) {
                        viewModel.index.value = 0
                        subscriptionCard.visibility = View.GONE
                    } else {
                        subscriptionCard.card.performClick()
                        data2.buttonText?.let { it1 -> buttonText.add(it1) }
                        data2.heading.let { it1 -> headingText.add(it1) }
                        subscriptionCard.title.text = data2.courseHeading
                        TextViewCompat.setTextAppearance(
                            subscriptionCard.title,
                            R.style.TextAppearance_JoshTypography_Body_Text_Small_Semi_Bold
                        )
                        subscriptionCard.title.setTextColor(resources.getColor(R.color.colorPrimary))
                        subscriptionCard.txt_currency.text = data2.discount?.get(0).toString()
                        if (data2.perCoursePrice.isNullOrBlank()) {
                            subscriptionCard.per_course_text.visibility = View.GONE
                        } else {
                            subscriptionCard.per_course_text.visibility = View.VISIBLE
                            subscriptionCard.per_course_text.text = data2.perCoursePrice
                        }
                        subscriptionCard.txt_currency.text = data2.discount?.get(0).toString()
                        subscriptionCard.txt_final_price.text = data2.discount?.substring(1)
                        subscriptionCard.txt_og_price.text =
                            getString(R.string.price, data2.actualAmount)
                        subscriptionCard.txt_og_price.paintFlags =
                            subscriptionCard.txt_og_price.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        subscriptionCard.txt_saving.text =
                            getString(R.string.savings, data2.savings)
                        subscriptionCard.course_rating.rating = data2.rating?.toFloat() ?: 4f
                        subscriptionCard.txt_total_reviews.text =
                            "(" + String.format("%,d", data2.ratingsCount) + ")"
                    }
                    try {
                        viewModel.paymentButtonText.postValue(buttonText.get(index))
                        binding.txtLabelHeading.text = headingText.get(index)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }

                if (isNewFlowActive) {
                    it.combinedMessage?.get(0)?.let { list ->
                        for (i in list.indices) {
                            val englishTextView: AppCompatTextView = createTextViewsDynamically()
                            englishTextView.text = list[i]
                            englishCard.course_info.addView(englishTextView)
                        }
                    }

                    it.combinedMessage?.get(1)?.let { list ->
                        subscriptionCard.course_info.visibility = View.VISIBLE
                        for (i in list.indices) {
                            val subscriptionTextView: AppCompatTextView = createTextViewsDynamically()
                            subscriptionTextView.text = list[i]
                            subscriptionCard.course_info.addView(subscriptionTextView)
                        }
                    }
                    subscriptionCard.card.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_blue_bound_stroke)
                    englishCard.card.elevation = resources.getDimension(R.dimen._6sdp)
                    subscriptionCard.card.elevation = resources.getDimension(R.dimen._6sdp)

                } else {
                    binding.oldViewCourseInfo.visibility = View.VISIBLE
                    binding.seeCourseList.visibility = View.VISIBLE
                    if (PrefManager.getStringValue(CURRENT_COURSE_ID) != DEFAULT_COURSE_ID)
                        binding.seeCourseList.visibility = View.GONE
                    it.subHeadings?.let { list ->
                        for (i in list.indices) {
                            val infoTextView: AppCompatTextView = createTextViewsDynamically()
                            infoTextView.text = list[i]
                            binding.oldViewCourseInfo.addView(infoTextView)
                        }
                    }
                    subscriptionCard.card.background =
                        ContextCompat.getDrawable(
                            requireContext(),
                            R.drawable.blue_rectangle_with_blue_bound_stroke
                        )
                }

                englishCard.card.background =
                    ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_grey_stroke)

                if (it.expireTime != null) {
                    binding.freeTrialTimer.visibility = View.VISIBLE
                    if (it.expireTime.time >= System.currentTimeMillis()) {
                        startTimer(it.expireTime.time - System.currentTimeMillis())
                    } else {
                        binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
                        PrefManager.put(IS_FREE_TRIAL_ENDED, true)
                    }
                } else {
                    binding.freeTrialTimer.visibility = View.GONE
                }
                pdfUrl = it.pdfUrl
                totalPointsScored = it.totalPoints
                if (it.totalPoints > 100) {
                    isPointsScoredMoreThanEqualTo100 = true
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        if (isNewFlowActive) {
            subscriptionCard.iv_minimise.visibility = View.VISIBLE
            englishCard.iv_expand.visibility = View.VISIBLE
            subscriptionCard.see_course_list_new.visibility = View.VISIBLE
            binding.seeCourseList.visibility = View.GONE
        }
        if (!isNewFlowActive && isSyllabusActive) binding.syllabusLayout.visibility = View.VISIBLE

        if (!requireContext().isNetworkAvailable()) {
            binding.freeTrialTimer.visibility = View.INVISIBLE
            binding.txtLabelHeading.visibility = View.INVISIBLE
            binding.oldViewCourseInfo.visibility = View.INVISIBLE
            binding.cardsContainer.visibility = View.INVISIBLE
            binding.syllabusLayout.visibility = View.INVISIBLE
            binding.syllabusLayout.english_syllabus_pdf.visibility = View.INVISIBLE
        }
    }

    private fun initABTest() {
        viewModel.abTestRepository.apply {
            isSyllabusActive = isVariantActive(VariantKeys.ESD_ENABLED)
            isNewFlowActive = isVariantActive(VariantKeys.BUY_LAYOUT_ENABLED)
            is100PointsActive = isVariantActive(VariantKeys.POINTS_HUNDRED_ENABLED)
        }
        if (viewModel.abTestRepository.isVariantActive(VariantKeys.ICP_ENABLED)) {
            viewModel.postGoal("ICP_BUY_PAGE_SEEN", CampaignKeys.INCREASE_COURSE_PRICE.name)
        }
    }

    private fun dynamicCardCreation() {
        val inflater: LayoutInflater =
            requireActivity().getSystemService(AppCompatActivity.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        englishCard = inflater.inflate(R.layout.price_card, null, true)
        subscriptionCard = inflater.inflate(R.layout.price_card, null, true)
        binding.cardsContainer.addView(englishCard)
        binding.cardsContainer.addView(subscriptionCard)
    }

    private fun startPdfDownload() {
        if (pdfUrl.isNullOrBlank().not()) {
            pdfUrl?.let { getPermissionAndDownloadSyllabus(it) }
        } else {
            showToast("Something Went wrong")
        }
    }

    private fun performActionOnEnglishTapped() {
        try {
            viewModel.index.value = 0
            subscriptionCard.card.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_grey_stroke)

            binding.txtLabelHeading.text = headingText[index]
            viewModel.paymentButtonText.postValue(buttonText.get(index))

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (isNewFlowActive) {
            englishCard.card.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_blue_bound_stroke)
            if (isSyllabusActive) englishCard.syllabus_layout_new.visibility = View.VISIBLE
        } else englishCard.card.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.blue_rectangle_with_blue_bound_stroke)

        if (is100PointsActive) {
            if (!isPointsScoredMoreThanEqualTo100 && !PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED)) {
                viewModel.event.postValue(Message().apply {
                    what = DISABLE_BUY_BUTTON
                })
                viewModel.paymentButtonText.postValue(getString(R.string.achieve_100_points_to_buy))
            }
        }


        if (englishCard.iv_expand.visibility == View.VISIBLE) {
            subscriptionCard.iv_expand.visibility = View.VISIBLE
            subscriptionCard.iv_minimise.visibility = View.GONE
            subscriptionCard.course_info.visibility = View.GONE

            englishCard.course_info.visibility = View.VISIBLE
            englishCard.iv_minimise.visibility = View.VISIBLE
            englishCard.iv_expand.visibility = View.GONE

            MixPanelTracker.publishEvent(MixPanelEvent.BUY_PAGE_COURSE_INFO_EXPAND)
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                .addParam(
                    ParamKeys.COURSE_PRICE,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                )
                .addParam(
                    ParamKeys.COURSE_NAME,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                )
                .push()
        } else if (englishCard.iv_minimise.visibility == View.VISIBLE)
            performActionOntapped(englishCard)

        binding.seeCourseList.visibility = View.GONE
        subscriptionCard.see_course_list_new.visibility = View.GONE
    }

    private fun performActionOnSubscriptionTapped() {
        englishCard.syllabus_layout_new.visibility = View.GONE
        try {
            viewModel.index.value = 1
            englishCard.card.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_grey_stroke)
            viewModel.paymentButtonText.postValue(buttonText[index])
            binding.txtLabelHeading.text = headingText[index]
            binding.seeCourseList.visibility = View.VISIBLE
            scrollToBottom()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        if (isNewFlowActive) subscriptionCard.card.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.white_rectangle_with_blue_bound_stroke)
        else subscriptionCard.card.background =
            ContextCompat.getDrawable(requireContext(), R.drawable.blue_rectangle_with_blue_bound_stroke)

        if (subscriptionCard.iv_expand.visibility == View.VISIBLE) {
            englishCard.course_info.visibility = View.GONE
            englishCard.iv_minimise.visibility = View.GONE
            englishCard.iv_expand.visibility = View.VISIBLE
            binding.seeCourseList.visibility = View.GONE

            subscriptionCard.iv_expand.visibility = View.GONE
            subscriptionCard.iv_minimise.visibility = View.VISIBLE
            subscriptionCard.course_info.visibility = View.VISIBLE
            subscriptionCard.see_course_list_new.visibility = View.VISIBLE

            MixPanelTracker.publishEvent(MixPanelEvent.BUY_PAGE_COURSE_INFO_EXPAND)
                .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
                .addParam(
                    ParamKeys.COURSE_PRICE,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount
                )
                .addParam(
                    ParamKeys.COURSE_NAME,
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName
                )
                .push()
        } else if (subscriptionCard.iv_minimise.visibility == View.VISIBLE)
            performActionOntapped(subscriptionCard)
    }

    private fun getPermissionAndDownloadSyllabus(url: String) {
        PermissionUtils.storageReadAndWritePermission(requireActivity(),
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                            downloadDigitalCopy(url)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(requireActivity())
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

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    binding.freeTrialTimer
                    binding.freeTrialTimer.text = getString(
                        R.string.free_trial_end_in,
                        UtilTime.timeFormatted(millis)
                    )
                }
            }

            override fun onTimerFinish() {
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
                binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
            }
        }
        countdownTimerBack?.startTimer()
    }

    private fun downloadDigitalCopy(url: String) {
        registerDownloadReceiver()
        currentTime = System.currentTimeMillis()
        fileName = Utils.getFileNameFromURL(url)
        fileName = fileName.split(".").get(0)
        fileName = fileName + currentTime.toString() + ".pdf"

        val request: DownloadManager.Request =
            DownloadManager.Request(Uri.parse(url))
                .setTitle(getString(R.string.app_name))
                .setDescription("Downloading syllabus")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            request.setRequiresCharging(false)
                .setRequiresDeviceIdle(false)
        }

        val downloadManager =
            AppObjectController.joshApplication.getSystemService(Context.DOWNLOAD_SERVICE) as (DownloadManager)
        downloadID = downloadManager.enqueue(request)
        showToast(getString(R.string.downloading_start))
    }

    fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun performActionOntapped(card: View) {
        card.iv_minimise.visibility = View.GONE
        card.course_info.visibility = View.GONE
        card.iv_expand.visibility = View.VISIBLE
        card.see_course_list_new.visibility = View.GONE
        binding.seeCourseList.visibility = View.GONE
        englishCard.syllabus_layout_new.visibility = View.GONE

        MixPanelTracker.publishEvent(MixPanelEvent.BUY_PAGE_COURSE_INFO_COLLAPSE)
            .addParam(ParamKeys.TEST_ID, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId)
            .addParam(ParamKeys.COURSE_PRICE, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.discount)
            .addParam(ParamKeys.COURSE_NAME, viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName)
            .push()
    }

    private var onDownloadCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                val fileDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + File.separator + fileName
                PdfViewerActivity.startPdfActivity(
                    context = requireActivity(),
                    pdfId = "788900765",
                    courseName = "Course Syllabus",
                    pdfPath = fileDir,
                    conversationId = requireActivity().intent.getStringExtra(CONVERSATION_ID)
                )
                showToast(getString(R.string.downloaded_syllabus))
                viewModel.saveImpression(D2P_COURSE_SYLLABUS_OPENED)
                PrefManager.put(IS_ENGLISH_SYLLABUS_PDF_OPENED, value = true)
            }
        }
    }

    private fun createTextViewsDynamically(): AppCompatTextView {
        val textView = AppCompatTextView(requireContext())
        val drawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_blue_tick_round)
        textView.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
        textView.compoundDrawablePadding = 76
        textView.gravity = Gravity.CENTER or Gravity.START
        textView.setPadding(0, 0, 0, 30)
        TextViewCompat.setTextAppearance(
            textView,
            R.style.TextAppearance_JoshTypography_Body_Text_Small_Regular
        )
        return textView
    }

    private fun registerDownloadReceiver() {
        AppObjectController.joshApplication.registerReceiver(
            onDownloadCompleteListener,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    override fun onDestroyView() {
        try {
            requireActivity().unregisterReceiver(onDownloadCompleteListener)
        } catch (ex: Exception) {
        }
        countdownTimerBack?.stop()
        super.onDestroyView()

    }
}