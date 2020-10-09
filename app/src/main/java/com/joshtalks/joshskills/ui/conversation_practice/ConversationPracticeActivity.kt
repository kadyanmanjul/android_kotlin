package com.joshtalks.joshskills.ui.conversation_practice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.core.analytics.AppAnalytics
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityConversationPractice2Binding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseSubmitEventBus
import com.joshtalks.joshskills.repository.local.eventbus.VPPageChangeEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.PractiseViewPagerAdapter
import com.joshtalks.joshskills.ui.conversation_practice.extra.ConversationPracticeIntro
import com.joshtalks.joshskills.ui.conversation_practice.history.SubmittedPractiseActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

const val PRACTISE_ID = "practise_id"
const val IMAGE_URL = "image_url"

class ConversationPracticeActivity : CoreJoshActivity() {

    private val compositeDisposable = CompositeDisposable()
    private lateinit var practiseId: String
    private lateinit var binding: ActivityConversationPractice2Binding

    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation_practice_2)
        binding.lifecycleOwner = this
        binding.handler = this
        addObserver()
        intent?.getStringExtra(PRACTISE_ID)?.run {
            practiseId = this
            viewModel.fetchConversationPractice(this)
        }
        logConversationPracticeEvent(practiseId)
    }

    private fun logConversationPracticeEvent(id: String) {
        AppAnalytics.create(AnalyticsEvent.CONVERSATION_PRACTISE_STARTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam(AnalyticsEvent.CONVERSATION_PRACTISE_ID.NAME, id)
            .push()
    }

    private fun addObserver() {
        viewModel.apiCallStatusLiveData.observe(this, Observer {
            if (ApiCallStatus.START == it) {
                FullScreenProgressDialog.showProgressBar(this)
            } else {
                FullScreenProgressDialog.hideProgressBar(this)
                binding.progressBar.visibility = View.GONE
            }

        })
        viewModel.conversationPracticeLiveData.observe(this, Observer { it ->
            it?.let { obj ->
                openIntroScreen(obj)
                obj.listen.sortedBy { it.sortOrder }.filter { it.name == obj.characterNameB }
                    .listIterator().forEach {
                        it.viewType = ViewTypeForPractiseUser.SECOND.type
                    }
                binding.viewPager.adapter =
                    PractiseViewPagerAdapter(this@ConversationPracticeActivity, obj)
                initViewPagerTab()
            }
        })
        viewModel.successApiLiveData.observe(this, Observer {
            SubmittedPractiseActivity.startSubmittedPractiseActivity(this, practiseId)
            this.finish()
        })
    }

    private fun initViewPagerTab() {
        val tabName = resources.getStringArray(R.array.c_practise_tab)
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = tabName[position]

        }.attach()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (position == 3) {
                    binding.historyIv.visibility = View.VISIBLE
                } else {
                    binding.historyIv.visibility = View.INVISIBLE
                }
            }
        })
    }

    private fun openIntroScreen(conversationPractiseModel: ConversationPractiseModel) {
        ConversationPracticeIntro.newInstance(conversationPractiseModel)
            .show(supportFragmentManager, "Conversation Practice Intro")
    }


    private fun subscribeBus() {
        compositeDisposable.add(
            RxBus2.listen(ViewPagerDisableEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.viewPager.isUserInputEnabled = it.flag.not()

                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(ConversationPractiseSubmitEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    viewModel.submitPractise(practiseId, it.text)
                }, {
                    it.printStackTrace()
                })
        )
        compositeDisposable.add(
            RxBus2.listen(VPPageChangeEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    binding.viewPager.currentItem = binding.viewPager.currentItem + 1
                }, {
                    it.printStackTrace()
                })
        )
    }

    fun openSubmittedPractise() {
        SubmittedPractiseActivity.startSubmittedPractiseActivity(this, practiseId)
    }

    override fun onResume() {
        super.onResume()
        subscribeBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onBackPressed() {
        this.finish()
        super.onBackPressed()
    }

    companion object {
        fun startConversationPracticeActivity(
            activity: Activity,
            requestCode: Int,
            practiseId: String,
            imageUrl: String? = null
        ) {
            val intent = Intent(activity, ConversationPracticeActivity::class.java).apply {
                putExtra(PRACTISE_ID, practiseId)
                putExtra(IMAGE_URL, imageUrl)
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }
}

