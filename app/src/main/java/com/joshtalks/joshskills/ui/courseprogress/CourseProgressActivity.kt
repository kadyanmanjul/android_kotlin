package com.joshtalks.joshskills.ui.courseprogress

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Fade
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.bumptech.glide.Glide
import com.bumptech.glide.integration.webp.decoder.WebpDrawable
import com.bumptech.glide.integration.webp.decoder.WebpDrawableTransformation
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.AnimationView
import com.joshtalks.joshskills.core.custom_ui.TextDrawable
import com.joshtalks.joshskills.core.interfaces.OnDismissClaimCertificateDialog
import com.joshtalks.joshskills.core.interfaces.OnDismissDialog
import com.joshtalks.joshskills.databinding.ActivityCourseProgressBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.entity.ChatModel
import com.joshtalks.joshskills.repository.local.eventbus.ContentClickEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenClickProgressEventBus
import com.joshtalks.joshskills.repository.local.minimalentity.InboxEntity
import com.joshtalks.joshskills.repository.local.model.User
import com.joshtalks.joshskills.repository.server.CertificateDetail
import com.joshtalks.joshskills.ui.chat.CHAT_ROOM_OBJECT
import com.joshtalks.joshskills.ui.chat.FOCUS_ON_CHAT_ID
import com.joshtalks.joshskills.ui.chat.PRACTISE_SUBMIT_REQUEST_CODE
import com.joshtalks.joshskills.ui.chat.PRACTISE_UPDATE_MESSAGE_KEY
import com.joshtalks.joshskills.ui.chat.course_content.ContentTimelineAdapter
import com.joshtalks.joshskills.ui.courseprogress.course_certificate.ClaimCertificateFragment
import com.joshtalks.joshskills.ui.practise.PractiseSubmitActivity
import com.joshtalks.joshskills.ui.video_player.VideoPlayerActivity
import com.joshtalks.joshskills.ui.view_holders.PerformHeaderViewHolder
import com.joshtalks.joshskills.ui.view_holders.PerformItemViewHolder
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.anko.backgroundResource
import retrofit2.HttpException
import java.net.ProtocolException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class CourseProgressActivity : CoreJoshActivity(), OnDismissDialog,
    OnDismissClaimCertificateDialog {

    private lateinit var inboxEntity: InboxEntity
    private val compositeDisposable = CompositeDisposable()
    private val updatePractiseIdList = arrayListOf<String>()
    private var updatePractiseId = EMPTY
    private var updateIndex = -1
    private var id = -1
    private var completePercent: Double = 0.0
    private var unlockPercent: Double = 0.0
    private var certificateDetail: CertificateDetail? = null
    private lateinit var appAnalytics: AppAnalytics

    private val viewModel: CourseProgressViewModel by lazy {
        ViewModelProvider(this).get(CourseProgressViewModel::class.java)
    }

    companion object {
        fun startCourseProgressActivity(
            activity: Activity,
            requestCode: Int,
            inboxEntity: InboxEntity
        ) {
            val intent = Intent(activity, CourseProgressActivity::class.java)
            intent.putExtra(CHAT_ROOM_OBJECT, inboxEntity)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private lateinit var binding: ActivityCourseProgressBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_course_progress)
        binding.handler = this
        if (intent.hasExtra(CHAT_ROOM_OBJECT)) {
            inboxEntity = intent.getParcelableExtra(CHAT_ROOM_OBJECT)!!
            if (inboxEntity.report_status) {
                PrefManager.put(inboxEntity.conversation_id.trim().plus(CERTIFICATE_GENERATE), true)
            }
        }
        appAnalytics = AppAnalytics.create(AnalyticsEvent.CERTIFICATE_SCREEN.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.COURSE_NAME.NAME, inboxEntity.course_name)
            .addParam(AnalyticsEvent.SAMPLE_CERTIFICATE_OPEN.NAME, false)
            .addParam(AnalyticsEvent.CERTIFICATE_PROGRESS_CLICKED.NAME, false)
            .addParam(AnalyticsEvent.PERFORMANCE_CLICKED.NAME, false)
        initView()
        getProgressOfCourse()


    }

    @SuppressLint("DefaultLocale")
    private fun initView() {
        setUserImage(User.getInstance().photo)
        binding.tvUserName.text = User.getInstance().firstName.capitalize()

        binding.tvCourseCompleteStatus.text = HtmlCompat.fromHtml(
            getString(R.string.course_progress_detail),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        binding.sampleCertificateTv.text = HtmlCompat.fromHtml(
            getString(R.string.sample_certificate),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        val sampleCertificate = getString(R.string.sample_certificate)
        val content = SpannableString(sampleCertificate)
        content.setSpan(UnderlineSpan(), 0, sampleCertificate.length, 0)
        binding.sampleCertificateTv.text = content
        addViewInRV()
    }

    fun setUserImage(url: String?) {
        if (url.isNullOrEmpty()) {
            val font = Typeface.createFromAsset(assets, "fonts/OpenSans-SemiBold.ttf")
            val drawable: TextDrawable = TextDrawable.builder()
                .beginConfig()
                .textColor(ContextCompat.getColor(applicationContext, R.color.white))
                .useFont(font)
                .fontSize(Utils.dpToPx(28))
                .toUpperCase()
                .endConfig()
                .buildRound(
                    getUserNameInShort(),
                    ContextCompat.getColor(applicationContext, R.color.button_color)
                )
            binding.userImage.background = drawable
            binding.userImage.setImageDrawable(drawable)
        } else {
            Glide.with(applicationContext)
                .load(url)
                .override(Target.SIZE_ORIGINAL)
                .optionalTransform(
                    WebpDrawable::class.java,
                    WebpDrawableTransformation(CircleCrop())
                )
                .apply(RequestOptions.circleCropTransform())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        return false

                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        setUserImage(null)
                        return false
                    }
                })
                .into(binding.userImage)
        }
    }

    private fun getProgressOfCourse() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val cprponnse =
                    AppObjectController.chatNetworkService.getCourseProgressDetailsAsync(inboxEntity.conversation_id)
                hideProgressBar()
                if (cprponnse.code() == 200) {
                    var cpr = cprponnse.body()!!
                    CoroutineScope(Dispatchers.Main).launch {
                        completePercent = cpr.completePercent
                        appAnalytics.addParam(
                            AnalyticsEvent.COURSE_PROGRESS_PERCENT.NAME,
                            completePercent
                        )
                        unlockPercent = cpr.unlockPercent
                        certificateDetail = cpr.certificateDetail
                        setImageInProgressView(cpr.link)
                        binding.tvCourseDuration.text =
                            getString(
                                R.string.course_duration_day,
                                cpr.startedDay.toString(),
                                cpr.duration.toString()
                            )

                        binding.tvWelcome.text = cpr.header
                        binding.tvCourseStatus.text = cpr.statement

                        val sb = SpannableStringBuilder(
                            getString(
                                R.string.unlock_progress_header,
                                cpr.unlockPercent.toString().plus("%")
                            )
                        )
                        sb.setSpan(
                            StyleSpan(Typeface.BOLD),
                            0,
                            sb.length,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        sb.setSpan(
                            StyleSpan(Typeface.ITALIC),
                            0,
                            sb.length,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )

                        binding.textView1.text = sb

                        val sb2 = SpannableStringBuilder(
                            getString(
                                R.string.course_progress_detail,
                                cpr.completePercent.toString().plus("%")
                            )
                        )
                        val endPos = 19 + cpr.completePercent.toString().length + 1
                        sb2.setSpan(
                            StyleSpan(Typeface.BOLD),
                            19,
                            endPos,
                            Spannable.SPAN_INCLUSIVE_INCLUSIVE
                        )
                        sb2.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    applicationContext,
                                    R.color.black
                                )
                            ), 19, endPos, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        binding.tvCourseCompleteStatus.text = sb2

                        binding.courseProgressBar.progress = abs(cpr.completePercent).toInt()
                        if (cpr.completePercent >= cpr.unlockPercent) {
                            binding.claimCertificateBtn.icon = null
                            binding.claimCertificateBtn.backgroundTintList = ColorStateList.valueOf(
                                ContextCompat.getColor(
                                    applicationContext,
                                    R.color.button_color
                                )
                            )
                        }
                        binding.progressDetailRv.addView(
                            PerformHeaderViewHolder(
                                cpr.totalVideoPractice,
                                cpr.seenVideoPractice
                            )
                        )

                        if (cpr.moduleData.isNullOrEmpty().not()) {
                            var index = 0
                            cpr.moduleData.forEachIndexed { _, moduleData ->
                                index++
                                binding.progressDetailRv.addView(
                                    PerformItemViewHolder(
                                        applicationContext,
                                        moduleData, index
                                    )
                                )
                            }
                        }
                        binding.rootView.setBackgroundColor(
                            ContextCompat.getColor(
                                applicationContext,
                                R.color.wh_fc
                            )
                        )
                        binding.bottomImageView.visibility = View.VISIBLE
                        binding.bottomImageView.setImageResource(R.drawable.bk_progress)
                        val transition: Transition = Fade()
                        transition.duration = 250
                        transition.addTarget(binding.subRootView.id)
                        TransitionManager.beginDelayedTransition(binding.rootView, transition)
                        binding.subRootView.visibility = View.VISIBLE
                    }
                } else if (cprponnse.code() == 204) {
                    showCourseContents()
                }
            } catch (ex: Exception) {
                when (ex) {
                    is ProtocolException -> {
                        showCourseContents()
                    }
                    is HttpException -> {
                        if (ex.code() == 204) {
                            showCourseContents()
                        } else {
                            showToast(getString(R.string.generic_message_for_error))
                        }
                    }
                    is SocketTimeoutException, is UnknownHostException -> {
                        showToast(getString(R.string.internet_not_available_msz))
                    }
                    else -> {
                        showToast(getString(R.string.generic_message_for_error))
                        FirebaseCrashlytics.getInstance().recordException(ex)
                    }
                }
                hideProgressBar()
            }
        }
    }

    private fun showCourseContents() {
        CoroutineScope(Dispatchers.Main).launch {
            val layoutManager = LinearLayoutManager(applicationContext)
            binding.recyclerView.layoutManager = layoutManager
            binding.recyclerView.setHasFixedSize(false)
            viewModel.userContentViewModel.observe(this@CourseProgressActivity, Observer { list ->
                val contentTimelineAdapter =
                    ContentTimelineAdapter(list.filter { it.title.isNullOrEmpty().not() })
                binding.recyclerView.adapter = contentTimelineAdapter
                val transition: Transition = Fade()
                transition.duration = 250
                transition.addTarget(binding.subRootView2.id)
                TransitionManager.beginDelayedTransition(binding.rootView, transition)
                binding.rootView.setBackgroundColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.wh_f4
                    )
                )
                binding.mainView.setBackgroundColor(
                    ContextCompat.getColor(
                        applicationContext,
                        R.color.wh_f4
                    )
                )
                binding.subRootView2.visibility = View.VISIBLE
            })
            viewModel.getReceivedCourseContent(inboxEntity.conversation_id)
        }
    }


    private fun hideProgressBar() {
        CoroutineScope(Dispatchers.Main).launch {
            binding.progressBar.visibility = View.GONE
        }
    }


    private fun addViewInRV() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.progressDetailRv.builder
            .setLayoutManager(linearLayoutManager)
        binding.progressDetailRv.addItemDecoration(
            DividerItemDecoration(
                applicationContext,
                DividerItemDecoration.VERTICAL
            )
        )
        binding.progressDetailRv.itemAnimator = null
    }

    fun certificateProgressView() {
        appAnalytics.addParam(AnalyticsEvent.CERTIFICATE_PROGRESS_CLICKED.NAME, true)
        if (binding.certficateProgressConatiner.isVisible) {
            AnimationView.collapse(binding.certficateProgressConatiner)
            binding.cpIv.setImageResource(R.drawable.ic_baseline_expand_more)
        } else {

            AnimationView.expand(binding.certficateProgressConatiner)
            binding.cpIv.setImageResource(R.drawable.ic_baseline_expand_less)
        }
    }

    fun performanceView() {
        appAnalytics.addParam(AnalyticsEvent.PERFORMANCE_CLICKED.NAME, true)

        if (binding.performanceContainer.isVisible) {
            AnimationView.collapse(binding.performanceContainer)
            binding.performanceLl.backgroundResource = R.drawable.round_rect_default
            binding.ivPv.setImageResource(R.drawable.ic_baseline_expand_more)

        } else {
            binding.performanceLl.backgroundResource = R.drawable.upper_round_rect
            AnimationView.expand(binding.performanceContainer)
            binding.ivPv.setImageResource(R.drawable.ic_baseline_expand_less)
        }
    }

    fun openSampleCertificate() {
        appAnalytics.addParam(AnalyticsEvent.SAMPLE_CERTIFICATE_OPEN.NAME, true)
        val url = AppObjectController.getFirebaseRemoteConfig().getString("CERTIFICATE_URL")
        showPromotionScreen(null, url)
    }

    fun requestForCertificate() {
        if (completePercent > 0 && unlockPercent > 0 && completePercent >= unlockPercent && certificateDetail != null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag("claim_certificate_dialog")
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            appAnalytics.addParam(AnalyticsEvent.CLAIM_CERTIFICATE.NAME, "Clicked")
            fragmentTransaction.addToBackStack(null)
            ClaimCertificateFragment.newInstance(inboxEntity.conversation_id, certificateDetail!!)
                .show(supportFragmentManager, "claim_certificate_dialog")
        } else appAnalytics.addParam(AnalyticsEvent.CLAIM_CERTIFICATE.NAME, "Locked")

    }

    private fun setImageInProgressView(url: String) {
        Glide.with(applicationContext)
            .load(url)
            .override(Target.SIZE_ORIGINAL)
            .optionalTransform(
                WebpDrawable::class.java,
                WebpDrawableTransformation(CircleCrop())
            )
            .into(binding.imageView)
    }


    private fun subscribeBus() {
        compositeDisposable.add(
            RxBus2.listen(OpenClickProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .debounce(2L, TimeUnit.SECONDS)
                .subscribe({
                    CoroutineScope(Dispatchers.IO).launch {
                        val obj: ChatModel? = AppObjectController.appDatabase.chatDao()
                            .getPractiseFromQuestionId(it.id.toString())
                        if (obj != null) {
                            id = it.id
                            updateIndex = it.postion
                            if (it.practiseOpen) {
                                AppAnalytics.create(AnalyticsEvent.PRACTICE_CLICKED_COURSE_OVERVIEW.NAME)
                                    .addParam("Question Id ", it.id).push()
                                updatePractiseId = obj.chatId
                                PractiseSubmitActivity.startPractiseSubmissionActivity(
                                    this@CourseProgressActivity,
                                    PRACTISE_SUBMIT_REQUEST_CODE,
                                    obj
                                )
                            } else {
                                AppAnalytics.create(AnalyticsEvent.VIDEO_CLICKED_COURSE_OVERVIEW.NAME)
                                    .addParam("Question Id ", it.id).push()
                                VideoPlayerActivity.startConversionActivity(
                                    this@CourseProgressActivity,
                                    obj,
                                    inboxEntity.course_name
                                )
                            }
                        }/* else {
                          //  showToast(getString(R.string.viewing_support_app_not_exist))
                        }*/
                    }
                }, {
                    it.printStackTrace()
                })
        )

        compositeDisposable.add(
            RxBus2.listen(ContentClickEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .subscribe({
                    val resultIntent = Intent().apply {
                        putExtra(FOCUS_ON_CHAT_ID, it.courseContentEntity.chat_id)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }, {
                    it.printStackTrace()
                })
        )


    }

    override fun onResume() {
        super.onResume()
        subscribeBus()
    }

    override fun onStop() {
        super.onStop()
        appAnalytics.push()
        compositeDisposable.clear()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PRACTISE_SUBMIT_REQUEST_CODE) {
                updatePractiseIdList.add(updatePractiseId)
                updatePractiseId = EMPTY
                showToast(getString(R.string.answer_submitted))
            }
            updateView()
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateView() {
        val obj: PerformItemViewHolder =
            binding.progressDetailRv.getViewResolverAtPosition(updateIndex) as PerformItemViewHolder

        if (obj.moduleData.questionIncomplete.contains(id)) {
            obj.moduleData.questionIncomplete.remove(id)
            obj.moduleData.questionComplete.add(id)
        }
        if (obj.moduleData.practiceIncomplete.contains(id)) {
            obj.moduleData.practiceIncomplete.remove(id)
            obj.moduleData.practiceComplete.add(id)
        }
        AppObjectController.uiHandler.postDelayed({
            binding.progressDetailRv.refreshView(obj)
        }, 250)
    }


    override fun onBackPressed() {
        super.onBackPressed()
        val resultIntent = Intent().apply {
            putStringArrayListExtra(PRACTISE_UPDATE_MESSAGE_KEY, updatePractiseIdList)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onDismiss() {
        AppAnalytics.create(AnalyticsEvent.SAMPLE_CERTIFICATE_CLOSE.NAME).push()
    }

    override fun onDismiss(certificateDetail: CertificateDetail?) {
        if (this.certificateDetail == null || this.certificateDetail?.name.isNullOrEmpty()) {
            this.certificateDetail = certificateDetail
        }
    }

}
