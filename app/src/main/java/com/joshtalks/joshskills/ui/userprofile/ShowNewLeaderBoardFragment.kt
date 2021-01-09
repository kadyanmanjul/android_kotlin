package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
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
import com.joshtalks.joshskills.databinding.FragmentShowNewLeaderboardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.AnimatedLeaderBoardResponse
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardItemViewHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowNewLeaderBoardFragment : DialogFragment() {

    private lateinit var binding: FragmentShowNewLeaderboardBinding
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var linearSmoothScroller: LinearSmoothScroller
    private var award: Award? = null
    private val viewModel by lazy { ViewModelProvider(requireActivity()).get(UserProfileViewModel::class.java) }
    private val targetRank: Int = 0
    private var previousRank: Int = 0
    private var position: Int = -1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_FRAME, R.style.full_dialog)
        changeDialogConfiguration()

        arguments?.let {
            award = it.getParcelable(LEADERBOARD_DETAILS)
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
        it.currentMentor?.let { current_mentor ->
            previousRank = current_mentor.ranking
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
        it.leaderBoardMentorList?.forEach {
            binding.recyclerView.addView(
                LeaderBoardItemViewHolder(
                    it,
                    requireContext(),
                    false,
                    false
                )
            )
        }
        binding.rank.text = it.currentMentor?.ranking.toString()
        it.currentMentor?.photoUrl?.let { it1 -> binding.userPic.setImage(it1) }
        binding.name.text = it.currentMentor?.name
        binding.points.text = it.currentMentor?.points.toString()
        binding.recyclerView.isNestedScrollingEnabled = false

        val last_item_position = binding.recyclerView.getAdapter()?.getItemCount()?.minus(1)
        last_item_position?.let { it1 ->
            position = last_item_position.minus(6)
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                Log.d(
                    "Manjul",
                    "initView() called ${linearLayoutManager.findFirstCompletelyVisibleItemPosition()}"
                )
                if (linearLayoutManager.findFirstCompletelyVisibleItemPosition() > targetRank) {
                    delay(1000)
                    animateCard()
                } else animateToPosition()
            }
        }
    }

    fun animateToPosition() {
        binding.recyclerView.removeOnScrollListener(onScrollListener)
        binding.recyclerView.addOnScrollListener(onScrollListener)
        linearSmoothScroller.targetPosition = position
        linearLayoutManager.startSmoothScroll(linearSmoothScroller)
    }

    var onScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            when (newState) {
                SCROLL_STATE_IDLE -> {
                    recyclerView.removeOnScrollListener(this)
                    if (position == targetRank) {
                        animateCard()
                    } else {
                        binding.rank.text = position.plus(2).toString()
                        position = position.minus(1)
                        animateToPosition()
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
            override fun onAnimationStart(p0: Animation?) {}

            override fun onAnimationEnd(p0: Animation?) {
                (binding.recyclerView.getViewResolverAtPosition(0) as LeaderBoardItemViewHolder).showCurrentUserItem()
                binding.userItem.visibility = View.GONE
                binding.recyclerView.removeView(
                    binding.recyclerView.getAdapter()?.getItemCount()?.minus(3)
                )
            }

            override fun onAnimationRepeat(p0: Animation?) {}

        })
        binding.userItem.startAnimation(animation)
    }


    companion object {
        const val LEADERBOARD_DETAILS = "leader_board_details"
        const val TAG = "ShowNewLeaderBoardFragment"

        @JvmStatic
        fun newInstance(award: Award?) =
            ShowNewLeaderBoardFragment()
                .apply {
                    award?.let {
                        arguments = Bundle().apply {
                            putParcelable(LEADERBOARD_DETAILS, award)
                        }
                    }
                }

        fun showDialog(
            supportFragmentManager: FragmentManager,
            award: Award?
        ) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            val prev = supportFragmentManager.findFragmentByTag(TAG)
            if (prev != null) {
                fragmentTransaction.remove(prev)
            }
            fragmentTransaction.addToBackStack(null)
            newInstance(award)
                .show(supportFragmentManager, TAG)
        }

    }

}
