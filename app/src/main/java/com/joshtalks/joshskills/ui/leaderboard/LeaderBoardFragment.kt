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
import androidx.recyclerview.widget.RecyclerView
import com.joshtalks.joshskills.R
import com.joshtalks.joshskills.core.EMPTY
import com.joshtalks.joshskills.core.setUserImageOrInitials
import com.joshtalks.joshskills.databinding.FragmentLeaderboardViewPagerBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardResponse
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.util.Locale

class LeaderBoardFragment : Fragment() {

    private lateinit var binding: FragmentLeaderboardViewPagerBinding
    private lateinit var type: String
    private var leaderboardResponse: LeaderboardResponse? = null
    private var compositeDisposable = CompositeDisposable()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private var userPosition: Int = 0
    private var userRank: Int = Int.MAX_VALUE
    private val viewModel by lazy { ViewModelProvider(this).get(LeaderBoardViewModel::class.java) }


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
        setListener()
        //viewModel.getLeaderBoardData(Mentor.getInstance().getId(), type)
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
        /*binding.recyclerView.addOnScrollListener(object :
            EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                if (page > 0 && page < leaderboardResponse?.totalpage ?: 0)
                    viewModel.getMentorDataViaPage(Mentor.getInstance().getId(), type, page)
            }
        })*/

        binding.recyclerView.addOnScrollListener(object :
            RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

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
        leaderboardResponse?.let {
            //binding.infoText.text = it.info
            userRank = it.current_mentor?.ranking ?: 0
            it.current_mentor?.let {
                setCurrentUserDetails(it)
            }
            it.lastWinner?.let {
                binding.recyclerView.addView(LeaderBoardWinnerItemViewHolder(it, requireContext()))
            }
            binding.recyclerView.addView(
                LeaderBoardItemViewHolder(
                    LeaderboardMentor(
                        null, null, null, null, null, 0, 0
                    ), requireContext(), isHeader = true
                )
            )
            it.top_50_mentor_list?.forEach {
                binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
            }

            if (userRank in 1..47) {
                userPosition = userRank.minus(3)
            } else if (userRank in 48..50) {
                userPosition = userRank.minus(3)
            } else {
                userPosition = 53
            }

            if (it.below_three_mentor_list.isNullOrEmpty().not() &&
                it.below_three_mentor_list?.get(0)?.ranking!! > 51
            )
                binding.recyclerView.addView(EmptyItemViewHolder())
            it.below_three_mentor_list?.forEach {
                binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
            }
            if (userPosition == 53)
                it.current_mentor?.let {
                    binding.recyclerView.addView(
                        LeaderBoardItemViewHolder(
                            it,
                            requireContext(),
                            true
                        )
                    )
                }

            it.above_three_mentor_list?.forEach {
                binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
            }
            var lastPosition = it.current_mentor?.ranking ?: 0
            if (it.above_three_mentor_list?.isNullOrEmpty()!!.not()) {
                lastPosition =
                    it.above_three_mentor_list.get(it.above_three_mentor_list.size.minus(1)).ranking
            }

            binding.recyclerView.addView(EmptyItemViewHolder())

            it.last_mentor_list?.forEach {
                binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
            }
        }

        viewModel.leaderBoardDataOfPage.observe(this.viewLifecycleOwner, Observer { data ->
            data?.let {
                it.above_three_mentor_list?.forEach {
                    binding.recyclerView.addView(LeaderBoardItemViewHolder(it, requireContext()))
                }
            }

        })
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
        binding.userPic.setUserImageOrInitials(response.photoUrl, response.name!!)
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
