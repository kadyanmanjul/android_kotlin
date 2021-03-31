package com.joshtalks.joshskills.ui.chat.vh

import android.view.View
import android.widget.FrameLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.repository.local.entity.AwardMentorModel
import com.joshtalks.joshskills.repository.local.entity.AwardTypes
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.ui.assessment.view.Stub

class BestStudentPerformerViewHolder(view: View, userId: String) : BaseViewHolder(view, userId) {

    val rootSubView: FrameLayout = view.findViewById(R.id.root_sub_view)

    private var studentOfDayStub: Stub<StudentOfTheDayView> =
        Stub(view.findViewById(R.id.student_of_day_stub))
    private var studentOfWeekStub: Stub<StudentOfTheWeekView> =
        Stub(view.findViewById(R.id.student_of_week_stub))
    private var studentOfMonthStub: Stub<StudentOfTheMonthView> =
        Stub(view.findViewById(R.id.student_of_month_stub))

    private var message: ChatModel? = null


    override fun bind(message: ChatModel, previousMessage: ChatModel?) {
        this.message = message
        message.awardMentorModel?.let { awardMentorModel ->
            setupUI(awardMentorModel)
        }
    }

    private fun setupUI(mentorModel: AwardMentorModel) {
        if (mentorModel.awardType == AwardTypes.SOTD) {
            studentOfDayStub.resolved().let {
                studentOfDayStub.get().visibility = View.VISIBLE
                studentOfDayStub.get().setup(mentorModel)
            }

        } else if (mentorModel.awardType == AwardTypes.SOTW) {
            studentOfWeekStub.resolved().let {
                studentOfWeekStub.get().visibility = View.VISIBLE
                studentOfWeekStub.get().setup(mentorModel)
            }
        }else if (mentorModel.awardType == AwardTypes.SOTM) {
            studentOfMonthStub.resolved().let {
                studentOfMonthStub.get().visibility = View.VISIBLE
                studentOfMonthStub.get().setup(mentorModel)
            }
        }
    }

    override fun unBind() {

    }
}
