package com.joshtalks.joshskills.ui.userprofile

import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.AppObjectController
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FragmentShowNewLeaderboardBinding
import com.joshtalks.joshskills.repository.local.model.Mentor
import com.joshtalks.joshskills.repository.server.Award
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.ui.leaderboard.LeaderBoardItemViewHolder
import com.joshtalks.joshskills.util.showAppropriateMsg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ShowNewLeaderBoardFragment: Fragment()  {

    private lateinit var binding: FragmentShowNewLeaderboardBinding
    private var award: Award? = null
    val leaderBoardData: MutableLiveData<LeaderboardResponse> = MutableLiveData()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            award = it.getParcelable(LEADERBOARD_DETAILS)
        }
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
        leaderBoardData.observe(viewLifecycleOwner, {
            it?.let {
                initView(it)
            }
        })
        CoroutineScope(Dispatchers.IO).launch {
            leaderBoardData.postValue(getMentorData(Mentor.getInstance().getId(), "TODAY"))
        }

    }

    private fun initView(it: LeaderboardResponse) {

        val linearLayoutManager = LinearLayoutManager(activity)
        linearLayoutManager.isSmoothScrollbarEnabled = true


        val linearSmoothScroller: LinearSmoothScroller =
            object : LinearSmoothScroller(requireContext()) {
                override fun calculateSpeedPerPixel(displayMetrics: DisplayMetrics): Float {
                    return 500.div(displayMetrics.densityDpi).toFloat()
                }
            }

        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)


        it.lastWinner?.award_url?.let { it1 -> binding.image.setImage(it1) }
        binding.titleTv.text=it.info
        binding.rankTv.text="Current rank : ${it.current_mentor?.ranking}"
        it.top_50_mentor_list?.forEach {
            binding.recyclerView.addView(LeaderBoardItemViewHolder(it,requireContext(),false,false))
        }
        binding.rank.text=it.current_mentor?.ranking.toString()
        it.current_mentor?.photoUrl?.let { it1 -> binding.userPic.setImage(it1) }
        binding.name.text=it.current_mentor?.name
        binding.points.text=it.current_mentor?.points.toString()
        binding.recyclerView.isNestedScrollingEnabled=false

        val last_item_position=binding.recyclerView.getAdapter()?.getItemCount()?.minus(1)
        last_item_position?.let { it1 ->
            CoroutineScope(Dispatchers.Main).launch {
                //scrollToPosition(0, it1)

                linearSmoothScroller.targetPosition = last_item_position //the last position of the item in the list
                linearLayoutManager.startSmoothScroll(linearSmoothScroller)
            }
        }
    }

    private suspend fun scrollToPosition(i: Int, lastItemPosition: Int) {
        var lastItem=lastItemPosition

        binding.recyclerView.smoothScrollToPosition(i)
        while (false){
            binding.recyclerView.smoothScrollToPosition(lastItem.minus(1))
            lastItem=lastItem.minus(1)
            delay(100)
        }
    }

    suspend fun getMentorData(mentorId: String, type: String): LeaderboardResponse? {
        try {
            val response =
                AppObjectController.commonNetworkService.getLeaderBoardData(mentorId, type)
            if (response.isSuccessful && response.body() != null) {
                return response.body()!!
            }

        } catch (ex: Throwable) {
            ex.showAppropriateMsg()
        }
        return null
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
                        }                    }
                }
    }

}
