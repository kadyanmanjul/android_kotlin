package com.joshtalks.joshskills.ui.payment

import android.app.Activity
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
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat.setTextAppearance
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.AppObjectController.Companion.uiHandler
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics.logNewPaymentPageOpened
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.databinding.ActivityFreeTrialPaymentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.PromoCodeSubmitEventBus
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.referral.EnterReferralCodeFragment
import com.joshtalks.joshskills.ui.pdfviewer.PdfViewerActivity
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.voip.CallForceDisconnect
import com.joshtalks.joshskills.ui.voip.IS_DEMO_P2P
import com.joshtalks.joshskills.ui.voip.WebRtcService
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.math.BigDecimal
import java.util.*
import kotlinx.android.synthetic.main.fragment_sign_up_profile_for_free_trial.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.io.File

const val FREE_TRIAL_PAYMENT_TEST_ID = "102"
const val IS_FAKE_CALL = "is_fake_call"

class FreeTrialPaymentActivity : CoreJoshActivity(),
    PaymentResultListener {

    private lateinit var binding: ActivityFreeTrialPaymentBinding
    private val viewModel: FreeTrialPaymentViewModel by lazy {
        ViewModelProvider(this).get(FreeTrialPaymentViewModel::class.java)
    }
    private var razorpayOrderId = EMPTY
    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    var index = 1
    var expiredTime: Long = -1
    var buttonText = mutableListOf<String>()
    var headingText = mutableListOf<String>()
    private var countdownTimerBack: CountdownTimerBack? = null
    private var couponApplied = false
    private var compositeDisposable = CompositeDisposable()
    var isDiscount = false

    var pdfUrl : String?= null
    private var downloadID: Long = -1
    private var isEnglishCardTapped = false
    lateinit var fileName : String
    var isPointsScoredMoreThanEqualTo100 = false

    private var onDownloadCompleteListener = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadID == id) {
                val fileDir = Environment.getExternalStoragePublicDirectory( Environment.DIRECTORY_DOWNLOADS)?.absolutePath + File.separator + fileName
                PdfViewerActivity.startPdfActivity(
                    context = this@FreeTrialPaymentActivity,
                    pdfId = "788900765",
                    courseName = "Course Syllabus",
                    pdfPath = fileDir,
                    conversationId = this@FreeTrialPaymentActivity.intent.getStringExtra(CONVERSATION_ID)
                )
                showToast(getString(R.string.downloaded_syllabus))
                viewModel.saveImpression(D2P_COURSE_SYLLABUS_OPENED)
                PrefManager.put(IS_ENGLISH_SYLLABUS_PDF_OPENED, value = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        binding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_free_trial_payment
        )
        binding.handler = this
        binding.lifecycleOwner = this
        window.statusBarColor = ContextCompat.getColor(this, R.color.black)

        if (intent.hasExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)) {
            testId = intent.getStringExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)
                ?: FREE_TRIAL_PAYMENT_TEST_ID
        }
        if (intent.hasExtra(EXPIRED_TIME)) {
            expiredTime = intent.getLongExtra(EXPIRED_TIME, -1)
        }
        if (intent.hasExtra(IS_FAKE_CALL)) {
            forceDisconnectCall()
            val nameArr = User.getInstance().firstName?.split(" ")
            val firstName = if (nameArr != null) nameArr[0] else EMPTY
            showToast(getString(R.string.feature_locked, firstName), Toast.LENGTH_LONG)
        }
        if (testId.isBlank()) {
            testId = AppObjectController.getFirebaseRemoteConfig()
                .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID)
        }

        setObservers()
        setListeners()
        viewModel.getPaymentDetails(testId.toInt())
        viewModel.getPointsSummary()
        logNewPaymentPageOpened()

        viewModel.getD2pSyllabusPdfData()
        binding.syllabusPdfCard.setOnClickListener {
            if(pdfUrl.isNullOrBlank().not()) {
                getPermissionAndDownloadSyllabus(pdfUrl!!)
            }else{
                showToast("Something Went wrong")
            }
        }
        viewModel.saveImpression(BUY_ENGLISH_COURSE_BUTTON_CLICKED)
    }

    private fun getPermissionAndDownloadSyllabus(url: String) {
        PermissionUtils.storageReadAndWritePermission(this,
            object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    report?.areAllPermissionsGranted()?.let { flag ->
                        if (flag) {
                                downloadDigitalCopy(url)
                            return
                        }
                        if (report.isAnyPermissionPermanentlyDenied) {
                            PermissionUtils.permissionPermanentlyDeniedDialog(this@FreeTrialPaymentActivity)
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

    private fun downloadDigitalCopy(url: String) {
        registerDownloadReceiver()
        fileName = Utils.getFileNameFromURL(url)

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

    private fun registerDownloadReceiver() {
        AppObjectController.joshApplication.registerReceiver(
            onDownloadCompleteListener,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        )
    }

    private fun forceDisconnectCall() {
        val serviceIntent = Intent(
            this,
            WebRtcService::class.java
        ).apply {
            action = CallForceDisconnect().action
            putExtra(IS_FAKE_CALL, true)
        }
        serviceIntent.startServiceForWebrtc()
    }

    private fun setListeners() {
        binding.ivExpand.setOnClickListener {
            try {
                showEnglishButtonTextAndCardDecoration()
                binding.linearLayoutCompatEnglish.visibility = View.VISIBLE
                binding.ivExpand.visibility = View.GONE
                binding.ivMinimise.visibility = View.VISIBLE
                binding.syllabusPdfCard.visibility = View.VISIBLE

                binding.linearLayoutCompatSubscription.visibility = View.GONE
                binding.ivExpand1.visibility = View.VISIBLE
                binding.ivMinimise1.visibility = View.GONE
                binding.seeCourseList.visibility = View.GONE
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        binding.ivMinimise.setOnClickListener {
            binding.linearLayoutCompatEnglish.visibility = View.GONE
            binding.ivExpand.visibility = View.VISIBLE
            binding.ivMinimise.visibility = View.GONE
            binding.syllabusPdfCard.visibility = View.GONE
        }

        binding.ivExpand1.setOnClickListener {
            try {
                index = 1
                binding.subscriptionCard.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.blue_rectangle_with_blue_bound_stroke
                    )
                binding.englishCard.background =
                    ContextCompat.getDrawable(this, R.drawable.white_rectangle_with_grey_stroke)
                binding.materialTextView.text = buttonText.get(index)
                binding.materialTextView.isEnabled = true
                binding.materialTextView.alpha = 1f
                binding.txtLabelHeading.text = headingText.get(index)
                isEnglishCardTapped = false
                scrollToBottom()

                binding.linearLayoutCompatSubscription.visibility = View.VISIBLE
                binding.ivExpand1.visibility = View.GONE
                binding.ivMinimise1.visibility = View.VISIBLE
                binding.seeCourseList.visibility = View.VISIBLE

                binding.linearLayoutCompatEnglish.visibility = View.GONE
                binding.ivExpand.visibility = View.VISIBLE
                binding.ivMinimise.visibility = View.GONE
                binding.syllabusPdfCard.visibility = View.GONE
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        binding.ivMinimise1.setOnClickListener {
            binding.linearLayoutCompatSubscription.visibility = View.GONE
            binding.ivExpand1.visibility = View.VISIBLE
            binding.ivMinimise1.visibility = View.GONE
            binding.seeCourseList.visibility = View.GONE
        }
        binding.ivBack.setOnClickListener {
            onBackPressed()
        }
        binding.englishCard.setOnClickListener {
            try {
                showEnglishButtonTextAndCardDecoration()
                if(binding.ivExpand.visibility == View.VISIBLE){
                    binding.linearLayoutCompatEnglish.visibility = View.VISIBLE
                    binding.ivExpand.visibility = View.GONE
                    binding.ivMinimise.visibility = View.VISIBLE
                    binding.syllabusPdfCard.visibility = View.VISIBLE

                    binding.linearLayoutCompatSubscription.visibility = View.GONE
                    binding.ivExpand1.visibility = View.VISIBLE
                    binding.ivMinimise1.visibility = View.GONE
                    binding.seeCourseList.visibility = View.GONE

                }else if(binding.ivMinimise.visibility == View.VISIBLE){
                    binding.linearLayoutCompatEnglish.visibility = View.GONE
                    binding.ivExpand.visibility = View.VISIBLE
                    binding.ivMinimise.visibility = View.GONE
                    binding.syllabusPdfCard.visibility = View.GONE
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        binding.subscriptionCard.setOnClickListener {
            try {
                index = 1
                binding.subscriptionCard.background =
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.blue_rectangle_with_blue_bound_stroke
                    )
                binding.englishCard.background =
                    ContextCompat.getDrawable(this, R.drawable.white_rectangle_with_grey_stroke)
                binding.materialTextView.text = buttonText.get(index)
                binding.materialTextView.isEnabled = true
                binding.materialTextView.alpha = 1f
                binding.txtLabelHeading.text = headingText.get(index)
                isEnglishCardTapped = false
                scrollToBottom()

                if(binding.ivExpand1.visibility == View.VISIBLE){
                    binding.linearLayoutCompatSubscription.visibility = View.VISIBLE
                    binding.ivExpand1.visibility = View.GONE
                    binding.ivMinimise1.visibility = View.VISIBLE
                    binding.seeCourseList.visibility = View.VISIBLE

                    binding.linearLayoutCompatEnglish.visibility = View.GONE
                    binding.ivExpand.visibility = View.VISIBLE
                    binding.ivMinimise.visibility = View.GONE
                    binding.syllabusPdfCard.visibility = View.GONE

                }else if(binding.ivMinimise1.visibility == View.VISIBLE){
                    binding.linearLayoutCompatSubscription.visibility = View.GONE
                    binding.ivExpand1.visibility = View.VISIBLE
                    binding.ivMinimise1.visibility = View.GONE
                    binding.seeCourseList.visibility = View.GONE
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
        binding.seeCourseList.setOnClickListener {
            viewModel.saveImpression(SEE_COURSE_LIST_BUTTON_CLICKED)
            CourseExploreActivity.startCourseExploreActivity(
                this,
                COURSE_EXPLORER_CODE,
                null,
                state = ActivityEnum.FreeTrial,
                isClickable = false
            )
        }
        binding.applyCoupon.setOnClickListener {
            viewModel.saveImpression(IMPRESSION_CLICKED_APPLY_COUPON)
            val bottomSheetFragment = EnterReferralCodeFragment.newInstance(true)
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(PromoCodeSubmitEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (it.promoCode.isNullOrEmpty().not())
                        showProgressBar()
                    couponApplied = true
                    viewModel.getPaymentDetails(testId.toInt(), it.promoCode!!)
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun showEnglishButtonTextAndCardDecoration(){
            index = 0
            binding.subscriptionCard.background =
                ContextCompat.getDrawable(this, R.drawable.white_rectangle_with_grey_stroke)
            binding.englishCard.background =
                ContextCompat.getDrawable(
                    this,
                    R.drawable.blue_rectangle_with_blue_bound_stroke
                )
            isEnglishCardTapped = true
            if(PrefManager.getBoolValue(IS_FREE_TRIAL_ENDED) == false && isPointsScoredMoreThanEqualTo100 == false){
                binding.materialTextView.text = getString(R.string.achieve_100_points_first)
                binding.materialTextView.isEnabled = false
                binding.materialTextView.alpha = .5f
            }else{
                binding.materialTextView.text = buttonText.get(index)
                binding.materialTextView.isEnabled = true
                binding.materialTextView.alpha = 1f
            }
            binding.txtLabelHeading.text = headingText.get(index)
    }

    override fun onStart() {
        super.onStart()
        binding.subscriptionCard.performClick()
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
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

    fun scrollToBottom() {
        binding.scrollView.post {
            binding.scrollView.fullScroll(View.FOCUS_DOWN)
        }
    }

    private fun setObservers() {
        viewModel.paymentDetailsLiveData.observe(this) {
            binding.englishCard.elevation = resources.getDimension(R.dimen._15sdp)
            binding.subscriptionCard.elevation = resources.getDimension(R.dimen._15sdp)
            try {
                buttonText = mutableListOf<String>()
                headingText = mutableListOf<String>()
                it.subHeadings?.get(0)?.let { list ->
                    for (i in list.indices) {
                        val textViewEnglish = AppCompatTextView(this)
                        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_blue_tick_round)
                        textViewEnglish.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        textViewEnglish.setCompoundDrawablePadding(76)
                        textViewEnglish.gravity = Gravity.CENTER or Gravity.START
                        textViewEnglish.setPadding(0, 0, 0, 30)
                        setTextAppearance(textViewEnglish, R.style.TextAppearance_JoshTypography_Body_Text_Small_Regular)
                        textViewEnglish.text = list[i]
                        binding.linearLayoutCompatEnglish.addView(textViewEnglish)
                    }
                }

                it.subHeadings?.get(1)?.let { list ->
                    for (i in list.indices) {
                        val textViewSubscription = AppCompatTextView(this)
                        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_blue_tick_round)
                        textViewSubscription.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                        textViewSubscription.setCompoundDrawablePadding(76)
                        textViewSubscription.gravity = Gravity.CENTER or Gravity.START
                        textViewSubscription.setPadding(0, 0, 0, 30)
                        setTextAppearance(textViewSubscription, R.style.TextAppearance_JoshTypography_Body_Text_Small_Regular)
                        textViewSubscription.text = list[i]
                        binding.linearLayoutCompatSubscription.addView(textViewSubscription)
                    }
                }

                it.courseData?.let {
                    val data1 = it.get(0)
                    if (data1 != null) {
                        data1.buttonText?.let { it1 -> buttonText.add(it1) }
                        data1.heading.let { it1 -> headingText.add(it1) }

                        binding.title1.text = data1.courseHeading
                        binding.txtCurrency1.text = data1.discount?.get(0).toString()
                        binding.txtFinalPrice1.text = data1.discount?.substring(1)
                        binding.txtOgPrice1.text = getString(R.string.price, data1.actualAmount)
                        binding.txtOgPrice1.paintFlags =
                            binding.txtOgPrice1.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        binding.txtSaving1.text = getString(R.string.savings, data1.savings)
                        binding.courseRating1.rating = data1.rating?.toFloat() ?: 4f
                        binding.txtTotalReviews1.text =
                            "(" + String.format("%,d", data1.ratingsCount) + ")"
                    } else {
                        binding.englishCard.visibility = View.GONE
                    }

                    val data2 = it.get(1)
                    if (data2 != null) {
                        data2.buttonText?.let { it1 -> buttonText.add(it1) }
                        data2.heading.let { it1 -> headingText.add(it1) }
                        binding.title2.text = data2.courseHeading
                        binding.txtCurrency2.text = data2.discount?.get(0).toString()
                        if (data2.perCoursePrice.isNullOrBlank()) {
                            binding.perCourseText.visibility = View.GONE
                        } else {
                            binding.perCourseText.visibility = View.VISIBLE
                            binding.perCourseText.text = data2.perCoursePrice
                        }
                        binding.txtCurrency2.text = data2.discount?.get(0).toString()
                        binding.txtFinalPrice2.text = data2.discount?.substring(1)
                        binding.txtOgPrice2.text = getString(R.string.price, data2.actualAmount)
                        binding.txtOgPrice2.paintFlags =
                            binding.txtOgPrice2.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                        binding.txtSaving2.text = getString(R.string.savings, data2.savings)
                        binding.courseRating2.rating = data2.rating?.toFloat() ?: 4f
                        binding.txtTotalReviews2.text =
                            "(" + String.format("%,d", data2.ratingsCount) + ")"
                    } else {
                        binding.subscriptionCard.visibility = View.GONE
                    }
                    try {
                        binding.materialTextView.text = buttonText.get(index)
                        binding.txtLabelHeading.text = headingText.get(index)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                    }
                }
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
                if (couponApplied) {
                    hideProgressBar()
                    when (it.couponDetails.isPromoCode) {
                        true -> {
                            showToast("Coupon Applied Successfully")
                            viewModel.saveImpression(IMPRESSION_APPLY_COUPON_SUCCESS)
                            binding.discount.text = it.couponDetails.header
                            binding.applyCoupon.text = getString(R.string.coupon_applied)
                            binding.discount.visibility = View.VISIBLE
                            binding.applyCoupon.isClickable = false
                            isDiscount = true
                        }
                        false -> {
                            showToast(getString(R.string.invalid_coupon_code))
                        }
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

        viewModel.orderDetailsLiveData.observe(this) {
            initializeRazorpayPayment(it)
        }

        viewModel.isProcessing.observe(this) { isProcessing ->
            if (isProcessing) {
                binding.progressBar.visibility = View.VISIBLE
            } else {
                binding.progressBar.visibility = View.GONE
                binding.englishCard.elevation = resources.getDimension(R.dimen._15sdp)
                binding.subscriptionCard.elevation = resources.getDimension(R.dimen._15sdp)
            }
        }

        viewModel.d2pSyllabusPdfResponse.observe(this,{
            pdfUrl = it.syllabusPdfLink
        })
        viewModel.pointsHistoryLiveData.observe(this, {
            if(it.totalPoints != null && it.totalPoints >= 100){
                isPointsScoredMoreThanEqualTo100 = true
            }
        })
    }

    private fun initializeRazorpayPayment(orderDetails: OrderDetailResponse) {
        binding.progressBar.visibility = View.VISIBLE
        val checkout = Checkout()
        checkout.setImage(R.mipmap.ic_launcher)
        checkout.setKeyID(orderDetails.razorpayKeyId)
        try {
            val preFill = JSONObject()

            if (User.getInstance().email.isNullOrEmpty().not()) {
                preFill.put("email", User.getInstance().email)
            } else {
                preFill.put("email", Utils.getUserPrimaryEmail(applicationContext))
            }
            //preFill.put("contact", "9999999999")

            val options = JSONObject()
            options.put("key", orderDetails.razorpayKeyId)
            options.put("name", "Josh Skills")
            try {
                options.put(
                    "description",
                    viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName + "_app"
                )
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            options.put("order_id", orderDetails.razorpayOrderId)
            options.put("currency", orderDetails.currency)
            options.put("amount", orderDetails.amount * 100)
            options.put("prefill", preFill)
            checkout.open(this, options)
            razorpayOrderId = orderDetails.razorpayOrderId
            binding.progressBar.visibility = View.GONE
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startPayment() {
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        binding.englishCard.elevation = 0f
        binding.subscriptionCard.elevation = 0f

        var phoneNumber = getPhoneNumber()
        if (phoneNumber.isEmpty()) {
            phoneNumber = "+919999999999"
        }

        viewModel.getOrderDetails(
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.testId ?: testId,
            phoneNumber,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.encryptedText ?: EMPTY
        )
    }

    private fun String.verifyPayment() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val data = mapOf("razorpay_order_id" to this@verifyPayment)
                AppObjectController.commonNetworkService.verifyPayment(data)
            } catch (ex: HttpException) {
                ex.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPaymentError(p0: Int, p1: String?) {
        // isBackPressDisabled = true
        viewModel.mentorPaymentStatus.observe(this, {
            when(it) {
                true ->{
                    if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
                        PrefManager.put(IS_DEMO_P2P, false)
                    }
                    val freeTrialTestId = AppObjectController.getFirebaseRemoteConfig()
                        .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID)
                    if (testId == freeTrialTestId) {
                        PrefManager.put(IS_COURSE_BOUGHT, true)
                    }
                    // isBackPressDisabled = true
                    razorpayOrderId.verifyPayment()
                    MarketingAnalytics.coursePurchased(BigDecimal(viewModel.orderDetailsLiveData.value?.amount ?: 0.0))
                    //viewModel.updateSubscriptionStatus()

                    uiHandler.post {
                        PrefManager.put(IS_PAYMENT_DONE, true)
                        showPaymentProcessingFragment()
                    }

                    uiHandler.postDelayed({
                        navigateToStartCourseActivity()
                    }, 1000L * 5L)
                }
                false -> {
                    uiHandler.post {
                        showPaymentFailedDialog()
                    }
                }
            }
        })

    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
        if (isDiscount) {
            viewModel.saveImpression(IMPRESSION_PAY_DISCOUNT)
        } else {
            viewModel.saveImpression(IMPRESSION_PAY_FULL_FEES)
        }
        if (PrefManager.getBoolValue(IS_DEMO_P2P, defValue = false)) {
            PrefManager.put(IS_DEMO_P2P, false)
        }
        val freeTrialTestId = AppObjectController.getFirebaseRemoteConfig()
            .getString(FirebaseRemoteConfigKey.FREE_TRIAL_PAYMENT_TEST_ID)
        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
            if(isEnglishCardTapped && PrefManager.getBoolValue(IS_ENGLISH_SYLLABUS_PDF_OPENED)){
                viewModel.saveImpression(SYLLABUS_OPENED_AND_ENGLISH_COURSE_BOUGHT)
            }
            if(isEnglishCardTapped && isPointsScoredMoreThanEqualTo100){
                viewModel.saveImpression(POINTS_100_OBTAINED_ENGLISH_COURSE_BOUGHT)
            }
        }
        // isBackPressDisabled = true
        razorpayOrderId.verifyPayment()
        MarketingAnalytics.coursePurchased(
            BigDecimal(
                viewModel.orderDetailsLiveData.value?.amount ?: 0.0
            )
        )
        //viewModel.updateSubscriptionStatus()

        uiHandler.post {
            PrefManager.put(IS_PAYMENT_DONE, true)
            showPaymentProcessingFragment()
        }

        uiHandler.postDelayed({
            navigateToStartCourseActivity()
        }, 1000L * 5L)
    }

    private fun showPaymentProcessingFragment() {
        // binding.container.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentProcessingFragment.newInstance(),
                "Payment Processing"
            )
            .commitAllowingStateLoss()
    }

    private fun showPaymentFailedDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                PaymentFailedDialogFragment.newInstance(
                    viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
                ),
                "Payment Success"
            )
            .commitAllowingStateLoss()
    }

    private fun navigateToStartCourseActivity() {
        StartCourseActivity.openStartCourseActivity(
            this,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.courseName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.teacherName ?: EMPTY,
            viewModel.paymentDetailsLiveData.value?.courseData?.get(index)?.imageUrl ?: EMPTY,
            viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
        )
        this.finish()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    override fun onDestroy() {
        try {
            this.unregisterReceiver(onDownloadCompleteListener)
        } catch (ex: Exception) {
        }
        super.onDestroy()
        countdownTimerBack?.stop()
    }

    companion object {
        const val EXPIRED_TIME = "expired_time"

        fun startFreeTrialPaymentActivity(
            activity: Activity,
            testId: String,
            expiredTime: Long? = null
        ) {
            Intent(activity, FreeTrialPaymentActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(EXPIRED_TIME, expiredTime)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(
                    R.anim.slide_up_dialog,
                    R.anim.slide_out_top
                )
            }
        }

        fun getFreeTrialPaymentActivityIntent(
            context: Context,
            testId: String,
            expiredTime: Long? = null,
            isFakeCall: Boolean = false
        ) =
            Intent(context, FreeTrialPaymentActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(EXPIRED_TIME, expiredTime)
                if (isFakeCall) {
                    putExtra(IS_FAKE_CALL, isFakeCall)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

    }
}
