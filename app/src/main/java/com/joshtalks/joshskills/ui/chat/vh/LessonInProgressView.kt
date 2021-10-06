package com.joshtalks .joshskills.ui.chat.vh

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.res.ResourcesCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.google.android.material.textview.MaterialTextView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.extension.setImageInLessonView
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.LESSON_STATUS
import com.joshtalks.joshskills.repository.local.entity.LessonModel
import com.joshtalks.joshskills.repository.local.eventbus.LessonItemClickEventBus
import com.joshtalks.joshskills.ui.lesson.GRAMMAR_POSITION
import com.joshtalks.joshskills.ui.lesson.READING_POSITION
import com.joshtalks.joshskills.ui.lesson.SPEAKING_POSITION
import com.joshtalks.joshskills.ui.lesson.VOCAB_POSITION

private const val TAG = "LessonInProgressView"
class LessonInProgressView : FrameLayout {
    private lateinit var rootView: FrameLayout
    private var lessonModel: LessonModel? = null
    var lastLessonPosition:Int = 0

    private lateinit var imageView: AppCompatImageView
    private lateinit var lessonNameTv: AppCompatTextView
    private lateinit var startLessonTv: AppCompatTextView
    private lateinit var startLessonTvShimmer: LottieAnimationView
    private lateinit var continueLessonTv: AppCompatTextView

    private lateinit var grammarStatus: LottieAnimationView
    private lateinit var vocabStatus: LottieAnimationView
    private lateinit var readingStatus: LottieAnimationView
    private lateinit var speakingStatus: LottieAnimationView

