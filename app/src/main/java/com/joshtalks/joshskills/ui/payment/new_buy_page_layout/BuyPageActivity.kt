package com.joshtalks.joshskills.ui.payment.new_buy_page_layout

import `in`.juspay.services.HyperServices
import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.commit
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.constants.PAYMENT_FAILED
import com.joshtalks.joshskills.constants.PAYMENT_PENDING
import com.joshtalks.joshskills.constants.PAYMENT_SUCCESS
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.FirebaseRemoteConfigKey.Companion.BUY_PAGE_SUPPORT_PHONE_NUMBER
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.core.analytics.MarketingAnalytics
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.custom_ui.JoshRatingBar
import com.joshtalks.joshskills.core.notification.HAS_NOTIFICATION
import com.joshtalks.joshskills.core.notification.NotificationCategory
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.databinding.ActivityBuyPageBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.PaymentFailedDialogNew
import com.joshtalks.joshskills.ui.payment.PaymentInProcessFragment
import com.joshtalks.joshskills.ui.payment.PaymentPendingFragment
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment.CouponCardFragment
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment.RatingAndReviewFragment
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.BuyCourseFeatureModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.paymentManager.PaymentGatewayListener
import com.joshtalks.joshskills.ui.paymentManager.PaymentManager
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.utils.*
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.termsandconditions.WebViewFragment
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.joshtalks.joshskills.voip.Utils.Companion.onMultipleBackPress
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.math.BigDecimal

const val FREE_TRIAL_PAYMENT_TEST_ID = "102"
const val SUBSCRIPTION_TEST_ID = "10"
const val IS_FAKE_CALL = "is_fake_call"

class BuyPageActivity : BaseActivity(), PaymentGatewayListener {

    var englishCourseCard: View? = null
    var otherCourseCard: View? = null
    var teacherDetailsCard: View? = null
    var teacherRatingAndReviewCard: View? = null
    var proceedButtonCard: View? = null
    var clickRatingOpen: ImageView? = null
    var courseDescListCard: View? = null
    var priceForPaymentProceed: CourseDetailsList? = null
    private val hyperInstance by lazy { HyperServices(this) }
    var isPaymentInitiated = false

    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    var expiredTime: Long = -1
    private var flowFrom: String = EMPTY

    private var countdownTimerBack: CountdownTimerBack? = null

