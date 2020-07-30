package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.google.android.material.card.MaterialCardView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.local.model.assessment.Assessment
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestion
import com.joshtalks.joshskills.repository.local.model.assessment.Choice
import com.joshtalks.joshskills.repository.server.assessment.AssessmentStatus
import com.joshtalks.joshskills.repository.server.assessment.AssessmentType
import com.joshtalks.joshskills.repository.server.assessment.ChoiceType
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
                } ?: Crashlytics.logException(InvalidClassException("Choice ImageUrl is Null"))
            }

            else -> {
                Timber.tag("Wrong Choice Type").e("Wrong Choice Type in  MCQViewHolder")
                Crashlytics.logException(InvalidClassException("Wrong Choice Type"))
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
        onClickListener.onChoiceClick(choiceData)

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
        setTextColor(R.color.light_grey)
        setBackgroundColor(R.color.artboard_color)
        setBorderColor(R.color.artboard_color)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_unchecked, R.color.light_grey)
        } else {
            setDrawableStart(R.drawable.ic_check_box_outline_blank, R.color.light_grey)
        }
    }

    private fun setSelectedChoiceView() {
        setTextColor(R.color.button_color)
        setBackgroundColor(R.color.light_blue)
        setBorderColor(R.color.button_color)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.button_color)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.button_color)
        }
    }

    private fun setWrongChoiceView() {
        setTextColor(R.color.error_color)
        setBackgroundColor(R.color.light_red)
        setBorderColor(R.color.error_color)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.error_color)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.error_color)
        }
    }

    private fun setCorrectlySelectedChoiceView() {
        setTextColor(R.color.green_right_answer)
        setBackgroundColor(R.color.lighter_green)
        setBorderColor(R.color.green_right_answer)
        if (type == ChoiceType.SINGLE_SELECTION_TEXT || type == ChoiceType.SINGLE_SELECTION_IMAGE) {
            setDrawableStart(R.drawable.ic_radio_button_checked, R.color.green_right_answer)
        } else {
            setDrawableStart(R.drawable.ic_check_box, R.color.green_right_answer)
        }
    }

    private fun setCorrectButNotSelectedChoiceView() {
        setTextColor(R.color.white)
        setBackgroundColor(R.color.green_right_answer)
        setBorderColor(R.color.green_right_answer)
        setDrawableStart(R.drawable.ic_tick_small, R.color.white)
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
