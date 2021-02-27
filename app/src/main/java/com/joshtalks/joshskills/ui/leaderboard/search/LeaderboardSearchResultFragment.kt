package com.joshtalks.joshskills.ui.leaderboard.search

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
import com.joshtalks.joshskills.core.USER_PROFILE_FLOW_FROM
import com.joshtalks.joshskills.databinding.FragmentLeaderboardSearchBinding
import com.joshtalks.joshskills.messaging.RxBus2
import com.joshtalks.joshskills.repository.local.eventbus.OpenUserProfile
import com.joshtalks.joshskills.repository.server.LeaderboardMentor
import com.joshtalks.joshskills.repository.server.LeaderboardType
import com.joshtalks.joshskills.ui.leaderboard.EndlessRecyclerViewScrollListener
import com.joshtalks.joshskills.ui.userprofile.UserProfileActivity
import com.mindorks.placeholderview.SmoothLinearLayoutManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class LeaderboardSearchResultFragment : Fragment() {

    private lateinit var binding: FragmentLeaderboardSearchBinding
    private lateinit var type: LeaderboardType
    private var compositeDisposable = CompositeDisposable()
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: LeaderboardSearchItemAdapter
    lateinit var itemList: MutableList<LeaderboardMentor>
    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(
            LeaderBoardSearchViewModel::class.java
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            type = (it.getSerializable(TYPE) ?: LeaderboardType.TODAY) as LeaderboardType
        }
        itemList = ArrayList()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_leaderboard_search,
                container,
                false
            )
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRV()
        addObserver()
    }

    private fun initRV() {
        linearLayoutManager = SmoothLinearLayoutManager(requireContext())
        linearLayoutManager.isSmoothScrollbarEnabled = true
        binding.recyclerView.layoutManager = linearLayoutManager
        adapter = LeaderboardSearchItemAdapter(requireContext(), itemList)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.addOnScrollListener(object :
            EndlessRecyclerViewScrollListener(linearLayoutManager) {
            override fun onLoadMore(page: Int, totalItemsCount: Int, view: RecyclerView?) {
                viewModel.getMoreResults(type, page)
            }

        })
    }

    private fun addObserver() {
        viewModel.searchedKeyLiveData.observe(viewLifecycleOwner, {
            itemList.clear()
            adapter.notifyDataSetChanged()
        })

        when (type) {
            LeaderboardType.TODAY -> {
                viewModel.leaderBoardDataOfToday.observe(viewLifecycleOwner, Observer {
                    setData(it)
                })
            }
            LeaderboardType.WEEK -> {
                viewModel.leaderBoardDataOfWeek.observe(viewLifecycleOwner, Observer {
                    setData(it)
                })
            }
            LeaderboardType.MONTH -> {
                viewModel.leaderBoardDataOfMonth.observe(viewLifecycleOwner, Observer {
                    setData(it)
                })
            }
        }
    }

    private fun setData(leaderboardResponse1: List<LeaderboardMentor>) {
        itemList.addAll(leaderboardResponse1)
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        subscribeRXBus()
    }

    override fun onStop() {
        super.onStop()
        compositeDisposable.clear()
    }

    private fun subscribeRXBus() {
        compositeDisposable.add(
            RxBus2.listen(OpenUserProfile::class.java)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    it.id?.let { id ->
                        openUserProfileActivity(id, type.name)
                    }
                }, {
                    it.printStackTrace()
                })
        )
    }

    private fun openUserProfileActivity(id: String, intervalType: String) {
        itemList.first { it.id == id }.name?.let {
            viewModel.insertRecentSearch(it)
        }
        context?.let {
            UserProfileActivity.startUserProfileActivity(
                requireActivity(),
                id,
                arrayOf(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT),
                intervalType,
                USER_PROFILE_FLOW_FROM.LEADERBOARD.value
            )
        }
    }

    companion object {
        private const val TYPE = "leadberboard_type"

        @JvmStatic
        fun newInstance(type: LeaderboardType) =
            LeaderboardSearchResultFragment().apply {
                arguments = Bundle().apply {
                    putSerializable(TYPE, type)
                }
            }
    }
}