    private val binding by lazy<ActivityBuyPageBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_buy_page)
    }

    private var errorView: Stub<ErrorView>? = null
    private var errorShowCount = 0
    private val viewModel by lazy {
        ViewModelProvider(this)[BuyPageViewModel::class.java]
    }
    private val backPressMutex = Mutex(false)

    private val paymentManager: PaymentManager by lazy {
        PaymentManager(
            this,
            viewModel.viewModelScope,
            this
        )
    }

    var openVideoPlayerActivity: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getLongExtra(
                CURRENT_VIDEO_PROGRESS_POSITION,
                0
            )?.let { progress ->
                binding.videoView.progress = progress
                binding.videoView.onResume()
            }
        }
    }

    override fun getArguments() {
        super.getArguments()
        if (intent.hasExtra(HAS_NOTIFICATION)) {
            flowFrom = "NOTIFICATION"
            if (!PrefManager.getBoolValue(IS_FREE_TRIAL))
                finish()
        }
        if (intent.hasExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)) {
            testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
            } else {
                PrefManager.getStringValue(PAID_COURSE_TEST_ID)
            }
        }
        flowFrom = intent.getStringExtra(FLOW_FROM) ?: EMPTY
        Log.d("BuyPageActivity.kt", "SAGAR => getArguments:120 $testId")

        if (intent.hasExtra(IS_FAKE_CALL)) {
            val nameArr = User.getInstance().firstName?.split(" ")
            val firstName = if (nameArr != null) nameArr[0] else EMPTY
            showToast(getString(R.string.feature_locked, firstName), Toast.LENGTH_LONG)
        }
        if (testId.isBlank()) {
            testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
            } else {
                PrefManager.getStringValue(PAID_COURSE_TEST_ID)
            }
        }
        Log.d("BuyPageActivity.kt", "SAGAR => getArguments:97 $testId")
    }

    override fun initViewBinding() {
        binding.vm = viewModel
        binding.executePendingBindings()
        paymentManager.initializePaymentGateway()
    }

    override fun onCreated() {
        viewModel.testId = testId

        errorView = Stub(findViewById(R.id.error_view))
        addObserver()
        if (Utils.isInternetAvailable()) {
            viewModel.getCourseContent()
            viewModel.getCoursePriceList(null)
            viewModel.getValidCouponList(OFFERS)
            errorView?.resolved()?.let {
                errorView!!.get().onSuccess()
            }

        } else {
            showErrorView()
        }

        viewModel.saveImpressionForBuyPageLayout(OPEN_BUY_PAGE_LAYOUT, flowFrom)
        NotificationUtils(this).updateNotificationDb(NotificationCategory.AFTER_BUY_PAGE)
        MarketingAnalytics.openPreCheckoutPage()
        initToolbar()
    }

    override fun initViewState() {
        event.observe(this) {
            when (it.what) {
                OPEN_COUPON_LIST_SCREEN -> {
                    openCouponList()
                }
                BUY_COURSE_LAYOUT_DATA -> {
                    try {
                        dynamicCardCreation(it.obj as BuyCourseFeatureModel)
                        paymentButton()
                        clickRatingOpen?.setOnClickListener {
                            openRatingAndReviewScreen()
                        }
                        setFreeTrialTimer(it.obj as BuyCourseFeatureModel)
                    } catch (ex: Exception) {

                    }
                }
                CLICK_ON_PRICE_CARD -> {
                    setCoursePrices(it.obj as CourseDetailsList, it.arg1)
                }
                CLICK_ON_COUPON_APPLY -> {
                    val coupon = it.obj as Coupon
                    updateListItem(coupon)
                    couponApplied(coupon)
                }
                CLOSE_SAMPLE_VIDEO -> closeIntroVideoPopUpUi()
                OPEN_COURSE_EXPLORE -> openCourseExplorerActivity()
                MAKE_PHONE_CALL -> makePhoneCall()
                BUY_PAGE_BACK_PRESS -> popBackStack()
                APPLY_COUPON_BUTTON_SHOW -> showApplyButton()
                COUPON_APPLIED -> couponApplied(it.obj as Coupon)
                SCROLL_TO_BOTTOM -> binding.btnCallUs.post {
                    binding.scrollView.smoothScrollTo(
                        binding.buyPageParentContainer.width,
                        binding.buyPageParentContainer.height,
                        2000
                    )
                }
                PAYMENT_SUCCESS -> onPaymentSuccess()
                PAYMENT_FAILED -> showPaymentFailedDialog()
                PAYMENT_PENDING -> showPendingDialog()
            }
        }
    }

    fun addObserver() {
        viewModel.apiStatus.observe(this) {
            when (it) {
                ApiCallStatus.SUCCESS -> {
                    errorView?.resolved()?.let {
                        errorView!!.get().onSuccess()
                    }
                }
                else -> {}
            }
        }
    }

    fun couponApplied(coupon: Coupon) {
        showToast("Coupon applied")
        onBackPressed()
        onCouponApply(coupon)
    }

    private fun setFreeTrialTimer(buyCourseFeatureModel: BuyCourseFeatureModel) {
        if (buyCourseFeatureModel.expiryTime != null) {
            if (buyCourseFeatureModel.expiryTime?.time ?: 0 >= System.currentTimeMillis()) {
                if (buyCourseFeatureModel.expiryTime?.time ?: 0 > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                    binding.freeTrialTimer.visibility = View.GONE
                } else {
                    startTimer(
                        (buyCourseFeatureModel.expiryTime?.time ?: 0) - System.currentTimeMillis()
                    )
                }
            } else {
                binding.freeTrialTimer.visibility = View.VISIBLE
                binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            }
        } else {
            binding.freeTrialTimer.visibility = View.GONE
        }
    }

    private fun makePhoneCall() {
        viewModel.saveImpressionForBuyPageLayout(BUY_PAGE_CALL_CLICKED)
        Utils.call(
            this@BuyPageActivity,
            AppObjectController.getFirebaseRemoteConfig().getString(BUY_PAGE_SUPPORT_PHONE_NUMBER)
        )
    }

    private fun updateListItem(coupon: Coupon) {
        viewModel.applyCoupon(coupon)
        Log.e("Sagar", "updateListItem: ${viewModel.couponList}")
    }

    private fun setCoursePrices(list: CourseDetailsList, position: Int) {
        priceForPaymentProceed = list
    }

    private fun openCouponList() {
        viewModel.saveImpressionForBuyPageLayout(OPEN_COUPON_PAGE)
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.buy_page_parent_container, CouponCardFragment(), "CouponCardFragment")
            addToBackStack("CouponCardFragment")
        }
    }

    private fun openRatingAndReviewScreen() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.buy_page_parent_container,
                RatingAndReviewFragment.newInstance(testId.toInt()),
                COURSE_CONTENT
            )
            addToBackStack(COURSE_CONTENT)
        }
    }

    private fun dynamicCardCreation(buyCourseFeatureModel: BuyCourseFeatureModel) {
        binding.shimmer1.visibility = View.GONE
        binding.shimmer1.stopShimmer()
        binding.shimmer1Layout.visibility = View.VISIBLE
        binding.shimmer2.visibility = View.GONE
        binding.shimmer2.stopShimmer()
        binding.shimmer2Layout.visibility = View.VISIBLE
        val courseDetailsInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (buyCourseFeatureModel.teacherName == null && buyCourseFeatureModel.teacherImage == null && buyCourseFeatureModel.video == null && buyCourseFeatureModel.youtubeChannel == null) {
            englishCourseCard =
                courseDetailsInflate.inflate(R.layout.english_course_card, null, true)
            binding.courseTypeContainer.removeAllViews()
            val image = englishCourseCard?.findViewById<ImageView>(R.id.img_skill_logo)
            image?.setImage(buyCourseFeatureModel.otherCourseImage?: EMPTY)
            val text = englishCourseCard?.findViewById<TextView>(R.id.sample_text)
            text?.text = buyCourseFeatureModel.courseName
            binding.courseTypeContainer.addView(englishCourseCard)
            viewModel.isGovernmentCourse.set(false)
        } else {
            viewModel.isGovernmentCourse.set(true)
            otherCourseCard = courseDetailsInflate.inflate(R.layout.other_course_card, null, true)
            binding.courseTypeContainer.removeAllViews()
            binding.courseTypeContainer.addView(otherCourseCard)
            val teacherVideoButton =
                otherCourseCard?.findViewById<RelativeLayout>(R.id.play_video_button)
            teacherVideoButton?.setOnClickListener {
                viewModel.saveImpressionForBuyPageLayout(PLAY_SAMPLE_VIDEO)
                playSampleVideo(buyCourseFeatureModel.video ?: EMPTY)
            }

            teacherDetailsCard =
                courseDetailsInflate.inflate(R.layout.teacher_details_card, null, true)
            binding.teacherDetails.removeAllViews()
            binding.teacherDetails.addView(teacherDetailsCard)

            val name = teacherDetailsCard?.findViewById<TextView>(R.id.teacher_name)
            name?.text = buyCourseFeatureModel.teacherName

            val image = teacherDetailsCard?.findViewById<CircleImageView>(R.id.teacher_image)
            image?.setUserImageOrInitials(
                buyCourseFeatureModel.sumanProfile,
                buyCourseFeatureModel.teacherName ?: EMPTY,
                isRound = true
            )

            val teacherDesc = teacherDetailsCard?.findViewById<TextView>(R.id.desc_about_teacher)
            teacherDesc?.text = buyCourseFeatureModel.teacherDesc

            val youtubeLink = teacherDetailsCard?.findViewById<TextView>(R.id.youtube_link)
            youtubeLink?.text = buyCourseFeatureModel.youtubeChannel
            youtubeLink?.paintFlags = youtubeLink?.paintFlags?.or(Paint.UNDERLINE_TEXT_FLAG)!!


            youtubeLink.setOnClickListener {
                try {
                    viewModel.saveImpressionForBuyPageLayout(YOUTUBE_LINK_CLICK)
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(buyCourseFeatureModel.youtubeLink)
                    intent.setPackage("com.google.android.youtube")
                    startActivity(intent)
                } catch (e: Exception) {
                    showToast("You don't have youtube")
                }
            }
        }

        setRating(buyCourseFeatureModel)

        buyCourseFeatureModel.information?.forEach { it ->
            val view = getCourseDescriptionList(it)
            if (view != null) {
                binding.courseDescList.addView(view)
            }
        }
    }

    private fun getCourseDescriptionList(buyCourseFeatureModel: String): View? {
        val courseDescListInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        courseDescListCard =
            courseDescListInflate.inflate(R.layout.item_course_description, null, true)

        val courseDesc = courseDescListCard?.findViewById<TextView>(R.id.course_desc)
        courseDesc?.text = buyCourseFeatureModel
        return courseDescListCard
    }

    private fun setRating(buyCourseFeatureModel: BuyCourseFeatureModel) {
        binding.shimmer3.visibility = View.GONE
        binding.shimmer3.stopShimmer()
        binding.shimmer3Layout.visibility = View.VISIBLE
        val ratingInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        teacherRatingAndReviewCard =
            ratingInflate.inflate(R.layout.teacher_rating_and_review_card, null, true)
        binding.teacherRatingAndReview.removeAllViews()
        binding.teacherRatingAndReview.addView(teacherRatingAndReviewCard)

        val ratingCount = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_count)
        ratingCount?.text = buyCourseFeatureModel.ratingCount.toString() + " Reviews"

        clickRatingOpen = teacherRatingAndReviewCard?.findViewById(R.id.btn_ration_click)

        val teacherRating =
            teacherRatingAndReviewCard?.findViewById<JoshRatingBar>(R.id.teacher_rating)
        teacherRating?.rating = buyCourseFeatureModel.rating ?: 0.0f

        val ratingInText = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_in_text)
        ratingInText?.text = buyCourseFeatureModel.rating.toString() + " out of 5"
    }

    private fun paymentButton() {
        val paymentInflate: LayoutInflater =
            getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        proceedButtonCard = paymentInflate.inflate(R.layout.payment_button_card, null, true)
        val paymentButton = proceedButtonCard?.findViewById<MaterialButton>(R.id.btn_payment_course)
        binding.paymentProceedBtnCard.removeAllViews()
        binding.paymentProceedBtnCard.addView(proceedButtonCard)
        paymentButton?.setOnSingleClickListener {
            isPaymentInitiated = true
            viewModel.saveImpressionForBuyPageLayout(PROCEED_PAYMENT_CLICK, testId)
            startPayment()
        }
        proceedButtonCard?.findViewById<MaterialTextView>(R.id.text_view_privacy)
            ?.setOnSingleClickListener {
                showPrivacyPolicyDialog()
            }
    }

    fun startPayment() {
        if (Utils.isInternetAvailable().not()) {
            showToast(getString(R.string.internet_not_available_msz))
            return
        }

        var phoneNumber = getPhoneNumber()
        if (phoneNumber.isEmpty()) {
            phoneNumber = "+919999999999"
        }
        NotificationUtils(applicationContext).removeScheduledNotification(NotificationCategory.AFTER_BUY_PAGE)
        NotificationUtils(applicationContext).updateNotificationDb(NotificationCategory.PAYMENT_INITIATED)
        try {
            paymentManager.createOrder(priceForPaymentProceed?.testId ?: EMPTY,
                phoneNumber,
                priceForPaymentProceed?.encryptedText ?: EMPTY)
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
    }

    private fun showPaymentFailedDialog() {
        try {
            viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace(
                R.id.buy_page_parent_container,
                PaymentFailedDialogNew.newInstance(paymentManager) {
                      onBackPressed()
                },
                "Payment Failed"
            )
            disallowAddToBackStack()
        }
    }

    private fun navigateToStartCourseActivity() {
        try {
            StartCourseActivity.openStartCourseActivity(
                this,
                priceForPaymentProceed?.courseName ?: EMPTY,
                priceForPaymentProceed?.teacherName ?: EMPTY,
                priceForPaymentProceed?.imageUrl ?: EMPTY,
                paymentManager.getJoshTalksId(),
                priceForPaymentProceed?.testId ?: EMPTY,
                priceForPaymentProceed?.discountedPrice ?: EMPTY
            )
            this.finish()
        } catch (ex: Exception) {
            com.joshtalks.joshskills.core.showToast(getString(R.string.something_went_wrong))
            ex.printStackTrace()
        }
    }

    companion object {
        fun startBuyPageActivity(
            activity: Activity,
            testId: String,
            flowFrom: String
        ) {
            Intent(activity, BuyPageActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(FLOW_FROM, flowFrom)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.run {
                activity.startActivity(this)
                activity.overridePendingTransition(
                    R.anim.slide_up_dialog,
                    R.anim.slide_out_top
                )
            }
        }
    }

    fun playSampleVideo(videoUrl: String) {
        val currentVideoProgressPosition = binding.videoView.progress
        openVideoPlayerActivity.launch(
            VideoPlayerActivity.getActivityIntent(
                this,
                EMPTY,
                null,
                videoUrl,
                currentVideoProgressPosition,
                conversationId = getConversationId()
            )
        )
    }

    private fun closeIntroVideoPopUpUi() {
        viewModel.isVideoPopUpShow.set(false)
        binding.videoView.onStop()
    }

    fun getConversationId(): String? {
        return null
    }

    private fun openCourseExplorerActivity() {
        viewModel.saveImpressionForBuyPageLayout(OPEN_COURSE_EXPLORER_SCREEN)
        CourseExploreActivity.startCourseExploreActivity(
            this,
            COURSE_EXPLORER_CODE,
            null,
            state = com.joshtalks.joshskills.core.BaseActivity.ActivityEnum.BuyPage,
            isClickable = false
        )
    }

    private fun initToolbar() {
        //balanceTv = findViewById<TextView>(R.id.iv_earn)
        with(findViewById<View>(R.id.iv_back)) {
            visibility = View.VISIBLE
            setOnClickListener {
                popBackStack()
            }
        }
    }

    private fun showApplyButton() {
        with(findViewById<View>(R.id.btn_apply_coupon)) {
            isVisible = !viewModel.isOfferOrInsertCodeVisible.get()
            setOnClickListener {
                openCouponList()
            }
        }
    }

    private fun popBackStack() {
        try {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
            } else {
                onBackPressed()
            }
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        }
    }

    fun onCouponApply(coupon: Coupon) {
        val dialogView = showCustomDialog(R.layout.coupon_applied_alert_dialog)
        val btnGotIt = dialogView.findViewById<AppCompatTextView>(R.id.got_it_button)
        dialogView.findViewById<TextView>(R.id.coupon_name_text).text =
            coupon.couponCode + " applied"
        dialogView.findViewById<TextView>(R.id.coupon_price_text).text =
            "You saved upto ₹" + coupon.maxDiscountAmount.toString()

        btnGotIt.setOnClickListener {
            dialogView.dismiss()
        }
    }

    private fun showCustomDialog(view: Int): Dialog {
        val dialogView = Dialog(this)
        dialogView.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogView.setCancelable(true)
        dialogView.setContentView(view)
        dialogView.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialogView.show()
        return dialogView
    }

    private fun startTimer(startTimeInMilliSeconds: Long) {
        countdownTimerBack = object : CountdownTimerBack(startTimeInMilliSeconds) {
            override fun onTimerTick(millis: Long) {
                AppObjectController.uiHandler.post {
                    binding.freeTrialTimer.visibility = View.VISIBLE
                    binding.freeTrialTimer.text = getString(
                        R.string.free_trial_end_in,
                        UtilTime.timeFormatted(millis)
                    )
                }
            }

            override fun onTimerFinish() {
                binding.freeTrialTimer.visibility = View.VISIBLE
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
                binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
            }
        }
        countdownTimerBack?.startTimer()
    }

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("privacy_policy_url")
        showWebViewDialog(url)
    }

    fun showWebViewDialog(webUrl: String) {
        WebViewFragment.showDialog(supportFragmentManager, webUrl)
    }

    fun showProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.VISIBLE
        }
    }

    fun hideProgressBar() {
        lifecycleScope.launch(Dispatchers.Main) {
            binding.progressBar.visibility = View.GONE
        }
    }

    fun showErrorView() {
        errorView?.resolved().let {
            errorView?.get()?.onFailure(object : ErrorView.ErrorCallback {
                override fun onRetryButtonClicked() {
                    if (Utils.isInternetAvailable()) {
                        viewModel.getCourseContent()
                        viewModel.getCoursePriceList(null)
                        viewModel.getValidCouponList(OFFERS)
                    } else {
                        errorView?.get()?.enableRetryBtn()
                        Snackbar.make(
                            binding.root,
                            getString(R.string.internet_not_available_msz),
                            Snackbar.LENGTH_SHORT
                        )
                            .setAction(getString(R.string.settings)) {
                                startActivity(
                                    Intent(
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                                            Settings.Panel.ACTION_INTERNET_CONNECTIVITY
                                        else
                                            Settings.ACTION_WIRELESS_SETTINGS
                                    )
                                )
                            }.show()
                    }
                }
            })
        }
    }

    override fun onProcessStart() {
        showProgressBar()
    }

    override fun onProcessStop() {
        viewModel.saveImpressionForBuyPageLayout(GATEWAY_INITIALISED, "BUY_PAGE_LAYOUT")
        hideProgressBar()
    }

    override fun onPaymentFinished(isPaymentSuccessful: Boolean) {

    }

    override fun onPaymentProcessing(orderId: String, status: String) {
        if (status == "pending_vbv") {
            showPendingDialog()
            return
        }
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentInProcessFragment()
            val bundle = Bundle().apply {
                putString("ORDER_ID", orderId)
            }
            fragment.arguments = bundle
            replace(R.id.buy_page_parent_container, fragment, "Payment Processing")
            disallowAddToBackStack()
        }
    }

    private fun showPendingDialog() {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            val fragment = PaymentPendingFragment()
            replace(R.id.buy_page_parent_container, fragment, "Payment Pending")
            disallowAddToBackStack()
        }
    }

    override fun onWarmUpEnded(error: String?) {

    }

    private fun onPaymentSuccess() {
        if (viewModel.isDiscount) {
            viewModel.saveImpression(IMPRESSION_PAY_DISCOUNT)
        } else {
            viewModel.saveImpression(IMPRESSION_PAY_FULL_FEES)
        }
        val freeTrialTestId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
            Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
        } else {
            PrefManager.getStringValue(PAID_COURSE_TEST_ID)
        }

        if (testId == freeTrialTestId) {
            PrefManager.put(IS_COURSE_BOUGHT, true)
            PrefManager.put(IS_FREE_TRIAL, false)
            PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
        }
        viewModel.removeEntryFromPaymentTable(paymentManager.getJustPayOrderId())
        MarketingAnalytics.coursePurchased(
            BigDecimal(priceForPaymentProceed?.discountedPrice?.replace("₹", "").toString()),
            true,
            testId = freeTrialTestId,
            courseName = priceForPaymentProceed?.courseName ?: EMPTY,
            juspayPaymentId = paymentManager.getJustPayOrderId()
        )
        try {
            NotificationUtils(applicationContext).removeAllScheduledNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppObjectController.uiHandler.postDelayed({
            PrefManager.put(IS_PAYMENT_DONE, true)
            navigateToStartCourseActivity()
        }, 2 * 1000L)
    }

    override fun onBackPressed() {
        if (!isPaymentInitiated) {
            super.onBackPressed()
        } else {
            backPressMutex.onMultipleBackPress {
                val backPressHandled = paymentManager.getJuspayBackPress()
                if (!backPressHandled) {
                    viewModel.saveImpressionForBuyPageLayout(BACK_PRESSED_ON_GATEWAY, "BUY_PAGE_LAYOUT")
                    super.onBackPressed()
                } else {
                    viewModel.saveImpressionForBuyPageLayout(BACK_PRESSED_ON_LOADING, "BUY_PAGE_LAYOUT")
                }
            }
        }
    }
}