    private val drawableAttempted: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_lesson_green_tick,
            null
        )
    }
    private val drawableUnattempted: Drawable? by lazy {
        ResourcesCompat.getDrawable(
            AppObjectController.joshApplication.resources,
            R.drawable.ic_lesson_disabled_tick,
            null
        )
    }


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
        View.inflate(context, R.layout.lesson_progress_view, this)
        rootView = findViewById(R.id.root_view)
        rootView.setOnClickListener {
            lessonModel?.let {
                RxBus2.publish(LessonItemClickEventBus(it.id, it.isNewGrammar))
            }
        }
        imageView = findViewById(R.id.lesson_iv)
        lessonNameTv = findViewById(R.id.lesson_name_tv)
        startLessonTv = findViewById<MaterialTextView>(R.id.start_lesson_tv)
        startLessonTvShimmer = findViewById<LottieAnimationView>(R.id.start_lesson_tv_shimmer)
        continueLessonTv = findViewById(R.id.continue_lesson_tv)

        grammarStatus = when (GRAMMAR_POSITION) {
            0 -> findViewById(R.id.view1)
            1 -> findViewById(R.id.view2)
            2 -> findViewById(R.id.view3)
            3 -> findViewById(R.id.view4)
            else -> findViewById(R.id.view1)
        }
        vocabStatus = when (VOCAB_POSITION) {
            0 -> findViewById(R.id.view1)
            1 -> findViewById(R.id.view2)
            2 -> findViewById(R.id.view3)
            3 -> findViewById(R.id.view4)
            else -> findViewById(R.id.view2)
        }
        readingStatus = when (READING_POSITION) {
            0 -> findViewById(R.id.view1)
            1 -> findViewById(R.id.view2)
            2 -> findViewById(R.id.view3)
            3 -> findViewById(R.id.view4)
            else -> findViewById(R.id.view3)
        }
        speakingStatus = when (SPEAKING_POSITION) {
            0 -> findViewById(R.id.view1)
            1 -> findViewById(R.id.view2)
            2 -> findViewById(R.id.view3)
            3 -> findViewById(R.id.view4)
            else -> findViewById(R.id.view4)
        }
    }

    fun setup(lesson: LessonModel,lastLesson:Int?) {
        this.lessonModel = lesson
        lessonNameTv.text = context.getString(
            R.string.lesson_name,
            lesson.lessonNo,
            lesson.lessonName
        )
        imageView.setImageInLessonView(lesson.thumbnailUrl)
        setupUI(lesson,lastLesson)
    }

     fun setupUI(lesson: LessonModel,lastLesson: Int?) {
        var lastLessonNumber:Int? = lastLesson
        lastLessonPosition = lesson.lessonNo
        if (lesson.status == LESSON_STATUS.AT) {
            startLessonTv.visibility = GONE
            startLessonTvShimmer.visibility = GONE
            continueLessonTv.visibility = View.VISIBLE
            grammarStatus.visibility = View.VISIBLE
            vocabStatus.visibility = View.VISIBLE
            readingStatus.visibility = View.VISIBLE
            speakingStatus.visibility = View.VISIBLE

            if (lesson.grammarStatus == LESSON_STATUS.CO) {
                setUpLottieGreen(grammarStatus)
            } else {
                showBounce(lesson,lastLessonNumber)
            }
            if (lesson.speakingStatus == LESSON_STATUS.CO) {
                setUpLottieGreen(speakingStatus)
            } else {
                showBounce(lesson,lastLessonNumber)
            }
            if (lesson.vocabStatus == LESSON_STATUS.CO) {
                setUpLottieGreen(vocabStatus)
            } else {
                showBounce(lesson,lastLessonNumber)
            }
            if (lesson.readingStatus == LESSON_STATUS.CO) {
                setUpLottieGreen(readingStatus)
            } else {
                showBounce(lesson,lastLessonNumber)
            }

            if (lesson.grammarStatus == LESSON_STATUS.CO && lesson.speakingStatus == LESSON_STATUS.CO
                && lesson.vocabStatus == LESSON_STATUS.CO && lesson.readingStatus == LESSON_STATUS.CO){
//                //MOVE TO PREVIOUS LESSON
//                lastLessonNumber = lastLessonNumber?.minus(1)
//                lastLessonPosition=  lastLessonPosition.minus(1)
//                showBounce(lesson,lastLessonNumber)
            }

        } else if(lesson.status == LESSON_STATUS.NO || lesson.status == LESSON_STATUS.CO){
//            lastLessonNumber = lastLessonNumber?.minus(1)
//            lastLessonPosition=  lastLessonPosition.minus(1)
//
//            //previous par ja ke check karna hoga ki kon sa complete ho gaya kon sa bacha hai or acsending order bhi maintain karna hoga
//            //yaha par showBounce method ko nahi call karna hai yaha par setUPUI ko vapis se call call karna hai lesson last = lastlesson -1 pass karna hai
//            showBounce(lesson,lastLessonNumber)
        } else{
            grammarStatus.visibility = GONE
            vocabStatus.visibility = GONE
            readingStatus.visibility = GONE
            speakingStatus.visibility = GONE
            startLessonTv.visibility = View.VISIBLE
            startLessonTvShimmer.visibility = View.VISIBLE
            continueLessonTv.visibility = GONE
        }
    }

    fun showBounce(lesson: LessonModel,position:Int?) {
        if (position == lastLessonPosition && lesson.status == LESSON_STATUS.AT) {
            if (lesson.grammarStatus != LESSON_STATUS.CO) {
                setUpLottie(grammarStatus, GRAMMAR_POSITION)
            } else if (lesson.speakingStatus != LESSON_STATUS.CO) {
                setUpLottie(speakingStatus, SPEAKING_POSITION)
            } else if (lesson.vocabStatus != LESSON_STATUS.CO) {
                setUpLottie(vocabStatus, VOCAB_POSITION)
            } else if (lesson.readingStatus != LESSON_STATUS.CO) {
                setUpLottie(readingStatus, READING_POSITION)
            } else {
                if (lesson.grammarStatus == LESSON_STATUS.CO)
                    setUpLottieGreen(grammarStatus)
                if (lesson.speakingStatus == LESSON_STATUS.CO)
                    setUpLottieGreen(speakingStatus)
                if (lesson.vocabStatus == LESSON_STATUS.CO)
                    setUpLottieGreen(vocabStatus)
                if (lesson.readingStatus == LESSON_STATUS.CO)
                    setUpLottieGreen(readingStatus)
            }
        }
    }
    fun setUpLottie(lottieAnimationView:LottieAnimationView,position: Int){
        Log.d(
            "sagar",
            "setUpLottie() called with: lottieAnimationView = ${lottieAnimationView.isAnimating}, position = $position , lesson = ${lessonModel?.lessonNo}"
        )
        when (position) {
            GRAMMAR_POSITION -> {
                lottieAnimationView.loop(true)
                lottieAnimationView.scale = 1f
                lottieAnimationView.speed = 1f
                lottieAnimationView.playAnimation()
            }
            SPEAKING_POSITION -> {
                lottieAnimationView.loop(true)
                lottieAnimationView.scale = 1f
                lottieAnimationView.speed = 1f
                lottieAnimationView.playAnimation()
            }
            VOCAB_POSITION -> {
                lottieAnimationView.loop(true)
                lottieAnimationView.scale = 1f
                lottieAnimationView.speed = 1f
                lottieAnimationView.playAnimation()
            }
            READING_POSITION -> {
                lottieAnimationView.loop(true)
                lottieAnimationView.scale = 1f
                lottieAnimationView.speed = 1f
                lottieAnimationView.playAnimation()
            }
        }
        Log.d("sagar1", "setUpLottie: ${lottieAnimationView.isAnimating}")
    }

    fun setUpLottieGreen(lottieAnimationView:LottieAnimationView){
        lottieAnimationView.setAnimation("lottie/green.json")
    }

    fun setUpLottieUnAttempted(lottieAnimationView:LottieAnimationView){
        lottieAnimationView.setAnimation("lottie/tickbounce.json")
    }


}