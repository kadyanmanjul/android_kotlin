package com.joshtalks.joshskills.ui.introduction


import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.DEMO_P2P_CALLEE_NAME
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.PrefManager
import com.joshtalks.joshskills.core.showToast
import com.joshtalks.joshskills.databinding.DemoSpeakingPractiseFragmentBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.SnackBarEvent
import com.joshtalks.joshskills.ui.lesson.LessonViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.inbox_toolbar.text_message_title

class DemoSpeakingPractiseActivity : BaseActivity() {

    private lateinit var binding: DemoSpeakingPractiseFragmentBinding
    private var compositeDisposable = CompositeDisposable()
    private var topicId: String? = EMPTY
    private val viewModel: LessonViewModel by lazy {
        ViewModelProvider(this).get(LessonViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.demo_speaking_practise_fragment)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.rootView.layoutTransition?.setAnimateParentHierarchy(false)
        topicId = intent.getStringExtra(TOPIC_ID)
        initTabLayout(intent.getIntExtra(LESSON_NAME, 0))
        addObservers()
    }

    private fun initTabLayout(stringExtra: Int) {

        text_message_title.text =
            getString(R.string.lesson_no, stringExtra)
        val helpIv: ImageView = findViewById(R.id.iv_help)
        helpIv.visibility = View.VISIBLE
        findViewById<View>(R.id.iv_back).visibility = View.GONE
        findViewById<View>(R.id.iv_back).setOnClickListener {
            onBackPressed()
        }
        helpIv.setOnClickListener {
            openHelpActivity()
        }
    }


    private fun addObservers() {

        binding.btnStartNow.setOnClickListener {
            startDemoCourseDetail()
        }

        viewModel.speakingTopicLiveData.observe(this, { response ->
            binding.progressView.visibility = View.GONE
            if (response == null) {
                showToast(AppObjectController.joshApplication.getString(R.string.generic_message_for_error))
            } else {
                try {
                    binding.tvPractiseTime.text = response.alreadyTalked.toString().plus(" / ").plus(response.duration.toString())
                    val name = PrefManager.getStringValue(DEMO_P2P_CALLEE_NAME)
                    if (name.isBlank()) {
                        binding.spokeWith.visibility = View.INVISIBLE
                    } else {
                        binding.spokeWith.visibility = View.VISIBLE
                        binding.spokeWith.text =
                            getString(R.string.spoke_with, response.alreadyTalked.toString(), name)
                    }
                    binding.progressBar.progress = response.alreadyTalked.toFloat()
                    binding.progressBar.progressMax = response.duration.toFloat()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                }
            }
        })
    }

    private fun startDemoCourseDetail() {
        DemoCourseDetailsActivity.startDemoCourseDetailsActivity(
            activity = this,
            testId = 201,
            startedFrom = "DemoCourseDetailsActivity",
            flags = arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    private fun subscribeRXBus() {

        if (topicId.isNullOrBlank().not()) {
            viewModel.getTopicDetail(topicId!!)
        }

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(SnackBarEvent::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({

                }, {
                    it.printStackTrace()
                })
        )
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    companion object {
        const val TOPIC_ID = "topic-id"
        const val LESSON_NAME = "lesson_name"

        fun startDemoSpeakingActivity(
            activity: Activity,
            topicId: String?,
            lessonName: Int?,
            flags: Array<Int> = arrayOf()
        ) {
            Intent(activity, DemoSpeakingPractiseActivity::class.java).apply {
                putExtra(TOPIC_ID, topicId)
                putExtra(LESSON_NAME, lessonName)
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }

        fun getIntent(
            context: Context,
            topicId: String?,
            lessonName: Int?,
            flags: Array<Int> = arrayOf(),
        ) = Intent(context, DemoSpeakingPractiseActivity::class.java).apply {
            putExtra(TOPIC_ID, topicId)
            putExtra(LESSON_NAME, lessonName)
            flags.forEach { flag ->
                this.addFlags(flag)
            }
        }
    }

}
