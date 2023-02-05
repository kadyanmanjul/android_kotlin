package com.joshtalks.joshskills.premium.ui.startcourse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.premium.R
import com.joshtalks.joshskills.base.constants.CALLING_SERVICE_ACTION
import com.joshtalks.joshskills.base.constants.SERVICE_BROADCAST_KEY
import com.joshtalks.joshskills.base.constants.STOP_SERVICE
import com.joshtalks.joshskills.premium.core.CURRENT_COURSE_ID
import com.joshtalks.joshskills.premium.core.CoreJoshActivity
import com.joshtalks.joshskills.premium.core.DEFAULT_COURSE_ID
import com.joshtalks.joshskills.premium.core.EMPTY
import com.joshtalks.joshskills.premium.core.PrefManager
import com.joshtalks.joshskills.premium.core.Utils
import com.joshtalks.joshskills.premium.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.premium.core.analytics.AppAnalytics
import com.joshtalks.joshskills.premium.core.analytics.MixPanelEvent
import com.joshtalks.joshskills.premium.core.analytics.MixPanelTracker
import com.joshtalks.joshskills.premium.core.analytics.ParamKeys
import com.joshtalks.joshskills.premium.databinding.ActivityStartCourseBinding
import com.joshtalks.joshskills.premium.repository.local.model.User
import com.joshtalks.joshskills.premium.ui.payment.order_summary.TRANSACTION_ID
import com.joshtalks.joshskills.premium.ui.pdfviewer.COURSE_NAME
import com.joshtalks.joshskills.premium.ui.signup.FLOW_FROM
import com.joshtalks.joshskills.premium.ui.signup.SignUpActivity
import com.joshtalks.joshskills.premium.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

const val TEACHER_NAME = "teacher_name"
const val IMAGE_URL = "image_url"
const val COURSE_PRICE = "course_price"

class StartCourseActivity : CoreJoshActivity() {

    private var isUserRegistered = false
    var courseName: String = EMPTY
    var teacherName: String = EMPTY
    var imageUrl: String = EMPTY
    var transactionId: String = EMPTY
    var coursePrice: String = EMPTY
    private lateinit var binding: ActivityStartCourseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start_course)
        binding.lifecycleOwner = this
        getIntentData()
        isUserRegistered = User.getInstance().isVerified
        initView()
        setListeners()
    }

    private fun initView() {
        if (isUserRegistered) {
            binding.materialButton.text = resources.getText(R.string.start)
            binding.course.text = getCustomString(
                getString(
                    R.string.starting_info_2,
                    courseName
                )
            ).toString().plus(" with ").plus(teacherName)
            binding.startRegister.text = resources.getText(R.string.tutor_start)
        } else {
            binding.materialButton.text = resources.getText(R.string.register_now)
            binding.course.text = getCustomString(
                getString(
                    R.string.starting_info,
                    courseName
                )
            )
            binding.startRegister.text = resources.getText(R.string.tutor_register)
        }

        if (transactionId.toInt() > 0)
            binding.transationId.text = getCustomString(
                getString(
                    R.string.trx_id,
                    transactionId
                )
            ) else {
            binding.transationId.visibility = View.GONE
        }
        val multi = MultiTransformation(
            RoundedCornersTransformation(
                Utils.dpToPx(ROUND_CORNER),
                0,
                RoundedCornersTransformation.CornerType.ALL
            )
        )
        Glide.with(applicationContext)
            .load(imageUrl)
            .apply(RequestOptions.bitmapTransform(multi))
            .override(Target.SIZE_ORIGINAL)
            .into(binding.circleDp)
    }

    private fun getCustomString(string: String) = SpannableStringBuilder(string)

    private fun getIntentData() {
        val string = dataFromIntent(COURSE_NAME).split("with")
        courseName = string[0]
        teacherName = dataFromIntent(TEACHER_NAME)
        imageUrl = dataFromIntent(IMAGE_URL)
        transactionId = dataFromIntent(TRANSACTION_ID)
        coursePrice = dataFromIntent(COURSE_PRICE)
    }

    private fun dataFromIntent(courseName: String): String {
        if (intent.hasExtra(courseName)) {
            val courseName = intent.getStringExtra(courseName)
            if (courseName.isNullOrEmpty().not()) {
                return courseName?: EMPTY
            }
        }
        return EMPTY
    }

    companion object {
        fun openStartCourseActivity(
            context: Context,
            courseName: String,
            teacherName: String,
            imageUrl: String,
            transactionId: Int,
            coursePrice: String
        ) {
            Intent(context, StartCourseActivity::class.java).apply {
                putExtra(COURSE_NAME, courseName)
                putExtra(TEACHER_NAME, teacherName)
                putExtra(IMAGE_URL, imageUrl)
                putExtra(TRANSACTION_ID, transactionId.toString())
                putExtra(COURSE_PRICE,coursePrice)
            }.run {
                context.startActivity(this)
            }
        }
    }

    private fun setListeners() {
        binding.materialButton.setOnClickListener {
            if (isUserRegistered) {

                AppAnalytics.create(AnalyticsEvent.COURSE_START_CLCIKED.NAME)
                    .addUserDetails()
                    .addBasicParam()
                    .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseName)
                    .addParam(AnalyticsEvent.TRANSACTION_ID.NAME, transactionId)
                    .push()
                if(isRegProfileComplete())
                startActivity(getInboxActivityIntent())
                else {
                    val intent = Intent(this, SignUpActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(FLOW_FROM, "payment journey")
                    }
                    startActivity(intent)
                    val broadcastIntent=Intent().apply {
                        action = CALLING_SERVICE_ACTION
                        putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
                    }
                    LocalBroadcastManager.getInstance(this@StartCourseActivity).sendBroadcast(broadcastIntent)
                    this.finish()
                }
            } else {
                AppAnalytics.create(AnalyticsEvent.REGISTER_NOW_CLICKED.NAME)
                    .addUserDetails()
                    .addBasicParam()
                    .addParam(AnalyticsEvent.COURSE_NAME.NAME, courseName)
                    .addParam(AnalyticsEvent.TRANSACTION_ID.NAME, transactionId)
                    .push()
                val intent = Intent(this, SignUpActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(FLOW_FROM, "payment journey")
                }
                startActivity(intent)
                val broadcastIntent=Intent().apply {
                    action = CALLING_SERVICE_ACTION
                    putExtra(SERVICE_BROADCAST_KEY, STOP_SERVICE)
                }
                LocalBroadcastManager.getInstance(this@StartCourseActivity).sendBroadcast(broadcastIntent)
                this.finish()
            }
        }
    }

    override fun onBackPressed() {
        MixPanelTracker.publishEvent(MixPanelEvent.BACK).push()
        if (isUserRegistered) {
            if(isRegProfileComplete())
                startActivity(getInboxActivityIntent())
            else {
                val intent = Intent(this, SignUpActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(FLOW_FROM, "payment journey")
                }
                startActivity(intent)
                this.finish()
            }
        } else {
            val intent = Intent(this, SignUpActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(FLOW_FROM, "payment journey")
            }
            startActivity(intent)
            this.finish()
        }
    }
}
