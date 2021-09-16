package com.joshtalks.joshskills.ui.leaderboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.*
import com.joshtalks.joshskills.core.analytics.LogException
import com.joshtalks.joshskills.core.videotranscoder.enforceSingleScrollDirection
import com.joshtalks.joshskills.databinding.FragmentLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.track.CONVERSATION_ID
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity.Companion.tooltipTextList
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardViewPagerActivity.Companion.winnerMap
import com.joshtalks.joshskills.ui.leaderboard.constants.NEED_VIEW_BITMAP
import com.joshtalks.joshskills.ui.leaderboard.constants.PROFILE_ITEM_CLICKED
import com.joshtalks.joshskills.ui.leaderboard.constants.SCROLL_TO_TOP
import com.joshtalks.joshskills.ui.tooltip.TooltipUtils.Companion.getOverlayItemFromView
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import com.skydoves.balloon.*
import com.skydoves.balloon.overlay.BalloonOverlayAnimation
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LeaderBoardFragment : Fragment(), ViewInflated {
    private val TAG = "LeaderBoardFragment"
    private lateinit var binding: FragmentLeaderboardViewPagerBinding
    private lateinit var type: String
    private lateinit var courseId: String
    private var compositeDisposable = CompositeDisposable()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var userPosition: Int = 0
    private var userRank: Int = Int.MAX_VALUE
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(LeaderBoardViewModel::class.java) }
    private var liveUserPosition = -1
    private var listener : ViewBitmap? = null
    private var winnerAnimationListener : WinnerAnimation? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if(context is ViewBitmap)
            listener = context
        //if(context is WinnerAnimation)
        //    winnerAnimationListener = context
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate: $this")
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString(TYPE) ?: EMPTY
            courseId = it.getString(COURSE_ID) ?: EMPTY
        }
            viewModel.eventLiveData.observe(this) {
                Log.d(TAG, "onCreate: $this --- $it")
                it?.let {
                    Log.d(TAG, "onCreate: eventLiveData -->  $it --- $type")
                    when(it.eventType) {
                        NEED_VIEW_BITMAP -> {
                            if(it.type == type) {
                                getBitmap()
                                viewModel.eventLiveData.postValue(null)
                            }
                        }
                        PROFILE_ITEM_CLICKED -> {
                            if(it.type == type) {
                                val view = binding.recyclerView.getChildAt(3)
                                view.performClick()
                                viewModel.eventLiveData.postValue(null)
                            }
                        }
                        SCROLL_TO_TOP -> {
                            if(it.type == type) {
                                binding.recyclerView.scrollToPosition(0)
                            }
                        }
                    }
                }
            }
    }

    fun scrollToTop() {
        try {
            scrollJob?.cancel()
        } catch (e : Exception) {
            e.printStackTrace()
        }
        scrollJob = CoroutineScope(Dispatchers.Main).launch {
            binding.recyclerView.scrollToPosition(0)
            //delay(300)
            //val view = binding.recyclerView.getChildAt(3)
            //Log.d(TAG, "scrollToTop : $view --- $type")
            //winnerAnimationListener?.showWinnerAnimation()
        }
    }

    fun getBitmap() {
        try {
            bitmapJob?.cancel()
        } catch (e : Exception) {
            e.printStackTrace()
        }
        bitmapJob = CoroutineScope(Dispatchers.Main).launch {
            //binding.recyclerView.scrollToPosition(0)
            binding.recyclerView.stopScroll()
            //binding.recyclerView.scrollToPosition(0)
            delay(200)
            val view = binding.recyclerView.getChildAt(3)
            Log.d(TAG, "needBitmapLiveData : $view --- $type")
            listener?.onViewBitmap(getOverlayItemFromView(view), type)
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_leaderboard_view_pager,
                container,
                false
            )
        binding.lifecycleOwner = this
        return binding.root
    }

    companion object {
        private const val TYPE = "leadberboard_type"
        private var bitmapJob : Job? = null
        private var scrollJob : Job? = null

        @JvmStatic
        fun newInstance(type: String, courseId: String?) =
            LeaderBoardFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE, type)
                    putString(COURSE_ID, courseId)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        addObserver()
        setListener()
        // viewModel.getLeaderBoardData(Mentor.getInstance().getId(), type)
    }

    private fun setListener() {
        binding.userLayout.setOnClickListener {
            scrollToUserPosition()
            binding.userLayout.visibility = View.GONE
        }
    }

    private fun scrollToUserPosition() {
        if (userPosition > 0)
            linearLayoutManager.scrollToPositionWithOffset(userPosition, 0)
        else linearLayoutManager.scrollToPositionWithOffset(0, 0)
    }

    private fun initRV() {
        linearLayoutManager = SmoothLinearLayoutManager(requireContext())
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
        binding.recyclerView.enforceSingleScrollDirection()
        /*binding.recyclerView.addOnScrollListener(object :
            EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (page > 0 && page < leaderboardResponse?.totalpage ?: 0)
                    viewModel.getMentorDataViaPage(Mentor.getInstance().getId(), type, page)
            }
        })*/

        binding.recyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() < userPosition.plus(
                        4
                    ) && linearLayoutManager.findLastCompletelyVisibleItemPosition() > userPosition.plus(
                        2
                    )
                ) {
                    binding.userLayout.visibility = View.GONE
                } else {
                    binding.userLayout.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun addObserver() {
        when (type) {
            "TODAY" -> {
                viewModel.leaderBoardDataOfToday.observe(
                    viewLifecycleOwner,
                    Observer {
                        setData(it)
                    }
                )
            }
            "WEEK" -> {
                viewModel.leaderBoardDataOfWeek.observe(
                    viewLifecycleOwner,
                    Observer {
                        setData(it)
                    }
                )
            }
            "MONTH" -> {
                viewModel.leaderBoardDataOfMonth.observe(
                    viewLifecycleOwner,
                    Observer {
                        setData(it)
                    }
                )
            }
            "BATCH" -> {
                viewModel.leaderBoardDataOfBatch.observe(
                    viewLifecycleOwner,
                    Observer {
                        setData(it)
                    }
                )
            }
            "LIFETIME" -> {
                viewModel.leaderBoardDataOfLifeTime.observe(
                    viewLifecycleOwner,
                    Observer {
                        setData(it)
                    }
                )
            }
        }

        viewModel.leaderBoardDataOfPage.observe(
            viewLifecycleOwner,
            { data ->
                data?.let {
                    it.above_three_mentor_list?.forEach {
                        binding.recyclerView.addView(
                            LeaderBoardItemViewHolder(
                                it,
                                requireContext()
                            )
                        )
                    }
                }
            }
        )
    }

    private fun setData(leaderboardResponse1: LeaderboardResponse) {
        var additionalIndexCount = 0
        binding.recyclerView.removeAllViews()
        if (leaderboardResponse1.info.isNullOrBlank().not()) {
            // TODO handel this count as well in other places where using position of recycler view eg. in tooltip
            additionalIndexCount = additionalIndexCount.plus(1)
            tooltipTextList[4] = leaderboardResponse1.info ?: ""
            binding.recyclerView.addView(
                LeaderboardInfoItemViewHolder(
                    leaderboardResponse1.info!!,
                    requireContext(),
                    type
                )
            )
        }
        userRank = leaderboardResponse1.current_mentor?.ranking ?: 0
        leaderboardResponse1.current_mentor?.let {
            setCurrentUserDetails(it)
        }

        leaderboardResponse1.lastWinner?.let {
            if (it.id.isNullOrEmpty().not()) {
                Log.d(TAG, "setData: lastWinner")
                binding.recyclerView.addView(
                    LeaderBoardWinnerItemViewHolder(
                        it,
                        requireContext(),
                        type,
                        onViewInflated = this
                    )
                )
                winnerMap[type] = it
                //viewModel.overlayLiveData.postValue(it)
            }
        }
        binding.recyclerView.addView(
            LeaderBoardItemViewHolder(
                LeaderboardMentor(
                    null, null, null, null, null, 0, 0
                ),
                requireContext(), isHeader = true
            )
        )

        if (type == "TODAY") {
            liveUserPosition =
                leaderboardResponse1.top_50_mentor_list?.indexOfFirst { it.isOnline } ?: 0
            if (liveUserPosition < 0 || liveUserPosition >= 3) {
                liveUserPosition = 2
                leaderboardResponse1.top_50_mentor_list?.listIterator(liveUserPosition)
                    ?.next()?.isOnline = true
                liveUserPosition = liveUserPosition.plus(2)
            } else {
                liveUserPosition = liveUserPosition.plus(2)
            }
        }

        leaderboardResponse1.top_50_mentor_list?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
        }

        if (userRank in 1..47) {
            userPosition = userRank.minus(3)
        } else if (userRank in 48..50) {
            userPosition = userRank.minus(3)
        } else {
            userPosition = 53
        }

        if (leaderboardResponse1.below_three_mentor_list.isNullOrEmpty().not() &&
            leaderboardResponse1.below_three_mentor_list?.get(0)?.ranking!! > 51
        )
            binding.recyclerView.addView(EmptyItemViewHolder())
        leaderboardResponse1.below_three_mentor_list?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
        }
        if (userPosition == 53)
            leaderboardResponse1.current_mentor?.let {
                binding.recyclerView.addView(
                    LeaderBoardItemViewHolder(
                        it,
                        requireContext(),
                        true
                    )
                )
            }

        leaderboardResponse1.above_three_mentor_list?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
        }

        binding.recyclerView.addView(EmptyItemViewHolder())

        leaderboardResponse1.last_mentor_list?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
        }
        AppObjectController.uiHandler.postDelayed({
            addOnlineTooltip()
        }, 500)
    }

    private fun setCurrentUserDetails(response: LeaderboardMentor) {
        binding.rank.text = response.ranking.toString()
        val resp = StringBuilder()
        response.name?.split(" ")?.forEach {
            resp.append(it.toLowerCase(Locale.getDefault()).capitalize(Locale.getDefault()))
                .append(" ")
        }
        binding.name.text = resp
        binding.points.text = response.points.toString()
        binding.userPic.setUserImageOrInitials(
            response.photoUrl,
            response.name ?: getRandomName(),
            isRound = true
        )
        binding.userLayout.visibility = View.VISIBLE
        binding.onlineStatusIv.visibility = if (response.isOnline) {
            View.VISIBLE
        } else {
            View.GONE
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
                            openUserProfileActivity(id, type, it.isUserOnline)
                        }
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
        context?.let {
            UserProfileActivity.startUserProfileActivity(
                requireActivity(),
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                intervalType,
                USER_PROFILE_FLOW_FROM.LEADERBOARD.value,
                conversationId = requireActivity().intent.getStringExtra(CONVERSATION_ID)
            )
        }
    }


    private fun addOnlineTooltip() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                if (type == "TODAY") {
                    val flag = PrefManager.getBoolValue(ONLINE_HINT_SHOW)
                    if (flag) {
                        showAnotherTooltip()
                        return@launch
                    }
                    if ((requireActivity() is LeaderBoardViewPagerActivity) && (requireActivity() as LeaderBoardViewPagerActivity).isTooltipShow) {
                        showAnotherTooltip()
                        return@launch
                    }
                    val lbOpenCount = PrefManager.getIntValue(LEADER_BOARD_OPEN_COUNT)
                    val b = viewModel.isUserHad4And5Lesson()
                    if (lbOpenCount >= 3 || b) {
                        //    delay(250)
                        val item =
                            binding.recyclerView.getViewResolverAtPosition(liveUserPosition) as LeaderBoardItemViewHolder
                        val balloon = Balloon.Builder(requireContext())
                            .setText(getString(R.string.online_tooltip))
                            .setTextSize(15F)
                            .setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                            .setDismissWhenTouchOutside(true)
                            .setCornerRadius(12f)
                            .setWidth(BalloonSizeSpec.WRAP)
                            .setArrowOrientation(ArrowOrientation.BOTTOM)
                            .setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
                            .setPadding(8)
                            .setMarginTop(8)
                            .setIsVisibleOverlay(true) // sets the visibility of the overlay for highlighting an anchor.
                            .setOverlayColorResource(R.color.pd_transparent_bg_v2) // background color of the overlay using a color resource.
                            //  .setOverlayPadding(2f) // sets a padding value of the overlay shape in
                            .setBalloonOverlayAnimation(BalloonOverlayAnimation.FADE) // default is fade.
                            .setDismissWhenOverlayClicked(false) // disable di
                            .setBackgroundColorResource(R.color.white)
                            .setBalloonAnimation(BalloonAnimation.FADE)
                            .setLifecycleOwner(this@LeaderBoardFragment)
                            .setDismissWhenClicked(true)
                            .build()
                        balloon.showAlignBottom(item.onlineStatusLayout)
                        PrefManager.put(ONLINE_HINT_SHOW, true)
                    } else {
                        showAnotherTooltip()
                    }
                }
            } catch (ex: Exception) {
                LogException.catchException(ex)
            }
        }
    }

    private fun showAnotherTooltip() {
        (requireActivity() as LeaderBoardViewPagerActivity).addSearchTooltip()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    override fun onViewInflated(response: LeaderboardMentor) {
        viewModel.overlayLiveData.postValue(response)
    }
}

interface ViewBitmap {
    fun onViewBitmap(overlayItem : ItemOverlay, type : String)
}

interface WinnerAnimation {
    fun showWinnerAnimation()
}