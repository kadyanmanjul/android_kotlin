package com.joshtalks.joshskills.ui.leaderboard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.BaseActivity
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.databinding.ActivityPreviousLeaderboardBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.DeleteProfilePicEventBus
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.PreviousLeaderboardResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class PreviousLeaderboardActivity : BaseActivity() {

    lateinit var binding: ActivityPreviousLeaderboardBinding
    private var intervalType: String = EMPTY
    private val compositeDisposable = CompositeDisposable()

    private val viewModel by lazy {
        ViewModelProvider(this).get(
            PreviousLeaderBoardViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_previous_leaderboard)
        binding.lifecycleOwner = this
        intervalType = intent.getStringExtra(INTERVAL_TYPE)
        addObserver()
        initRV()
        initToolbar()
        getLeaderBoardData(intervalType)
        setOnClickListeners()
    }

    override fun getConversationId(): String? {
        return intent.getStringExtra(CONVERSATION_ID)
    }

    private fun initRV() {
        val linearLayoutManager = SmoothLinearLayoutManager(this)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
    }

    private fun getLeaderBoardData(intervalType: String) {
        viewModel.getPreviousLeaderboardData(Mentor.getInstance().getId(), intervalType)
    }

    private fun setOnClickListeners() {
    }

    private fun initToolbar() {
        with(binding.ivBack) {
            visibility = View.VISIBLE
            setOnClickListener {
                onBackPressed()
            }
        }
    }

    private fun addObserver() {
        viewModel.previousLeaderBoardData.observe(
            this,
            { response ->
                response?.let {
                    initToolbarTitle(it.title)
                    setData(it)
                }
            }
        )
    }

    private fun initToolbarTitle(title: String?) {
        if (title.isNullOrBlank().not()) {
            binding.textMessageTitle.text = title
        }
    }

    private fun setData(leaderboardResponse: PreviousLeaderboardResponse) {

        val userRank = leaderboardResponse.currentMentor?.ranking ?: 0
        var userPosition = 0

        leaderboardResponse.top50MentorList?.forEachIndexed { index, data ->
            if (index == 0) {
                binding.recyclerView.addView(
                    leaderboardResponse.awardUrl?.let {
                        LeaderBoardPreviousWinnerItemViewHolder(
                            data, this,
                            it
                        )
                    }
                )
            } else binding.recyclerView.addView(LeaderBoardItemViewHolder(data, this))
        }

        if (userRank in 1..47) {
            userPosition = userRank.minus(3)
        } else if (userRank in 48..50) {
            userPosition = userRank.minus(3)
        } else {
            userPosition = 53
        }

        if (leaderboardResponse.belowThreeMentorList.isNullOrEmpty().not() &&
            leaderboardResponse.belowThreeMentorList?.get(0)?.ranking!! > 51
        )
            binding.recyclerView.addView(EmptyItemViewHolder())
        leaderboardResponse.belowThreeMentorList?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, this))
        }
        if (userPosition == 53)
            leaderboardResponse.currentMentor?.let {
                binding.recyclerView.addView(
                    LeaderBoardItemViewHolder(
                        it,
                        this,
                        true
                    )
                )
            }

        leaderboardResponse.aboveThreeMentorList?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, this))
        }

        binding.recyclerView.addView(EmptyItemViewHolder())

        leaderboardResponse.lastMentorList?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, this))
        }
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    private fun subscribeRXBus() {

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(OpenUserProfile::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe(
                    {
                        it.id?.let { id ->
                            openUserProfileActivity(id, intervalType, it.isUserOnline)
                        }
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )

        compositeDisposable.add(
            RxBus2.listenWithoutDelay(DeleteProfilePicEventBus::class.java)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    {
                    /*viewModel.userData.value?.photoUrl = it.url
                    if (it.url.isBlank()) {
                        viewModel.completingProfile("")
                    }*/
                    },
                    {
                        it.printStackTrace()
                    }
                )
        )
    }

    private fun openUserProfileActivity(
        id: String,
        intervalType: String,
        isOnline: Boolean = false
    ) {
        UserProfileActivity.startUserProfileActivity(
            this,
            id,
            arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
            intervalType,
            USER_PROFILE_FLOW_FROM.LEADERBOARD.value,
            isOnline,
            conversationId = intent.getStringExtra(CONVERSATION_ID)
        )
    }

    override fun onStop() {
        compositeDisposable.clear()
        super.onStop()
    }

    companion object {
        const val INTERVAL_TYPE = "interval_type"

        fun startPreviousLeaderboardActivity(
            activity: Activity,
            flags: Array<Int> = arrayOf(),
            intervalType: String? = null,
            conversationId: String? = null
        ) {
            Intent(activity, PreviousLeaderboardActivity::class.java).apply {
                putExtra(CONVERSATION_ID, conversationId)
                intervalType?.let {
                    putExtra(INTERVAL_TYPE, it)
                }
                flags.forEach { flag ->
                    this.addFlags(flag)
                }
            }.run {
                activity.startActivity(this)
            }
        }
    }
}
