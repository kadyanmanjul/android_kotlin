package com.joshtalks.joshskills.ui.conversation_practice.extra

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.repository.server.conversation_practice.AnswersModel
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.Job
import timber.log.Timber


class QuizPractiseOptionView : LinearLayout, View.OnClickListener {

    private val context = AppObjectController.joshApplication
    private var compositeDisposable = CompositeDisposable()
    private val jobs = arrayListOf<Job>()

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

    private fun init(answersModel: List<AnswersModel>) {
        answersModel.sortedBy { it.sortOrder }.forEach {


        }

        val mTextureFrame =
            LayoutInflater.from(context).inflate(R.layout.quiz_practise_option_view, this, false)
        addView(mTextureFrame)


        View.inflate(context, R.layout.quiz_practise_option_view, this)

    }

    override fun onClick(v: View) {
        if (v.id == R.id.btnPlay) {

        } else if (v.id == R.id.btnPause) {

        }
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        Timber.tag("onAttachedToWindow").e("AudioPlayer")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }
}
