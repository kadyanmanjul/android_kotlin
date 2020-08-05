package com.joshtalks.joshskills.ui.assessment.viewholder


import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatImageView
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.target.Target
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.custom_ui.MiniExoPlayer
import com.joshtalks.joshskills.core.custom_ui.custom_textview.JoshTextView
import com.joshtalks.joshskills.repository.local.model.assessment.AssessmentQuestionWithRelations
import com.joshtalks.joshskills.repository.server.assessment.AssessmentMediaType
import com.joshtalks.joshskills.ui.assessment.view.AudioPlayerView
import com.joshtalks.joshskills.ui.assessment.view.Stub
import timber.log.Timber

class QuestionView : FrameLayout {

    private var assessmentQuestion: AssessmentQuestionWithRelations? = null
    private lateinit var questionTV: JoshTextView
    private lateinit var cardView: CardView
    private var miniExoPlayerStub: Stub<MiniExoPlayer>? = null
    private var audioPlayerStub: Stub<AudioPlayerView>? = null
    private var imageViewStub: Stub<AppCompatImageView>? = null

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
        questionTV = findViewById(R.id.tv_question)
        cardView = findViewById(R.id.card_view)
        miniExoPlayerStub = Stub(findViewById(R.id.video_view_stub))
        audioPlayerStub = Stub(findViewById(R.id.audio_player_stub))
        imageViewStub = Stub(findViewById(R.id.image_view_stub))
    }


    fun bind(assessmentQuestion: AssessmentQuestionWithRelations) {
        this.assessmentQuestion = assessmentQuestion
        setUpUI()
    }

    fun unBind() {
    }

    private fun setUpUI() {
        assessmentQuestion?.let { it ->
            questionTV.text =
                HtmlCompat.fromHtml(it.question.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
            when (it.question.mediaType) {
                AssessmentMediaType.IMAGE -> {
                    imageViewStub?.run {
                        if (this.resolved().not()) {
                            this.get()?.let { imageView ->
                                Glide.with(context)
                                    .load(it.question.mediaUrl)
                                    .override(Target.SIZE_ORIGINAL)
                                    .optionalTransform(
                                        WebpDrawable::class.java,
                                        WebpDrawableTransformation(CircleCrop())
                                    )
                                    .into(imageView)

                            }
                        }
                    }
                    return@let

                }
                AssessmentMediaType.AUDIO -> {
                    audioPlayerStub?.run {
                        if (this.resolved().not()) {
                            this.get()
                                ?.setupAudio(it.question.remoteId.toString(), it.question.mediaUrl)
                            cardView.cardElevation = 0F
                            cardView.radius = 0F
                        }
                    }
                    return@let
                }
                AssessmentMediaType.VIDEO -> {
                    miniExoPlayerStub?.run {
                        if (this.resolved().not()) {
                            this.get()?.setUrl(it.question.mediaUrl, it.question.videoThumbnailUrl,it.question.remoteId)
                        }
                    }
                    return@let
                }
                else -> {
                    cardView.visibility= View.GONE
                    return@let
                }

            }
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Timber.tag("onDetachedFromWindow").e("QuestionView")
    }
}
