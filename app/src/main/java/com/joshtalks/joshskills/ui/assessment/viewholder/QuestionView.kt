package com.joshtalks.joshskills.ui.assessment.viewholder


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.MiniExoPlayer
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.server.assessment.AssessmentMediaType
import com.joshtalks.joshskills.repository.server.assessment.AssessmentQuestionResponse
import com.joshtalks.joshskills.ui.assessment.view.AudioPlayerView
import com.joshtalks.joshskills.ui.assessment.view.Stub
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class QuestionView : FrameLayout {

    private var message: AssessmentQuestionResponse? = null
    private var miniExoPlayerStub: Stub<MiniExoPlayer>? = null
    private lateinit var questionTV: JoshTextView
    private lateinit var audioPlayerView: AudioPlayerView
    private lateinit var imageView: AppCompatImageView
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

    private fun init() {
        View.inflate(context, R.layout.question_view, this)
        miniExoPlayerStub = Stub(findViewById(R.id.video_view_stub))
        questionTV = findViewById(R.id.tv_question)
        audioPlayerView = findViewById(R.id.audio_player_view)
        imageView = findViewById(R.id.image_view)
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        jobs.forEach { it.cancel() }
        miniExoPlayerStub?.get()?.pausePlayer()

    }

    fun bind(message: AssessmentQuestionResponse) {
        this.message = message
        setUpUI()
    }

    private fun setUpUI() {
        message?.let { it ->
            jobs += CoroutineScope(Dispatchers.Main).launch {
                questionTV.text = HtmlCompat.fromHtml(it.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
                when (it.mediaType) {
                    AssessmentMediaType.IMAGE -> {
                        imageView.visibility = View.VISIBLE
                        Glide.with(context)
                            .load(it.mediaUrl)
                            .override(Target.SIZE_ORIGINAL)
                            .optionalTransform(
                                WebpDrawable::class.java,
                                WebpDrawableTransformation(CircleCrop())
                            )
                            .into(imageView)
                    }
                    AssessmentMediaType.AUDIO -> {
                        audioPlayerView.visibility = View.VISIBLE
                        audioPlayerView.setupAudio(it.id, it.mediaUrl)
                    }
                    AssessmentMediaType.VIDEO -> {
                        miniExoPlayerStub?.get()?.visibility = View.VISIBLE
                        miniExoPlayerStub?.get()?.run {
                            this.setUrl(it.mediaUrl)
                            this.initVideo()
                        }
                    }
                }
            }
        }


    }
}
