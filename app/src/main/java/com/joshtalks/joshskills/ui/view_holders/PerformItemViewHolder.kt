package com.joshtalks.joshskills.ui.view_holders


import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenClickProgressEventBus
import com.joshtalks.joshskills.repository.server.ModuleData
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import com.mindorks.placeholderview.annotations.View
import com.vanniktech.emoji.Utils


@Layout(R.layout.performance_item_layout)
class PerformItemViewHolder(
    var context: Context,
    var moduleData: ModuleData,
    private var postion: Int
) :
    android.view.View.OnClickListener {
    private var layoutInflater: LayoutInflater = LayoutInflater.from(context)

    @View(R.id.ll_practise)
    lateinit var llPractise: LinearLayout

    @View(R.id.tv_practise_content)
    lateinit var practiseContentTV: AppCompatTextView

    @View(R.id.image_view_status)
    lateinit var statusImageView: AppCompatImageView

    @View(R.id.tv_practise_status)
    lateinit var practiseStatusTV: AppCompatTextView

    private var allQuestionComplete =
        moduleData.questionComplete.isNotEmpty() && moduleData.practiceIncomplete.isEmpty()


    @Resolve
    fun onViewInflated() {
        llPractise.removeAllViewsInLayout()
        practiseContentTV.text = moduleData.moduleName.capitalize()
        practiseStatusTV.text = moduleData.statement.capitalize()
        statusImageView.setBackgroundResource(0)
        statusImageView.setImageResource(R.drawable.ic_check_bold)
        if (moduleData.questionComplete.isNotEmpty() && moduleData.practiceIncomplete.isEmpty()) {
            allQuestionComplete = true
            statusImageView.setColorFilter(
                ContextCompat.getColor(context, R.color.wa_color),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

        } else {
            if (moduleData.questionIncomplete.isEmpty()) {
                statusImageView.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.practise_complete_tint
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
            } else {
                statusImageView.setColorFilter(
                    ContextCompat.getColor(
                        context,
                        R.color.disable_color
                    ), android.graphics.PorterDuff.Mode.SRC_IN
                )
                statusImageView.setImageResource(0)
                statusImageView.setBackgroundResource(R.drawable.circle_for_question)
            }
        }

        moduleData.practiceComplete.forEach {
            llPractise.addView(getPractiseView(it, isCompleted = true))
        }
        moduleData.practiceIncomplete.forEach {
            llPractise.addView(getPractiseView(it, isCompleted = false))
        }
    }

    override fun onClick(v: android.view.View) {
        RxBus2.publish(OpenClickProgressEventBus(v.id, postion))
    }

    @Click(R.id.image_view_status)
    fun onClick() {
        if (moduleData.questionComplete.isNotEmpty()) {
            RxBus2.publish(
                OpenClickProgressEventBus(
                    moduleData.questionComplete[0],
                    postion,
                    false
                )
            )
        } else {
            if (moduleData.questionIncomplete.isNotEmpty()) {
                RxBus2.publish(
                    OpenClickProgressEventBus(
                        moduleData.questionIncomplete[0],
                        postion,
                        false
                    )
                )
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun getPractiseView(questionId: Int, isCompleted: Boolean = false): android.view.View {
        val view: AppCompatImageView =
            layoutInflater.inflate(R.layout.practise_progress_layout, null) as AppCompatImageView
        view.setImageResource(R.drawable.ic_check_small)
        if (allQuestionComplete) {
            view.setColorFilter(
                ContextCompat.getColor(context, R.color.wa_color),
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
        } else {
            if (isCompleted) {
                view.setColorFilter(
                    ContextCompat.getColor(context, R.color.practise_complete_tint),
                    android.graphics.PorterDuff.Mode.MULTIPLY
                )
            } else {
                view.setImageResource(0)
                view.setBackgroundResource(R.drawable.circle_for_progress)
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, Utils.dpToPx(context, 4F), 0)

                view.layoutParams = params
            }
        }
        view.setOnClickListener(this)
        view.id = questionId
        return view
    }
}
