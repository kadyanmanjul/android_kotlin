package com.joshtalks.joshskills.ui.payment

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.facebook.appevents.AppEventsConstants
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.core.analytics.BranchIOAnalytics
import com.joshtalks.joshskills.databinding.ActivityPaymentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.BuyCourseEventBus
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.CourseDetailsModel
import com.joshtalks.joshskills.repository.server.CourseExploreModel
import com.joshtalks.joshskills.repository.server.PaymentDetailsResponse
import com.joshtalks.joshskills.ui.explore.CourseExploreActivity
import com.joshtalks.joshskills.ui.sign_up_old.OnBoardActivity
import com.joshtalks.joshskills.ui.signup.IS_ACTIVITY_FOR_RESULT
import com.joshtalks.joshskills.ui.view_holders.CourseDetailViewHolder
import com.muddzdev.styleabletoast.StyleableToast
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import io.branch.referral.util.BRANCH_STANDARD_EVENT
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.HttpException
import java.util.*


const val COURSE_OBJECT = "course"
const val COURSE_ID = "course_ID"


class PaymentActivity : CoreJoshActivity(),
    PaymentResultListener, CouponCodeSubmitFragment.OnCouponCodeSubmitListener,
    CoursePurchaseDetailFragment.OnCourseDetailInteractionListener {

    private lateinit var activityPaymentBinding: ActivityPaymentBinding
    private var courseModel: CourseExploreModel? = null
    private var compositeDisposable = CompositeDisposable()
    private val uiHandler = Handler(Looper.getMainLooper())
    private var courseId: String = EMPTY
    private var currency: String = "INR"
    private var amount: Double = 0.0
    private var courseName = EMPTY
    private var userSubmitCode = EMPTY
    private lateinit var titleView: AppCompatTextView

    companion object {
        fun startPaymentActivity(
            context: Activity,
            requestCode: Int,
            courseModel: CourseExploreModel
        ) {
            Intent(context, PaymentActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                putExtra(COURSE_OBJECT, courseModel)
            }.run {
                context.startActivityForResult(this, requestCode)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = if (Build.VERSION.SDK_INT == 26) {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        } else {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        super.onCreate(savedInstanceState)
        activityPaymentBinding = DataBindingUtil.setContentView(this, R.layout.activity_payment)
        activityPaymentBinding.lifecycleOwner = this
        activityPaymentBinding.handler = this
        if (intent.hasExtra(COURSE_OBJECT)) {
            courseModel = intent.getSerializableExtra(COURSE_OBJECT) as CourseExploreModel
            courseId = courseModel?.id.toString()
        }
        if (intent.hasExtra(COURSE_ID)) {
            courseId = intent.getStringExtra(COURSE_ID)!!
        }
        if (courseId.isEmpty()) {
            invalidCourseId()
            return
        }
        initRV()
        initView()
        getCourseDetails()
        Checkout.preload(application)
        openWhatsAppHelp()
    }

    private fun invalidCourseId() {
        startActivity(Intent(applicationContext, CourseExploreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        })
        finishAndRemoveTask()

    }


    private fun initView() {
        titleView = findViewById(R.id.text_message_title)
        if (courseModel != null) {
            titleView.text = courseModel?.courseName
            courseName = courseModel?.courseName ?: EMPTY
        } else {
            titleView.text = getString(R.string.explorer_course)

        }
        findViewById<View>(R.id.iv_back).visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            this@PaymentActivity.finish()
        }
    }

    private fun initRV() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        activityPaymentBinding.recyclerView.builder
            .setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        activityPaymentBinding.recyclerView.itemAnimator = null
    }

    private fun getCourseDetails() {
        CoroutineScope(Dispatchers.IO).launch {
            try {

                val data = mapOf("test" to courseId)
                val courseDetailsModelList: List<CourseDetailsModel> =
                    AppObjectController.signUpNetworkService.explorerCourseDetails(data).await()
                CoroutineScope(Dispatchers.Main).launch {
                    if (courseDetailsModelList.isNullOrEmpty().not()) {
                        courseDetailsModelList.forEach {
                            activityPaymentBinding.recyclerView.addView(
                                CourseDetailViewHolder(
                                    it
                                )
                            )
                        }
                    }
                    activityPaymentBinding.progressBar.visibility = View.GONE
                }

                amount = courseDetailsModelList[0].testCourseDetail.amount
                courseName = courseDetailsModelList[0].testCourseDetail.courseName
                if (courseName.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        titleView.text = courseName
                    }

                }

            } catch (ex: HttpException) {
                if (ex.code() == 500) {
                    invalidCourseId()
                }
                ex.printStackTrace()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }


    override fun onPaymentError(p0: Int, p1: String?) {
        userSubmitCode = EMPTY
        StyleableToast.Builder(this).gravity(Gravity.TOP)
            .backgroundColor(ContextCompat.getColor(applicationContext, R.color.error_color))
            .text(getString(R.string.something_went_wrong)).cornerRadius(16)
            .textColor(ContextCompat.getColor(applicationContext, R.color.white))
            .length(Toast.LENGTH_LONG).solidBackground().show()

        Log.i("error", p1 ?: "")
    }

    override fun onPaymentSuccess(p0: String?) {
        val extras: HashMap<String, String> = HashMap()
        extras["test_id"] = courseId
        extras["payment_id"] = p0 ?: EMPTY
        extras["currency"] = currency
        extras["amount"] = (courseModel?.amount ?: amount).toString()
        extras["course_name"] = courseName
        BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.PURCHASE, extras)

        try {
            val params = Bundle().apply {
                putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, courseId)
            }
            AppObjectController.facebookEventLogger.logPurchase(
                amount.toBigDecimal(),
                Currency.getInstance(currency.trim()),
                params
            )
        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        uiHandler.post {
            courseModel?.run {
                PaymentProcessFragment.newInstance(this)
                    .show(supportFragmentManager, "Payment Process")
            }
        }
        uiHandler.postDelayed({
            startActivity(getInboxActivityIntent())
            this@PaymentActivity.finish()
        }, 1000 * 59)

    }

    private fun getPaymentDetails(testId: String?) {
        activityPaymentBinding.progressBar.visibility = View.VISIBLE
        BranchIOAnalytics.pushToBranch(BRANCH_STANDARD_EVENT.INITIATE_PURCHASE)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (testId.isNullOrEmpty().not() && testId.equals("null").not()) {
                    courseId = testId!!
                }

                val map = HashMap<String, String>()
                map["mobile"] = User.getInstance().phoneNumber
                map["id"] = courseId
                if (userSubmitCode.isEmpty()) {
                    map["code"] = EMPTY
                } else {
                    map["code"] = userSubmitCode
                }
                val response: PaymentDetailsResponse =
                    AppObjectController.signUpNetworkService.getPaymentDetails(map).await()

                if (courseModel == null) {
                    courseModel = CourseExploreModel()
                    courseModel?.amount = response.amount
                    courseModel?.courseName = response.courseName
                    courseModel?.id = courseId.toInt()
                }
                initializeRazorpayPayment(response)
            } catch (ex: HttpException) {
                ex.printStackTrace()
            } catch (ex: Exception) {
                activityPaymentBinding.progressBar.visibility = View.GONE
                ex.printStackTrace()
            }
        }

    }

    private fun initializeRazorpayPayment(response: PaymentDetailsResponse) {
        CoroutineScope(Dispatchers.Main).launch {
            activityPaymentBinding.progressBar.visibility = View.VISIBLE
            val checkout = Checkout()
            checkout.setImage(R.mipmap.ic_launcher)
            checkout.setKeyID(response.razorpayKeyId)
            try {
                val preFill = JSONObject()
                    .put("email", Utils.getUserPrimaryEmail(applicationContext))
                    .put("contact", User.getInstance().phoneNumber)
                val options = JSONObject()
                options.put("key", response.razorpayKeyId)
                options.put("name", "Josh Skills")
                options.put("description", response.courseName + "_app")
                options.put("order_id", response.razorpayOrderId)
                options.put("currency", response.currency)
                options.put("amount", response.amount)
                options.put("prefill", preFill)
                currency = response.currency
                amount = response.amount
                courseModel?.amount = amount
                activityPaymentBinding.progressBar.visibility = View.GONE

                checkout.open(this@PaymentActivity, options)

                val params = Bundle().apply {
                    putString(AppEventsConstants.EVENT_PARAM_CONTENT_ID, courseId)
                }
                AppObjectController.facebookEventLogger.logEvent(
                    AppEventsConstants.EVENT_NAME_INITIATED_CHECKOUT,
                    params
                )

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    fun buyCourse() {

        val courseModel = CourseExploreModel()
        courseModel.amount = amount
        courseModel.courseName = courseName
        courseModel.id = courseId.toInt()

        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("purchase_details_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        CoursePurchaseDetailFragment.newInstance(courseModel)
            .show(supportFragmentManager, "purchase_details_dialog")
    }


    private fun addObserver() {
        compositeDisposable.add(
            RxBus2.listen(BuyCourseEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onResume() {
        super.onResume()
        addObserver()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
        Checkout.clearUserData(applicationContext)
        uiHandler.removeCallbacksAndMessages(null)
    }

    private fun openWhatsAppHelp() {
        AppObjectController.uiHandler.postDelayed({
            if (courseModel != null && courseModel!!.whatsappUrl != null) {
                activityPaymentBinding.whatsappHelpContainer.visibility = View.VISIBLE
                activityPaymentBinding.whatsappHelp.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse(courseModel?.whatsappUrl)
                    intent.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
                activityPaymentBinding.ivHideHelp.setOnClickListener {
                    activityPaymentBinding.whatsappHelpContainer.visibility = View.GONE

                }
            }
        }, 2500)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            requestForPayment()
        }
        super.onActivityResult(requestCode, resultCode, data)

    }

    override fun getCouponCode(code: String?) {
        code?.run {
            userSubmitCode = this
        }
        requestForPayment()
    }


    private fun requestForPayment() {
        if (User.getInstance().token == null) {
            startActivityForResult(Intent(this, OnBoardActivity::class.java).apply {
                putExtra(IS_ACTIVITY_FOR_RESULT, true)
            }, 101)
            return
        }
        if (userSubmitCode.isEmpty().not() && userSubmitCode.equals(
                Mentor.getInstance().referralCode,
                ignoreCase = true
            )
        ) {
            invalidCodeDialog()
            return
        }
        if (courseModel != null) {
            getPaymentDetails(courseModel?.id.toString())
        } else {
            getPaymentDetails(courseId)
        }
    }


    private fun showCouponCodeEnterScreen() {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val prev = supportFragmentManager.findFragmentByTag("coupon_code_dialog")
        if (prev != null) {
            fragmentTransaction.remove(prev)
        }
        fragmentTransaction.addToBackStack(null)
        CouponCodeSubmitFragment.newInstance()
            .show(supportFragmentManager, "coupon_code_dialog")
    }

    private fun invalidCodeDialog() {
        val dialog = Dialog(this@PaymentActivity)
        dialog.setContentView(R.layout.invalid_coupon_code_layout)
        dialog.findViewById<View>(R.id.tv_ok).setOnClickListener {
            dialog.dismiss()
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setDimAmount(0.7f)
        dialog.show()
    }


    override fun onCompletePayment() {
        requestForPayment()
    }

    override fun onCouponCode() {
        showCouponCodeEnterScreen()
    }

}
