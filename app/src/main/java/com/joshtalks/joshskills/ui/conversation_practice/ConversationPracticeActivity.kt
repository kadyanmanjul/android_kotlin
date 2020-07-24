package com.joshtalks.joshskills.ui.conversation_practice

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.tabs.TabLayoutMediator
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.CoreJoshActivity
import com.joshtalks.joshskills.databinding.ActivityConversationPracticeBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenClickProgressEventBus
import com.joshtalks.joshskills.repository.server.conversation_practice.ConversationPractiseModel
import com.joshtalks.joshskills.ui.conversation_practice.adapter.PractiseViewPagerAdapter
import com.joshtalks.joshskills.ui.conversation_practice.extra.ConversationPracticeIntro
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class ConversationPracticeActivity : CoreJoshActivity() {

    private val compositeDisposable = CompositeDisposable()

    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)
    }

    companion object {
        fun startConversationPracticeActivity(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, ConversationPracticeActivity::class.java)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    private lateinit var binding: ActivityConversationPracticeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_conversation_practice)
        addObserver()
    }

    private fun addObserver() {
        viewModel.apiCallStatusLiveData.observe(this, Observer {

        })

    }

    private fun initView() {
        /*val assessmentResponse = AppObjectController.gsonMapperForLocal.fromJson(
            loadJSONFromAsset("assessmentJson.json"),
            AssessmentResponse::class.java
        )
*/
        var pq: ConversationPractiseModel? = null
        // binding.questionViewPager.offscreenPageLimit=1
        binding.viewPager.adapter =
            PractiseViewPagerAdapter(
                this, pq!!
            )
        TabLayoutMediator(binding.tabLayout, binding.viewPager,
            TabLayoutMediator.TabConfigurationStrategy { tab, position -> }
        ).attach()
    }

    private fun openIntroScreen() {
        ConversationPracticeIntro.newInstance()
            .show(supportFragmentManager, "Conversation Practice Intro")
    }


    private fun subscribeBus() {
        compositeDisposable.add(
            RxBus2.listen(OpenClickProgressEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({

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
        compositeDisposable.clear()
    }
}

