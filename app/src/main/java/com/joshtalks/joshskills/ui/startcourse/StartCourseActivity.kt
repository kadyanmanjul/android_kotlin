package com.joshtalks.joshskills.ui.startcourse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.View
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.Utils
import com.joshtalks.joshskills.databinding.ActivityStartCourseBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.ui.payment.order_summary.TRANSACTION_ID
import com.joshtalks.joshskills.ui.pdfviewer.COURSE_NAME
import com.joshtalks.joshskills.ui.signup_v2.SignUpV2Activity
import com.joshtalks.joshskills.ui.view_holders.ROUND_CORNER
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

const val TEACHER_NAME = "teacher_name"
const val IMAGE_URL = "image_url"

class StartCourseActivity : CoreJoshActivity() {

    val isUserRegistered by lazy { Mentor.getInstance().getId().isNotBlank() }
    var courseName: String = EMPTY
    var teacherName: String = EMPTY
    var imageUrl: String = EMPTY
    var transactionId: String = EMPTY
    private lateinit var binding: ActivityStartCourseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_start_course)
        binding.lifecycleOwner = this
        getIntentData()
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

        binding.transationId.text = getCustomString(
            getString(
                R.string.trx_id,
                transactionId
            )
        )
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
    }

    private fun dataFromIntent(courseName: String): String {
        if (intent.hasExtra(courseName)) {
            val courseName = intent.getStringExtra(courseName)
            if (courseName.isNullOrEmpty().not()) {
                return courseName
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
            transactionId: Int
        ) {
            Intent(context, StartCourseActivity::class.java).apply {
                putExtra(COURSE_NAME, courseName)
                putExtra(TEACHER_NAME, teacherName)
                putExtra(IMAGE_URL, imageUrl)
                putExtra(TRANSACTION_ID, transactionId.toString())
            }.run {
                context.startActivity(this)
            }
        }
    }

    override fun onBackPressed() {

    }

    private fun setListeners() {
        binding.materialButton.setOnClickListener(View.OnClickListener {
            if (isUserRegistered) {
                startActivity(getInboxActivityIntent())
                this.finish()
            } else {
                startActivity(Intent(this, SignUpV2Activity::class.java))
                this.finish()
            }
        })

    }
}
