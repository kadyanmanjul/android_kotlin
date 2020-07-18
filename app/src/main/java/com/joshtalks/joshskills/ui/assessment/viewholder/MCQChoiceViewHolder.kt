package com.joshtalks.joshskills.ui.assessment.viewholder

import android.content.Context
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.crashlytics.android.Crashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
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
    private var assessmentType: AssessmentType,
    private var assessmentStatus: AssessmentStatus,
    private var isQuestionAttempted: Boolean,
    private var onClickListener: OnChoiceClickListener,
    override val context: Context = AppObjectController.joshApplication
) : ChoiceBaseCell(type, sequenceNumber, choiceData, context) {

    @com.mindorks.placeholderview.annotations.View(R.id.choice_container)
    lateinit var container: CardView

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

        setColor()
    }

    @Click(R.id.choice_container)
    fun onClick() {
        choiceData.isSelectedByUser = true
        onClickListener.onChoiceClick(choiceData)

    }

    private fun setColor() {

        if (assessmentType == AssessmentType.QUIZ) {
            // For AssessmentType.QUIZ

            if (assessmentStatus == AssessmentStatus.NOT_STARTED || assessmentStatus == AssessmentStatus.STARTED) {
                // For AssessmentStatus.STARTED OR AssessmentStatus.NOT_STARTED

                if (isQuestionAttempted) {
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

                if (isQuestionAttempted) {
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

            if (assessmentStatus == AssessmentStatus.NOT_STARTED || assessmentStatus == AssessmentStatus.STARTED) {
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

                if (isQuestionAttempted) {
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
        setDrawableEnd(null)
        setBackgroundColor(R.color.artboard_color)
    }

    private fun setSelectedChoiceView() {
        setTextColor(R.color.button_color)
        setDrawableEnd(null)
        setBackgroundColor(R.color.light_blue)
    }

    private fun setWrongChoiceView() {
        setTextColor(R.color.white)
        setDrawableEnd(R.drawable.ic_cross)
        setBackgroundColor(R.color.error_color)
    }

    private fun setCorrectlySelectedChoiceView() {
        setTextColor(R.color.white)
        setDrawableEnd(R.drawable.ic_small_tick)
        setBackgroundColor(R.color.green_right_answer)
    }

    private fun setCorrectButNotSelectedChoiceView() {
        setTextColor(R.color.green_right_answer)
        setDrawableEnd(null)
        setBackgroundColor(R.color.lighter_green)
    }

    private fun setTextColor(colorId: Int) =
        choiceTextView.setTextColor(ContextCompat.getColor(context, colorId))

    private fun setDrawableEnd(drawableId: Int?) =
        choiceTextView.setCompoundDrawablesWithIntrinsicBounds(
            null, null, drawableId?.let {
                ContextCompat.getDrawable(
                    context,
                    it
                )
            }, null
        )

    private fun setBackgroundColor(colorId: Int) = container.setCardBackgroundColor(
        ContextCompat.getColor(
            context,
            colorId
        )
    )

}
