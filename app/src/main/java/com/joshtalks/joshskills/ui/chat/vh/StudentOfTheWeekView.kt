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
import com.joshtalks.joshskills.repository.local.eventbus.AwardItemClickedEventBus
import com.joshtalks.joshskills.repository.server.Award
import de.hdodenhof.circleimageview.CircleImageView
import java.util.Locale

class StudentOfTheWeekView : FrameLayout {
    private lateinit var userPic: CircleImageView
    private lateinit var awardImage: AppCompatImageView
    private lateinit var studentName: AppCompatTextView
    private lateinit var rankOutOfStudents: AppCompatTextView
    private lateinit var userText: AppCompatTextView
    private lateinit var awardDate: AppCompatTextView
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
        View.inflate(context, R.layout.cell_best_performer_of_the_week_message, this)

        awardImage = findViewById(R.id.iv_award)
        studentName = findViewById(R.id.student_name)
        userPic = findViewById(R.id.user_pic)
        rankOutOfStudents = findViewById(R.id.student_text)

        userText = findViewById(R.id.user_text)
        awardDate = findViewById(R.id.student_text_date)
        rootView = findViewById(R.id.root_view_fl)

        rootView.setOnClickListener {
            awardMentorModel?.let {
                //RxBus2.publish(LessonItemClickEventBus(it.id))
            }
        }
    }

    fun setup(awardMentorModel: AwardMentorModel) {
        this.awardMentorModel = awardMentorModel

        val resp = StringBuilder()
        awardMentorModel.performerName?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        studentName.text = resp
        rankOutOfStudents.text =
            awardMentorModel.totalPointsText?.let { HtmlCompat.fromHtml(it, HtmlCompat.FROM_HTML_MODE_LEGACY) }
        userText.text = awardMentorModel.description
        userPic.post {
            userPic.setUserImageOrInitials(
                awardMentorModel.performerPhotoUrl,
                awardMentorModel.performerName?.capitalize(Locale.getDefault()).toString(),
                dpToPx = 28
            )
        }
        awardDate.text = awardMentorModel.dateText

        awardMentorModel.awardImageUrl?.let {
            awardImage.setImage(it, AppObjectController.joshApplication)
        }
        awardImage.setOnClickListener {
            RxBus2.publish(
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
            )
        }

    }

}