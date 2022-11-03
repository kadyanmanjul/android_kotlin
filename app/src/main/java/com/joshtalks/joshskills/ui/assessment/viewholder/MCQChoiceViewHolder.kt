package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
import com.joshtalks.joshskills.ui.assessment.listener.OnChoiceClickListener
import com.mindorks.placeholderview.annotations.Click
import com.mindorks.placeholderview.annotations.Layout
import com.mindorks.placeholderview.annotations.Resolve
import timber.log.Timber
import java.io.InvalidClassException

@Layout(R.layout.mcq_choice_view_holder)
class MCQChoiceViewHolder(
    override val type: ChoiceType,
    override val sequenceNumber: Int,
    override var choiceData: Choice,
    private var assessment: Assessment,
    private var assessmentQuestion: AssessmentQuestion,
    private var onClickListener: OnChoiceClickListener,
    override val context: Context = AppObjectController.joshApplication
) : ChoiceBaseCell(type, sequenceNumber, choiceData, context) {

    @com.mindorks.placeholderview.annotations.View(R.id.choice_container)
    lateinit var container: MaterialCardView

    @com.mindorks.placeholderview.annotations.View(R.id.choice_textview)
    lateinit var choiceTextView: AppCompatTextView

    @com.mindorks.placeholderview.annotations.View(R.id.choice_imgview)
    lateinit var choiceImgView: AppCompatImageView

    @Resolve
    fun onResolved() {
        when (type) {

            ChoiceType.SINGLE_SELECTION_TEXT, ChoiceType.MULTI_SELECTION_TEXT -> {
                choiceTextView.text = choiceData.text
                choiceTextView.visibility = View.VISIBLE
            }

            ChoiceType.SINGLE_SELECTION_IMAGE, ChoiceType.MULTI_SELECTION_IMAGE -> {
                choiceData.imageUrl?.let {
                    setDefaultImageView(choiceImgView, it)
                    choiceImgView.visibility = View.VISIBLE
                } ?: FirebaseCrashlytics.getInstance()
                    .recordException(InvalidClassException("Choice ImageUrl is Null"))
            }

            else -> {
                Timber.tag("Wrong Choice Type").e("Wrong Choice Type in  MCQViewHolder")
                FirebaseCrashlytics.getInstance()
                    .recordException(InvalidClassException("Wrong Choice Type"))
            }

        }

        if (assessmentQuestion.isAttempted) {
            container.setRippleColorResource(R.color.transparent)
        } else {
            container.setRippleColorResource(R.color.dark_grey)
        }

        setColor()
    }

    @Click(R.id.choice_container)
    fun onClick() {
        choiceData.imageUrl?.let {
            logImageClickedEvent(choiceData)
        }
        onClickListener.onChoiceClick(choiceData)

    }

    private fun logImageClickedEvent(choice: Choice) {
        AppAnalytics.create(AnalyticsEvent.ASSESSMENT_IMAGE_CLICKED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.CHOICE_ID.NAME,choice.remoteId)
            .push()
    }

    private fun setColor() {

        if (assessment.type == AssessmentType.QUIZ) {
            // For AssessmentType.QUIZ

            if (assessment.status == AssessmentStatus.NOT_STARTED || assessment.status == AssessmentStatus.STARTED) {
                // For AssessmentStatus.STARTED OR AssessmentStatus.NOT_STARTED

                if (assessmentQuestion.isAttempted) {
                    // For Question Submitted

                    if (choiceData.isSelectedByUser) {
                        // For Choice Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectlySelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setWrongChoiceView()
                        }

                    } else {
                        // For Choice Not Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectButNotSelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setUnselectedChoiceView()
                        }

                    }

                } else {
                    // For Question Not Submitted Yet

                    if (choiceData.isSelectedByUser) {
                        // For Choice Selected
                        setSelectedChoiceView()
                    } else {
                        // For Choice Not Selected
                        setUnselectedChoiceView()

                    }

                }

            } else {
                // For AssessmentStatus.COMPLETED

                if (assessmentQuestion.isAttempted) {
                    // For Question Submitted

                    if (choiceData.isSelectedByUser) {
                        // For Choice Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectlySelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setWrongChoiceView()
                        }

                    } else {
                        // For Choice Not Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectButNotSelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setUnselectedChoiceView()
                        }

                    }

                } else {
                    // For Question Not Submitted Yet

                    if (choiceData.isCorrect) {
                        // For Choice Selected
                        setCorrectButNotSelectedChoiceView()
                    } else {
                        // For Choice Not Selected
                        setUnselectedChoiceView()

                    }

                }

            }

        } else {
            // For AssessmentType.TEST

            if (assessment.status == AssessmentStatus.NOT_STARTED || assessment.status == AssessmentStatus.STARTED) {
                // For AssessmentStatus.STARTED OR AssessmentStatus.NOT_STARTED


                if (choiceData.isSelectedByUser) {
                    // For Choice Selected
                    setSelectedChoiceView()
                } else {
                    // For Choice Not Selected
                    setUnselectedChoiceView()

                }


            } else {
                // For AssessmentStatus.COMPLETED

                if (assessmentQuestion.isAttempted) {
                    // For Question Submitted

                    if (choiceData.isSelectedByUser) {
                        // For Choice Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectlySelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setWrongChoiceView()
                        }

                    } else {
                        // For Choice Not Selected

                        if (choiceData.isCorrect) {
                            // For Choice isCorrectAnswer
                            setCorrectButNotSelectedChoiceView()
                        } else {
                            // For Choice isNotCorrectAnswer
                            setUnselectedChoiceView()
                        }

                    }

                } else {
                    // For Question Not Submitted Yet

                    if (choiceData.isCorrect) {
                        // For Choice Selected
                        setCorrectButNotSelectedChoiceView()
                    } else {
                        // For Choice Not Selected
                        setUnselectedChoiceView()

                    }

                }

            }

        }

    }

    private fun setUnselectedChoiceView() {
        setTextColor(R.color.dark_grey)
        setBackgroundColor(R.color.pure_grey)
        setBorderColor(R.color.pure_grey)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_unchecked, R.color.dark_grey)
        } else {
            setDrawableStart(R.drawable.ic_check_box_outline_blank, R.color.dark_grey)
        }
    }

    private fun setSelectedChoiceView() {
        setTextColor(R.color.primary_500)
        setBackgroundColor(R.color.primary_400)
        setBorderColor(R.color.primary_500)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.primary_500)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.primary_500)
        }
    }

    private fun setWrongChoiceView() {
        setTextColor(R.color.critical)
        setBackgroundColor(R.color.surface_critical)
        setBorderColor(R.color.critical)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.critical)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.critical)
        }
    }

    private fun setCorrectlySelectedChoiceView() {
        setTextColor(R.color.success)
        setBackgroundColor(R.color.surface_success)
        setBorderColor(R.color.success)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.success)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.success)
        }
    }

    private fun setCorrectButNotSelectedChoiceView() {
        setTextColor(R.color.pure_white)
        setBackgroundColor(R.color.success)
        setBorderColor(R.color.success)
        setDrawableStart(R.drawable.ic_tick_small, R.color.pure_white)
    }

    private fun setTextColor(colorId: Int) =
        choiceTextView.setTextColor(ContextCompat.getColor(context, colorId))

    private fun setDrawableStart(drawableId: Int?, tintColorId: Int) {
        val drawable = drawableId?.let {
            ContextCompat.getDrawable(
                context,
                it
            )
        }
        drawable?.setTint(
            ContextCompat.getColor(
                context,
                tintColorId
            )
        )
        choiceTextView.setCompoundDrawablesWithIntrinsicBounds(
            drawable, null, null, null
        )
    }

    private fun setBackgroundColor(colorId: Int) = container.setCardBackgroundColor(
        ContextCompat.getColor(
            context,
            colorId
        )
    )

    private fun setBorderColor(colorId: Int) {
        container.strokeColor = ContextCompat.getColor(
            context,
            colorId
        )
    }

}
