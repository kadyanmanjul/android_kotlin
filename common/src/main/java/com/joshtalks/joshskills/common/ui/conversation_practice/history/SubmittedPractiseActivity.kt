package com.joshtalks.joshskills.common.ui.conversation_practice.history

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.common.R
import com.joshtalks.joshskills.common.core.CoreJoshActivity
import com.joshtalks.joshskills.common.core.Utils
import com.joshtalks.joshskills.common.core.analytics.AnalyticsEvent
import com.joshtalks.joshskills.common.core.analytics.AppAnalytics
import com.joshtalks.joshskills.common.core.custom_ui.decorator.LayoutMarginDecoration
import com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player.AudioModel
import com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player.AudioPlayerEventListener
import com.joshtalks.joshskills.common.core.custom_ui.exo_audio_player.ExoAudioPlayerView
import com.joshtalks.joshskills.common.messaging.RxBus2
import com.joshtalks.joshskills.common.repository.local.eventbus.RequestAudioPlayEventBus
import com.joshtalks.joshskills.common.repository.server.conversation_practice.SubmittedConversationPractiseModel
import com.joshtalks.joshskills.common.ui.conversation_practice.ConversationPracticeViewModel
import com.joshtalks.joshskills.common.ui.conversation_practice.PRACTISE_ID
import com.mindorks.placeholderview.PlaceHolderView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import jp.wasabeef.recyclerview.animators.SlideInUpAnimator
import java.util.ArrayList
import java.util.LinkedList

class SubmittedPractiseActivity : CoreJoshActivity(), AudioPlayerEventListener {

    private var cPosition = -1
    private val viewModel: ConversationPracticeViewModel by lazy {
        ViewModelProvider(this).get(ConversationPracticeViewModel::class.java)
    }
    private val compositeDisposable = CompositeDisposable()
    private val list: ArrayList<SubmittedConversationPractiseModel> = arrayListOf()

    private val audioPlayer by lazy {
        findViewById<ExoAudioPlayerView>(R.id.audio_player)
    }
    private val recyclerView by lazy {
        findViewById<PlaceHolderView>(R.id.recycler_view)
    }
    private val ivBack by lazy {
        findViewById<AppCompatImageView>(R.id.iv_back)
    }

    companion object {
        fun startSubmittedPractiseActivity(
            activity: Activity,
            practiseId: String
        ) {
            val intent = Intent(activity, SubmittedPractiseActivity::class.java).apply {
                putExtra(PRACTISE_ID, practiseId)
            }
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_submitted_practise)
        setupUI()
        addObserver()
        intent?.getStringExtra(PRACTISE_ID)?.run {
            viewModel.fetchAllSubmittedConversation(this)

        }
        logConvoRecordSubmittedEvent()
    }


    private fun logConvoRecordSubmittedEvent() {
        AppAnalytics.create(AnalyticsEvent.CON_RECORDING_SUBMITTED.NAME)
            .addBasicParam()
            .addUserDetails()
            .addParam("flow", "record practise")
            .push()
    }

    private fun setupUI() {
        ivBack.setOnClickListener {
            this.finish()
        }
        recyclerView.builder.setHasFixedSize(true)
        recyclerView.itemAnimator = SlideInUpAnimator(OvershootInterpolator(1f))
        recyclerView.addItemDecoration(LayoutMarginDecoration(Utils.dpToPx(this, 8f)))
    }

    private fun addObserver() {
        viewModel.submittedPracticeLiveData.observe(this, Observer {
            if (list.isEmpty()) {
                list.addAll(it.sortedByDescending { obj -> obj.created })
            }
            list.forEachIndexed { index, obj ->
                recyclerView.addView(
                    SubmittedPractiseItemHolder(
                        index,
                        obj
                    )
                )
            }
        })
    }

    private fun subscribeBus() {
        compositeDisposable.add(
            RxBus2.listen(RequestAudioPlayEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    if (cPosition >= 0) {
                        list[cPosition].isPlaying = false
                    }
                    cPosition = it.position

                    list[cPosition].isPlaying = true
                    recyclerView.refresh()
                    playAudioPlayer(it.url, it.duration)
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun playAudioPlayer(url: String, duration: Int) {
        val list: LinkedList<AudioModel> = LinkedList()
        list.add(AudioModel(url, url.hashCode().toString(), duration))
        audioPlayer.addAudios(list)
        audioPlayer.onPlay()
    }

    override fun onResume() {
        super.onResume()
        subscribeBus()
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable.clear()
    }

    override fun onPlayerPause() {
        list[cPosition].isPlaying = false
        recyclerView.refreshView(cPosition)
    }

    override fun onPlayerResume() {
    }

    override fun onCurrentTimeUpdated(lastPosition: Long) {
    }

    override fun onTrackChange(tag: String?) {
    }

    override fun onPositionDiscontinuity(lastPos: Long, reason: Int) {
    }

    override fun onPositionDiscontinuity(reason: Int) {
    }

    override fun onPlayerReleased() {
    }

    override fun onPlayerEmptyTrack() {
    }

    override fun complete() {
    }
}
