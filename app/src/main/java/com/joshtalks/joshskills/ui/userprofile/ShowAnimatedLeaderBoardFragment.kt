package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentShowNewLeaderboardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AnimatedLeaderBoardResponse
import com.joshtalks.joshskills.repository.server.OutrankedDataResponse
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardItemViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowAnimatedLeaderBoardFragment : DialogFragment() {

    private lateinit var binding: FragmentShowNewLeaderboardBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var linearSmoothScroller: LinearSmoothScroller
    private var outrankData: OutrankedDataResponse? = null
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(UserProfileViewModel::class.java) }
    private var previousRank: Int = 0
    private var position: Int = 0
    private var startRank: Int = 0

    private var startIndexRank: Int = 0
    private var newCardIndex: Int = 0
    private var oldRankIndex: Int = -1
    private var currentRankIndex: Int = 0
    private var currentRank: Int = 0
    private var animatePosition = false
    private var isNewCardAdded = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        changeDialogConfiguration()
        arguments?.let {
            outrankData = it.getParcelable(RANK_DETAILS)
        }
        if (outrankData == null) {
            //dismiss()
        }
        viewModel.getMentorData(Mentor.getInstance().getId())
    }


    override fun onStart() {
        super.onStart()
        dialog?.run {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.MATCH_PARENT
            window?.setLayout(width, height)
        }
    }

    private fun changeDialogConfiguration() {
        val params: WindowManager.LayoutParams? = dialog?.window?.attributes
        params?.width = WindowManager.LayoutParams.MATCH_PARENT
        params?.height = WindowManager.LayoutParams.MATCH_PARENT
        params?.gravity = Gravity.CENTER
        dialog?.window?.attributes = params
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_show_new_leaderboard,
                container,
                false
            )
        binding.lifecycleOwner = this
        binding.fragment = this
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.animatedLeaderBoardData.observe(viewLifecycleOwner, {
            it?.let {
                initView(it)
            }
        })

    }

    private fun initView(it: AnimatedLeaderBoardResponse) {

        linearLayoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        linearLayoutManager.isSmoothScrollbarEnabled = true
        linearLayoutManager.stackFromEnd = true

        linearSmoothScroller =
            object : LinearSmoothScroller(requireContext()) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return 500.div(displayMetrics.densityDpi).toFloat()
                }
            }

        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)


        it.awardUrl?.let { it1 -> binding.image.setImage(it1) }
        binding.titleTv.text = it.title
        binding.rankTv.text = "Current rank : ${it.currentMentor?.ranking}"
        previousRank = outrankData?.old?.rank!!


        binding.rank.text = outrankData?.old?.rank.toString()
        binding.userPic.post {
            binding.userPic.setUserImageOrInitials(it.currentMentor?.photoUrl, it.currentMentor?.name!!)
        }
        binding.name.text = it.currentMentor?.name
        binding.points.text = it.currentMentor?.points.toString()
        binding.recyclerView.isNestedScrollingEnabled = false

        it.currentMentor?.let { current_mentor ->
            currentRank = it.currentMentor.ranking
            binding.recyclerView.addView(
                LeaderBoardItemViewHolder(
                    current_mentor,
                    requireContext(),
                    true,
                    false,
                    true
                )
            )
        }

        it.leaderBoardMentorList?.forEachIndexed { index, item ->
            if (item.ranking == outrankData?.old?.rank) {
                oldRankIndex = index.plus(1)
                startIndexRank = item.ranking
                newCardIndex = index.plus(1)
                position = index.plus(4)
            }
            binding.recyclerView.addView(
                LeaderBoardItemViewHolder(
                    item,
                    requireContext(),
                    false,
                    false
                )
            )
        }


        if (oldRankIndex == -1) {
            position = binding.recyclerView.adapter?.itemCount?.minus(1)!!
            animatePosition = true
        } else if (oldRankIndex == 1) {
            position = position
        } else if (oldRankIndex <= 3) {
            position = position.plus(1)
            animatePosition = true
        } else {
            position = oldRankIndex.plus(2)
            animatePosition = true
        }
        binding.recyclerView.scrollToPosition(position)


        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            if (animatePosition) {
                position = position.minus(4)
                animateToPosition()
            } else {
                delay(1000)
                animateCard()
            }
        }
    }

    fun animateToPosition() {
        binding.recyclerView.removeOnScrollListener(onScrollListener)
        binding.recyclerView.addOnScrollListener(onScrollListener)
        linearSmoothScroller.targetPosition = 0
        position = 0
        linearLayoutManager.startSmoothScroll(linearSmoothScroller)
    }

    var onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            val scrolledPosition2 =
                (recyclerView.layoutManager as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                    ?: return
            binding.rank.text =
                outrankData?.old?.rank?.minus(oldRankIndex.minus(scrolledPosition2.plus(2)))
                    .toString()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                SCROLL_STATE_IDLE -> {
                    recyclerView.removeOnScrollListener(this)
                    if (position == currentRankIndex) {
                        animateCard()
                    }
                }
            }
        }
    }

    private fun animateCard() {
        val animation = TranslateAnimation(
            0f, (binding.recyclerView.left.toFloat().minus(binding.userItem.left.toFloat())),
            0f, (binding.recyclerView.top.toFloat().minus(binding.userItem.top.toFloat()))
        )
        animation.setDuration(1000)
        animation.setFillAfter(false)
        animation.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(p0: Animation?) {
                binding.rank.text = currentRank.plus(1).toString()
            }

            override fun onAnimationEnd(p0: Animation?) {
                if (animatePosition)
                    (binding.recyclerView.getViewResolverAtPosition(0) as LeaderBoardItemViewHolder).showCurrentUserItem()
                binding.userItem.visibility = View.GONE
                if (isNewCardAdded) {
                    binding.recyclerView.removeView(newCardIndex)
                }
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })
        binding.userItem.startAnimation(animation)
    }


    companion object {
        const val RANK_DETAILS = "rank_details"
        const val TAG = "ShowNewLeaderBoardFragment"

        @JvmStatic
        fun newInstance(outrankedDataResponse: OutrankedDataResponse) =
            ShowAnimatedLeaderBoardFragment()
                .apply {
                    arguments = Bundle().apply {
                        putParcelable(RANK_DETAILS, outrankedDataResponse)
                    }
                }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            outrankedDataResponse: OutrankedDataResponse
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(outrankedDataResponse)
                .show(supportFragmentManager, TAG)
        }

    }

}
