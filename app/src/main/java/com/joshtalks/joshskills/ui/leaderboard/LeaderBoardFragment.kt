package com.joshtalks.joshskills.ui.leaderboard

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setImage
import com.joshtalks.joshskills.databinding.FragmentLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class LeaderBoardFragment private constructor() : Fragment() {

    private lateinit var binding: FragmentLeaderboardViewPagerBinding
    private lateinit var type: String
    private var leaderboardResponse: LeaderboardResponse? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var userPosition: Int = Int.MAX_VALUE
    private var userRank: Int = Int.MAX_VALUE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = it.getString(TYPE) ?: EMPTY
            leaderboardResponse = it.getParcelable(RESPONSE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
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
        private const val RESPONSE = "leadberboard_response"

        @JvmStatic
        fun newInstance(type: String, leaderboardResponse: LeaderboardResponse?) =
            LeaderBoardFragment().apply {
                arguments = Bundle().apply {
                    putString(TYPE, type)
                    putParcelable(RESPONSE, leaderboardResponse)
                }
            }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        addObserver()
        //setListener()
        //viewModel.getLeaderBoardData(Mentor.getInstance().getId(), type)
    }

    private fun setListener() {
        val firstposition = linearLayoutManager.findFirstCompletelyVisibleItemPosition()
        val lastposition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

    }

    private fun initRV() {
        linearLayoutManager = SmoothLinearLayoutManager(requireContext())
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.builder.setHasFixedSize(true)
            .setLayoutManager(linearLayoutManager)
    }

    private fun addObserver() {
        leaderboardResponse?.let {
            binding.infoText.text = it.info
            userRank = it.current_mentor?.ranking ?: 0
            it.current_mentor?.let {
                setCurrentUserDetails(it)
            }
            it.top_50_mentor_list?.forEach {
                binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
            }
            if (userRank in 1..47) {
                userPosition = userRank
            } else if (userRank in 48..50) {
                userPosition = userRank
            } else {
                binding.recyclerView.addView(EmptyItemViewHolder())

                it.below_three_mentor_list?.forEach {
                    binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
                }
                it.current_mentor?.let {
                    binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
                }
                it.above_three_mentor_list?.forEach {
                    binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
                }
            }
        }
    }

    private fun setCurrentUserDetails(response: LeaderboardMentor) {
        binding.rank.text = response.ranking.toString()
        binding.name.text = response.name.toString()
        binding.points.text = response.points.toString()
        response.photoUrl?.let {
            binding.userPic.setImage(it)
        }
        binding.userLayout.visibility = View.VISIBLE
    }


    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }


    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(OpenUserProfile::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    it.id?.let { id ->
                        openUserProfileActivity(id)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun openUserProfileActivity(id: String) {
        context?.let {
            UserProfileActivity.startUserProfileActivity(
                requireActivity(),
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }
}
