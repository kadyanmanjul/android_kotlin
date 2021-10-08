package com.joshtalks.joshskills.ui.chat.vh

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.HtmlCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModel
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import java.util.*

class StudentOfTheWeekNewView : FrameLayout {
    private lateinit var studentOfDash: AppCompatTextView
    private lateinit var userPic: AppCompatImageView
    private lateinit var awardImage: AppCompatImageView
    private lateinit var studentName: AppCompatTextView
    private lateinit var totalPoints: AppCompatTextView
    private lateinit var userText: AppCompatTextView
    private lateinit var date: AppCompatTextView
    private lateinit var rootView: FrameLayout
    private var awardMentorModel: AwardMentorModel? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init()

    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    ) {
        init()

    }

    private fun init() {
        View.inflate(context, R.layout.cell_best_performer_of_the_week_new_message, this)
        studentOfDash = findViewById(R.id.tv_student_of)
        userPic = findViewById(R.id.user_pic)
        awardImage = findViewById(R.id.iv_award)
        studentName = findViewById(R.id.student_name)
        totalPoints = findViewById(R.id.total_points)
        userText = findViewById(R.id.user_text)
        date = findViewById(R.id.date)
        rootView = findViewById(R.id.root_view_fl)
    }

    fun setup(awardMentorModel: AwardMentorModel) {
        this.awardMentorModel = awardMentorModel

        rootView.setOnClickListener {
            awardMentorModel.mentorId?.let {
                RxBus2.publish(OpenUserProfile(it))
            }
        }

        val resp = StringBuilder()
        awardMentorModel.performerName?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        studentName.text = resp
        totalPoints.text =
            awardMentorModel.totalPointsText?.let {
                HtmlCompat.fromHtml(
                    it,
                    HtmlCompat.FROM_HTML_MODE_LEGACY
                )
            }
        date.text = awardMentorModel.dateText
        userText.text = awardMentorModel.description
        studentOfDash.text = awardMentorModel.awardText
        userPic.post {
            userPic.setUserImageOrInitials(
                awardMentorModel.performerPhotoUrl,
                awardMentorModel.performerName?.capitalize(Locale.getDefault()).toString(),
                dpToPx = 28,isRound = true
            )
        }
        awardMentorModel.awardImageUrl?.let {
            awardImage.setImage(it, AppObjectController.joshApplication)
        }
        awardImage.setOnClickListener {
            /*RxBus2.publish(
                AwardItemClickedEventBus(
                    Award(
                        awardMentorModel.id,
                        awardMentorModel.awardText,
                        0,
                        null,
                        null,
                        awardMentorModel.awardDescription,
                        true,
                        true
                    )
                )
            )*/
            awardMentorModel.mentorId?.let {
                RxBus2.publish(OpenUserProfile(it))
            }
        }

    }

}