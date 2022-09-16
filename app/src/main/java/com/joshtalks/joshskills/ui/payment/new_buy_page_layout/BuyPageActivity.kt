package com.joshtalks.joshskills.ui.payment.new_buy_page_layout

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.greentoad.turtlebody.mediapicker.util.UtilTime
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.base.BaseActivity
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.*
import com.joshtalks.joshskills.core.countdowntimer.CountdownTimerBack
import com.joshtalks.joshskills.core.custom_ui.JoshRatingBar
import com.joshtalks.joshskills.core.notification.NotificationUtils
import com.joshtalks.joshskills.databinding.ActivityBuyPageBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.OrderDetailResponse
import com.joshtalks.joshskills.ui.assessment.view.Stub
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.extra.setOnSingleClickListener
import com.joshtalks.joshskills.ui.inbox.COURSE_EXPLORER_CODE
import com.joshtalks.joshskills.ui.payment.*
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment.CouponCardFragment
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.fragment.RatingAndReviewFragment
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.BuyCourseFeatureModel
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.CourseDetailsList
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.model.Coupon
import com.joshtalks.joshskills.ui.payment.new_buy_page_layout.viewmodel.BuyPageViewModel
import com.joshtalks.joshskills.ui.payment.order_summary.PaymentSummaryActivity
import com.joshtalks.joshskills.ui.pdfviewer.CURRENT_VIDEO_PROGRESS_POSITION
import com.joshtalks.joshskills.ui.special_practice.utils.*
import com.joshtalks.joshskills.ui.startcourse.StartCourseActivity
import com.joshtalks.joshskills.ui.termsandconditions.WebViewFragment
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.util.showAppropriateMsg
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import de.hdodenhof.circleimageview.CircleImageView
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.branch.referral.util.CurrencyType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.math.BigDecimal

class BuyPageActivity : BaseActivity(), PaymentResultListener {

    var englishCourseCard: View? = null
    var otherCourseCard: View? = null
    var teacherDetailsCard: View? = null
    var teacherRatingAndReviewCard: View? = null
    var proceedButtonCard: View? = null
    var clickRatingOpen: ImageView? = null
    var courseDescListCard: View? = null
    var priceForPaymentProceed: CourseDetailsList? = null

    var testId = FREE_TRIAL_PAYMENT_TEST_ID
    var expiredTime: Long = -1
    private var razorpayOrderId = EMPTY

    private var countdownTimerBack: CountdownTimerBack? = null

    private val binding by lazy<ActivityBuyPageBinding> {
        DataBindingUtil.setContentView(this, R.layout.activity_buy_page)
    }
    private var errorView: Stub<ErrorView>? = null

    private val viewModel by lazy {
        ViewModelProvider(this)[BuyPageViewModel::class.java]
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
        if (intent.hasExtra(PaymentSummaryActivity.TEST_ID_PAYMENT)) {
            testId = if (PrefManager.getStringValue(FREE_TRIAL_TEST_ID).isEmpty().not()) {
                Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
            } else {
                PrefManager.getStringValue(PAID_COURSE_TEST_ID)
            }
        }
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
    }

