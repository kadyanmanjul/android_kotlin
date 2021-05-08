package com.joshtalks.joshskills.ui.online_test

import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.WebRtcMiddlewareActivity
import com.joshtalks.joshskills.core.extension.transaltionAnimationNew
import com.joshtalks.joshskills.databinding.ActivityOnlineTestActivityBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.AnimateAtsOtionViewEvent
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.lesson.grammar_new.CustomWord
import com.joshtalks.joshskills.ui.points_history.viewmodel.PointsViewModel
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class OnlineTestActivity : WebRtcMiddlewareActivity() {
    private val viewModel: PointsViewModel by lazy {
        ViewModelProvider(this).get(PointsViewModel::class.java)
    }
    private lateinit var binding: ActivityOnlineTestActivityBinding
    private val compositeDisposable = CompositeDisposable()
    private var customView: CustomWord? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding =
            DataBindingUtil.setContentView(this, R.layout.activity_online_test_activity)
        binding.lifecycleOwner = this
        binding.handler = this
        binding.startTestContainer.visibility = View.VISIBLE
        binding.testCompletedContainer.visibility = View.GONE
        binding.parentContainer.visibility = View.GONE

    }

    fun startOnlineExamTest() {
        binding.parentContainer.visibility = View.VISIBLE
        supportFragmentManager
            .beginTransaction()
            .replace(
                R.id.parent_Container,
                OnlineTestFragment.getInstance(),
                OnlineTestFragment.TAG
            )
            .commitAllowingStateLoss()
    }

    fun showTestCompletedScreen(messageText : String) {
        binding.startTestContainer.visibility = View.GONE
        binding.parentContainer.visibility = View.GONE
        binding.testCompletedContainer.visibility = View.VISIBLE
        if (messageText.isNullOrBlank().not()){
            binding.text.text = messageText
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeRxBus()
    }

    private fun subscribeRxBus() {
        compositeDisposable.add(
            RxBus2.listenWithoutDelay(AnimateAtsOtionViewEvent::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { event ->
                    if (customView == null) {
                        customView = CustomWord(this, event.customWord.choice)
                    } else {
                        binding.rootView.removeView(customView)
                        customView?.updateChoice(event.customWord.choice)
                        //customView?.choice = event.customWord.choice
                    }
                    customView?.apply {
                        binding.rootView.addView(this)
                        this.text = event.customWord.choice.text
                        this.x = event.fromLocation[0].toFloat()
                        this.y = event.fromLocation[1].toFloat() - event.height.toFloat()
                        val toLocation = IntArray(2)
                        event.customWord.getLocationOnScreen(toLocation)
                        toLocation[1] = toLocation[1] - (event.height) + CustomWord.mPaddingTop
                        this.transaltionAnimationNew(
                            toLocation,
                            event.customWord,
                            event.optionLayout
                        )
                    }
                }
        )
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            this.finish()
            return
        }
        super.onBackPressed()
    }

}
