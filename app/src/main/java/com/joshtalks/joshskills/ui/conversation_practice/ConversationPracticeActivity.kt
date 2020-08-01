package com.joshtalks.joshskills.ui.conversation_practice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.ApiCallStatus
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.core.ViewTypeForPractiseUser
import com.joshtalks.joshskills.core.custom_ui.FullScreenProgressDialog
import com.joshtalks.joshskills.databinding.ActivityConversationPracticeBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.ConversationPractiseSubmitEventBus
import com.joshtalks.joshskills.repository.local.eventbus.ViewPagerDisableEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.PractiseViewPagerAdapter
import com.joshtalks.joshskills.ui.conversation_practice.extra.ConversationPracticeIntro
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

const val PRACTISE_ID = "practise_id"
const val IMAGE_URL = "image_url"

class ConversationPracticeActivity : CoreJoshActivity() {

    private val compositeDisposable = CompositeDisposable()
    private val mFragmentNames = arrayOf("Listen", "Quiz", "Practice", "Record")
    lateinit var practiseId: String


    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)
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
            }
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private lateinit var binding: ActivityConversationPracticeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation_practice)
        addObserver()
        intent?.getStringExtra(PRACTISE_ID)?.run {
            practiseId = this
            viewModel.fetchConversationPractice(this)
        }
    }

    private fun initView() {
        TabLayoutMediator(binding.tabLayout, binding.viewPager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position ->
                tab.text = mFragmentNames[position]
            }
        ).attach()

        binding.tabLayout.addOnTabSelectedListener(object :
            TabLayout.OnTabSelectedListener {
            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabSelected(tab: TabLayout.Tab) {
                /* if (binding.viewPager.isUserInputEnabled) {
                     binding.viewPager.currentItem = tab.position
                 }*/
            }

        })
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
                initView()
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
                    viewModel.submitPractise(practiseId)
                }, {
                    it.printStackTrace()
                })
        )


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
}

