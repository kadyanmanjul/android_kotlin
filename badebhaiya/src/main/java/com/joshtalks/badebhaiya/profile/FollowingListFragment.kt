package com.joshtalks.badebhaiya.profile

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import androidx.paging.compose.collectAsLazyPagingItems
import com.joshtalks.badebhaiya.feed.FeedActivity
import com.joshtalks.badebhaiya.feed.FeedViewModel
import com.joshtalks.badebhaiya.profile.response.FollowingListScreen
import com.joshtalks.badebhaiya.signup.fragments.PeopleToFollowScreen
import kotlinx.android.synthetic.main.activity_feed.*
import timber.log.Timber
import java.lang.Exception

class FollowingListFragment : Fragment() {

    private val viewModel by lazy {
        ViewModelProvider(requireActivity()).get(ProfileViewModel::class.java)
    }

    private val feedViewModel by lazy {
        ViewModelProvider(requireActivity()).get(FeedViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment

        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                activity?.run {
                        Timber.d("back from profile")
                        supportFragmentManager.popBackStack()
                }
            }
        })
        return ComposeView(
            requireContext()
        ).apply {
            setContent {
                    val peopleList = feedViewModel.followingList.collectAsLazyPagingItems()
                FollowingListScreen(
                        peopleList,
                        onItemClick = {
                          viewModel.openProfile(it.user_id)
                        },
                        onCloseCall = {
                            dismissFragment() }

                    )
            }
        }


    }

    private fun dismissFragment(){
        requireActivity().supportFragmentManager.popBackStack()
    }

    companion object {

        fun open(supportFragmentManager: FragmentManager, @IdRes containerId: Int){

            supportFragmentManager
                .beginTransaction()
                .add(containerId, FollowingListFragment())
                .addToBackStack(null)
                .commit()
        }
    }
}