    override fun onCreated() {
        viewModel.testId = testId

        Log.d("BuyPageActivity.kt", "SAGAR => onCreated:111 $testId")
        viewModel.getCourseContent()
        viewModel.getCoursePriceList(null)
        viewModel.getValidCouponList(OFFERS)
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
                ORDER_DETAILS_VALUE -> {
                    initializeRazorpayPayment(it.obj as OrderDetailResponse)
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
                    binding.scrollView.smoothScrollTo(binding.buyPageParentContainer.width, binding.buyPageParentContainer.height, 2000)
                }
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
            binding.freeTrialTimer.visibility = View.VISIBLE
            if (buyCourseFeatureModel.expiryTime?.time ?: 0 >= System.currentTimeMillis()) {
                if (buyCourseFeatureModel.expiryTime?.time ?: 0 > (System.currentTimeMillis() + 24 * 60 * 60 * 1000)) {
                    binding.freeTrialTimer.visibility = View.GONE
                } else {
                    startTimer((buyCourseFeatureModel.expiryTime?.time ?: 0) - System.currentTimeMillis())
                }
            } else {
                binding.freeTrialTimer.text = getString(R.string.free_trial_ended)
                PrefManager.put(IS_FREE_TRIAL_ENDED, true)
            }
        } else {
            binding.freeTrialTimer.visibility = View.GONE
        }
    }

    private fun makePhoneCall() {
        viewModel.saveImpressionForBuyPageLayout(BUY_PAGE_CALL_CLICKED)
        Utils.call(this@BuyPageActivity, "+918634503202")//change the number
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
            val fragment = RatingAndReviewFragment(testId.toInt())
            replace(R.id.buy_page_parent_container, fragment, COURSE_CONTENT)
            addToBackStack(COURSE_CONTENT)
        }
    }

    private fun dynamicCardCreation(buyCourseFeatureModel: BuyCourseFeatureModel) {
        val courseDetailsInflate: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        if (buyCourseFeatureModel.teacherName == null && buyCourseFeatureModel.teacherImage == null && buyCourseFeatureModel.video == null && buyCourseFeatureModel.youtubeChannel == null) {
            englishCourseCard = courseDetailsInflate.inflate(R.layout.english_course_card, null, true)
            binding.courseTypeContainer.addView(englishCourseCard)
            viewModel.isGovernmentCourse.set(false)
        } else {
            viewModel.isGovernmentCourse.set(true)
            otherCourseCard = courseDetailsInflate.inflate(R.layout.other_course_card, null, true)
            binding.courseTypeContainer.addView(otherCourseCard)
            val teacherVideoButton = otherCourseCard?.findViewById<RelativeLayout>(R.id.play_video_button)
            teacherVideoButton?.setOnClickListener {
                viewModel.saveImpressionForBuyPageLayout(PLAY_SAMPLE_VIDEO)
                playSampleVideo(buyCourseFeatureModel.video ?: EMPTY)
            }

            teacherDetailsCard = courseDetailsInflate.inflate(R.layout.teacher_details_card, null, true)
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
        val courseDescListInflate: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        courseDescListCard = courseDescListInflate.inflate(R.layout.item_course_description, null, true)

        val courseDesc = courseDescListCard?.findViewById<TextView>(R.id.course_desc)
        courseDesc?.text = buyCourseFeatureModel
        return courseDescListCard
    }

    private fun setRating(buyCourseFeatureModel: BuyCourseFeatureModel) {
        val ratingInflate: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

        teacherRatingAndReviewCard = ratingInflate.inflate(R.layout.teacher_rating_and_review_card, null, true)
        binding.teacherRatingAndReview.addView(teacherRatingAndReviewCard)

        val ratingCount = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_count)
        ratingCount?.text = buyCourseFeatureModel.ratingCount.toString() + " Reviews"

        clickRatingOpen = teacherRatingAndReviewCard?.findViewById(R.id.btn_ration_click)

        val teacherRating = teacherRatingAndReviewCard?.findViewById<JoshRatingBar>(R.id.teacher_rating)
        teacherRating?.rating = buyCourseFeatureModel.rating ?: 0.0f

        val ratingInText = teacherRatingAndReviewCard?.findViewById<TextView>(R.id.rating_in_text)
        ratingInText?.text = buyCourseFeatureModel.rating.toString() + " out of 5"
    }

    private fun paymentButton() {
        val paymentInflate: LayoutInflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        proceedButtonCard = paymentInflate.inflate(R.layout.payment_button_card, null, true)
        val paymentButton = proceedButtonCard?.findViewById<MaterialButton>(R.id.btn_payment_course)
        binding.paymentProceedBtnCard.removeAllViews()
        binding.paymentProceedBtnCard.addView(proceedButtonCard)
        paymentButton?.setOnSingleClickListener {
            startPayment()
        }
        proceedButtonCard?.findViewById<MaterialTextView>(R.id.text_view_privacy)?.setOnSingleClickListener{
            showPrivacyPolicyDialog()
        }
    }

    private fun initializeRazorpayPayment(orderDetails: OrderDetailResponse) {
        Log.e("sagar", "initializeRazorpayPayment:1 ")
        viewModel.isProcessing.set(true)
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
                    priceForPaymentProceed?.courseName + "_app"
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
            Log.e("sagar", "initializeRazorpayPayment:2 ${e.message}")
            e.printStackTrace()
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

        try {
            viewModel.getOrderDetails(
                priceForPaymentProceed?.testId ?: EMPTY,
                phoneNumber,
                priceForPaymentProceed?.encryptedText ?: EMPTY
            )
        } catch (ex: Exception) {
            ex.showAppropriateMsg()
        }
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
        viewModel.mentorPaymentStatus.observe(this) {
            when (it) {
                true -> {
                    val freeTrialTestId = Utils.getLangPaymentTestIdFromTestId(PrefManager.getStringValue(FREE_TRIAL_TEST_ID))
                    if (testId == freeTrialTestId) {
                        PrefManager.put(IS_COURSE_BOUGHT, true)
                        PrefManager.put(IS_FREE_TRIAL, false)
                        PrefManager.put(IS_FREE_TRIAL, false)

                        PrefManager.removeKey(IS_FREE_TRIAL_ENDED)
                    }
                    // isBackPressDisabled = true
                    razorpayOrderId.verifyPayment()
                    //viewModel.updateSubscriptionStatus()

                    AppObjectController.uiHandler.post {
                        PrefManager.put(IS_PAYMENT_DONE, true)
                        showPaymentProcessingFragment()
                    }

                    AppObjectController.uiHandler.postDelayed({
                        navigateToStartCourseActivity()
                    }, 1000L * 5L)
                }
                false -> {
                    AppObjectController.uiHandler.post {
                        showPaymentFailedDialog()
                    }
                }
            }
        }
        try {
            //MarketingAnalytics.paymentFail(razorpayOrderId, testId)
            viewModel.removeEntryFromPaymentTable(razorpayOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    override fun onPaymentSuccess(razorpayPaymentId: String) {
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
        // isBackPressDisabled = true
        razorpayOrderId.verifyPayment()
        viewModel.removeEntryFromPaymentTable(razorpayOrderId)
        MarketingAnalytics.coursePurchased(
            BigDecimal(viewModel.orderDetailsLiveData.value?.amount ?: 0.0),
            true,
            testId = freeTrialTestId,
            courseName = priceForPaymentProceed?.courseName ?: EMPTY,
            razorpayPaymentId = razorpayOrderId
        )
        //viewModel.updateSubscriptionStatus()
        try {
            val extras: HashMap<String, String> = HashMap()
            var guestMentorId = EMPTY
            if (PrefManager.getBoolValue(IS_FREE_TRIAL)) {
                guestMentorId = Mentor.getInstance().getId()
            }
            extras["test_id"] = priceForPaymentProceed?.testId.toString()
            extras["payment_id"] = razorpayPaymentId
            extras["currency"] = CurrencyType.INR.name
            extras["amount"] = priceForPaymentProceed?.discountedPrice?.replace("₹", "").toString()
            extras["course_name"] = priceForPaymentProceed?.courseName.toString()
            extras["device_id"] = Utils.getDeviceId()
            extras["guest_mentor_id"] = guestMentorId
            BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.PURCHASE, extras)
            NotificationUtils(applicationContext).removeAllScheduledNotification()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        AppObjectController.uiHandler.post {
            PrefManager.put(IS_PAYMENT_DONE, true)
            showPaymentProcessingFragment()
        }

        AppObjectController.uiHandler.postDelayed({
            navigateToStartCourseActivity()
        }, 1000L * 5L)
    }

    private fun showPaymentProcessingFragment() {
        // binding.container.visibility = View.GONE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.buy_page_parent_container,
                PaymentProcessingFragment.newInstance(),
                "Payment Processing"
            )
            .commitAllowingStateLoss()
    }

    private fun showPaymentFailedDialog() {
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.buy_page_parent_container,
                PaymentFailedDialogFragment.newInstance(
                    viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0
                ),
                "Payment Success"
            )
            .commitAllowingStateLoss()
    }

    private fun navigateToStartCourseActivity() {
        try {
            StartCourseActivity.openStartCourseActivity(
                this,
                priceForPaymentProceed?.courseName ?: EMPTY,
                priceForPaymentProceed?.teacherName?: EMPTY,
                priceForPaymentProceed?.imageUrl?: EMPTY,
                viewModel.orderDetailsLiveData.value?.joshtalksOrderId ?: 0,
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
            expiredTime: Long? = null
        ) {
            Intent(activity, BuyPageActivity::class.java).apply {
                putExtra(PaymentSummaryActivity.TEST_ID_PAYMENT, testId)
                putExtra(FreeTrialPaymentActivity.EXPIRED_TIME, expiredTime)
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
        dialogView.findViewById<TextView>(R.id.coupon_name_text).text = coupon.couponCode + " applied"
        dialogView.findViewById<TextView>(R.id.coupon_price_text).text = "You saved ₹" + coupon.maxDiscountAmount.toString()

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

    fun showPrivacyPolicyDialog() {
        val url = AppObjectController.getFirebaseRemoteConfig().getString("terms_condition_url")
        showWebViewDialog(url)
    }

    fun showWebViewDialog(webUrl: String) {
        WebViewFragment.showDialog(supportFragmentManager, webUrl)
    }
}